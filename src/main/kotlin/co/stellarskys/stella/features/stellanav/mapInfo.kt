package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.render.*
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import net.minecraft.client.gui.GuiGraphics

@Module
object mapInfo: Feature("separateMapInfo", island = SkyBlockIsland.THE_CATACOMBS) {
    const val name = "Map Info"

    override fun initialize() {
        HUDManager.registerCustom(name, 200, 30,this::HUDEditorRender, "separateMapInfo")

        on<GuiEvent.RenderHUD> { event -> RenderNormal(event.context) }
    }

    fun HUDEditorRender(context: GuiGraphics){
        RenderMapInfo(
            context,
            true
        )
    }

    fun RenderNormal(context: GuiGraphics) {
        val matrix = context.pose()

        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale, scale)

        RenderMapInfo(context, false)

        matrix.popMatrix()
    }

    fun RenderMapInfo(context: GuiGraphics, preview: Boolean) {
        val matrix = context.pose()

        var mapLine1 = Dungeon.mapLine1
        var mapLine2 = Dungeon.mapLine2

        if (preview) {
            mapLine1 = "§7Secrets: §b?§8-§e?§8-§c?        §7Score: §c0"
            mapLine2 = "§7Deaths: §a0 §8| §7M: §c✘ §8| §7P: §c✘ §8| §7Crypts: §c0"
        }
        val w1 = mapLine1.width()
        val w2 = mapLine2.width()

        matrix.pushMatrix()
        matrix.translate( 100f, 5f,)

        Render2D.drawString(context, mapLine1,-w1 / 2, 0)
        Render2D.drawString(context, mapLine2,-w2 / 2, 10)

        matrix.popMatrix()
    }
}