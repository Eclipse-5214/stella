package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.render.mapRender
import co.stellarskys.stella.hud.HUDManager
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import net.minecraft.client.gui.GuiGraphics

@Module
object map: Feature("mapEnabled", island = SkyBlockIsland.THE_CATACOMBS) {
    private const val name = "StellaNav"

    override fun initialize() {
        HUDManager.registerCustom(name, 148, 148, this::HUDEditorRender)

        on<GuiEvent.RenderHUD> { event ->
            RenderMap(event.context)
        }
    }

    fun HUDEditorRender(context: GuiGraphics){
        mapRender.renderPreview(context, 5f, 5f, 1f)
    }

    fun RenderMap(context: GuiGraphics) {
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        mapRender.render(context, x, y, scale)
    }
}