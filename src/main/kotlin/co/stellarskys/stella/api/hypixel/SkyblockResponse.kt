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
import com.google.gson.JsonElement

data class SkyblockResponse(
    val success: Boolean = false,
    val profiles: List<SkyblockProfile>? = null
) {
    fun getActiveMember(uuid: String): SkyblockMember? {
        val cleanUuid = uuid.replace("-", "")
        val activeProfile = profiles?.find { it.selected } ?: return null
        val member = activeProfile.members[cleanUuid] ?: return null
        member.profile = activeProfile
        member.allProfiles = profiles
        member.uuid = UndashedUuid.fromStringLenient(cleanUuid)
        member.inventory.loadout = member.loadout
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

    data class MemberProfile(
        @SerializedName("bank_account") val personalBank: Double = 0.0
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
        val collection: Map<String, Double> = emptyMap(),
        @SerializedName("profile") val memberProfile: MemberProfile? = null,
        val currencies: Currencies = Currencies(),
        @SerializedName("loadout") val loadout: Loadout = Loadout(),
        @SerializedName("mining_core") val miningCore: MiningCore = MiningCore(),
        @SerializedName("skill_tree") val skillTree: SkillTree = SkillTree(),
        @SerializedName("glacite_player_data") val glacite: GlacitePlayerData = GlacitePlayerData(),
        val objectives: Objectives = Objectives(),
        @Transient var profile: SkyblockProfile? = null,
        @Transient var allProfiles: List<SkyblockProfile>? = null,
        @Transient var uuid: UUID? = null,
        @Transient var museumValue: Long = 0L
    ) {
        val sbLevel get() = leveling.experience / 100
        val sbLevelProgress get() = leveling.experience % 100

        val inventoryApi get() = inventory.invContents.data.isNotEmpty()
        val assumedMagicalPower get() =
            if (accessoryBag.highestMP > 0) accessoryBag.highestMP
            else (accessoryBag.tuning.currentTunings.values.sum() * 10).toLong()

        val allItems get() = inventory.invContents.itemStacks +
                inventory.eChestContents.itemStacks +
                inventory.loadoutItemStacks +
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

    data class Objectives(
        val tutorial: List<String> = emptyList()
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

    data class Pet(
        val uuid: UUID? = null,
        val uniqueId: UUID? = null,
        val type: String = "",
        val exp: Double = 0.0,
        val active: Boolean = false,
        val tier: String = "",
        val heldItem: String? = null,
        val candyUsed: Int = 0,
        val skin: String? = null
    ) {
        val rarity by lazy { SkyBlockRarity.entries.find { it.name == tier } ?: SkyBlockRarity.COMMON }
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
        @SerializedName("wardrobe_equipped_slot") val wdEquipped: Int = 0,
        @SerializedName("equipment_contents") val equipment: InventoryContents = InventoryContents(),
        @SerializedName("personal_vault_contents") val personalVault: InventoryContents = InventoryContents(),
        @SerializedName("bag_contents") val bags: BagContents = BagContents(),
        @SerializedName("sacks_counts") val sacks: Map<String, Long> = emptyMap()
    ) {
        @Transient var loadout: Loadout = Loadout()

        val enderChestPages get() = eChestContents.items().chunked(45)
        val invAndHotbar get() = invContents.items().take(9) to invContents.items().drop(9)

        val fullWardrobe: List<ItemStack> get() {
            val armorMap = loadout.armorSets
            if (armorMap.isEmpty()) return emptyList()
            val maxSlot = armorMap.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
            val totalPages = (maxSlot + 8) / 9
            val items = mutableListOf<ItemStack>()

            for (page in 0 until totalPages) {
                val startSlot = page * 9 + 1
                val endSlot = startSlot + 8

                for (piece in listOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")) {
                    for (slotId in startSlot..endSlot) {
                        val slotData = armorMap[slotId.toString()]
                        val contents = when (piece) {
                            "HELMET" -> slotData?.helmet
                            "CHESTPLATE" -> slotData?.chestplate
                            "LEGGINGS" -> slotData?.leggings
                            "BOOTS" -> slotData?.boots
                            else -> null
                        }
                        val decoded = contents?.items()?.firstOrNull() ?: ItemStack.EMPTY
                        items.add(decoded)
                    }
                }
            }

            if (items.isNotEmpty() && wdEquipped > 0) {
                invArmor.items().reversed().forEachIndexed { row, armorPiece ->
                    val slotIdx = (((wdEquipped - 1) / 9) * 36) + (row * 9) + ((wdEquipped - 1) % 9)
                    if (slotIdx < items.size && !armorPiece.isEmpty) {
                        items[slotIdx] = armorPiece
                    }
                }
            }

            return items
        }

        val loadoutItemStacks: List<ItemData?> get() {
            val armorMap = loadout.armorSets
            if (armorMap.isEmpty()) return emptyList()
            val maxSlot = armorMap.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
            val totalPages = (maxSlot + 8) / 9
            val itemDatas = mutableListOf<ItemData?>()

            for (page in 0 until totalPages) {
                val startSlot = page * 9 + 1
                val endSlot = startSlot + 8
                
                for (piece in listOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")) {
                    for (slotId in startSlot..endSlot) {
                        val slotData = armorMap[slotId.toString()]
                        val contents = when (piece) {
                            "HELMET" -> slotData?.helmet
                            "CHESTPLATE" -> slotData?.chestplate
                            "LEGGINGS" -> slotData?.leggings
                            "BOOTS" -> slotData?.boots
                            else -> null
                        }
                        val decoded = contents?.itemStacks?.firstOrNull()
                        itemDatas.add(decoded)
                    }
                }
            }
            return itemDatas
        }

        val fullEquipmentWardrobe: List<ItemStack> get() {
            val equipMap = loadout.equipmentSets
            if (equipMap.isEmpty()) return emptyList()
            val maxSlot = equipMap.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0
            val totalPages = (maxSlot + 8) / 9
            val items = mutableListOf<ItemStack>()

            for (page in 0 until totalPages) {
                val startSlot = page * 9 + 1
                val endSlot = startSlot + 8

                for (piece in listOf("necklace", "cloak", "belt", "gloves")) {
                    for (slotId in startSlot..endSlot) {
                        val slotData = equipMap[slotId.toString()]
                        val contents = when (piece) {
                            "necklace" -> slotData?.necklace
                            "cloak" -> slotData?.cloak
                            "belt" -> slotData?.belt
                            "gloves" -> slotData?.gloves
                            else -> null
                        }
                        items.add(contents?.items()?.firstOrNull() ?: ItemStack.EMPTY)
                    }
                }
            }

            return items
        }
    }

    data class BagContents(
        @SerializedName("talisman_bag") val talismanBag: InventoryContents = InventoryContents(),
        @SerializedName("quiver") val quiver: InventoryContents = InventoryContents(),
        @SerializedName("fishing_bag") val fishingBag: InventoryContents = InventoryContents(),
        @SerializedName("potion_bag") val potionBag: InventoryContents = InventoryContents(),
    ) {
        val accessoryBagPages get() = talismanBag.items().chunked(45)
    }

    data class Loadout(
        val armor: Map<String, JsonElement> = emptyMap(),
        val equipment: Map<String, JsonElement> = emptyMap()
    ) {
        val armorSets: Map<String, LoadoutArmor> get() {
            val gson = com.google.gson.Gson()
            return armor
                .filter { (_, v) -> v.isJsonObject }
                .mapValues { (_, v) -> gson.fromJson(v, LoadoutArmor::class.java) }
        }

        val equipmentSets: Map<String, LoadoutEquipment> get() {
            val gson = com.google.gson.Gson()
            return equipment
                .filter { (_, v) -> v.isJsonObject }
                .mapValues { (_, v) -> gson.fromJson(v, LoadoutEquipment::class.java) }
        }
    }

    data class LoadoutArmor(
        val id: Int = 0,
        @SerializedName("HELMET") val helmet: InventoryContents = InventoryContents(),
        @SerializedName("CHESTPLATE") val chestplate: InventoryContents = InventoryContents(),
        @SerializedName("LEGGINGS") val leggings: InventoryContents = InventoryContents(),
        @SerializedName("BOOTS") val boots: InventoryContents = InventoryContents(),
    )

    data class LoadoutEquipment(
        val id: Int = 0,
        @SerializedName("EQUIPMENT_SLOT_1") val necklace: InventoryContents = InventoryContents(),
        @SerializedName("EQUIPMENT_SLOT_2") val cloak: InventoryContents = InventoryContents(),
        @SerializedName("EQUIPMENT_SLOT_3") val belt: InventoryContents = InventoryContents(),
        @SerializedName("EQUIPMENT_SLOT_4") val gloves: InventoryContents = InventoryContents(),
    )

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

    data class MuseumResponse(
        val success: Boolean = false,
        val members: Map<String, MuseumMember> = emptyMap()
    )

    data class MuseumMember(
        val value: Long = 0L,
        val appraisal: Boolean = false
    )

    data class MiningCore(
        @SerializedName("received_free_tier") val receivedFreeTier: Boolean = false,
        @SerializedName("powder_mithril") val powderMithril: Int = 0,
        @SerializedName("powder_mithril_total") val powderMithrilTotal: Int = 0,
        @SerializedName("powder_spent_mithril") val powderSpentMithril: Int = 0,
        @SerializedName("powder_gemstone") val powderGemstone: Int = 0,
        @SerializedName("powder_gemstone_total") val powderGemstoneTotal: Int = 0,
        @SerializedName("powder_spent_gemstone") val powderSpentGemstone: Int = 0,
        @SerializedName("powder_glacite") val powderGlacite: Int = 0,
        @SerializedName("powder_glacite_total") val powderGlaciteTotal: Int = 0,
        @SerializedName("powder_spent_glacite") val powderSpentGlacite: Int = 0,
        val crystals: Map<String, CrystalData> = emptyMap(),
        val biomes: Map<String, BiomeData> = emptyMap(),
        @SerializedName("commissions_completed") val commissionsCompleted: Int = 0,
        val tokens: Int = 0,
        @SerializedName("tokens_spent") val tokensSpent: Int = 0
    )

    data class GlacitePlayerData(
        @SerializedName("fossils_donated") val fossilsDonated: List<String> = emptyList(),
        @SerializedName("corpses_looted") val corpsesLooted: Map<String, Int> = emptyMap(),
        @SerializedName("mineshafts_entered") val mineshaftsEntered: Int = 0
    )

    data class CrystalData(
        val state: String = "NOT_FOUND",
        @SerializedName("total_found") val totalFound: Int = 0,
        @SerializedName("total_placed") val totalPlaced: Int = 0
    )

    data class BiomeData(
        @SerializedName("king_quests_completed") val kingQuestsCompleted: Int = 0
    )

    data class SkillTree(
        val nodes: Map<String, Map<String, Any>> = emptyMap(),
        @SerializedName("tokens_spent") val tokensSpent: Map<String, Int> = emptyMap(),
        val experience: Map<String, Double> = emptyMap(),
        @SerializedName("selected_ability") val selectedAbility: Map<String, String> = emptyMap()
    )
}