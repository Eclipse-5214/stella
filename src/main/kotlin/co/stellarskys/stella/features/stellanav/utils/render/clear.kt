package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.features.stellanav.utils.*
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.map.Room
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayer
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.utils.skyblock.dungeons.utils.Checkmark
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorState
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomType
import dev.deftu.omnicore.api.client.player
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color
import java.util.UUID
import kotlin.math.PI

object clear {
    // Constants
    private const val roomSize = 18
    private const val gapSize = 4
    private const val spacing = roomSize + gapSize

    /** Main render entry point */
    fun renderMap(context: GuiGraphics) {
        val matrix = context.pose()
        val mapOffset = if (Dungeon.floorNumber == 1) 10.6f else 0f
        val mapScale = oscale(Dungeon.floorNumber)

        matrix.pushMatrix()
        matrix.translate(5f, 5f)
        matrix.translate(mapOffset, 0f)
        matrix.scale(mapScale, mapScale)

        renderRooms(context)
        renderCheckmarks(context)
        renderRoomLabels(context, RoomType.PUZZLE, mapConfig.puzzleCheckmarks, mapConfig.pcsize)
        renderRoomLabels(context, RoomType.NORMAL, mapConfig.roomCheckmarks, mapConfig.rcsize)
        renderPlayers(context)

        matrix.popMatrix()
    }

    /** Renders discovered and explored rooms, doors, and connectors */
    fun renderRooms(context: GuiGraphics) {
        Dungeon.discoveredRooms.values.forEach { room ->
            Render2D.drawRect(
                context,
                room.x * spacing,
                room.z * spacing,
                roomSize,
                roomSize,
                Color(65 / 255f, 65 / 255f, 65 / 255f, 1f)
            )
        }

        Dungeon.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            val color = roomTypeColors[room.type] ?: return@forEach
            room.components.forEach { (x, z) ->
                Render2D.drawRect(context, x * spacing, z * spacing, roomSize, roomSize, color)
            }
            renderRoomConnectors(context, room)
        }

        Dungeon.uniqueDoors.forEach { door ->
            if (door.state != DoorState.DISCOVERED) return@forEach
            val type = if (door.opened) DoorType.NORMAL else door.type
            val color = doorTypeColors[type] ?: return@forEach
            val (cx, cy) = door.getComp().let { it.first / 2 * spacing to it.second / 2 * spacing }
            val vert = door.rotation == 0
            val (w, h) = if (vert) 6 to 4 else 4 to 6
            val (x, y) = if (vert) cx + 6 to cy + 18 else cx + 18 to cy + 6
            Render2D.drawRect(context, x, y, w, h, color)
        }
    }

    /** Renders checkmarks for discovered and explored rooms */
    fun renderCheckmarks(context: GuiGraphics) {
        val scale = mapConfig.checkmarkScale

        Dungeon.discoveredRooms.values.forEach { room ->
            val x = room.x * spacing + roomSize / 2 - 5
            val y = room.z * spacing + roomSize / 2 - 6
            context.withMatrix {
                context.pose().translate(x.toFloat(), y.toFloat())
                context.pose().scale(scale, scale)
                Render2D.drawImage(context, questionMark, 0, 0, 10, 12)
            }
        }

        Dungeon.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            val checkmark = getCheckmarks(room.checkmark) ?: return@forEach
            if (mapConfig.roomCheckmarks > 0 && room.type in setOf(
                    RoomType.NORMAL,
                    RoomType.RARE
                ) && room.secrets != 0
            ) return@forEach
            if ((mapConfig.puzzleCheckmarks > 0 && room.type == RoomType.PUZZLE) || room.type == RoomType.ENTRANCE) return@forEach

            val (centerX, centerZ) = room.center()
            val x = (centerX * spacing).toInt() + roomSize / 2 - 6
            val y = (centerZ * spacing).toInt() + roomSize / 2 - 6

            context.withMatrix {
                context.pose().translate(x.toFloat(), y.toFloat())
                context.pose().scale(scale, scale)
                Render2D.drawImage(context, checkmark, 0, 0, 12, 12)
            }
        }
    }

    /** Renders room names and secret counts */
    fun renderRoomLabels(context: GuiGraphics, type: RoomType, checkmarkMode: Int, scaleFactor: Float) {
        Dungeon.uniqueRooms.forEach { room ->
            if (!room.explored || room.type != type || checkmarkMode < 1) return@forEach

            val secrets = if (room.checkmark == Checkmark.GREEN) room.secrets else room.secretsFound
            val textColor = getTextColor(room.checkmark)
            val roomText = room.name ?: "???"
            val secretText = "$secrets/${room.secrets}"

            val lines = buildList {
                if (checkmarkMode in listOf(1, 3)) addAll(roomText.split(" "))
                if (checkmarkMode in listOf(2, 3) && room.secrets != 0) add(secretText)
            }

            val (centerX, centerZ) = room.center()
            val x = (centerX * spacing).toInt() + roomSize / 2
            val y = (centerZ * spacing).toInt() + roomSize / 2
            val scale = 0.75f * scaleFactor

            context.withMatrix {
                context.pose().translate(x.toFloat(), y.toFloat())
                context.pose().scale(scale, scale)

                lines.forEachIndexed { i, line ->
                    val ly = (9 * i - (lines.size * 9) / 2).toFloat()
                    val drawX = (-line.width() / 2).toInt()
                    val drawY = ly.toInt()
                    drawShadowedText(context, line, drawX, drawY, scale)
                    Render2D.drawString(context, textColor + line, drawX, drawY)
                }
            }
        }
    }

    /** Renders player icons and optional nametags */
    fun renderPlayers(context: GuiGraphics) {
        val you = player ?: return
        for (player in DungeonPlayerManager.players) {
            if (player == null || (!player.alive && player.name != you.name.string)) continue

            val iconX = player.iconX ?: continue
            val iconY = player.iconZ ?: continue
            val rotation = player.yaw ?: continue

            val x = iconX / 125.0 * 128.0
            val y = iconY / 125.0 * 128.0
            val ownName = mapConfig.dontShowOwn && player.name == you.name.string

            if (Dungeon.holdingLeaps && mapConfig.showNames && !ownName) {
                context.withMatrix {
                    context.pose().translate(x.toFloat(), y.toFloat())
                    renderNametag(context, player.name, mapConfig.iconScale / 1.3f)
                }
            }

            renderPlayerIcon(context, player, x, y, rotation)
        }
    }

    /** Renders connectors between adjacent room components */
    fun renderRoomConnectors(context: GuiGraphics, room: Room) {
        val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))

        for ((x, z) in room.components) {
            for ((dx, dz) in directions) {
                val nx = x + dx
                val nz = z + dz
                if (!room.hasComponent(nx, nz)) continue

                val cx = (x + nx) / 2 * spacing
                val cy = (z + nz) / 2 * spacing
                val isVertical = dx == 0
                val w = if (isVertical) roomSize else gapSize
                val h = if (isVertical) gapSize else roomSize
                val drawX = if (isVertical) cx else cx + roomSize
                val drawY = if (isVertical) cy + roomSize else cy

                Render2D.drawRect(context, drawX, drawY, w, h, roomTypeColors[room.type] ?: Color.GRAY)
            }
        }

        // Special case: 2x2 room center connector
        if (room.components.size == 4 && room.shape == "2x2") {
            val x = room.components[0].first * spacing + roomSize
            val y = room.components[0].second * spacing + roomSize
            Render2D.drawRect(context, x, y, gapSize, gapSize, roomTypeColors[room.type] ?: Color.GRAY)
        }
    }

    /** Calculates the center of a room, accounting for L-shapes */
    fun Room.center(): Pair<Double, Double> {
        val minX = components.minOf { it.first }
        val minZ = components.minOf { it.second }
        val maxX = components.maxOf { it.first }
        val maxZ = components.maxOf { it.second }

        val width = maxX - minX
        val height = maxZ - minZ

        var centerZ = minZ + height / 2.0
        if (shape == "L") {
            val topEdgeCount = components.count { it.second == minZ }
            centerZ += if (topEdgeCount == 2) -height / 2.0 else height / 2.0
        }

        return Pair(minX + width / 2.0, centerZ)
    }

    /** Scoped matrix push/pop wrapper */
    inline fun GuiGraphics.withMatrix(block: () -> Unit) {
        pose().pushMatrix()
        block()
        pose().popMatrix()
    }

    /** Renders a text string with a soft shadow */
    fun drawShadowedText(context: GuiGraphics, text: String, x: Int, y: Int, scale: Float) {
        val offsets = listOf(Pair(scale, 0f), Pair(-scale, 0f), Pair(0f, scale), Pair(0f, -scale))
        for ((dx, dy) in offsets) {
            context.withMatrix {
                context.pose().translate(dx, dy)
                Render2D.drawString(context, "§0$text", x, y)
            }
        }
    }

    /** Renders a player's icon (head or marker) */
    fun renderPlayerIcon(context: GuiGraphics, player: DungeonPlayer, x: Double, y: Double, rotation: Float) {
        context.withMatrix {
            val matrix = context.pose()
            matrix.translate(x.toFloat(), y.toFloat())
            matrix.rotate((rotation * (PI / 180)).toFloat())
            matrix.scale(mapConfig.iconScale, mapConfig.iconScale)

            if (mapConfig.showPlayerHead) {
                val borderColor =
                    if (mapConfig.iconClassColors) getClassColor(player.dclass.displayName) else mapConfig.iconBorderColor
                Render2D.drawRect(context, -6, -6, 12, 12, borderColor)
                matrix.scale(1f - mapConfig.iconBorderWidth, 1f - mapConfig.iconBorderWidth)
                Render2D.drawPlayerHead(context, -6, -6, 12, player.uuid ?: UUID(0, 0))
            } else {
                val head = if (player.name == dev.deftu.omnicore.api.client.player?.name?.string) GreenMarker else WhiteMarker
                Render2D.drawImage(context, head, -4, -5, 7, 10)
            }
        }
    }
}
