package co.stellarskys.stella.utils.render.layers

/*
import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.TickUtils
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.render.BuiltBuffer
import net.minecraft.client.render.RenderLayer
import java.util.OptionalDouble
import java.util.OptionalInt

class ChromaRenderLayer(
    name: String, size: Int, hasCrumbling: Boolean, translucent: Boolean, pipeline: RenderPipeline, phases: MultiPhaseParameters,
) : RenderLayer.MultiPhase(name, size, hasCrumbling, translucent, pipeline, phases ) {

    override fun draw(buffer: BuiltBuffer) {
        val renderPipeline = this.pipeline
        this.startDrawing()

        try {
            val gpuBuffer = renderPipeline.vertexFormat.uploadImmediateVertexBuffer(buffer.buffer)
            val gpuBuffer2: GpuBuffer
            val indexType: VertexFormat.IndexType
            if (buffer.sortedBuffer == null) {
                val shapeIndexBuffer = RenderSystem.getSequentialBuffer(buffer.drawParameters.mode())
                gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(buffer.drawParameters.indexCount())
                indexType = shapeIndexBuffer.indexType
            } else {
                gpuBuffer2 = renderPipeline.vertexFormat.uploadImmediateIndexBuffer(buffer.sortedBuffer)
                indexType = buffer.drawParameters.comp_753()
            }

            val framebuffer = phases.target.get()
            val colorAttachment = framebuffer.colorAttachment
            val depthAttachment = if (framebuffer.useDepthAttachment) framebuffer.depthAttachment else null

            RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                colorAttachment, OptionalInt.empty(),
                depthAttachment, OptionalDouble.empty()
            ).use { renderPass ->
                // Set custom chroma uniforms
                val chromaSize: Float = 30f * (Stella.mc.window.scaledWidth / 100f)
                val ticks = TickUtils.getCurrentClientTick().toInt()
                val timeOffset: Float = ticks * (6f / 360f)
                val saturation = 0.75f
                val forwardDirection = 1

                renderPass.setUniform("chromaSize", chromaSize)
                renderPass.setUniform("timeOffset", timeOffset)
                renderPass.setUniform("saturation", saturation)
                renderPass.setUniform("forwardDirection", forwardDirection)

                renderPass.setPipeline(renderPipeline)
                renderPass.setVertexBuffer(0, gpuBuffer)

                if (RenderSystem.SCISSOR_STATE.isEnabled) {
                    renderPass.enableScissor(RenderSystem.SCISSOR_STATE)
                }

                for (i in 0..11) {
                    val gpuTexture = RenderSystem.getShaderTexture(i)
                    if (gpuTexture != null) {
                        renderPass.bindSampler("Sampler$i", gpuTexture)
                    }
                }

                renderPass.setIndexBuffer(gpuBuffer2, indexType)
                renderPass.drawIndexed(0, buffer.drawParameters.comp_753())
            }
        } catch (exception: Throwable) {
            try {
                buffer.close()
            } catch (exception2: Throwable) {
                exception.addSuppressed(exception2)
            }

            throw exception
        }

        buffer.close()
        this.endDrawing()
    }

}
*/