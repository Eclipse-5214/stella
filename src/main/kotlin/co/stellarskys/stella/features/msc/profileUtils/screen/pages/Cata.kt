package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.handlers.Signal.color
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockItemsRepo
import java.util.Locale
import kotlin.collections.sumOf

class Cata(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
) : Page("catacombs stats", name, navigate) {
    override val icon: ItemStack = SkyBlockItemsRepo.getItemStackOrDefault("dungeon_chest_key")

    override fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        drawCatacombs(context, 10, 25)
        drawBossCollections(context, 10, 80)
        drawClassLevels(context, 120, 25)
        drawFloorLogs(context, 235, 25)
    }

    private fun drawLevelBox(context: GuiGraphics, x: Int, y: Int, w: Int, h: Int, icon: ItemStack?, textComp: MutableComponent, progress: Double, lvl: Int) {
        ren2d.drawHollowRect(context, x, y, w, h, 1, Palette.Purple)
        icon?.let { ren2d.renderItem(context, it, x + 3f, y + 6f, 1f) }

        val textX = x + if (icon != null) 21 else 9
        drawComp(context, textComp, textX, y + 4)

        ren2d.drawRect(context, textX, y + 16, 75, 5, Palette.Crust)
        val barColor = if (lvl >= 50) Palette.Sapphire else Palette.Green
        ren2d.drawRect(context, textX, y + 16, (75f * progress).toInt(), 5, barColor)
    }

    private fun drawCatacombs(context: GuiGraphics, x: Int, y: Int) {
        val normalData = member.dungeons.dungeonTypes.catacombs
        val level = calculateLevel(normalData.experience)
        val cataComp = Component.literal("§dCatacombs§7: §6${level.first}")
            .onHover("§bCatacombs\n§dXP§7: §6%,.3f".format(normalData.experience))

        ren2d.drawHollowRect(context, x, y, 105, 50, 1, Palette.Purple)
        drawComp(context, cataComp, x + 12, y + 6)
        ren2d.drawRect(context, x + 10, y + 19, 85, 5, Palette.Crust)
        val barColor = if (level.first >= 50) Palette.Sapphire else Palette.Green
        ren2d.drawRect(context, x + 10, y + 19, (85f * level.second).toInt(), 5, barColor)
        ren2d.drawString(context, "§bProgress§7: §e${(level.second * 100).toInt()}%", x + 12, y + 29)
    }

    private fun drawBossCollections(context: GuiGraphics, x: Int, y: Int) = with(member.dungeons.dungeonTypes) {
        ren2d.drawHollowRect(context, x, y, 105, 130, 1, Palette.Purple)
        ren2d.drawString(context, "§b§nBoss Collections", x + 5, y + 5)

        listOf("Bonzo", "Scarf", "Professor", "Thorn", "Livid", "Sadan", "Necron").forEachIndexed { i, name ->
            val total = (catacombs.tierComps[(i + 1).toString()]?.toInt() ?: 0) + ((mastermode.tierComps[(i + 1).toString()]?.toInt() ?: 0) * 2)
            ren2d.drawString(context, "§e$name§7: §f$total", x + 8, y + 20 + (i * 11))
        }
        ren2d.drawString(context, "§3Normal Runs§7: §a${(1..7).sumOf { catacombs.tierComps[it.toString()]?.toInt() ?: 0 }}", x + 5, y + 105)
        ren2d.drawString(context, "§cMaster Runs§7: §a${(1..7).sumOf { mastermode.tierComps[it.toString()]?.toInt() ?: 0 }}", x + 5, y + 116)
    }

    private fun drawClassLevels(context: GuiGraphics, x: Int, y: Int) {
        ren2d.drawHollowRect(context, x, y, 110, 185, 1, Palette.Purple)
        ren2d.drawString(context, "§b§nClass Levels", x + 5, y + 5)

        DungeonClass.Companion.valid.forEachIndexed { i, dClass ->
            val exp = member.dungeons.classes[dClass.name.lowercase(Locale.ROOT)]?.experience ?: 0.0
            val level = calculateLevel(exp)
            val comp = Component.literal(dClass.displayName).color(dClass.color?.rgb ?: -1).append("§7: §6${level.first}")
                .onHover(Component.literal(dClass.displayName).color(dClass.color?.rgb ?: -1).append("\n§dXP§7: §6%,.3f".format(exp)))

            drawLevelBox(context, x + 3, y + 18 + (i * 33), 104, 28, dClass.iconProvider(), comp, level.second, level.first)
        }
    }

    private fun drawFloorLogs(context: GuiGraphics, x: Int, y: Int) = with(member.dungeons) {
        val avgC = DungeonClass.Companion.valid.sumOf { Dungeon.calculateDungeonLevel(classes[it.name.lowercase(Locale.ROOT)]?.experience ?: 0.0) }.let { if (DungeonClass.Companion.valid.isNotEmpty()) it / DungeonClass.Companion.valid.size else 0.0 }

        ren2d.drawHollowRect(context, x, y, 105, 45, 1, Palette.Purple)
        ren2d.drawString(context, "§bSecrets§7: §e$secrets", x + 5, y + 6)
        ren2d.drawString(context, "§bAvg/Run§7: §a${"%.2f".format(averageSecrets)}", x + 5, y + 17)
        ren2d.drawString(context, "§bClass Avg§7: §a${"%.2f".format(avgC)}", x + 5, y + 28)

        ren2d.drawHollowRect(context, x, y + 50, 105, 135, 1, Palette.Purple)
        ren2d.drawString(context, "§b§nFloor Runs", x + 5, y + 55)

        (1..7).forEach { f ->
            val drawLog = { data: Any, pref: String, code: String, ox: Int ->
                val runs = dungeonTypes.let { if(pref == "F") it.catacombs else it.mastermode }.tierComps[f.toString()]?.toInt() ?: 0
                val pb = dungeonTypes.let { if(pref == "F") it.catacombs else it.mastermode }.fastestSPlus[f.toString()]?.toLong()?.toMMSS() ?: "§7None"
                drawComp(context, Component.literal("$code$pref$f§7: §f$runs").onHover("${if(pref == "F") "§bNormal" else "§cMaster"} Floor $f\n§dPersonal Best S+§7: $pb"), x + ox, y + 60 + (f * 15))
            }
            drawLog(dungeonTypes.catacombs, "F", "§3", 6)
            drawLog(dungeonTypes.mastermode, "M", "§c", 58)
        }
    }

    fun calculateLevel(xp: Double): Pair<Int, Double> = Dungeon.calculateDungeonLevel(xp).let { it.toInt() to (it - it.toInt()) }
    private fun Long.toMMSS(): String = if (this <= 0) "§7None" else "§a%d:%02d.%03d".format(this / 60000, (this % 60000) / 1000, this % 1000)
}