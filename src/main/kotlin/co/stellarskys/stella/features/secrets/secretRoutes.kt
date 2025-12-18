package co.stellarskys.stella.features.secrets

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland


@Module
object secretRoutes: Feature("secretRoutes", island = SkyBlockIsland.THE_CATACOMBS) {
    val onlyRenderAfterClear by config.property<Boolean>("dungeonWaypoints.onlyRenderAfterClear")
    val stopRenderAfterGreen by config.property<Boolean>("dungeonWaypoints.stopRenderAfterGreen")
    val renderText by config.property<Boolean>("dungeonWaypoints.renderText")
    val textRenderDistance by config.property<Double>("dungeonWaypoints.textRenderDistance")
    val textScale by config.property<Double>("dungeonWaypoints.textScale")

    val startColor by config.property<RGBA>("dungeonWaypoints.startColor")
    val mineColor by config.property<RGBA>("dungeonWaypoints.mineColor")
    val superBoomColor by config.property<RGBA>("dungeonWaypoints.superboomColor")
    val etherWarpColor by config.property<RGBA>("dungeonWaypoints.etherwarpColor")
    val secretColor by config.property<RGBA>("dungeonWaypoints.secretColor")
    val batColor by config.property<RGBA>("dungeonWaypoints.batColor")
    val leverColor by config.property<RGBA>("dungeonWaypoints.leverColor")

}
