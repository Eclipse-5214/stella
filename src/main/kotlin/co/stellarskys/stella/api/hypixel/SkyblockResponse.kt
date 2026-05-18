package co.stellarskys.stella.api.hypixel

import com.google.gson.annotations.SerializedName
import com.mojang.util.UndashedUuid
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.remote.hypixel.legacyStack
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.jvm.optionals.getOrNull

data class SkyblockResponse(
    val success: Boolean = false,
    val profiles: List<SkyblockProfile>? = null
) {
    fun getActiveMember(uuid: String): SkyblockMember? {
        val cleanUuid = uuid.replace("-", "")
        val activeProfile = profiles?.find { it.selected } ?: return null
        val member = activeProfile.members[cleanUuid] ?: return null
        member.profile = activeProfile
        member.uuid = UndashedUuid.fromStringLenient(cleanUuid)
        return member
    }

    data class SkyblockProfile(
        @SerializedName("profile_id") val id: String,
        val selected: Boolean = false,
        @SerializedName("cute_name") val cuteName: String? = null,
        val members: Map<String, SkyblockMember> = emptyMap(),
        val banking: Banking = Banking()
    )

    data class Banking(
        val balance: Double = 0.0
    )

    data class SkyblockMember(
        @SerializedName("player_stats") val stats: PlayerStats = PlayerStats(),
        @SerializedName("player_data") val playerData: PlayerData = PlayerData(), // Added
        @SerializedName("leveling") val leveling: Leveling = Leveling(),         // Added
        val slayer: SlayerData = SlayerData(),
        @SerializedName("pets_data") val petsData: PetsData = PetsData(),
        val dungeons: DungeonsData = DungeonsData(),
        @SerializedName("accessory_bag_storage") val accessoryBag: AccessoryBagStorage = AccessoryBagStorage(),
        val inventory: Inventory = Inventory(),
        @SerializedName("bank_account") val soloBank: Double = 0.0,
        val currencies: Currencies = Currencies(),
        var profile: SkyblockProfile? = null,
        var uuid: UUID? = null
    ) {
        val sbLevel get() = leveling.experience / 100
        val sbLevelProgress get() = leveling.experience % 100

        val inventoryApi get() = inventory.invContents.data.isNotEmpty()
        val assumedMagicalPower get() =
            if (accessoryBag.highestMP > 0) accessoryBag.highestMP
            else (accessoryBag.tuning.currentTunings.values.sum() * 10).toLong()

        val allItems get() = inventory.invContents.itemStacks +
                inventory.eChestContents.itemStacks +
                inventory.wardrobeContents.itemStacks +
                inventory.equipment.itemStacks +
                inventory.bags.fishingBag.itemStacks +
                inventory.bags.talismanBag.itemStacks +
                inventory.bags.quiver.itemStacks +
                inventory.personalVault.itemStacks +
                inventory.backpackContents.flatMap { it.value.itemStacks }
    }

    data class Leveling(
        val experience: Int = 0,
    )

    data class PlayerData(
        val experience: Map<String, Double> = emptyMap(),
        val perks: Map<String, Int> = emptyMap()
    )

    data class SlayerData(
        @SerializedName("slayer_bosses") val bosses: Map<String, SlayerBoss> = emptyMap()
    )

    data class SlayerBoss(
        @SerializedName("claimed_levels") val claimedLevels: Map<String, Boolean> = emptyMap(),
        val xp: Long = 0,
        @SerializedName("boss_kills_tier_0") val t1Kills: Int = 0,
        @SerializedName("boss_kills_tier_1") val t2Kills: Int = 0,
        @SerializedName("boss_kills_tier_2") val t3Kills: Int = 0,
        @SerializedName("boss_kills_tier_3") val t4Kills: Int = 0,
        @SerializedName("boss_kills_tier_4") val t5Kills: Int = 0
    ) {
        val totalKills get() = t1Kills + t2Kills + t3Kills + t4Kills + t5Kills
    }

    data class Currencies(
        @SerializedName("coin_purse") val purse: Double = 0.0,
        @SerializedName("motes_purse") val motes: Double = 0.0,
        val essence: Map<String, EssenceData> = emptyMap()
    )

    data class EssenceData(val current: Long = 0)

    data class PlayerStats(
        val kills: Map<String, Double> = emptyMap(),
        val deaths: Map<String, Double> = emptyMap(),
        @SerializedName("highest_damage") val highestDamage: Double = 0.0
    ) {
        val totalKills get() = kills["total"] ?: 0.0
        val totalDeaths get() = deaths["total"] ?: 0.0
        val bloodMobKills get() = ((kills["watcher_summon_undead"] ?: 0.0) + (kills["master_watcher_summon_undead"] ?: 0.0)).toInt()
    }

    data class DungeonsData(
        @SerializedName("dungeon_types") val dungeonTypes: DungeonTypes = DungeonTypes(),
        @SerializedName("player_classes") val classes: Map<String, ClassData> = emptyMap(),
        @SerializedName("selected_dungeon_class") val selectedClass: String? = null,
        val secrets: Long = 0
    ) {
        inline val totalRuns get() = (1..7).sumOf { tier -> (dungeonTypes.catacombs.tierComps["$tier"]?.toInt() ?: 0) + (dungeonTypes.mastermode.tierComps["$tier"]?.toInt() ?: 0) }
        inline val averageSecrets get() = if (totalRuns > 0) secrets.toDouble() / totalRuns else 0.0
    }

    data class DungeonTypes(
        val catacombs: DungeonTypeData = DungeonTypeData(),
        @SerializedName("master_catacombs") val mastermode: DungeonTypeData = DungeonTypeData()
    )

    data class DungeonTypeData(
        val experience: Double = 0.0,
        @SerializedName("tier_completions") val tierComps: Map<String, Float> = emptyMap(),
        @SerializedName("fastest_time_s_plus") val fastestSPlus: Map<String, Double> = emptyMap()
    )

    data class ClassData(val experience: Double = 0.0)

    data class PetsData(
        val pets: List<Pet> = emptyList(),
        @SerializedName("pet_care") val petCare: PetCare = PetCare()
    ) {
        val activePet get() = pets.find { it.active }
    }

    data class PetCare(
        @SerializedName("pet_types_sacrificed") val petTypesSacrificed: List<String> = emptyList()
    )

    data class Pet(val type: String = "", val active: Boolean = false, val tier: String = "", val heldItem: String? = null) {
        val rarity = SkyBlockRarity.entries.find { it.name == tier } ?: SkyBlockRarity.COMMON
    }

    data class AccessoryBagStorage(
        @SerializedName("highest_magical_power") val highestMP: Long = 0,
        val tuning: TuningData = TuningData()
    )

    data class TuningData(@SerializedName("slot_0") val currentTunings: Map<String, Int> = emptyMap())

    data class Inventory(
        @SerializedName("inv_contents") val invContents: InventoryContents = InventoryContents(),
        @SerializedName("ender_chest_contents") val eChestContents: InventoryContents = InventoryContents(),
        @SerializedName("backpack_contents") val backpackContents: Map<String, InventoryContents> = emptyMap(),
        @SerializedName("inv_armor") val invArmor: InventoryContents = InventoryContents(),
        @SerializedName("wardrobe_contents") val wardrobeContents: InventoryContents = InventoryContents(),
        @SerializedName("equipment_contents") val equipment: InventoryContents = InventoryContents(),
        @SerializedName("personal_vault_contents") val personalVault: InventoryContents = InventoryContents(),
        @SerializedName("bag_contents") val bags: BagContents = BagContents(),
        @SerializedName("sacks_counts") val sacks: Map<String, Long> = emptyMap()
    ) {
        val enderChestPages get() = eChestContents.items().chunked(45)
    }

    data class BagContents(
        @SerializedName("talisman_bag") val talismanBag: InventoryContents = InventoryContents(),
        @SerializedName("quiver") val quiver: InventoryContents = InventoryContents(),
        @SerializedName("fishing_bag") val fishingBag: InventoryContents = InventoryContents(),
        @SerializedName("potion_bag") val potionBag: InventoryContents = InventoryContents(),
    ) {
        val accessoryBagPages get() = talismanBag.items().chunked(45)
    }

    @OptIn(ExperimentalEncodingApi::class)
    data class InventoryContents(val type: Int? = null, val data: String = "") {
        val itemStacks: List<ItemData?> get() = with(data) {
            if (isEmpty()) return emptyList()
            val nbtCompound = NbtIo.readCompressed(Base64.decode(this).inputStream(), NbtAccounter.unlimitedHeap())
            val itemNBTList = nbtCompound.getList("i").getOrNull() ?: return emptyList()
            itemNBTList.indices.map { i ->
                val compound = itemNBTList.getCompound(i).getOrNull()?.takeIf { it.size() > 0 } ?: return@map null
                val tag = compound.get("tag")?.asCompound()?.get() ?: return@map null
                val id = tag.get("ExtraAttributes")?.asCompound()?.get()?.get("id")?.asString()?.get() ?: ""
                val display = tag.get("display")?.asCompound()?.get() ?: return@map null
                val name = display.get("Name")?.asString()?.get() ?: ""
                val lore = display.get("Lore")?.asList()?.get()?.mapNotNull { it.asString().getOrNull() } ?: emptyList()

                ItemData(name, id, lore)
            }
        }

        fun items(): List<ItemStack> = with(data) {
            if (isEmpty()) return emptyList()
            val nbtCompound = NbtIo.readCompressed(Base64.decode(this).inputStream(), NbtAccounter.unlimitedHeap())
            val itemNBTList = nbtCompound.getList("i").getOrNull() ?: return emptyList()
            itemNBTList.mapNotNull {
               runCatching { it.legacyStack() }.getOrDefault(ItemStack.EMPTY)
            }
        }
    }

    data class ItemData(val name: String, val id: String, val lore: List<String>, )
}
