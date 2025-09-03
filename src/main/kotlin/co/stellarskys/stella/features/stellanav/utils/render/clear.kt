package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.Stella
import co.stellarskys.stella.features.stellanav.utils.*
import co.stellarskys.stella.features.stellanav.utils.mapConfig.puzzleCheckmarks
import co.stellarskys.stella.features.stellanav.utils.mapConfig.roomCheckmarks
import co.stellarskys.stella.utils.CompatHelpers.UDrawContext
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.*
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.math.RotationAxis
import java.awt.Color

object clear{
    // constants
    private val roomSize = 18
    private val gapSize = 4
    private val spacing = roomSize + gapSize // 18 + 4 = 22

    fun renderMap(context: UDrawContext) {
        val matrix = context.matrices
        val mapOffset = if (Dungeon.floorNumber == 1) 10.6f else 0f
        val mapScale = oscale(Dungeon.floorNumber)

        matrix.push()
        matrix.translate(5f,5f, 0f)
        matrix.translate(mapOffset, 0f, 0f)
        matrix.scale(mapScale, mapScale, 1f)

        renderRooms(context)
        renderCheckmarks(context)
        renderPuzzleNames(context)
        renderRoomNames(context)
        renderPlayers(context)

        matrix.pop()
    }

    // room rendering
    fun renderRooms(context: UDrawContext) {
        DungeonScanner.discoveredRooms.forEach { (id, room) ->
            val color = Color(65 / 255f, 65 / 255f, 65 / 255f, 1f)

            val x = room.x * spacing
            val y = room.z * spacing
            val w = roomSize
            val h = roomSize

            Render2D.drawRect(context, x, y, w, h, color)
        }

        DungeonScanner.uniqueRooms.forEach { room ->
            if (room.explored) {
                val color = roomTypeColors[room.type] ?: return@forEach

                room.components.forEach { comp ->
                    val x = comp.first * spacing
                    val y = comp.second * spacing
                    val w = roomSize
                    val h = roomSize

                    //println("${room.name}, (${comp.first},${comp.second})")

                    Render2D.drawRect(context, x, y, w, h, color)
                }

                renderRoomConnectors(context, room)
            }
        }

        DungeonScanner.doors.forEach { door ->
            if (door?.state != DoorState.DISCOVERED) return@forEach

            val type = if (door.opened) DoorType.NORMAL else door.type
            val color = doorTypeColors[type] ?: return@forEach
            val comp = door.getComp()
            val cx = comp.first / 2  * 22
            val cy = comp.second / 2 * 22
            val vert = 0 == door.rotation
            val w = if (vert) 6 else 4
            val h = if (vert) 4 else 6

            val x = if (vert) cx + 6 else cx + 18
            val y = if (vert) cy + 18 else cy + 6

            Render2D.drawRect(context, x, y, w, h, color)
        }
    }

    // checkmark rendering
    fun renderCheckmarks(context: UDrawContext) {
        val matrix = context.matrices
        val scale = mapConfig.checkmarkScale

        DungeonScanner.discoveredRooms.forEach { (id, room) ->
            val w = 10
            val h = 12
            val x = room.x * spacing - w / 2 + roomSize / 2
            val y = room.z * spacing - h / 2 + roomSize / 2

            matrix.push()
            matrix.translate(x.toFloat(), y.toFloat(), 0f)
            matrix.scale(scale, scale, 1f)

            context.drawGuiTexture(RenderLayer::getGuiTextured , questionMark, 0, 0, w, h)

            matrix.pop()
        }

        DungeonScanner.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            val checkmark = getCheckmarks(room.checkmark) ?: return@forEach

            // Ts is horrible coding but if it works it works
            if (roomCheckmarks > 0 && room.type in setOf(RoomType.NORMAL, RoomType.RARE) && room.secrets != 0) return@forEach
            if ((puzzleCheckmarks > 0 && room.type == RoomType.PUZZLE) || room.type == RoomType.ENTRANCE) return@forEach

            val minX = room.components.minOf { it.first }
            val minZ = room.components.minOf { it.second }
            val maxX = room.components.maxOf { it.first }
            val maxZ = room.components.maxOf { it.second }

            val roomWidth = maxX - minX
            val roomHeight = maxZ - minZ

            val centerX = minX + roomWidth / 2.0
            var centerZ = minZ + roomHeight / 2.0

            if (room.shape == "L") {
                val topEdgeCount = room.components.count { it.second == minZ }
                centerZ += if (topEdgeCount == 2) -roomHeight / 2.0 else roomHeight / 2.0
            }

            val location = Pair(centerX, centerZ)

            val w = 12
            val h = 12

            val x = (location.first * spacing).toInt() - w / 2 + roomSize / 2
            val y = (location.second * spacing).toInt() - h / 2 + roomSize / 2

            matrix.push()
            matrix.translate(x.toFloat(), y.toFloat(), 0f)
            matrix.scale(scale, scale, 1f)

            Render2D.drawImage(context, checkmark, 0, 0, w, h)

            matrix.pop()
        }
    }

    // name & secret rendering
    fun renderPuzzleNames(context: UDrawContext){
        DungeonScanner.uniqueRooms.forEach { room ->
            val matrix = context.matrices

            if (!room.explored) return@forEach

            if (puzzleCheckmarks < 1) return@forEach
            if (room.type != RoomType.PUZZLE) return@forEach

            val secrets = if (room.checkmark == Checkmark.GREEN) room.secrets else room.secretsFound
            val textColor = getTextColor(room.checkmark)

            val roomText = room.name ?: "???"
            val secretText = "$secrets/${room.secrets}"

            val final = buildList {
                if (puzzleCheckmarks in listOf(1, 3)) addAll(roomText.split(" "))
                if (puzzleCheckmarks in listOf(2, 3) && room.secrets != 0) add(secretText)
            }


            val minX = room.components.minOf { it.first }
            val minZ = room.components.minOf { it.second }
            val maxX = room.components.maxOf { it.first }
            val maxZ = room.components.maxOf { it.second }

            val roomWidth = maxX - minX
            val roomHeight = maxZ - minZ

            val centerX = minX + roomWidth / 2.0
            var centerZ = minZ + roomHeight / 2.0

            if (room.shape == "L") {
                val topEdgeCount = room.components.count { it.second == minZ }
                centerZ += if (topEdgeCount == 2) -roomHeight / 2.0 else roomHeight / 2.0
            }

            val location = Pair(centerX, centerZ)
            val scale = 0.75f * mapConfig.pcsize


            val x = (location.first * spacing).toInt() + roomSize / 2
            val y = (location.second * spacing).toInt() + roomSize / 2

            matrix.push()
            matrix.translate(x.toFloat(), y.toFloat(), 0f)
            matrix.scale(scale,scale,1f)

            var i = 0
            for (line in final) {
                val ly = (9 * i - (final.size * 9) / 2).toFloat()
                val w = line.width().toFloat()

                val drawX = (-w / 2).toInt()
                val drawY = ly.toInt()

                // Shadow
                val offsets = listOf(
                    Pair(scale, 0f), Pair(-scale, 0f),
                    Pair(0f, scale), Pair(0f, -scale)
                )

                for ((dx, dy) in offsets) {
                    matrix.push()
                    matrix.translate(dx, dy, 0f)
                    Render2D.drawString(context, "§0$line", drawX, drawY)
                    matrix.pop()
                }

                Render2D.drawString(context, textColor + line, drawX, drawY)
                i++
            }

            matrix.pop()
        }
    }

    fun renderRoomNames(context: UDrawContext){
        DungeonScanner.uniqueRooms.forEach { room ->
            val matrix = context.matrices

            if (!room.explored) return@forEach

            if (roomCheckmarks < 1) return@forEach
            if (room.type !in setOf(RoomType.NORMAL, RoomType.RARE)) return@forEach

            val secrets = if (room.checkmark == Checkmark.GREEN) room.secrets else room.secretsFound
            val textColor = getTextColor(room.checkmark)

            val roomText = room.name ?: "???"
            val secretText = "$secrets/${room.secrets}"

            val final = buildList {
                if (roomCheckmarks in listOf(1, 3)) addAll(roomText.split(" "))
                if (roomCheckmarks in listOf(2, 3) && room.secrets != 0) add(secretText)
            }

            val minX = room.components.minOf { it.first }
            val minZ = room.components.minOf { it.second }
            val maxX = room.components.maxOf { it.first }
            val maxZ = room.components.maxOf { it.second }

            val roomWidth = maxX - minX
            val roomHeight = maxZ - minZ

            val centerX = minX + roomWidth / 2.0
            var centerZ = minZ + roomHeight / 2.0

            if (room.shape == "L") {
                val topEdgeCount = room.components.count { it.second == minZ }
                centerZ += if (topEdgeCount == 2) -roomHeight / 2.0 else roomHeight / 2.0
            }

            val location = Pair(centerX, centerZ)
            val scale = 0.75f * mapConfig.rcsize


            val x = (location.first * spacing).toInt() + roomSize / 2
            val y = (location.second * spacing).toInt() + roomSize / 2

            matrix.push()
            matrix.translate(x.toFloat(), y.toFloat(), 0f)
            matrix.scale(scale,scale,1f)

            var i = 0
            for (line in final) {
                val ly = (9 * i - (final.size * 9) / 2).toFloat()
                val w = line.width().toFloat()

                val drawX = (-w / 2).toInt()
                val drawY = ly.toInt()

                // Shadow
                val offsets = listOf(
                    Pair(scale, 0f), Pair(-scale, 0f),
                    Pair(0f, scale), Pair(0f, -scale)
                )

                for ((dx, dy) in offsets) {
                    matrix.push()
                    matrix.translate(dx, dy, 0f)
                    Render2D.drawString(context, "§0$line", drawX, drawY)
                    matrix.pop()
                }

                Render2D.drawString(context, textColor + line, drawX, drawY)
                i++
            }

            matrix.pop()
        }
    }

    // player rendering
    fun renderPlayers(context: UDrawContext) {
        for ((k, v) in Dungeon.players) {
            val player = DungeonScanner.players.find { it.name == v.name } ?: continue
            val you = Stella.mc.player ?: continue
            if (v.isDead && v.name != you.name.string) continue

            val iconX = player.iconX ?: continue
            val iconY = player.iconZ ?: continue
            val rotation = player.rotation ?: continue

            val x = (iconX / 125.0 * 128.0)
            val y = (iconY / 125.0 * 128.0)

            val matrix = context.matrices

            matrix.push()
            matrix.translate(x.toFloat(), y.toFloat(), 1f)
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation))
            matrix.scale(mapConfig.iconScale, mapConfig.iconScale, 1f)

            if (mapConfig.showPlayerHead) {
                val w = 12
                val h = 12

                val borderColor = if (mapConfig.iconClassColors) getClassColor(v.className) else mapConfig.iconBorderColor


                Render2D.drawRect(context, (-w.toDouble() / 2.0).toInt(), (-h.toDouble() / 2.0).toInt(), w, h, borderColor)

                val scale = 1f - mapConfig.iconBorderWidth

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
    }

    fun renderRoomConnectors(context: UDrawContext, room: Room) {
        val spacing = roomSize + gapSize
        val connectorSize = gapSize

        val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))

        for ((x, z) in room.components) {
            for ((dx, dz) in directions) {
                val nx = x + dx
                val nz = z + dz

                //println("${room.name} component ($x,$z) checking at ($nx,$nz)")
                //println("does this pass? ${room.hasComponent(nx,nz)}")

                if (room.hasComponent(nx, nz)) {
                    val cx = (x + nx) / 2 * spacing
                    val cy = (z + nz) / 2 * spacing

                    val isVertical = dx == 0 // Z direction means vertical connector
                    val w = if (isVertical) roomSize else connectorSize
                    val h = if (isVertical) connectorSize else roomSize

                    val drawX = if (isVertical) cx else cx + roomSize
                    val drawY = if (isVertical) cy + roomSize else cy

                    Render2D.drawRect(
                        context,
                        drawX,
                        drawY,
                        w,
                        h,
                        roomTypeColors[room.type] ?: Color.GRAY
                    )
                }
            }
        }

        if (room.components.size == 4 && room.shape == "2x2") {
            val x = room.components[0].first * spacing + roomSize
            val y = room.components[0].second * spacing + roomSize
            val size = connectorSize

            Render2D.drawRect(
                context,
                x, y,
                size,
                size,
                roomTypeColors[room.type] ?: Color.GRAY
            )
        }
    }
}