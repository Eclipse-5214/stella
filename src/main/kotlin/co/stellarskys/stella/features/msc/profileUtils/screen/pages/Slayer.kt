package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.features.msc.profileUtils.SlayerUtils
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import co.stellarskys.stella.features.msc.profileUtils.NetworthUtils.toReadable
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class Slayer(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
) : Page("slayer", name, navigate) {
    override val icon: ItemStack = Items.ZOMBIE_HEAD.defaultInstance

    private val slayerData = SlayerUtils.getSlayerData(member)
    private val totalXp = SlayerUtils.getTotalXp(member)

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        ren2d.drawString(context, "§d§nSlayer Statistics", 10, 23)
        drawSlayer(context, 10, 35, SlayerUtils.SlayerType.ZOMBIE, "§2", Items.ROTTEN_FLESH.defaultInstance)
        drawSlayer(context, 10, 95, SlayerUtils.SlayerType.WOLF, "§f", Items.BONE.defaultInstance)
        drawSlayer(context, 10, 156, SlayerUtils.SlayerType.BLAZE, "§6", Items.BLAZE_ROD.defaultInstance)
        drawSlayer(context, 120, 35, SlayerUtils.SlayerType.SPIDER, "§4", Items.SPIDER_EYE.defaultInstance)
        drawSlayer(context, 120, 95, SlayerUtils.SlayerType.ENDERMAN, "§5", Items.ENDER_PEARL.defaultInstance)
        drawSlayer(context, 120, 156, SlayerUtils.SlayerType.VAMPIRE, "§c", Items.GHAST_TEAR.defaultInstance)
        drawOverallStats(context, 230, 35)
    }

    private fun getKillsTooltip(type: SlayerUtils.SlayerType): String {
        val b = member.slayer.bosses[type.apiName]
        val t1 = "§7Tier 1: §f${"%,d".format(b?.t1Kills ?: 0)}"
        val t2 = "§7Tier 2: §f${"%,d".format(b?.t2Kills ?: 0)}"
        val t3 = "§7Tier 3: §f${"%,d".format(b?.t3Kills ?: 0)}"
        val t4 = "§7Tier 4: §f${"%,d".format(b?.t4Kills ?: 0)}"
        val t5 = if (type == SlayerUtils.SlayerType.VAMPIRE || (b?.t5Kills ?: 0) > 0) 
                  "§7Tier 5: §f${"%,d".format(b?.t5Kills ?: 0)}" else null
        
        val list = mutableListOf(t1, t2, t3, t4)
        if (t5 != null) list.add(t5)
        list.add("")
        list.add("§bTotal: §6${"%,d".format(b?.totalKills ?: 0)}")
        return list.joinToString("\n")
    }

    private fun drawSlayer(context: GuiGraphicsExtractor, x: Int, y: Int, type: SlayerUtils.SlayerType, color: String, icon: ItemStack) {
        val d = slayerData[type] ?: return
        val isMax = d.level == type.maxLevel
        val hover = getKillsTooltip(type)

        ren2d.drawHollowRect(context, x, y, 105, 54, 1, Palette.Purple)
        ren2d.renderItem(context, icon, x + 3f, y + 4f, 1f)

        val title = Component.literal("$color${type.displayName.split(" ").first()}§7: §6${d.level}").onHover(hover)
        drawComp(context, title, x + 25, y + 4)
        ren2d.drawRect(context, x + 25, y + 16, 75, 5, Palette.Crust)
        ren2d.drawRect(context, x + 25, y + 16, (75 * d.progress).toInt(), 5, if (isMax) Palette.Sapphire else Palette.Green)
        ren2d.drawString(context, "§7XP: §f${"%,d".format(d.currentXp)}", x + 5, y + 35, 1f)
    }

    private fun drawOverallStats(context: GuiGraphicsExtractor, x: Int, y: Int) {
        ren2d.drawHollowRect(context, x, y, 110, 175, 1, Palette.Purple)
        ren2d.drawString(context, "§b§nOverall Stats", x + 5, y + 5, 1f)
        ren2d.drawString(context, "§dTotal Slayer XP", x + 5, y + 23, 1f)
        ren2d.drawString(context, "§6${totalXp.toReadable()}", x + 10, y + 34, 1f)
        ren2d.drawString(context, "§dHighest Damage", x + 5, y + 51, 1f)
        ren2d.drawString(context, "§6${member.stats.highestDamage.toLong().toReadable()}", x + 10, y + 62, 1f)
        ren2d.drawString(context, "§dTotal Overall Kills", x + 5, y + 79, 1f)
        ren2d.drawString(context, "§6${"%,d".format(member.stats.totalKills.toLong())}", x + 10, y + 90, 1f)
        ren2d.drawString(context, "§dTotal Deaths", x + 5, y + 107, 1f)
        ren2d.drawString(context, "§c${"%,d".format(member.stats.totalDeaths.toLong())}", x + 10, y + 118, 1f)
        val kd = member.stats.totalKills / maxOf(1.0, member.stats.totalDeaths)
        ren2d.drawString(context, "§dK/D Ratio", x + 5, y + 135, 1f)
        ren2d.drawString(context, "§6${String.format("%.2f", kd)}", x + 10, y + 146, 1f)
    }
}
