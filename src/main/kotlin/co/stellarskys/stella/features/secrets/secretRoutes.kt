package co.stellarskys.stella.features.secrets

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.secrets.utils.RouteRecorder
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object secretRoutes: Feature("secretRoutes", island = SkyBlockIsland.THE_CATACOMBS) {
    val onlyRenderAfterClear by config.property<Boolean>("secretRoutes.onlyRenderAfterClear")
    val stopRenderAfterGreen by config.property<Boolean>("secretRoutes.stopRenderAfterGreen")
    val renderText by config.property<Boolean>("secretRoutes.renderText")
    val textRenderDistance by config.property<Double>("secretRoutes.textRenderDistance")
    val textScale by config.property<Double>("secretRoutes.textScale")

    val startColor by config.property<RGBA>("secretRoutes.startColor")
    val mineColor by config.property<RGBA>("secretRoutes.mineColor")
    val superBoomColor by config.property<RGBA>("secretRoutes.superboomColor")
    val etherWarpColor by config.property<RGBA>("secretRoutes.etherwarpColor")
    val secretColor by config.property<RGBA>("secretRoutes.secretColor")
    val batColor by config.property<RGBA>("secretRoutes.batColor")
    val leverColor by config.property<RGBA>("secretRoutes.leverColor")

    val recordingHud by config.property<Boolean>("secretRoutes.recordingHud")
    val rHudName = "rhud"

    init {
        HUDManager.registerCustom(rHudName, 100, 110, RouteRecorder::hudPreview, "secretRoutes.recordingHud")
        on<GuiEvent.RenderHUD> { if (recordingHud) RouteRecorder.hud(it.context) }
    }

}
