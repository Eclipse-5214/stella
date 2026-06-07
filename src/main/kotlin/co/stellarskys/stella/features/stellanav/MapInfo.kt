package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.features.stellanav.render.MapRenderer
import net.minecraft.client.gui.GuiGraphicsExtractor
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.platform.pushPop

@Module
object MapInfo: Feature("separateMapInfo", island = SkyBlockIsland.THE_CATACOMBS) {
    const val name = "Map Info"

    override fun initialize() {
        HUDManager.registerCustom(name, 200, 30,this::hudEditorRender, "separateMapInfo")
        on<GuiEvent.RenderHUD> { event -> renderNormal(event.context) }
    }

    fun hudEditorRender(context: GuiGraphicsExtractor) {
        renderMapInfo(context, true)
    }

    fun renderNormal(context: GuiGraphicsExtractor) = HUDManager.renderHud(name, context) {
        renderMapInfo(context, false)
    }

    fun renderMapInfo(context: GuiGraphicsExtractor, preview: Boolean) = context.pushPop {
        context.pose().translate(100f, 5f)
        MapRenderer.renderStats(context, preview, 0f, 0f)
    }
}