package co.stellarskys.stella.api.nvg

import co.stellarskys.stella.api.zenith.Zenith
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix3x2f

object Batcher {
    private data class NVGRenderEntry(val scaled: Boolean, val snapshot: Matrix3x2f, val block: (Matrix3x2f) -> Unit)
    private val pendingRenders = mutableListOf<NVGRenderEntry>()

    fun flush(context: GuiGraphicsExtractor) {
        if (pendingRenders.isEmpty()) return
        val renders = pendingRenders.toList()
        pendingRenders.clear()
        NVGPIPRenderer.draw(context, 0, 0, context.guiWidth(), context.guiHeight()) {
            with(NVGRenderer) {
                val sf = Zenith.Res.scaleFactor.toFloat() / dpr
                for ((scaled, snapshot, block) in renders) {
                    push()
                    if (scaled) {
                        resetTransform()
                        setTransform(
                            snapshot.m00 * sf, snapshot.m01 * sf,
                            snapshot.m10 * sf, snapshot.m11 * sf,
                            snapshot.m20 * sf, snapshot.m21 * sf
                        )
                    }
                    block(snapshot)
                    pop()
                }
            }
        }
    }

    fun queue(context: GuiGraphicsExtractor, scaled: Boolean = true, block: (Matrix3x2f) -> Unit) {
        val snapshot = Matrix3x2f(context.pose())
        val entry = NVGRenderEntry(scaled, snapshot, block)
        pendingRenders.add(entry)
    }
}