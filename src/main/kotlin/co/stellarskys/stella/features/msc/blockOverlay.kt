package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.render.StellaRenderLayers
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.shapes.CollisionContext

@Module
object blockOverlay : Feature("overlayEnabled") {
    override fun initialize() {
        register<RenderEvent.World.BlockOutline> { event ->
            val blockPos = event.blockPos ?: return@register
            val mstack = event.context.matrixStack() ?: return@register
            val consumers = event.context.consumers() ?: return@register
            val camera = event.context.camera()
            val camPos = camera.position
            val blockShape = event.blockShape ?: return@register
            if (blockShape.isEmpty) return@register

            val outlineColor = config["blockHighlightColor"] as RGBA
            val outlineWidth = (config["overlayLineWidth"] as Int).toDouble()
            val fillColor = config["blockFillColor"] as RGBA

            event.cancel()

            val x = blockPos.x - camPos.x
            val y = blockPos.y - camPos.y
            val z = blockPos.z - camPos.z

            ShapeRenderer.renderShape(
                mstack,
                consumers.getBuffer(StellaRenderLayers.getLines(outlineWidth)),
                blockShape,
                x, y, z,
                outlineColor.toColorInt()
            )

            if (config["fillBlockOverlay"] as Boolean) {
                val (rRaw, gRaw, bRaw, aRaw) = fillColor
                val r = rRaw / 255f
                val g = gRaw / 255f
                val b = bRaw / 255f
                val a = aRaw / 255f

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