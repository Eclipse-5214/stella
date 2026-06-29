package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component
import kotlin.jvm.optionals.getOrNull

enum class NodeType {
    PERK, CORE, ABILITY, UNLEVELABLE
}

data class NodeInfo(
    val type: NodeType,
    val apiKey: String,
    val displayName: String,
    val maxLevel: Int,
    val tooltip: List<String>
)

object HotmUtils {

    val nucleusRunCrystals = listOf(
        "jade_crystal" to "Jade",
        "amber_crystal" to "Amber",
        "amethyst_crystal" to "Amethyst",
        "sapphire_crystal" to "Sapphire",
        "topaz_crystal" to "Topaz"
    )

    val otherCrystals = listOf(
        "ruby_crystal" to "Ruby",
        "jasper_crystal" to "Jasper",
        "opal_crystal" to "Opal",
        "aquamarine_crystal" to "Aquamarine",
        "citrine_crystal" to "Citrine",
        "peridot_crystal" to "Peridot",
        "onyx_crystal" to "Onyx"
    )

    val fossilsList = listOf(
        "claw_fossil" to "Claw",
        "spine_fossil" to "Spine",
        "clubbed_fossil" to "Clubbed",
        "ugly_fossil" to "Ugly",
        "helix_fossil" to "Helix",
        "footprint_fossil" to "Footprint",
        "webbed_fossil" to "Webbed",
        "tusk_fossil" to "Tusk"
    )

    val nodes: Map<Int, NodeInfo> by lazy {
        try {
            val stream = HotmUtils::class.java.getResourceAsStream("/assets/stella/pv/hotm_nodes.json")
            if (stream != null) {
                val reader = java.io.InputStreamReader(stream)
                val rawType = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                val rawList: List<Map<String, Any>> = com.google.gson.Gson().fromJson(reader, rawType)
                
                rawList.mapNotNull { data ->
                    val pos = data["position"] as? Map<*, *> ?: return@mapNotNull null
                    val col = (pos["col"] as? Number)?.toInt() ?: 0
                    val row = (pos["row"] as? Number)?.toInt() ?: 0
                    val slot = row * 9 + col
                    
                    val apiKey = data["apiKey"] as? String ?: return@mapNotNull null
                    val displayName = data["displayName"] as? String ?: return@mapNotNull null
                    val typeStr = data["type"] as? String ?: return@mapNotNull null
                    val type = try { NodeType.valueOf(typeStr) } catch (e: Exception) { NodeType.PERK }
                    val maxLevel = (data["maxLevel"] as? Number)?.toInt() ?: 0
                    val tooltipList = (data["tooltip"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    
                    slot to NodeInfo(type, apiKey, displayName, maxLevel, tooltipList)
                }.toMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    fun calcHotmTokens(hotmTier: Int, potmTier: Int): Int {
        var tokens = 0
        val hotmRewards = mapOf(1 to 1, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2, 7 to 3, 8 to 2, 9 to 2, 10 to 2)
        val potmRewards = mapOf(1 to 1, 5 to 1, 7 to 1, 10 to 2)

        for (tier in 1..hotmTier) {
            tokens += hotmRewards[tier] ?: 0
        }
        for (tier in 1..potmTier) {
            tokens += potmRewards[tier] ?: 0
        }
        return tokens
    }

    fun getHotmLevel(xp: Double): SkillUtils.Skill {
        val xpTable = doubleArrayOf(3000.0, 9000.0, 25000.0, 60000.0, 100000.0, 150000.0, 210000.0, 290000.0, 400000.0)
        var level = 1
        var r = xp
        for (i in xpTable.indices) {
            if (r < xpTable[i]) {
                val progress = (r / xpTable[i]).toFloat()
                return SkillUtils.Skill(
                    level = level + progress.toDouble(),
                    progress = progress,
                    cap = 10,
                    xp = xp,
                    progressXp = r,
                    nextLevelNeededXp = xpTable[i],
                    remainingXp = xpTable[i] - r
                )
            }
            r -= xpTable[i]
            level++
        }
        return SkillUtils.Skill(
            level = 10.0,
            progress = 1.0f,
            cap = 10,
            xp = xp,
            progressXp = 0.0,
            nextLevelNeededXp = 0.0,
            remainingXp = 0.0
        )
    }

    fun getActiveAbilityName(apiKey: String?): String {
        return when (apiKey) {
            "mining_speed_boost" -> "Mining Speed Boost"
            "pickobulus" -> "Pickobulus"
            "gemstone_infusion" -> "Gemstone Infusion"
            "sheer_force" -> "Sheer Force"
            "maniac_miner" -> "Maniac Miner"
            "anomalous_desire" -> "Tunnel Vision"
            else -> "None"
        }
    }

    fun getCommissionMilestone(tutorials: List<String>): Int {
        var milestone = 0
        tutorials.forEach { tutorial ->
            if (tutorial.startsWith("commission_milestone_reward_mining_xp_tier_")) {
                val tier = tutorial.split("_").lastOrNull()?.toIntOrNull() ?: 0
                if (tier > milestone) {
                    milestone = tier
                }
            }
        }
        return milestone
    }

    fun getNucleusRuns(crystals: Map<String, SkyblockResponse.CrystalData>): Int {
        return crystals
            .filterKeys { it in listOf("jade_crystal", "amber_crystal", "amethyst_crystal", "sapphire_crystal", "topaz_crystal") }
            .minOfOrNull { it.value.totalPlaced } ?: 0
    }

    fun getOtherCrystalsCount(crystals: Map<String, SkyblockResponse.CrystalData>): Int {
        return otherCrystals.count { (apiKey, _) ->
            val crystal = crystals[apiKey]
            crystal != null && crystal.state in listOf("FOUND", "PLACED")
        }
    }

    fun getFossilsCount(donated: List<String>): Int {
        return fossilsList.count { donated.contains(it.first.split("_").first().uppercase(java.util.Locale.ROOT)) }
    }

    fun getNodeItem(node: NodeInfo, level: Int, isEnabled: Boolean, hotmLevel: Int, selectedAbility: String?): ItemStack {
        val itemName = when (node.type) {
            NodeType.CORE -> {
                when {
                    level >= 10 -> "diamond_block"
                    level <= 0 -> "bedrock"
                    level == 1 -> "copper_block"
                    else -> "redstone_block"
                }
            }
            NodeType.ABILITY -> {
                when {
                    selectedAbility == node.apiKey -> "emerald_block"
                    level > 0 -> "redstone_block"
                    else -> "coal_block"
                }
            }
            else -> {
                val isMax = node.maxLevel in 1..level
                when {
                    level <= 0 -> "coal"
                    !isEnabled -> "redstone"
                    node.type == NodeType.UNLEVELABLE -> "diamond"
                    isMax -> "diamond"
                    else -> "emerald"
                }
            }
        }
        val itemLike = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName)).getOrNull()
        return itemLike?.defaultInstance ?: ItemStack.EMPTY
    }

    fun getFormattedTooltip(node: NodeInfo, level: Int, isEnabled: Boolean, hotmLevel: Int, selectedAbility: String?): List<Component> {
        val list = mutableListOf<Component>()
        
        val colorPrefix = when {
            level <= 0 -> "§7"
            !isEnabled -> "§c"
            else -> "§a"
        }
        list.add(Component.literal("$colorPrefix${node.displayName}"))
        
        if (node.type == NodeType.PERK || node.type == NodeType.CORE) {
            val maxLvl = if (node.type == NodeType.CORE) 10 else node.maxLevel
            if (maxLvl in 1..level) {
                list.add(Component.literal("§8Level $level (Maxed)"))
            } else if (level > 0) {
                list.add(Component.literal("§8Level $level/$maxLvl"))
            } else {
                list.add(Component.literal("§8Level 0/$maxLvl"))
            }
        } else if (node.type == NodeType.ABILITY) {
            if (level > 0) {
                list.add(Component.literal("§8Level $level"))
            }
        }
        
        list.add(Component.literal(""))
        
        if (node.type == NodeType.CORE) {
            val cotmRewards = listOf(
                "§8+§51 Token of the Mountain",
                "§8+§c1 Pickaxe Ability Level",
                "§a+1 Commission Slot",
                "§8+§21 Base Mithril Powder §7when mining Mithril",
                "§8+§52 Token of the Mountain",
                "§8+§d2 Base Gemstone Powder §7when mining Gemstones",
                "§8+§53 Token of the Mountain",
                "§8+§b3 Base Glacite Powder §7when mining Glacite",
                "§8+§a10% chance §7for §bGlacite Mineshafts §7to spawn",
                "§8+§55 Token of the Mountain"
            )
            list.add(Component.literal("§7Grants various bonuses based on tier level."))
            list.add(Component.literal(""))
            list.add(Component.literal("§dBonuses Unlocked:"))
            for (i in 0 until level.coerceIn(0, 10)) {
                list.add(Component.literal(cotmRewards[i]))
            }
            if (level <= 0) {
                list.add(Component.literal("§8- §cNone"))
            }
            if (level < 10) {
                list.add(Component.literal(""))
                list.add(Component.literal("§dNext Tier Bonus:"))
                list.add(Component.literal(cotmRewards[level]))
            }
        } else {
            val replacements = getReplacements(node.apiKey, level, hotmLevel)
            node.tooltip.forEach { rawLine ->
                var line = rawLine
                replacements.forEach { (key, value) ->
                    line = line.replace("%$key%", value)
                }
                if (level <= 0) {
                    val lvl1Replacements = getReplacements(node.apiKey, 1, hotmLevel)
                    lvl1Replacements.forEach { (key, value) ->
                        line = line.replace("%$key%", value)
                    }
                }
                val formattedLine = line
                list.add(Component.literal(formattedLine))
            }
        }
        
        list.add(Component.literal(""))
        when (node.type) {
            NodeType.CORE -> {
                if (level > 0) {
                    list.add(Component.literal("§aUNLOCKED"))
                } else {
                    list.add(Component.literal("§cLOCKED"))
                }
            }
            NodeType.ABILITY -> {
                if (selectedAbility == node.apiKey) {
                    list.add(Component.literal("§aSELECTED"))
                } else if (level > 0) {
                    list.add(Component.literal("§aUNLOCKED"))
                } else {
                    list.add(Component.literal("§cLOCKED"))
                }
            }
            else -> {
                if (level > 0) {
                    if (isEnabled) {
                        list.add(Component.literal("§aENABLED"))
                    } else {
                        list.add(Component.literal("§cDISABLED"))
                    }
                } else {
                    list.add(Component.literal("§cLOCKED"))
                }
            }
        }
        
        return list
    }



    fun getReplacements(nodeId: String, level: Int, hotmLevel: Int): Map<String, String> {
        val lvl = level.coerceAtLeast(0)
        val effLvl = (lvl - 1).coerceAtLeast(0)

        fun Double.fmt(): String {
            val longVal = this.toLong()
            return if (this == longVal.toDouble()) {
                Utils.formatCompact(longVal)
            } else {
                val formatted = if (this * 10 == (this * 10).toLong().toDouble()) {
                    "%.1f".format(this)
                } else {
                    "%.2f".format(this)
                }
                formatted.trimEnd('0').trimEnd('.')
            }
        }
        fun Float.fmt(): String {
            val longVal = this.toLong()
            return if (this == longVal.toFloat()) {
                Utils.formatCompact(longVal)
            } else {
                val formatted = if (this * 10 == (this * 10).toLong().toFloat()) {
                    "%.1f".format(this)
                } else {
                    "%.2f".format(this)
                }
                formatted.trimEnd('0').trimEnd('.')
            }
        }
        fun Int.fmt() = Utils.formatCompact(this.toLong())

        val map = mutableMapOf<String, String>()
        if (lvl <= 0) return map

        when (nodeId) {
            "quick_forge" -> {
                val reward = if (lvl == 20) 30.0 else (10.0 + lvl * 0.5 + (lvl / 20) * 10)
                map["reward"] = reward.fmt()
            }
            "powder_buff" -> map["reward"] = lvl.fmt()
            "warm_heart" -> map["reward"] = (lvl * 0.4).fmt()
            "keep_it_cool" -> map["reward"] = (lvl * 0.4).fmt()
            "gem_lover" -> map["reward"] = (20 + lvl * 4).fmt()
            "mole" -> map["reward"] = (50 + (lvl - 1) * (350.0 / 199.0)).fmt()
            "vanguard_seeker" -> map["reward"] = lvl.fmt()
            "eager_adventurer" -> map["reward"] = (lvl * 4).fmt()
            "metal_head" -> map["reward"] = (lvl * 5).fmt()
            "efficient_miner" -> map["reward"] = (lvl * 3).fmt()
            "strong_arm" -> map["reward"] = (lvl * 5).fmt()
            "seasoned_mineman" -> map["reward"] = (5.0 + lvl * 0.1).fmt()
            "blockhead" -> map["reward"] = (lvl * 5).fmt()
            "surveyor" -> map["reward"] = (lvl * 0.75).fmt()
            "no_stone_unturned" -> map["reward"] = (lvl * 0.5).fmt()
            "mining_master" -> map["reward"] = (lvl * 0.1).fmt()
            "daily_grind" -> map["reward"] = (hotmLevel * 500).fmt()
            "speedy_mineman" -> map["reward"] = (lvl * 40).fmt()
            "rags_to_riches" -> map["reward"] = (lvl * 4).fmt()
            "mining_speed" -> map["reward"] = (lvl * 20).fmt()
            "mining_fortune" -> map["reward"] = (lvl * 2).fmt()
            "old_school" -> map["reward"] = (lvl * 5).fmt()
            "crystalline" -> map["reward"] = (lvl * 0.5).fmt()
            "professional" -> map["reward"] = (50 + lvl * 5).fmt()
            "titanium_insanium" -> map["reward"] = (2.0 + lvl * 0.1).fmt()
            "fortunate_mineman" -> map["reward"] = (lvl * 3).fmt()
            "luck_of_the_cave" -> map["reward"] = (5 + lvl).fmt()
            "gifts_from_the_departed" -> map["reward"] = (lvl * 0.2).fmt()
            "steady_hand" -> map["reward"] = (lvl * 0.1).fmt()
            "daily_powder" -> map["reward"] = (hotmLevel * 500).fmt()
            "dead_mans_chest" -> map["reward"] = lvl.fmt()

            "maniac_miner" -> {
                map["fortune"] = (lvl * 5).fmt()
                map["duration"] = (25 + effLvl * 5).fmt()
            }
            "anomalous_desire" -> {
                map["chance"] = (30 + effLvl * 10).fmt()
                map["cooldown"] = (120 - effLvl * 10).fmt()
            }
            "subterranean_fisher" -> {
                map["fishing_speed"] = (5 + lvl * 0.5).fmt()
                map["sea_creature_chance"] = (1 + lvl * 0.1).fmt()
            }
            "great_explorer" -> {
                map["chance"] = (20 + 4 * effLvl).fmt()
                map["locks_removed"] = (1 + lvl / 5).fmt()
            }
            "mining_speed_boost" -> {
                map["effect"] = (200 + effLvl * 50).fmt()
                map["duration"] = (10 + effLvl * 5).fmt()
            }
            "pickobulus" -> {
                map["reward"] = (60 - effLvl * 10).fmt()
            }
            "gemstone_infusion" -> {
                map["reward"] = (20 + effLvl * 5).fmt()
            }
            "sheer_force" -> {
                map["reward"] = (20 + effLvl * 5).fmt()
            }
            "lonesome_miner" -> {
                map["reward"] = (5.0 + effLvl * 0.5).fmt()
            }
        }

        return map
    }
}