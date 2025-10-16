package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.render.mapRender
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.CompatHelpers.UDrawContext
//#if MC > 1.21.5
import co.stellarskys.stella.events.GuiEvent
//#elseif MC == 1.8.9
//$$ import co.stellarskys.stella.events.RenderEvent
//#endif

@Stella.Module
object map: Feature("mapEnabled", area = "catacombs") {
    private const val name = "StellaNav"

    override fun initialize() {
        HUDManager.registerCustom(name, 148, 148, this::HUDEditorRender)

        register<GuiEvent.HUD> { event ->
            if (HUDManager.isEnabled(name)) RenderMap(event.context)
        }
    }

    fun HUDEditorRender(context: UDrawContext, x: Float, y: Float, width: Int, height: Int, scale: Float, partialTicks: Float, previewMode: Boolean){
        mapRender.renderPreview(context, x, y, scale)
    }

    fun RenderMap(context: UDrawContext) {
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        mapRender.render(context, x, y, scale)
    }
}