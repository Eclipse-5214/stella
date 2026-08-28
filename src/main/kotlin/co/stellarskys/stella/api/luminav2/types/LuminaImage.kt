package co.stellarskys.stella.api.luminav2.types

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.zenith.Zenith
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.resources.Identifier

open class LuminaImage(val native: NativeImage, val filterMode: FilterMode = FilterMode.NEAREST) {
    private var uploaded = false
    private var destroyed = false

    val width = native.width
    val height = native.height

    var texture: GpuTexture? = null
    var textureView: GpuTextureView? = null

    fun upload() {
        if (uploaded) return
        if (destroyed) throw IllegalStateException("Texture is dead, necromancy is not supported :(")

        val device = RenderSystem.getDevice()
        val usage = GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING
        val tex = device.createTexture({ "lumina_image" }, usage, GpuFormat.RGBA8_UNORM, width, height, 1, 1)

        textureView = device.createTextureView(tex)
        texture = tex

        device.createCommandEncoder().writeToTexture(tex, native)
        native.close()
        uploaded = true
    }

    fun destroy() {
        if (!uploaded) {
            native.close()
            destroyed = true
            return
        }

        texture?.close(); texture = null
        textureView?.close(); textureView = null
        uploaded = false
        destroyed = true
    }

    companion object {
        fun fromResource(path: String, filterMode: FilterMode = FilterMode.NEAREST): LuminaImage = fromResource(Identifier.fromNamespaceAndPath(Stella.NAMESPACE, path), filterMode)
        fun fromResource(resource: Identifier, filterMode: FilterMode = FilterMode.NEAREST): LuminaImage {
            val stream = Zenith.resourceManager.getResource(resource)
                .orElseThrow { RuntimeException("Image not found: $resource") }
                .open()

            return LuminaImage(NativeImage.read(stream), filterMode)
        }

        fun fromBytes(bytes: ByteArray, filterMode: FilterMode = FilterMode.NEAREST): LuminaImage = LuminaImage(NativeImage.read(bytes), filterMode)
    }
}