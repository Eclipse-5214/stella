package co.stellarskys.stella.features.stellanav.render

import co.stellarskys.stella.features.stellanav.Map
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.players.DungeonPlayer
import co.stellarskys.stella.api.zenith.player
import net.minecraft.client.gui.GuiGraphicsExtractor
import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.platform.scale
import java.util.UUID
import kotlin.math.PI

object MapRenderer {
    private const val MAP_W = 138
    private const val MAP_H = 138

    fun render(context: GuiGraphicsExtractor, x: Float, y: Float, scale: Float) {
        context.pushPop {
            val matrix = context.pose()
            matrix.translate(x, y)
            matrix.scale(scale, scale)
            matrix.translate(5f, 5f)

            when {
                Dungeon.inBoss && Map.bossMapEnabled && !Dungeon.complete -> MapMode.BOSS
                Map.scoreMapEnabled && Dungeon.complete -> MapMode.SCORE
                Dungeon.inBoss && Map.hideInBoss -> null
                else -> MapMode.CLEAR
            }?.let { mode ->
                renderBackground(context)
                mode.renderer(context)
                if (Map.mapInfoUnder) renderInfoUnder(context, false)
                if (Map.mapBorder) renderBorder(context)
            }
        }
    }

    fun renderPreview(context: GuiGraphicsExtractor, x: Float, y: Float, scale: Float) = context.pushPop {
        context.pose().translate(x, y)
        context.scale(scale, scale)

        renderBackground(context)
        Render2D.drawImage(context, Map.DEFAULT_MAP, 5, 5, 128, 128)

        if (Map.mapInfoUnder) renderInfoUnder(context, true)
        if (Map.mapBorder) renderBorder(context)
    }


    fun renderInfoUnder(context: GuiGraphicsExtractor, preview: Boolean) {
        val (l1, l2) = if (preview) {
            "§7Secrets: §b?§8-§e?§8-§c?        §7Score: §c0"  to
            "§7Deaths: §a0 §8| §7M: §c✘ §8| §7P: §c✘ §8| §7Crypts: §c0"
        } else Dungeon.mapLine1 to Dungeon.mapLine2

        context.pushPop {
            val matrix = context.pose()
            matrix.translate(MAP_W / 2f, MAP_H - 3f)
            matrix.scale(0.6f, 0.6f)

            Render2D.drawString(context, l1, -l1.width() / 2, 0)
            Render2D.drawString(context, l2, -l2.width() / 2, 10)
        }
    }

    private fun totalHeight() = MAP_H + if (Map.mapInfoUnder) 10 else 0
    private fun renderBackground(context: GuiGraphicsExtractor) = Render2D.drawRect(context, 0, 0, MAP_W, totalHeight(), Map.mapBgColor)
    private fun renderBorder(context: GuiGraphicsExtractor) {
        val bw = Map.mapBdWidth
        val h = totalHeight()
        val c = Map.mapBdColor
        Render2D.drawRect(context, -bw, -bw, MAP_W + bw * 2, bw, c)
        Render2D.drawRect(context, -bw, h, MAP_W + bw * 2, bw, c)
        Render2D.drawRect(context, -bw, 0, bw, h, c)
        Render2D.drawRect(context, MAP_W, 0, bw, h, c)
    }

    fun renderPlayerIcon(context: GuiGraphicsExtractor, p: DungeonPlayer, x: Double, y: Double, rotation: Float) = context.pushPop {
        val matrix =  context.pose()
        val you = p.name == player?.name?.string
        renderNametag(context, p.name, x, y, Map.iconScale, you)

        matrix.translate(x.toFloat(), y.toFloat())
        matrix.rotate((rotation * (PI / 180)).toFloat())
        matrix.scale(Map.iconScale, Map.iconScale)

        if (Map.showPlayerHead && !(Map.ownDefault && you)) {
            Render2D.drawRect(context, -6, -6, 12, 12,  if (Map.iconClassColors) p.dclass.color ?: Map.iconBorderColor else Map.iconBorderColor)
            matrix.scale(1f - Map.iconBorderWidth, 1f - Map.iconBorderWidth)
            Render2D.drawPlayerHead(context, -6, -6, 12, p.uuid ?: UUID(0, 0))
        } else {
            val head = if (you) Map.SELF_MARKER else Map.OTHER_MARKER
            Render2D.drawImage(context, head, -4, -5, 7, 10)
        }
    }

    fun renderNametag(context: GuiGraphicsExtractor, name: String, x: Double, y: Double, scale: Float, ownName: Boolean) {
        if (!Dungeon.holdingLeaps || !Map.showNames || (Map.dontShowOwn && ownName)) return

        context.pushPop {
            val matrix = context.pose()
            val rx = (-name.width() / 2f).toInt()
            matrix.translate(x.toFloat(), y.toFloat())
            matrix.scale(scale, scale)
            matrix.translate(0f, 12f)

            drawShadowedText(context, name, rx, 0, scale)
            Render2D.drawString(context, name, rx, 0)
        }
    }

    fun drawShadowedText(context: GuiGraphicsExtractor, text: String, x: Int, y: Int, offset: Float) {
        if (!Map.textShadow) return
        val s = "§0$text"
        for (i in 0..3) {
            context.pushPop {
                val dx = if (i < 2) (if (i == 0) offset else -offset) else 0f
                val dy = if (i >= 2) (if (i == 2) offset else -offset) else 0f
                context.pose().translate(dx, dy)
                Render2D.drawString(context, s, x, y)
            }
        }
    }

    private enum class MapMode(val renderer: (GuiGraphicsExtractor) -> Unit) {
        CLEAR({ Clear.renderMap(it) }),
        BOSS({ Boss.renderMap(it) }),
        SCORE({ Score.render(it) })
    }
}
