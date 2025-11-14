package co.stellarskys.stella.utils.render

import co.stellarskys.stella.utils.clearCodes
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.SkullBlockEntity
import xyz.meowing.knit.api.KnitClient
import java.awt.Color
import java.util.Optional
import java.util.UUID


object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()
    private val mc = KnitClient.client

    fun drawImage(ctx: GuiGraphics, image: ResourceLocation, x: Int, y: Int, width: Int, height: Int) {
        ctx.blitSprite(RenderPipelines.GUI_TEXTURED, image, x, y, width, height)
    }

    fun drawTexture(
        ctx: GuiGraphics,
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
        ctx.blit(
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
    fun drawRect(ctx: GuiGraphics, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderPipelines.GUI, x, y, x + width, y + height, color.rgb)
    }

    @JvmOverloads
    fun drawString(ctx: GuiGraphics, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.pose()
        if (scale != 1f) {
            matrices.pushMatrix()
            matrices.scale(scale, scale)
        }

        ctx.drawString(
            mc.font,
            str.replace(formattingRegex, "${ChatFormatting.PREFIX_CODE}"),
            x,
            y,
            -1,
            shadow
        )

        if (scale != 1f) matrices.popMatrix()
    }

    fun renderItem(context: GuiGraphics, item: ItemStack, x: Float, y: Float, scale: Float) {
        context.pose().pushMatrix()
        context.pose().translate(x, y)
        context.pose().scale(scale, scale)

        context.renderItem(item, 0, 0)

        context.pose().popMatrix()
    }

    fun drawPlayerHead(context: GuiGraphics, x: Int, y: Int, size: Int, uuid: UUID) {
        val textures = SkullBlockEntity.fetchGameProfile(uuid)
            .getNow(Optional.empty())
            .map(mc.skinManager::getInsecureSkin)
            .orElseGet { DefaultPlayerSkin.get(uuid) }

        PlayerFaceRenderer.draw(context, textures, x, y, size)
    }

    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { mc.font.width(it.clearCodes()) }
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return mc.font.lineHeight * lineCount
    }
}