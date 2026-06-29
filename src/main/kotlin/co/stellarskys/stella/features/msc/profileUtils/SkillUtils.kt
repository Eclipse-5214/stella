package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.handlers.Quasar
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.utils.config
import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.Locale

object SkillUtils {
    private const val BASE_URL = "https://api.hypixel.net/v2/resources/skyblock/skills"
    private val overflow by config.property<Boolean>("profileViewer.overflow")

    @Volatile
    private var skillRegistry: Map<SkillType, ApiSkillData> = emptyMap()

    data class ApiSkillData(val maxLevel: Int, val levels: Map<Int, Long>)
    data class Skill(val level: Double, val progress: Float, val cap: Int, val xp: Double, val progressXp: Double = 0.0, val nextLevelNeededXp: Double = 0.0, val remainingXp: Double = 0.0)

    enum class SkillType(val apiName: String, val displayName: String, val icon: () -> ItemStack, val isCosmetic: Boolean = false) {
        FARMING("farming", "Farming", { Items.WHEAT.defaultInstance }),
        MINING("mining", "Mining", { Items.DIAMOND_PICKAXE.defaultInstance }),
        COMBAT("combat", "Combat", { Items.DIAMOND_SWORD.defaultInstance }),
        FORAGING("foraging", "Foraging", { Items.OAK_LOG.defaultInstance }),
        FISHING("fishing", "Fishing", { Items.FISHING_ROD.defaultInstance }),
        ENCHANTING("enchanting", "Enchanting", { Items.ENCHANTING_TABLE.defaultInstance }),
        ALCHEMY("alchemy", "Alchemy", { Items.BREWING_STAND.defaultInstance }),
        TAMING("taming", "Taming", { Items.BONE.defaultInstance }),
        CARPENTRY("carpentry", "Carpentry", { Items.CRAFTING_TABLE.defaultInstance }),
        HUNTING("hunting", "Hunting", { Items.LEAD.defaultInstance }),
        RUNECRAFTING("runecrafting", "Runecraft", { Items.MAGMA_CREAM.defaultInstance }, isCosmetic = true),
        SOCIAL("social", "Social", { Items.EMERALD.defaultInstance }, isCosmetic = true);

        val apiKey: String = "SKILL_${name.uppercase(Locale.ROOT)}"

        companion object {
            private val apiMap = entries.associateBy { it.apiName }
            private val keyMap = entries.associateBy { it.apiKey.lowercase(Locale.ROOT) }

            fun fromString(name: String): SkillType? {
                val clean = name.lowercase(Locale.ROOT)
                return apiMap[clean] ?: keyMap[clean]
            }
        }
    }

    fun load() {
        Quasar.fetch<JsonObject>(BASE_URL) { result ->
            skillRegistry = result.getOrNull()?.getAsJsonObject("skills")?.entrySet()?.mapNotNull { (key, value) ->
                val skillEnum = SkillType.fromString(key) ?: return@mapNotNull null
                val obj = value.asJsonObject
                val levelsMap = mutableMapOf(0 to 0L)

                obj.getAsJsonArray("levels").forEach {
                    val lvl = it.asJsonObject
                    levelsMap[lvl["level"].asInt] = lvl["totalExpRequired"].asLong
                }

                skillEnum to ApiSkillData(obj["maxLevel"].asInt, levelsMap)
            }?.toMap() ?: emptyMap()
        }
    }

    fun getSkillLevel(skillType: SkillType, member: SkyblockResponse.SkyblockMember, includeProgress: Boolean = true): Double {
        val skill = skillRegistry[skillType] ?: return 0.0
        val cap = getEffectiveCap(skillType, member)
        val totalXp = member.playerData.experience[skillType.apiKey] ?: 0.0
        val current = skill.levels.entries.lastOrNull { it.value < totalXp }
        val currentLevel = current?.key ?: 0

        if (currentLevel >= cap) return cap.toDouble()
        if (!includeProgress) return currentLevel.toDouble()

        val currentXpBoundary = current?.value ?: 0L
        val nextXp = skill.levels[currentLevel + 1] ?: return currentLevel.toDouble()
        val diff = nextXp - currentXpBoundary

        return if (diff <= 0) currentLevel.toDouble() else currentLevel + ((totalXp - currentXpBoundary) / diff)
    }

    fun getProgressToNextLevel(skillType: SkillType, member: SkyblockResponse.SkyblockMember): Float {
        val skill = skillRegistry[skillType] ?: return 0.0f
        val cap = getEffectiveCap(skillType, member)
        val totalXp = member.playerData.experience[skillType.apiKey] ?: 0.0

        val current = skill.levels.entries.lastOrNull { it.value < totalXp }
        val currentLevel = current?.key ?: 0
        if (currentLevel >= cap) return 1.0f

        val currentXpBoundary = current?.value ?: 0L
        val nextXp = skill.levels[currentLevel + 1] ?: return 0.0f
        val diff = nextXp - currentXpBoundary
        return if (diff <= 0) 0.0f else ((totalXp - currentXpBoundary) / diff).toFloat()
    }

    fun getSkillCap(skillType: SkillType) = skillRegistry[skillType]?.maxLevel ?: -1

    fun getEffectiveCap(skillType: SkillType, member: SkyblockResponse.SkyblockMember): Int {
        return when (skillType) {
            SkillType.TAMING -> member.petsData.petCare.petTypesSacrificed.size + 50
            SkillType.FARMING -> (member.playerData.perks["farming_level_cap"] ?: 0) + 50
            else -> getSkillCap(skillType)
        }
    }

    private fun getValidSkills(playerData: SkyblockResponse.PlayerData): List<SkillType> {
        return playerData.experience.keys.mapNotNull { key ->
            val type = SkillType.fromString(key) ?: return@mapNotNull null
            if (type.isCosmetic) null else type
        }
    }

    fun getSkillAverage(member: SkyblockResponse.SkyblockMember, includeProgress: Boolean = true): Double {
        val valid = getValidSkills(member.playerData)
        if (valid.isEmpty()) return 0.0
        return valid.sumOf { if (overflow) getOverflowSkill(it, member, includeProgress).level else getSkillLevel(it, member, includeProgress) } / valid.size
    }

    fun getCappedSkillAverage(member: SkyblockResponse.SkyblockMember, includeProgress: Boolean = true): Double {
        val valid = getValidSkills(member.playerData)
        if (valid.isEmpty()) return 0.0
        return valid.sumOf { type ->
            if (overflow) getOverflowSkill(type, member, includeProgress).level
            else getSkillLevel(type, member, includeProgress).coerceAtMost(getEffectiveCap(type, member).toDouble())
        } / valid.size
    }

    fun getSkill(skillType: SkillType, member: SkyblockResponse.SkyblockMember) = if (overflow) getOverflowSkill(skillType, member) else getNormalSkill(skillType, member)

    private fun getNormalSkill(skillType: SkillType, member: SkyblockResponse.SkyblockMember): Skill {
        val lvl = getSkillLevel(skillType, member, includeProgress = true)
        val prog = getProgressToNextLevel(skillType, member)
        return Skill(lvl, prog, getEffectiveCap(skillType, member),member.playerData.experience[skillType.apiKey] ?: 0.0)
    }

    private val levelingExp = listOf(
        50L, 125L, 200L, 300L, 500L, 750L, 1000L, 1500L, 2000L, 3500L,
        5000L, 7500L, 10000L, 15000L, 20000L, 30000L, 50000L, 75000L, 100000L, 200000L,
        300000L, 400000L, 500000L, 600000L, 700000L, 800000L, 900000L, 1000000L, 1100000L, 1200000L,
        1300000L, 1400000L, 1500000L, 1600000L, 1700000L, 1800000L, 1900000L, 2000000L, 2100000L, 2200000L,
        2300000L, 2400000L, 2500000L, 2600000L, 2750000L, 2900000L, 3100000L, 3400000L, 3700000L, 4000000L,
        4300000L, 4600000L, 4900000L, 5200000L, 5500000L, 5800000L, 6100000L, 6400000L, 6700000L, 7000000L
    )

    private fun getOverflowSkill(skillType: SkillType, member: SkyblockResponse.SkyblockMember, includeProgress: Boolean = true): Skill {
        val totalXp = member.playerData.experience[skillType.apiKey] ?: 0.0
        val skillData = skillRegistry[skillType] ?: return Skill(0.0, 0f, 0, totalXp)
        val naturalCap = skillData.maxLevel

        var level = 0
        var xpRemaining = totalXp

        while (level < 60) {
            val nextLevel = level + 1
            val cost = if (nextLevel <= naturalCap) {
                val currentTotal = skillData.levels[level] ?: 0L
                val nextTotal = skillData.levels[nextLevel] ?: Long.MAX_VALUE
                (nextTotal - currentTotal).toDouble()
            } else {
                levelingExp.getOrNull(nextLevel - 1)?.toDouble() ?: Double.MAX_VALUE
            }

            if (xpRemaining < cost) {
                val progress = if (cost <= 0) 0f else (xpRemaining / cost).toFloat()
                val finalLevel = if (includeProgress) level + progress.toDouble() else level.toDouble()
                return Skill(finalLevel, progress, naturalCap, totalXp)
            }
            xpRemaining -= cost
            level++
        }

        var slope = 600_000L
        var cost = 7_000_000L + slope

        while (xpRemaining >= cost) {
            xpRemaining -= cost
            level++
            if (level % 10 == 0) slope *= 2
            cost += slope
        }

        val progress = if (cost <= 0) 0f else (xpRemaining / cost).toFloat()
        val finalLevel = if (includeProgress) level + progress.toDouble() else level.toDouble()
        return Skill(finalLevel, progress, naturalCap, totalXp)
    }
}