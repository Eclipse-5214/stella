package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.features.stellanav.utils.mapConfig
import co.stellarskys.stella.features.stellanav.utils.mapConfig.mapInfoUnder
import co.stellarskys.stella.features.stellanav.utils.prevewMap
import co.stellarskys.stella.utils.CompatHelpers.UDrawContext
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon

object mapRender {
    private val defaultMapSize = Pair(138, 138)

    fun render(context: UDrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale, scale)
        matrix.translate(5f,5f)

        if(!Dungeon.inBoss() && !Dungeon.complete) {
            renderMapBackground(context)
            clear.renderMap(context)
            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapConfig.mapBorder) renderMapBorder(context)
        } else if (!Dungeon.complete && mapConfig.bossMapEnabled) {
            renderMapBackground(context)
            boss.renderMap(context)

            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapConfig.mapBorder) renderMapBorder(context)
        } else if (Dungeon.complete && mapConfig.scoreMapEnabled) {
            renderMapBackground(context)
            score.render(context)

            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapConfig.mapBorder) renderMapBorder(context)
        }

        matrix.popMatrix()
    }

    fun renderPreview(context: UDrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices

        matrix.pushMatrix()
        matrix.translate(x, y)

        renderMapBackground(context)

        Render2D.drawImage(context, prevewMap, 5,5,128,128)

        if (mapInfoUnder) renderInfoUnder(context, true)

        matrix.popMatrix()
    }

    fun renderInfoUnder(context: UDrawContext, preview: Boolean) {
        val matrix = context.matrices

        var mapLine1 = Dungeon.mapLine1
        var mapLine2 = Dungeon.mapLine2

        if (preview) {
            mapLine1 = "&7Secrets: &b?    &7Crypts: &c0    &7Mimic: &c✘";
            mapLine2 = "&7Min Secrets: &b?    &7Deaths: &a0    &7Score: &c0";
        }

        val w1 = mapLine1.width()
        val w2 = mapLine2.width()

        matrix.pushMatrix()
        matrix.translate(138f / 2f, 135f)
        matrix.scale(0.6f, 0.6f)

        Render2D.drawString(context, mapLine1, -w1 / 2, 0)
        Render2D.drawString(context, mapLine2, -w2 / 2, 10)

        matrix.popMatrix()
    }


    fun renderMapBackground(context: UDrawContext) {
        val w = defaultMapSize.first
        var h = defaultMapSize.second
        h += if (mapInfoUnder) 10 else 0

        Render2D.drawRect(context, 0, 0, w, h, mapConfig.mapBgColor)
    }

    fun renderMapBorder(context: UDrawContext) {
        val (w, baseH) = defaultMapSize
        val borderWidth = mapConfig.mapBdWidth
        val h = baseH + if (mapInfoUnder) 10 else 0
        val color = mapConfig.mapBdColor
        // Top border
        Render2D.drawRect(context, -borderWidth, -borderWidth, w + borderWidth * 2, borderWidth, color)

        // Bottom border
        Render2D.drawRect(context, -borderWidth, h, w + borderWidth * 2, borderWidth, color)

        // Left border
        Render2D.drawRect(context, -borderWidth, 0, borderWidth, h, color)

        // Right border
        Render2D.drawRect(context, w, 0, borderWidth, h, color)
    }
}