package co.stellarskys.stella.utils.skyblock

import co.stellarskys.stella.Stella
import com.google.gson.*
import com.mojang.serialization.Dynamic
import net.minecraft.SharedConstants
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.datafixer.Schemas
import net.minecraft.datafixer.TypeReferences
import net.minecraft.util.Identifier
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.BuiltinRegistries
import net.minecraft.registry.RegistryOps
import net.minecraft.registry.RegistryWrapper
import net.minecraft.text.Text
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.jvm.optionals.getOrNull

object NEUApi {
    private val client: CloseableHttpClient = HttpClients.createDefault()
    private val neuRepoDir: File get() = File("config/Stella/neu-repo")
    private val etagFile = File("config/Stella/neu-repo/etag.txt")
    private var cachedItems: List<NEUItem> = emptyList()
    private val cachedOverlays: MutableMap<String, NbtCompound> = mutableMapOf()
    private val cashedStacks: MutableMap<String, ItemStack> = mutableMapOf()
    val defaultRegistries: RegistryWrapper.WrapperLookup by lazy { BuiltinRegistries.createWrapperLookup() }

    fun init() {
        ensureNEURepoInstalled()
        loadAllItems()
        loadAllOverlays()
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

    fun loadAllOverlays(): Map<String, NbtCompound> {
        val overlayDir = File(neuRepoDir, "overlay")
        if (!overlayDir.exists()) return emptyMap()

        val overlays = overlayDir.listFiles { it.extension == "snbt" }?.mapNotNull { file ->
            try {
                val internalname = file.nameWithoutExtension
                val raw = StringNbtReader.readCompound(file.readText())

                val source = raw.getCompound("source")
                val dataVersion = source.getOrNull()?.getInt("dataVersion")?.getOrNull() ?: return emptyMap()

                val stripped = raw.copy().apply { remove("source") }

                val fixed = Schemas.getFixer().update(
                    TypeReferences.ITEM_STACK,
                    Dynamic(NbtOps.INSTANCE, stripped),
                    dataVersion,
                    SharedConstants.getGameVersion().dataVersion().id
                ).value as? NbtCompound ?: return@mapNotNull null

                cachedOverlays[internalname] = fixed
                internalname to fixed
            } catch (e: Exception) {
                Stella.LOGGER.warn("Failed to decode overlay: ${file.name}", e)
                null
            }
        }?.toMap() ?: emptyMap()

        return overlays
    }

    fun tryFindFromModernFormat(internalname: String): NbtCompound? {
        return cachedOverlays[internalname]
    }

    fun loadNEUItem(file: File): NEUItem {
        val json = JsonParser.parseString(file.readText()).asJsonObject
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

        val overlayFolder = File(extractedRoot, "itemsOverlay/4325")
        val targetOverlayDir = File(repoDir, "overlay")

        if (!overlayFolder.exists()) {
            Stella.LOGGER.warn("NEU repo missing itemsOverlay/4325 folder — skipping overlay import.")
            return
        }

        overlayFolder.copyRecursively(targetOverlayDir, overwrite = true)

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
            Stella.LOGGER.debug("NEU item not found for Skyblock ID: $id")
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
        return cashedStacks.getOrPut(item.internalname) {
            createDummyStackNow(item)
        }
    }

    fun createDummyStackNow(item: NEUItem): ItemStack {
        return try {
            var modernTag = tryFindFromModernFormat(item.internalname)
            var usedLegacy = false

            if (modernTag == null) {
                val legacyTag = getLegacyItemTag(item) ?: return ItemStack(Items.BARRIER)
                usedLegacy = true
                modernTag = convert189ToModern(legacyTag) ?: return ItemStack(Items.BARRIER)
            }

            val stack = decodeItemStack(modernTag) ?: return ItemStack(Items.BARRIER)

            if (usedLegacy) injectLegacyExtras(stack, modernTag)

            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(item.displayname))
            stack.set(DataComponentTypes.LORE, LoreComponent(item.lore.map { Text.literal(it) }))

            stack
        } catch (e: Exception) {
            Stella.LOGGER.error("Exception while creating dummy stack for ${item.internalname}", e)
            return ItemStack(Items.BARRIER)
        }
    }

    fun getLegacyItemTag(item: NEUItem): NbtCompound? {
        val raw = item.nbttag ?: return null
        return try {
            NbtCompound().apply {
                put("tag", LegNBTParser.parse(raw)) // uses new parser
                putString("id", item.itemid)
                putByte("Count", 1)
                putShort("Damage", item.damage.toShort())
            }
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to parse legacy NBT for ${item.internalname}", e)
            null
        }
    }

    fun convert189ToModern(nbt: NbtCompound): NbtCompound? = try {
        Schemas.getFixer().update(
            TypeReferences.ITEM_STACK,
            Dynamic(NbtOps.INSTANCE, nbt),
            -1,
            SharedConstants.getGameVersion().dataVersion().id
        ).value as? NbtCompound
    } catch (e: Exception) {
        Stella.LOGGER.error("Failed to data-fix legacy NBT", e)
        null
    }

    fun decodeItemStack(nbt: NbtCompound): ItemStack? {
        return ItemStack.CODEC.decode(
            RegistryOps.of(NbtOps.INSTANCE, NEUApi.defaultRegistries),
            nbt
        ).result().getOrNull()?.first
    }

    fun injectLegacyExtras(stack: ItemStack, legacyTag: NbtCompound) {
        val tag = legacyTag.getCompound("tag")
        tag.getOrNull()?.getCompound("ExtraAttributes")?.getOrNull()?.let {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(it))
        }
        tag.getOrNull()?.getString("ItemModel")?.getOrNull()?.let {
            stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of(it))
        }
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