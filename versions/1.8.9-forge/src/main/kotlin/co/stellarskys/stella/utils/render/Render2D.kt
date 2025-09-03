package co.stellarskys.stella.utils.render

import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.utils.CompatHelpers.DrawContext
import co.stellarskys.stella.utils.clearCodes
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color

object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()

    fun drawImage(ctx: DrawContext, image: ResourceLocation, x: Int, y: Int, width: Int, height: Int) {
        mc.textureManager.bindTexture(image)

        val zLevel = 0f
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.worldRenderer

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        buffer.pos(x.toDouble(),         (y + height).toDouble(), zLevel.toDouble()).tex(0.0, 1.0).endVertex()
        buffer.pos((x + width).toDouble(), (y + height).toDouble(), zLevel.toDouble()).tex(1.0, 1.0).endVertex()
        buffer.pos((x + width).toDouble(), y.toDouble(),           zLevel.toDouble()).tex(1.0, 0.0).endVertex()
        buffer.pos(x.toDouble(),         y.toDouble(),           zLevel.toDouble()).tex(0.0, 0.0).endVertex()
        tessellator.draw()
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
}