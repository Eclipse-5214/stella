/*
package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.getClassColor
import co.stellarskys.stella.utils.Fonts
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.deftu.omnicore.api.client.client
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.state.PlayerRenderState
import net.minecraft.network.chat.Component
import org.joml.Matrix4f
import java.awt.Color

//#if MC > 1.21.9
//$$ import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
//#endif

@Module
object customNametag: Feature("customNametags") {
    override fun initialize() {
        on<RenderEvent.Entity.Nametag> { event ->
            val state = event.state as? PlayerRenderState ?: return@on

            //#if MC > 1.21.9
            //$$ val username = state.nameTag?.stripped ?: return@on
            //#else
            val username = state.name
            //#endif

            val nametag = event.text.toFlatList().find { it.string.contains(username) } ?: return@on
            val final = Component.literal(nametag.string).withStyle {
                it.withFont(Fonts.montserrat)
                    .withBold(nametag.style.isBold)
            }

            var color = nametag.style.color?.value ?: return@on

            if (Dungeon.floor != null) {
                val player = Dungeon.players.firstOrNull { it?.name == username }
                color = getClassColor(player?.dclass?.name).rgb
            }

            val rankBarColor = (0xFF shl 24) or (color and 0xFFFFFF)
            val pos = state.nameTagAttachment?: return@on
            val matrix = event.matrices
            val consumers = event.vertex ?: return@on
            val cam = client.gameRenderer.mainCamera
            val textRenderer = client.font

            event.cancel()

            matrix.pushPose()
            matrix.translate(pos.x, pos.y + 0.7, pos.z);
            matrix.mulPose(cam.rotation())

            val scale = 0.025f
            matrix.scale(scale, -scale, scale)

            val mstack = matrix.last().pose()
            val textWidth = textRenderer.width(final).toFloat()
            val xOffset = -textWidth / 2f //else 0f

            val topPadding = 4f    // Increase this to make the box taller at the top
            val bottomPadding = 4f // Increase this to make the box taller at the bottom
            val textHeight = 8f    // Standard Minecraft text height

            drawRect(
                mstack,
                consumers.getBuffer(RenderType.textBackgroundSeeThrough()),
                xOffset - 2f,             // Left (slightly more horizontal padding)
                -topPadding,              // Top (was -1f, now -4f)
                xOffset + textWidth + 2f, // Right
                textHeight + bottomPadding, // Bottom (was 9f, now 12f)
                0x66000000.toInt()
            )

            val lineY = textHeight + bottomPadding

            drawRect(
                mstack,
                consumers.getBuffer(RenderType.textBackgroundSeeThrough()),
                xOffset - 2f,
                lineY,
                xOffset + textWidth + 2f,
                lineY + 1.5f,
                rankBarColor
            )

            drawRect(
                mstack,
                consumers.getBuffer(RenderType.textBackground()),
                xOffset - 2f,
                lineY,
                xOffset + textWidth + 2f,
                lineY + 1.5f,
                rankBarColor
            )

            textRenderer.drawInBatch(
                final,
                xOffset,
                0f,
                Color.WHITE.rgb,
                false, // drop shadow
                mstack,
                consumers,
                Font.DisplayMode.SEE_THROUGH,
                0,
                event.light
            )

            textRenderer.drawInBatch(
                final,
                xOffset,
                0f,
                Color.white.rgb,
                false, // drop shadow
                mstack,
                consumers,
                Font.DisplayMode.NORMAL,
                0,
                15728880 // Full bright lightmap
            )

        matrix.popPose()
        }
    }

    fun drawRect(matrix: Matrix4f, consumers: VertexConsumer, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val a = (color shr 24 and 255).toFloat() / 255.0f
        val r = (color shr 16 and 255).toFloat() / 255.0f
        val g = (color shr 8 and 255).toFloat() / 255.0f
        val b = (color and 255).toFloat() / 255.0f

        consumers.addVertex(matrix, x1, y2, 0f).setColor(r, g, b, a).setLight(15728880)
        consumers.addVertex(matrix, x2, y2, 0f).setColor(r, g, b, a).setLight(15728880)
        consumers.addVertex(matrix, x2, y1, 0f).setColor(r, g, b, a).setLight(15728880)
        consumers.addVertex(matrix, x1, y1, 0f).setColor(r, g, b, a).setLight(15728880)
    }
}
 */