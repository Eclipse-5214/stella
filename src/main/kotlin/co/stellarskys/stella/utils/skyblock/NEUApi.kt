package co.stellarskys.stella.utils.skyblock

import co.stellarskys.stella.Stella
import com.google.gson.*
import com.mojang.serialization.JsonOps
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtOps
import net.minecraft.text.Text
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object NEUApi {
    private val client: CloseableHttpClient = HttpClients.createDefault()
    private val neuRepoDir: File get() = File("config/Stella/neu-repo")
    private val etagFile = File("config/Stella/neu-repo/etag.txt")
    private var cachedItems: List<NEUItem> = emptyList()

    fun init() {
        ensureNEURepoInstalled()
        loadAllItems()
        Stella.LOGGER.info("Repo Initialized!")
    }


    fun loadAllItems(): List<NEUItem> {
        val itemsDir = File(neuRepoDir, "items")
        if (!itemsDir.exists()) return emptyList()

        val items = itemsDir.listFiles { f -> f.extension == "json" }?.mapNotNull {
            try {
                loadNEUItem(it)
            } catch (e: Exception) {
                Stella.LOGGER.warn("Failed to parse NEU item: ${it.name}", e)
                null
            }
        } ?: emptyList()

        cachedItems = items
        return items
    }

    fun loadNEUItem(file: File): NEUItem {
        val json = JsonParser().parse(file.readText()).asJsonObject
        return NEUItem(
            internalname = json["internalname"].asString,
            displayname = json["displayname"].asString,
            itemid = json["itemid"].asString, // ← updated field name
            damage = json["damage"].asInt,
            nbttag = json["nbttag"]?.asString,
            clickcommand = json["clickcommand"]?.asString,
            lore = json["lore"]?.asJsonArray?.map { it.asString } ?: emptyList()
        )
    }

    fun ensureNEURepoInstalled(force: Boolean = false) {
        val repoDir = neuRepoDir
        val etagFile = File(repoDir, "etag.txt")
        val lastETag = etagFile.takeIf { it.exists() }?.readText()?.trim()

        val headRequest = HttpHead("https://github.com/NotEnoughUpdates/NotEnoughUpdates-Repo/archive/master.zip").apply {
            lastETag?.let { setHeader("If-None-Match", it) }
        }

        val headResponse = client.execute(headRequest)
        val newETag = headResponse.getFirstHeader("ETag")?.value?.trim()
        val updateAvailable = headResponse.statusLine.statusCode != 304 || force

        if (!updateAvailable && repoDir.resolve("items").exists()) {
            Stella.LOGGER.info("NEU repo is up to date.")
            return
        }

        if (updateAvailable) {
            Stella.LOGGER.info("Update found for NEU repo — downloading latest version...")
        }

        val getRequest = HttpGet("https://github.com/NotEnoughUpdates/NotEnoughUpdates-Repo/archive/master.zip")
        val getResponse = client.execute(getRequest)

        if (getResponse.statusLine.statusCode != 200) {
            Stella.LOGGER.error("Failed to download NEU repo: ${getResponse.statusLine}")
            return
        }

        val tempZip = File.createTempFile("neu_repo", ".zip")
        getResponse.entity.content.use { input ->
            tempZip.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val tempExtractDir = File.createTempFile("neu_extract", "").apply {
            delete()
            mkdirs()
        }

        unzip(tempZip, tempExtractDir)
        tempZip.delete()

        val extractedRoot = File(tempExtractDir, "NotEnoughUpdates-Repo-master")
        val itemsFolder = File(extractedRoot, "items")
        val targetItemsDir = File(repoDir, "items")

        if (!itemsFolder.exists()) {
            Stella.LOGGER.warn("NEU repo missing items/ folder — aborting.")
            return
        }

        itemsFolder.copyRecursively(targetItemsDir, overwrite = true)
        Stella.LOGGER.info("NEU repo installed to ${targetItemsDir.absolutePath}")

        newETag?.let {
            etagFile.writeText(it)
            Stella.LOGGER.debug("Saved new ETag: $it")
        }
    }


    fun getItemBySkyblockId(id: String): NEUItem? {
        // 1. Check cached items
        val cached = cachedItems.find { it.internalname.equals(id, ignoreCase = true) }
        if (cached != null) return cached

        // 2. Try loading from disk
        val file = File(neuRepoDir, "items/$id.json")
        if (!file.exists()) {
            Stella.LOGGER.warn("NEU item not found for Skyblock ID: $id")
            return null
        }

        return try {
            val loaded = loadNEUItem(file)
            cachedItems += loaded // optional: cache it for future use
            loaded
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to load NEU item from disk: $id", e)
            null
        }
    }

    fun createDummyStack(item: NEUItem): ItemStack {
        val baseItem = Registries.ITEM.get(Identifier.of(item.itemid)).takeIf { it != Items.AIR } ?: Items.BARRIER
        val stack = ItemStack(baseItem)

        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(item.displayname))
        stack.set(DataComponentTypes.LORE, LoreComponent(item.lore.map { Text.literal(it) }))

        // Parse legacy NBT if available
        item.nbttag?.let { raw ->
            val legacyTag = try {
                LegNBTParser.parse(raw)
            } catch (e: Exception) {
                Stella.LOGGER.error("Failed to parse legacy NEU tag", e)
                null
            }

            legacyTag?.let { tag ->
                // Inject ExtraAttributes if present
                stack.se
            }
        }

        return stack
    }


    fun unzip(zipFile: File, outputDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outPath = File(outputDir, entry.name)

                if (entry.isDirectory) {
                    outPath.mkdirs()
                } else {
                    outPath.parentFile.mkdirs()
                    FileOutputStream(outPath).use { out ->
                        zip.copyTo(out)
                    }
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    fun sanitizeNEUNbtTag(raw: String): String {
        return raw
            // Fix array index notation: [0:"..."] → ["..."]
            .replace(Regex("""\[(\d+):"""), "[")
            // Quote keys: {key: → {"key":
            .replace(Regex("""([{\[,])(\w+):"""), "$1\"$2\":")
            // Unescape embedded quotes
            .replace("\\\"", "\"")
            // Fix stringified JSON inside petInfo
            .replace("\"{", "{")
            .replace("}\"", "}")
    }
}

data class NEUItem(
    val internalname: String,
    val displayname: String,
    val itemid: String, // ← updated field name
    val damage: Int,
    val nbttag: String?,
    val clickcommand: String?,
    val lore: List<String>
)