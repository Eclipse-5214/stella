package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.Utils.getNormalized
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.StellaRenderLayers
import net.minecraft.client.renderer.ShapeRenderer
import java.awt.Color

@Module
object blockOverlay : Feature("overlayEnabled") {
    override fun initialize() {
        on<RenderEvent.World.BlockOutline> { event ->
            val blockPos = event.context.blockPos ?: return@on
            val mstack = event.context.matrixStack ?: return@on
            val consumers = event.context.consumers ?: return@on
            val camera = event.context.camera
            val camPos = camera.position
            val blockShape = event.context.voxelShape ?: return@on
            if (blockShape.isEmpty) return@on

            val outlineColor by config.property<Color>("blockHighlightColor")
            val outlineWidth by config.property<Int>("overlayLineWidth")
            val fillColor by config.property<Color>("blockFillColor")

            event.cancel()

            val x = blockPos.x - camPos.x
            val y = blockPos.y - camPos.y
            val z = blockPos.z - camPos.z

            ShapeRenderer.renderShape(
                mstack,
                consumers.getBuffer(StellaRenderLayers.getLines(outlineWidth.toDouble())),
                blockShape,
                x, y, z,
                outlineColor.rgb
            )

            if (config["fillBlockOverlay"] as Boolean) {
                val (r, g, b, a) = fillColor.getNormalized()

                blockShape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                    ShapeRenderer.addChainedFilledBoxVertices(
                        mstack,
                        consumers.getBuffer( StellaRenderLayers.FILLED),
                        x + minX, y + minY, z + minZ,
                        x + maxX, y + maxY, z + maxZ,
                        r,g,b,a
                    )
                }
            }
        }
    }
}