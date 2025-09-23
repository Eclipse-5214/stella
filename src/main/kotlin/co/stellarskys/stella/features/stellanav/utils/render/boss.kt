package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.stellanav.utils.*
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.popMatrix
import co.stellarskys.stella.utils.render.pushMatrix
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonScanner
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d

object boss {
    fun renderMap(context: DrawContext) {
        val matrix = context.matrices

        val player = Stella.mc.player ?: return
        val playerPos = Vec3d(player.x, player.y, player.z)
        val bossMap = BossMapRegistry.getBossMap(Dungeon.floorNumber!!, playerPos) ?: return

        val texture = Identifier.of(Stella.NAMESPACE, "stellanav/boss/${bossMap.image}")
        val sprite = Stella.mc.guiAtlasManager.getSprite(texture)
        val size = 128

        val sizeInWorld = minOf(
            bossMap.widthInWorld,
            bossMap.heightInWorld,
            bossMap.renderSize ?: Int.MAX_VALUE
        )

        val textureWidth = sprite.contents.width.toDouble() // Replace with actual texture size if available
        val textureHeight = sprite.contents.height.toDouble()

        val pixelWidth = (textureWidth / bossMap.widthInWorld) * (bossMap.renderSize ?: bossMap.widthInWorld)
        val pixelHeight = (textureHeight / bossMap.heightInWorld) * (bossMap.renderSize ?: bossMap.heightInWorld)
        val sizeInPixels = minOf(pixelWidth, pixelHeight)

        val textureScale = size / sizeInPixels

        var topLeftHudLocX = ((playerPos.x - bossMap.topLeftLocation[0]) / sizeInWorld) * size - size / 2
        var topLeftHudLocZ = ((playerPos.z - bossMap.topLeftLocation[1]) / sizeInWorld) * size - size / 2

        topLeftHudLocX = topLeftHudLocX.coerceIn(0.0, maxOf(0.0, textureWidth * textureScale - size))
        topLeftHudLocZ = topLeftHudLocZ.coerceIn(0.0, maxOf(0.0, textureHeight * textureScale - size))

        val w = (textureWidth * textureScale).toInt()
        val h = (textureHeight * textureScale).toInt()

        // Apply transforms
        matrix.push()
        matrix.translate(5f,5f,0f)

        // Enable Scissor
        context.enableScissor(0, 0, size, size)

        context.drawGuiTexture(
            RenderLayer::getGuiTextured,
            texture,
            (-topLeftHudLocX).toInt(),
            (-topLeftHudLocZ).toInt(),
            w,
            h
        )

        context.disableScissor()
        matrix.pop()

        // players

        // Apply transforms
        matrix.push()
        matrix.translate(5f,5f,0f)

        // Enable Scissor
        context.enableScissor(0, 0, size, size)
        for ((k, v) in Dungeon.players) {
            val player = DungeonScanner.players.find { it.name == v.name } ?: continue
            val you = Stella.mc.player ?: continue
            if (v.isDead && v.name != you.name.string) continue

            val realX = player.realX ?: continue
            val realY = player.realZ ?: continue
            val rotation = player.rotation ?: continue

            val x = ((realX - bossMap.topLeftLocation[0]) / sizeInWorld) * size - topLeftHudLocX
            val y = ((realY - bossMap.topLeftLocation[1]) / sizeInWorld) * size - topLeftHudLocZ

            val matrix = context.matrices

            val ownName = mapConfig.dontShowOwn && v.name == you.name.string

            if (Dungeon.holdingLeaps && mapConfig.showNames && !ownName) {
                matrix.pushMatrix()
                matrix.translate(x.toFloat(), y.toFloat(), 1f)

                val scale = mapConfig.iconScale / 1.3f
                renderNametag(context, v.name, scale)
                matrix.popMatrix()
            }

            matrix.push()
            matrix.translate(x.toFloat(), y.toFloat(), 1f)
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation))
            matrix.scale(mapConfig.iconScale, mapConfig.iconScale, 1f)

            if (mapConfig.showPlayerHead) {
                val w = 12
                val h = 12

                val borderColor = if (mapConfig.iconClassColors) getClassColor(v.className) else mapConfig.iconBorderColor

                Render2D.drawRect(context, (-w.toDouble() / 2.0).toInt(), (-h.toDouble() / 2.0).toInt(), w, h, borderColor)

                val scale = 1f - 0.2f

                matrix.scale(scale, scale, 1f)

                context.drawTexture(
                    RenderLayer::getGuiTextured,                         // render layer provider
                    player.skin,
                    (-w.toDouble() / 2.0).toInt(),
                    (-h.toDouble() / 2.0).toInt(),
                    8f,
                    8f,
                    w,
                    h,
                    8,
                    8,
                    64,
                    64,
                )

                if (player.hat) {
                    context.drawTexture(
                        RenderLayer::getGuiTextured,                         // render layer provider
                        player.skin,
                        (-w.toDouble() / 2.0).toInt(),
                        (-h.toDouble() / 2.0).toInt(),
                        40f,
                        8f,
                        w,
                        h,
                        8,
                        8,
                        64,
                        64,
                    )
                }
            } else {
                val w = 7
                val h = 10
                val head = if (v.name == you.name.string) GreenMarker else WhiteMarker

                context.drawGuiTexture(
                    RenderLayer::getGuiTextured,
                    head,
                    (-w.toDouble() / 2.0).toInt(),
                    (-h.toDouble() / 2.0).toInt(),
                    w,
                    h
                )
            }

            matrix.pop()
        }

        context.disableScissor()
        matrix.pop()
    }
}