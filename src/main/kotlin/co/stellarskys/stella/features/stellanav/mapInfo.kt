package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.render.*
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.CompatHelpers.UDrawContext
//#if MC >= 1.21.5
import co.stellarskys.stella.events.GuiEvent
//#elseif MC == 1.8.9
//$$ import co.stellarskys.stella.events.RenderEvent
//#endif

@Stella.Module
object mapInfo: Feature("separateMapInfo", "catacombs") {
    const val name = "Map Info"

    override fun initialize() {
        HUDManager.registerCustom(name, 200, 30,this::HUDEditorRender)

        register<GuiEvent.HUD> { event -> if (HUDManager.isEnabled(name)) RenderNormal(event.context) }
    }

    fun HUDEditorRender(
        context: UDrawContext,
        x: Float, y: Float,
        width: Int, height: Int, scale: Float,
        partialTicks: Float, previewMode: Boolean
    ){
        val matrix = context.matrices

        matrix.pushMatrix()
        matrix.translate(x, y)

        RenderMapInfo(
            context,
            true
        )

        matrix.popMatrix()
    }

    fun RenderNormal(context: UDrawContext) {
        val matrix = context.matrices

        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale, scale)

        RenderMapInfo(context, false)

        matrix.popMatrix()
    }

    fun RenderMapInfo(context: UDrawContext, preview: Boolean) {
        val matrix = context.matrices

        var mapLine1 = Dungeon.mapLine1
        var mapLine2 = Dungeon.mapLine2

        if (preview) {
            mapLine1 = "§7Secrets: §b?    §7Crypts: §c0    §7Mimic: §c✘";
            mapLine2 = "§7Min Secrets: §b?    §7Deaths: §a0    §7Score: §c0";
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