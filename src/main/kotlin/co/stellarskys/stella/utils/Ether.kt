package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import kotlinx.coroutines.*
import kotlinx.io.IOException
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.*
import java.util.zip.*

@Module
object Ether {
    private val root = FabricLoader.getInstance().configDir.resolve(Stella.NAMESPACE)
    private val assetFolder = root.resolve("assets")
    private val versionFile = root.resolve("version.txt")
    private const val BASE_URL = "https://ether.stellarskys.co"

    init {
        sync()
    }

    fun sync(scope: CoroutineScope = Stella.scope) = scope.launch {
        if (!Files.exists(assetFolder)) Files.createDirectories(assetFolder)

        // 1. Check version
        val remoteHash = NetworkUtils.fetchString("$BASE_URL/version.txt").getOrNull()?.trim() ?: return@launch
        val localHash = if (Files.exists(versionFile)) Files.readString(versionFile).trim() else ""

        if (remoteHash != localHash) {
            Stella.LOGGER.info("[Ether] Updating assets")

            val tempZip = Files.createTempFile("stella_assets", ".zip")
            NetworkUtils.downloadFile("$BASE_URL/assets.zip", tempZip).onSuccess {
                try {
                    extractZip(tempZip, assetFolder)
                    Files.writeString(versionFile, remoteHash)
                    Stella.LOGGER.info("[Ether] Asset sync complete.")
                } catch (e: Exception) {
                    Stella.LOGGER.error("[Ether] Failed to extract assets!", e)
                }
            }.onFailure {
                Stella.LOGGER.error("[Ether] Failed to download assets!", it)
            }

            Files.deleteIfExists(tempZip)
        }
    }

    private fun extractZip(zipPath: Path, destDir: Path) {
        ZipInputStream(Files.newInputStream(zipPath)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newPath = destDir.resolve(entry.name).normalize()
                if (!newPath.startsWith(destDir)) throw IOException("Bad zip entry: ${entry.name}")

                if (entry.isDirectory) {
                    Files.createDirectories(newPath)
                } else {
                    Files.createDirectories(newPath.parent)
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING)
                }
                entry = zis.nextEntry
            }
        }
    }
}