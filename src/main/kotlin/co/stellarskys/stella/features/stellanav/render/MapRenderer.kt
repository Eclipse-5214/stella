package co.stellarskys.stella.features.stellanav.render

import co.stellarskys.stella.features.stellanav.map
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayer
import dev.deftu.omnicore.api.client.player
import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.platform.scale
import java.util.UUID
import kotlin.math.PI

object MapRenderer {
    private const val MAP_W = 138
    private const val MAP_H = 138

    fun render(context: GuiGraphics, x: Float, y: Float, scale: Float) {
        context.pushPop {
            val matrix = context.pose()
            matrix.translate(x, y)
            matrix.scale(scale, scale)
            matrix.translate(5f, 5f)

            when {
                Dungeon.inBoss && map.bossMapEnabled && !Dungeon.complete -> MapMode.BOSS
                map.scoreMapEnabled && Dungeon.complete -> MapMode.SCORE
                Dungeon.inBoss && map.hideInBoss -> null
                else -> MapMode.CLEAR
            }?.let { mode ->
                renderBackground(context)
                mode.renderer(context)
                if (map.mapInfoUnder) renderInfoUnder(context, false)
                if (map.mapBorder) renderBorder(context)
            }
        }
    }

    fun renderPreview(context: GuiGraphics, x: Float, y: Float, scale: Float) = context.pushPop {
        context.pose().translate(x, y)
        context.scale(scale, scale)

        renderBackground(context)
        Render2D.drawImage(context, map.DEFAULT_MAP, 5, 5, 128, 128)

        if (map.mapInfoUnder) renderInfoUnder(context, true)
        if (map.mapBorder) renderBorder(context)
    }


    fun renderInfoUnder(context: GuiGraphics, preview: Boolean) {
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

    private fun totalHeight() = MAP_H + if (map.mapInfoUnder) 10 else 0
    private fun renderBackground(context: GuiGraphics) = Render2D.drawRect(context, 0, 0, MAP_W, totalHeight(), map.mapBgColor)
    private fun renderBorder(context: GuiGraphics) {
        val bw = map.mapBdWidth
        val h = totalHeight()
        val c = map.mapBdColor
        Render2D.drawRect(context, -bw, -bw, MAP_W + bw * 2, bw, c)
        Render2D.drawRect(context, -bw, h, MAP_W + bw * 2, bw, c)
        Render2D.drawRect(context, -bw, 0, bw, h, c)
        Render2D.drawRect(context, MAP_W, 0, bw, h, c)
    }

    fun renderPlayerIcon(context: GuiGraphics, p: DungeonPlayer, x: Double, y: Double, rotation: Float) = context.pushPop {
        val matrix =  context.pose()
        val you = p.name == player?.name?.string
        renderNametag(context, p.name, x, y, map.iconScale, you)

        matrix.translate(x.toFloat(), y.toFloat())
        matrix.rotate((rotation * (PI / 180)).toFloat())
        matrix.scale(map.iconScale, map.iconScale)

        if (map.showPlayerHead && !(map.ownDefault && you)) {
            Render2D.drawRect(context, -6, -6, 12, 12,  if (map.iconClassColors) p.dclass.color ?: map.iconBorderColor else map.iconBorderColor)
            matrix.scale(1f - map.iconBorderWidth, 1f - map.iconBorderWidth)
            Render2D.drawPlayerHead(context, -6, -6, 12, p.uuid ?: UUID(0, 0))
        } else {
            val head = if (you) map.SELF_MARKER else map.OTHER_MARKER
            Render2D.drawImage(context, head, -4, -5, 7, 10)
        }
    }

    fun renderNametag(context: GuiGraphics, name: String, x: Double, y: Double, scale: Float, ownName: Boolean) {
        if (!Dungeon.holdingLeaps || !map.showNames || (map.dontShowOwn && ownName)) return

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

    fun drawShadowedText(context: GuiGraphics, text: String, x: Int, y: Int, scale: Float) {
        if (!map.textShadow) return
        val s = "§0$text"
        Render2D.drawString(context, s, (x + scale).toInt(), y)
        Render2D.drawString(context, s, (x - scale).toInt(), y)
        Render2D.drawString(context, s, x, (y + scale).toInt())
        Render2D.drawString(context, s, x, (y - scale).toInt())
    }

    private enum class MapMode(val renderer: (GuiGraphics) -> Unit) {
        CLEAR({ Clear.renderMap(it) }),
        BOSS({ Boss.renderMap(it) }),
        SCORE({ Score.render(it) })
    }
}
