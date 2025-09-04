package co.stellarskys.stella.utils.render

import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.utils.CompatHelpers.DrawContext
import co.stellarskys.stella.utils.clearCodes
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color

object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()

    fun drawTexture(
        ctx: DrawContext,
        image: ResourceLocation,
        x: Int,
        y: Int,
        u: Float,
        v: Float,
        width: Int,
        height: Int,
        regionWidth: Int,
        regionHeight: Int,
        textureWidth: Int = 256,
        textureHeight: Int = 256
    ) {
        mc.textureManager.bindTexture(image)
        Gui.drawScaledCustomSizeModalRect(
            x, y,
            u, v,
            regionWidth, regionHeight,
            width, height,
            textureWidth.toFloat(), textureHeight.toFloat()
        )
    }


    fun drawImage(ctx: DrawContext, image: ResourceLocation, x: Int, y: Int, width: Int, height: Int) {
        mc.textureManager.bindTexture(image)
        Gui.drawScaledCustomSizeModalRect(
            x, y,
            0f, 0f,
            256, 256, // assuming full texture region
            width, height,
            256f, 256f
        )
    }



    fun drawRect(ctx: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        drawRect(x, y, width, height, color)
    }

    fun drawRect(x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        Gui.drawRect(x, y, x + width, y + height, color.rgb)
    }

    fun drawString(context: DrawContext, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        drawString(str, x, y, scale, shadow)
    }

    fun drawString(str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        if (scale != 1f) {
            GlStateManager.pushMatrix()
            GlStateManager.scale(scale, scale, 1f)
        }

        mc.fontRendererObj.drawString(
            str.replace(formattingRegex, "§"),
            x.toFloat(),
            y.toFloat(),
            -1,
            shadow
        )

        if (scale != 1f) GlStateManager.popMatrix()
    }


    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { mc.fontRendererObj.getStringWidth(it.clearCodes()) }
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return mc.fontRendererObj.FONT_HEIGHT * lineCount
    }

    fun ensureTextureLoaded(textureManager: TextureManager, location: ResourceLocation) {
        if (textureManager.getTexture(location) == null) {
            println("🔄 Loading texture: $location")
            textureManager.loadTexture(location, SimpleTexture(location))
        }
    }
}