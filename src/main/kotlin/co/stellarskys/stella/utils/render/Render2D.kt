package co.stellarskys.stella.utils.render

import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.millis
import co.stellarskys.stella.api.nvg.Batcher
import co.stellarskys.stella.api.nvg.NVGRenderer
import co.stellarskys.stella.api.nvg.NVGPIPRenderer
import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.api.zenith.client
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f
import java.awt.Color
import java.util.Optional
import java.util.UUID
import tech.thatgravyboat.skyblockapi.platform.PlayerSkin
import tech.thatgravyboat.skyblockapi.platform.texture
import tech.thatgravyboat.skyblockapi.platform.textureUrl
import tech.thatgravyboat.skyblockapi.utils.extentions.stripColor

object Render2D {
    private val textureCache = mutableMapOf<UUID, PlayerSkin>()
    private var lastCacheClear = Chronos.zero
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()

    fun drawImage(ctx: GuiGraphicsExtractor, image: Identifier?, x: Int, y: Int, width: Int, height: Int) {
        if (image == null) return
        ctx.blit(RenderPipelines.GUI_TEXTURED, image, x, y, 0f, 0f, width, height, width, height, width, height)
    }

    @JvmOverloads
    fun drawRect(ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderPipelines.GUI, x, y, x + width, y + height, color.rgb)
    }

    @JvmOverloads
    fun drawString(ctx: GuiGraphicsExtractor, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.pose()
        if (scale != 1f) {
            matrices.pushMatrix()
            matrices.scale(scale, scale)
        }

        ctx.text(
            client.font,
            str.replace(formattingRegex, "${ChatFormatting.PREFIX_CODE}"),
            x,
            y,
            -1,
            shadow
        )

        if (scale != 1f) matrices.popMatrix()
    }

    @JvmOverloads
    fun drawString(ctx: GuiGraphicsExtractor, str: String, x: Int, y: Int, scale: Float = 1f, color: Color, shadow: Boolean = true) {
        val matrices = ctx.pose()
        if (scale != 1f) {
            matrices.pushMatrix()
            matrices.scale(scale, scale)
        }

        ctx.text(
            client.font,
            str.replace(formattingRegex, "${ChatFormatting.PREFIX_CODE}"),
            x,
            y,
            color.rgb,
            shadow
        )

        if (scale != 1f) matrices.popMatrix()
    }

    fun renderItem(context: GuiGraphicsExtractor, item: ItemStack, x: Float, y: Float, scale: Float) {
        context.pose().pushMatrix()
        context.pose().translate(x, y)
        context.pose().scale(scale, scale)
        context.item(item, 0, 0)
        context.pose().popMatrix()
    }

    fun drawPlayerHead(context: GuiGraphicsExtractor, x: Int, y: Int, size: Int, uuid: UUID) {
        if (lastCacheClear.since.millis > 300000L) {
            textureCache.clear()
            lastCacheClear = Chronos.now
        }

        val textures = textureCache.getOrElse(uuid) {
            val profile = client.connection?.getPlayerInfo(uuid)?.profile
            val skin = if (profile != null) { client.skinManager.get(profile).getNow(Optional.empty()).orElseGet { DefaultPlayerSkin.get(uuid) } } else { DefaultPlayerSkin.get(uuid) }
            val defaultSkin = DefaultPlayerSkin.get(uuid)
            if (skin.texture != defaultSkin.texture) textureCache[uuid] = skin
            skin
        }

        textures.textureUrl
        PlayerFaceExtractor.extractRenderState(context, textures, x, y, size)
    }


    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { client.font.width(it.stripColor()) }
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return client.font.lineHeight * lineCount
    }

    fun GuiGraphicsExtractor.drawNVG(scaled: Boolean = true, block: (snapshot: Matrix3x2f) -> Unit) {
        val snapshot = Matrix3x2f(this.pose())

        NVGPIPRenderer.draw(this, 0, 0, this.guiWidth(), this.guiHeight()) {
            val n = NVGRenderer
            val sf = Zenith.Res.scaleFactor.toFloat() / n.dpr

            if (scaled) {
                n.resetTransform()
                n.setTransform(
                    snapshot.m00 * sf, snapshot.m01 * sf,
                    snapshot.m10 * sf, snapshot.m11 * sf,
                    snapshot.m20 * sf, snapshot.m21 * sf
                )
            }

            block(snapshot)
        }
    }

    fun GuiGraphics.batchNVG(scaled: Boolean = true, block: (snapshot: Matrix3x2f) -> Unit) { Batcher.queue(this, scaled, block) }
    fun GuiGraphics.flushNVG() { Batcher.flush(this) }
}