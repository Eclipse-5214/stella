package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.stellanav.utils.*
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import dev.deftu.omnicore.api.client.player
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.PI

object boss {
    fun renderMap(context: GuiGraphics) {
        val matrix = context.pose()

        val player = player ?: return
        val playerPos = Vec3(player.x, player.y, player.z)
        val bossMap = BossMapRegistry.getBossMap(Dungeon.floorNumber!!, playerPos) ?: return

        val texture = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/boss/${bossMap.image}")
        val size = 128

        val sizeInWorld = minOf(
            bossMap.widthInWorld,
            bossMap.heightInWorld,
            bossMap.renderSize ?: Int.MAX_VALUE
        )

        val textureWidth = bossMap.width.toDouble()
        val textureHeight = bossMap.height.toDouble()

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
        matrix.pushMatrix()
        matrix.translate(5f,5f)

        // Enable Scissor
        context.enableScissor(0, 0, size, size)

        context.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            texture,
            (-topLeftHudLocX).toInt(),
            (-topLeftHudLocZ).toInt(),
            w,
            h
        )

        context.disableScissor()
        matrix.popMatrix()

        // players

        // Apply transforms
        matrix.pushMatrix()
        matrix.translate(5f,5f)

        // Enable Scissor
        context.enableScissor(0, 0, size, size)
        for (player in DungeonPlayerManager.players) {
            if (player == null) continue
            val you = dev.deftu.omnicore.api.client.player ?: continue
            if (!player.alive && player.name != you.name.string) continue

            val realX = player.realX ?: continue
            val realY = player.realZ ?: continue
            val rotation = player.yaw ?: continue

            val x = ((realX - bossMap.topLeftLocation[0]) / sizeInWorld) * size - topLeftHudLocX
            val y = ((realY - bossMap.topLeftLocation[1]) / sizeInWorld) * size - topLeftHudLocZ

            val matrix = context.pose()

            val ownName = mapConfig.dontShowOwn && player.name == you.name.string

            if (Dungeon.holdingLeaps && mapConfig.showNames && !ownName) {
                matrix.pushMatrix()
                matrix.translate(x.toFloat(), y.toFloat())

                val scale = mapConfig.iconScale / 1.3f
                renderNametag(context, player.name, scale)
                matrix.popMatrix()
            }

            matrix.pushMatrix()
            matrix.translate(x.toFloat(), y.toFloat())
            matrix.rotate((rotation * (PI / 180)).toFloat() )
            matrix.scale(mapConfig.iconScale, mapConfig.iconScale)

            if (mapConfig.showPlayerHead) {
                val w = 12
                val h = 12

                val borderColor = if (mapConfig.iconClassColors) getClassColor(player.dclass.displayName) else mapConfig.iconBorderColor

                Render2D.drawRect(context, (-w.toDouble() / 2.0).toInt(), (-h.toDouble() / 2.0).toInt(), w, h, borderColor)

                val scale = 1f - 0.2f

                matrix.scale(scale, scale)

                val uuid = player.uuid ?: UUID(0,0)
                Render2D.drawPlayerHead(context, -6,-6,12, uuid)
            } else {
                val w = 7
                val h = 10
                val head = if (player.name == you.name.string) GreenMarker else WhiteMarker

                context.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    head,
                    (-w.toDouble() / 2.0).toInt(),
                    (-h.toDouble() / 2.0).toInt(),
                    w,
                    h
                )
            }

            matrix.popMatrix()
        }

        context.disableScissor()
        matrix.popMatrix()
    }
}