package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.render.mapRender
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.skyblock.location.SkyBlockIsland
import net.minecraft.client.gui.DrawContext

@Module
object map: Feature("mapEnabled", island = SkyBlockIsland.THE_CATACOMBS) {
    private const val name = "StellaNav"

    override fun initialize() {
        HUDManager.registerCustom(name, 148, 148, this::HUDEditorRender)

        register<GuiEvent.RenderHUD> { event ->
            if (HUDManager.isEnabled(name)) RenderMap(event.context)
        }
    }

    fun HUDEditorRender(context: DrawContext, x: Float, y: Float, width: Int, height: Int, scale: Float, partialTicks: Float, previewMode: Boolean){
        mapRender.renderPreview(context, x, y, scale)
    }

    fun RenderMap(context: DrawContext) {
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        mapRender.render(context, x, y, scale)
    }
}