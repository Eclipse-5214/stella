package co.stellarskys.stella.utils.render

import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.features.stellanav.utils.prevewMap
import co.stellarskys.stella.utils.clearCodes
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import java.awt.Color


object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()

    fun drawImage(ctx: DrawContext, image: Identifier, x: Int, y: Int, width: Int, height: Int) {
        ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, image, x, y, width, height)
    }

    fun drawTexture(
        ctx: DrawContext,
        image: Identifier,
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
        ctx.drawTexture(
            RenderPipelines.GUI_TEXTURED, // or your custom layer provider
            image,
            x,
            y,
            u,
            v,
            width,
            height,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight
        )
    }


    @JvmOverloads
    fun drawRect(ctx: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderPipelines.GUI, x, y, x + width, y + height, color.rgb)
    }

    @JvmOverloads
    fun drawString(ctx: DrawContext, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.matrices
        if (scale != 1f) {
            matrices.pushMatrix()
            matrices.scale(scale, scale)
        }

        ctx.drawText(
            mc.textRenderer,
            str.replace(formattingRegex, "${Formatting.FORMATTING_CODE_PREFIX}"),
            x,
            y,
            -1,
            shadow
        )

        if (scale != 1f) matrices.popMatrix()
    }

    fun renderItem(context: DrawContext, item: ItemStack, x: Float, y: Float, scale: Float) {
        context.matrices.pushMatrix()
        context.matrices.translate(x, y)
        context.matrices.scale(scale, scale)

        context.drawItem(item, 0, 0)

        context.matrices.popMatrix()
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