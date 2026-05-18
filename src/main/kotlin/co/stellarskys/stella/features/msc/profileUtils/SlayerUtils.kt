package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.hypixel.SkyblockResponse
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object SlayerUtils {
    data class SlayerLevel(val level: Int, val currentXp: Long, val nextLevelXp: Long, val progress: Float)

    enum class SlayerType(
        val apiName: String,
        val displayName: String,
        val icon: () -> ItemStack,
        val xpTable: LongArray
    ) {
        ZOMBIE("zombie", "Revenant Horror", { Items.ROTTEN_FLESH.defaultInstance }, longArrayOf(5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000)),
        SPIDER("spider", "Tarantula Broodfather", { Items.SPIDER_EYE.defaultInstance }, longArrayOf(5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000)),
        WOLF("wolf", "Sven Packmaster", { Items.BONE.defaultInstance }, longArrayOf(10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000)),
        ENDERMAN("enderman", "Voidgloom Seraph", { Items.ENDER_PEARL.defaultInstance }, longArrayOf(10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000)),
        BLAZE("blaze", "Inferno Demonlord", { Items.BLAZE_ROD.defaultInstance }, longArrayOf(10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000)),
        VAMPIRE("vampire", "Riftstalker Bloodfiend", { Items.GHAST_TEAR.defaultInstance }, longArrayOf(20, 75, 240, 840, 2400));

        val maxLevel: Int = xpTable.size

        fun getLevel(xp: Long): SlayerLevel {
            var level = 0
            
            for (i in xpTable.indices) {
                if (xp >= xpTable[i]) {
                    level = i + 1
                } else {
                    break
                }
            }
            
            val currentLevelXp = if (level == 0) 0L else xpTable[level - 1]
            val nextLevelXp = if (level >= xpTable.size) xpTable.last() else xpTable[level]
            
            val progress = if (level >= xpTable.size) 1.0f 
            else (xp - currentLevelXp).toFloat() / (nextLevelXp - currentLevelXp).toFloat()
            
            return SlayerLevel(level, xp, nextLevelXp, progress)
        }
    }

    fun getSlayerData(member: SkyblockResponse.SkyblockMember): Map<SlayerType, SlayerLevel> {
        return SlayerType.entries.associateWith { type ->
            val xp = member.slayer.bosses[type.apiName]?.xp ?: 0L
            type.getLevel(xp)
        }
    }

    fun getTotalXp(member: SkyblockResponse.SkyblockMember): Long {
        return SlayerType.entries.sumOf { member.slayer.bosses[it.apiName]?.xp ?: 0L }
    }

    fun getTotalLevel(member: SkyblockResponse.SkyblockMember): Int {
        return SlayerType.entries.sumOf { type ->
            val xp = member.slayer.bosses[type.apiName]?.xp ?: 0L
            type.getLevel(xp).level
        }
    }

    fun getTotalBosses(member: SkyblockResponse.SkyblockMember): Int {
        return SlayerType.entries.sumOf { member.slayer.bosses[it.apiName]?.totalKills ?: 0 }
    }
}
