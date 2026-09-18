plugins {
    id("dev.kikugie.stonecutter")
    alias(libs.plugins.loom) apply false
}

stonecutter active "26.1"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["mod_id"] = "\"" + property("mod.id") + "\""
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        string(current.parsed >= "26.1") {
            replace("GuiGraphics", "GuiGraphicsExtractor")
        }

        string(current.parsed >= "26.3") {
            replace("com.mojang.blaze3d.buffers.GpuBuffer", "com.mojang.renderpearl.api.buffers.GpuBuffer")
            replace("com.mojang.blaze3d.buffers.GpuBufferSlice", "com.mojang.renderpearl.api.buffers.GpuBufferSlice")
            replace("com.mojang.blaze3d.pipeline.BlendFunction", "com.mojang.renderpearl.api.pipeline.BlendFunction")
            replace("com.mojang.blaze3d.pipeline.ColorTargetState", "com.mojang.renderpearl.api.pipeline.ColorTargetState")
            replace("com.mojang.blaze3d.pipeline.DepthStencilState","com.mojang.renderpearl.api.pipeline.DepthStencilState")
            replace("com.mojang.blaze3d.pipeline.RenderPipeline", "com.mojang.renderpearl.api.pipeline.RenderPipeline")
            replace("com.mojang.blaze3d.platform.BlendFactor", "com.mojang.renderpearl.api.pipeline.BlendFactor")
            replace("com.mojang.blaze3d.platform.BlendOp", "com.mojang.renderpearl.api.pipeline.BlendOp")
            replace("com.mojang.blaze3d.systems.GpuDevice", "com.mojang.renderpearl.api.device.GpuDevice")
            replace("com.mojang.blaze3d.systems.RenderPass", "com.mojang.renderpearl.api.commands.RenderPass")
            replace("com.mojang.blaze3d.systems.CommandEncoder", "com.mojang.renderpearl.api.commands.CommandEncoder")
            replace("com.mojang.blaze3d.textures.FilterMode", "com.mojang.renderpearl.api.textures.FilterMode")
            replace("com.mojang.blaze3d.textures.GpuTexture", "com.mojang.renderpearl.api.textures.GpuTexture")
            replace("com.mojang.blaze3d.textures.GpuTextureView", "com.mojang.renderpearl.api.textures.GpuTextureView")
            replace("com.mojang.blaze3d.textures.GpuSampler", "com.mojang.renderpearl.api.textures.GpuSampler")
            replace("com.mojang.blaze3d.textures.AddressMode", "com.mojang.renderpearl.api.textures.AddressMode")
            replace("com.mojang.blaze3d.vertex.VertexFormat", "com.mojang.renderpearl.api.vertex.VertexFormat")
            replace("com.mojang.blaze3d.GpuFormat", "com.mojang.renderpearl.api.GpuFormat")
            replace("com.mojang.blaze3d.PrimitiveTopology", "com.mojang.renderpearl.api.pipeline.PrimitiveTopology")
            replace("com.mojang.blaze3d.shaders.UniformType", "com.mojang.renderpearl.api.pipeline.UniformType")

        }
    }
}