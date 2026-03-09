package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.WorldUtils.toVec3
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.RenderQue
import java.awt.Color

@Module
object blockOverlay : Feature("overlayEnabled") {
    val outlineColor by config.property<Color>("blockHighlightColor")
    val outlineWidth by config.property<Int>("overlayLineWidth")
    val fillColor by config.property<Color>("blockFillColor")
    val fill by config.property<Boolean>("fillBlockOverlay")

    override fun initialize() {
        on<RenderEvent.World.BlockOutline> { event ->
            val blockPos = event.blockPos.toVec3()
            val blockShape = event.voxelShape
            if (blockShape.isEmpty) return@on
            event.cancel()

            RenderQue.queueVoxelOutline(
                blockShape,
                blockPos,
                outlineColor,
                true,
                outlineWidth.toFloat()
            )

            if (fill) {
                RenderQue.queueVoxelFill(
                    blockShape,
                    blockPos,
                    fillColor,
                    true
                )
            }
        }
    }
}