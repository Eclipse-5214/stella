package co.stellarskys.stella.utils.render

import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.features.stellanav.utils.prevewMap
import co.stellarskys.stella.utils.clearCodes
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import java.awt.Color


object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()

    fun drawImage(ctx: DrawContext, image: Identifier, x: Int, y: Int, width: Int, height: Int) {
        ctx.drawGuiTexture(RenderLayer::getGuiTextured, image, x, y, width, height)
    }

    @JvmOverloads
    fun drawRect(ctx: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderLayer.getGui(), x, y, x + width, y + height, color.rgb)
    }

    @JvmOverloads
    fun drawString(ctx: DrawContext, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.matrices
        if (scale != 1f) {
            matrices.push()
            matrices.scale(scale, scale, 1f)
        }

        ctx.drawText(
            mc.textRenderer,
            str.replace(formattingRegex, "${Formatting.FORMATTING_CODE_PREFIX}"),
            x,
            y,
            -1,
            shadow
        )

        if (scale != 1f) matrices.pop()
    }


    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { mc.textRenderer.getWidth(it.clearCodes()) }
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return mc.textRenderer.fontHeight * lineCount
    }
}

fun MatrixStack.pushMatrix() = this.push()
fun MatrixStack.popMatrix() = this.pop()