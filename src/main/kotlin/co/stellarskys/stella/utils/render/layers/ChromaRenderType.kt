package co.stellarskys.stella.utils.render.layers

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderType.CompositeRenderType
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*
import java.util.function.Supplier


/**
 * Chroma related codes adapted from SkyHanni under LGPL-2.1 license
 * @link [github.com/hannibal002/SkyHanni/blob/beta/LICENSE](https://github.com/hannibal002/SkyHanni/blob/beta/LICENSE)
 * @author hannibal2
 */
class ChromaRenderType(
    name: String?,
    bufferSize: Int,
    affectsCrumbling: Boolean,
    sortOnUpload: Boolean,
    renderPipeline: RenderPipeline?,
    state: CompositeState
) : CompositeRenderType(name, bufferSize, affectsCrumbling, sortOnUpload, renderPipeline, state) {
    override fun draw(meshData: MeshData) {
        val renderPipeline: RenderPipeline = this.renderPipeline
        this.setupRenderState()

        val dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(),
            Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
            //#if MC < 1.21.9
            RenderSystem.getModelOffset(),
            //#else
            //$$ Vector3f(),
            //#endif
            RenderSystem.getTextureMatrix(),
            RenderSystem.getShaderLineWidth()
        )

        if (GuiRendererHook.chromaBufferSlice == null) {
            GuiRendererHook.computeChromaBufferSlice()
        }

        try {
            val gpuBuffer = renderPipeline.vertexFormat.uploadImmediateVertexBuffer(meshData.vertexBuffer())
            val gpuBuffer2: GpuBuffer?
            val indexType: VertexFormat.IndexType?
            if (meshData.indexBuffer() == null) {
                val shapeIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode())
                gpuBuffer2 = shapeIndexBuffer.getBuffer(meshData.drawState().indexCount())
                indexType = shapeIndexBuffer.type()
            } else {
                gpuBuffer2 = renderPipeline.vertexFormat.uploadImmediateIndexBuffer(meshData.indexBuffer())
                indexType = meshData.drawState().indexType()
            }

            val framebuffer = this.state.outputState.renderTarget
            val colorAttachment = framebuffer.getColorTextureView()
            val depthAttachment = if (framebuffer.useDepth) framebuffer.getDepthTextureView() else null

            RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                Supplier { "Stella Immediate Chroma Pipeline Draw" },
                colorAttachment, OptionalInt.empty(),
                depthAttachment, OptionalDouble.empty()
            ).use { renderPass ->
                RenderSystem.bindDefaultUniforms(renderPass)
                renderPass.setUniform("DynamicTransforms", dynamicTransforms)
                renderPass.setUniform("ChromaUniforms", GuiRendererHook.chromaBufferSlice)

                renderPass.setPipeline(renderPipeline)
                renderPass.setVertexBuffer(0, gpuBuffer)

                val scissorState = RenderSystem.getScissorStateForRenderTypeDraws()
                if (scissorState.enabled()) {
                    scissorState.enable(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height())
                }

                for (i in 0..11) {
                    val gpuTextureView = RenderSystem.getShaderTexture(i)
                    if (gpuTextureView != null) {
                        renderPass.bindSampler("Sampler$i", gpuTextureView)
                    }
                }

                renderPass.setIndexBuffer(gpuBuffer2, indexType)
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1)
            }
        } catch (ex: Throwable) {
            try {
                meshData.close()
            } catch (ex2: Throwable) {
                ex.addSuppressed(ex2)
            }

            throw ex
        }

        meshData.close()
        this.clearRenderState()
    }
}