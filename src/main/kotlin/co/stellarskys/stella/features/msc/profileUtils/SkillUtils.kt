package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.handlers.Quasar
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import com.google.gson.JsonObject
import java.util.Locale

object SkillUtils {
    private const val BASE_URL = "https://api.hypixel.net/v2/resources/skyblock/skills"

    @Volatile
    private var skillRegistry: Map<String, ApiSkillData> = emptyMap()

    data class ApiSkillData(val maxLevel: Int, val levels: Map<Int, Long>)

    fun load() {
        Quasar.fetch<JsonObject>(BASE_URL) { result ->
            skillRegistry = result.getOrNull()?.getAsJsonObject("skills")?.entrySet()?.associate { (key, value) ->
                val obj = value.asJsonObject
                val levelsMap = mutableMapOf(0 to 0L)
                obj.getAsJsonArray("levels").forEach {
                    val lvl = it.asJsonObject
                    levelsMap[lvl["level"].asInt] = lvl["totalExpRequired"].asLong
                }

                key.lowercase(Locale.ROOT) to ApiSkillData(obj["maxLevel"].asInt, levelsMap)
            } ?: emptyMap()
        }
    }

    fun getSkillLevel(skillName: String, totalXp: Double, member: SkyblockResponse.SkyblockMember, includeProgress: Boolean = true): Double {
        val cleanName = skillName.lowercase(Locale.ROOT).replace("skill_", "")
        val skill = skillRegistry[cleanName] ?: return 0.0
        val cap = getEffectiveCap(cleanName, member)

        val current = skill.levels.entries.lastOrNull { it.value < totalXp }
        val currentLevel = current?.key ?: 0

        if (currentLevel >= cap) return cap.toDouble()
        if (!includeProgress) return currentLevel.toDouble()

        val currentXpBoundary = current?.value ?: 0L
        val nextXp = skill.levels[currentLevel + 1] ?: return currentLevel.toDouble()
        val diff = nextXp - currentXpBoundary

        return if (diff <= 0) currentLevel.toDouble() else currentLevel + ((totalXp - currentXpBoundary) / diff)
    }

    fun getProgressToNextLevel(skillName: String, totalXp: Double, member: SkyblockResponse.SkyblockMember): Float {
        val cleanName = skillName.lowercase(Locale.ROOT).replace("skill_", "")
        val skill = skillRegistry[cleanName] ?: return 0.0f
        val cap = getEffectiveCap(cleanName, member)

        val current = skill.levels.entries.lastOrNull { it.value < totalXp }
        val currentLevel = current?.key ?: 0
        if (currentLevel >= cap) return 1.0f

        val currentXpBoundary = current?.value ?: 0L
        val nextXp = skill.levels[currentLevel + 1] ?: return 0.0f
        val diff = nextXp - currentXpBoundary
        return if (diff <= 0) 0.0f else ((totalXp - currentXpBoundary) / diff).toFloat()
    }

    fun getSkillCap(skillName: String) = skillRegistry[skillName.lowercase(Locale.ROOT).replace("skill_", "")]?.maxLevel ?: -1

    fun getEffectiveCap(skillName: String, member: SkyblockResponse.SkyblockMember): Int {
        return when (skillName) {
            "taming" -> member.petsData.petCare.petTypesSacrificed.size + 50
            "farming" -> (member.playerData.perks["farming_level_cap"] ?: 0) + 50
            else -> getSkillCap(skillName)
        }
    }

    private fun getValidSkills(playerData: SkyblockResponse.PlayerData) = playerData.experience.entries.filter { (k, _) ->
        val name = k.lowercase(Locale.ROOT).substringAfter("skill_")
        name != "social" && name != "dungeoneering" && name != "runecrafting" && getSkillCap(name) != -1
    }

    fun getSkillAverage(member: SkyblockResponse.SkyblockMember, includeProgress: Boolean = true): Double {
        val valid = getValidSkills(member.playerData)
        if (valid.isEmpty()) return 0.0
        return valid.sumOf { getSkillLevel(it.key, it.value, member, includeProgress) } / valid.size
    }

    fun getCappedSkillAverage(member: SkyblockResponse.SkyblockMember, includeProgress: Boolean = true): Double {
        val valid = getValidSkills(member.playerData)
        if (valid.isEmpty()) return 0.0
        return valid.sumOf { (key, exp) ->
            val cleanName = key.lowercase(Locale.ROOT).substringAfter("skill_")
            getSkillLevel(cleanName, exp, member, includeProgress).coerceAtMost(getEffectiveCap(cleanName, member).toDouble())
        } / valid.size
    }
}