package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.features.stellanav.map
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.map.Room
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.utils.skyblock.dungeons.utils.*
import dev.deftu.omnicore.api.client.player
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import tech.thatgravyboat.skyblockapi.platform.pushPop
import java.awt.Color

object Clear {
    private const val ROOM = 18
    private const val GAP = 4
    private const val SPACING = ROOM + GAP
    private const val HALF = ROOM / 2

    private val DISCOVERED = Color(65, 65, 65, 255)

    private val RoomType.isPuzzle get() = this in setOf(RoomType.PUZZLE, RoomType.TRAP, RoomType.YELLOW)
    private val RoomType.isNormal get() = this == RoomType.NORMAL || this == RoomType.RARE

    fun renderMap(context: GuiGraphics) {
        val matrix = context.pose()
        val floor = Dungeon.floorNumber ?: 7
        val scale = floorScale(floor)
        val offset = if (floor == 1) 10.6f else 0f

        context.pushPop {
            matrix.translate(5f + offset, 5f)
            matrix.scale(scale, scale)
            renderRooms(context)
            renderCheckmarks(context)
            renderLabels(context)
            renderPlayers(context)
        }
    }

    private fun renderRooms(context: GuiGraphics) {
        Dungeon.discoveredRooms.values.forEach { Render2D.drawRect(context, it.x * SPACING, it.z * SPACING, ROOM, ROOM, DISCOVERED) }

        Dungeon.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            renderRoom(context, room, room.type.color ?: return@forEach)
        }

        Dungeon.uniqueDoors.forEach { door ->
            if (door.state != DoorState.DISCOVERED) return@forEach
            val vert = door.rotation == 0
            val (cx, cz) = door.getComp()
            Render2D.drawRect(context,
                (cx / 2 * SPACING) + if (vert) 6 else 18,
                (cz / 2 * SPACING) + if (vert) 18 else 6,
                if (vert) 6 else 4, if (vert) 4 else 6,
                (if (door.opened) DoorType.NORMAL else door.type).color
            )
        }
    }

    private fun renderRoom(context: GuiGraphics, room: Room, color: Color) {
        for ((x, z) in room.components) {
            val px = x * SPACING
            val pz = z * SPACING
            Render2D.drawRect(context, px, pz, ROOM, ROOM, color)
            if (room.hasComponent(x + 1, z)) Render2D.drawRect(context, px + ROOM, pz, GAP, ROOM, color)
            if (room.hasComponent(x, z + 1)) Render2D.drawRect(context, px, pz + ROOM, ROOM, GAP, color)
        }

        if (room.shape == "2x2" && room.components.size == 4) {
            val minX = room.components.minOf { it.first }
            val minZ = room.components.minOf { it.second }
            Render2D.drawRect(context, minX * SPACING + ROOM, minZ * SPACING + ROOM, GAP, GAP, color)
        }
    }

    private fun renderCheckmarks(context: GuiGraphics) {
        val scale = map.checkmarkScale
        Dungeon.discoveredRooms.values.forEach { drawIcon(context, it.x.toFloat() * SPACING + HALF, it.z.toFloat() * SPACING + HALF, scale, Checkmark.UNEXPLORED.texture!!, 10, 12, -5f) }

        Dungeon.uniqueRooms.forEach { room ->
            if (!room.explored || room.type == RoomType.ENTRANCE) return@forEach
            val tex = room.checkmark.texture ?: return@forEach
            if ((room.type.isNormal && map.roomCheckmarks > 0 && room.secrets != 0) || (room.type.isPuzzle && map.puzzleCheckmarks > 0)) return@forEach

            val (cx, cz) = room.center()
            drawIcon(context, cx.toFloat() * SPACING + HALF, cz.toFloat() * SPACING + HALF, scale, tex, 12, 12, -6f)
        }
    }

    private fun drawIcon(context: GuiGraphics, x: Float, y: Float, scale: Float, tex: ResourceLocation, w: Int, h: Int, off: Float) {
        context.pushPop {
            context.pose().translate(x, y)
            context.pose().scale(scale, scale)
            Render2D.drawImage(context, tex, off.toInt(), off.toInt(), w, h)
        }
    }

    private fun renderLabels(context: GuiGraphics) {
        val puzzleMode = map.puzzleCheckmarks
        val roomMode = map.roomCheckmarks
        if (puzzleMode == 0 && roomMode == 0) return

        Dungeon.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach

            val type = room.type
            val mode = when {
                type.isPuzzle -> puzzleMode
                type.isNormal -> roomMode
                else -> 0
            }
            if (mode == 0) return@forEach

            val lines = buildList {
                if (mode and 1 != 0) addAll(room.name?.split(" ") ?: listOf("???"))
                if (mode and 2 != 0 && room.secrets != 0) {
                    val count = if (room.checkmark == Checkmark.GREEN) room.secrets else room.secretsFound
                    add("$count/${room.secrets}")
                }
            }
            if (lines.isEmpty()) return@forEach

            val (cx, cz) = room.center()
            val x = cx * SPACING + HALF
            val y = cz * SPACING + HALF
            val scale = 0.75f * if (type.isNormal) map.rcsize else map.pcsize
            val matrix = context.pose()

            context.pushPop {
                matrix.translate(x.toFloat(), y.toFloat())
                matrix.scale(scale, scale)

                val color = room.checkmark.colorCode
                var dy = -(lines.size * 9) / 2

                for (line in lines) {
                    val dx = -line.width() / 2
                    MapRenderer.drawShadowedText(context,  line, dx, dy, scale)
                    Render2D.drawString(context, color + line, dx, dy)
                    dy += 9
                }
            }
        }
    }

    private fun renderPlayers(context: GuiGraphics) {
        val me = player ?: return

        DungeonPlayerManager.players.forEach { p ->
            if (p == null || (!p.alive && p.name != me.name.string)) return@forEach

            val pos = if (map.smoothMovement) p.pos.getLerped() else p.pos.raw
            val ix = pos?.iconX ?: return@forEach
            val iz = pos.iconZ ?: return@forEach
            val rot = pos.yaw?.toFloat() ?: 0f

            MapRenderer.renderPlayerIcon(context, p, ix / 125.0 * 128.0, iz / 125.0 * 128.0, rot)
        }
    }

    private fun Room.center(): Pair<Double, Double> {
        val xs = components.map { it.first }
        val zs = components.map { it.second }
        val minX = xs.min()
        val maxX = xs.max()
        val minZ = zs.min()
        val maxZ = zs.max()

        var cz = (minZ + maxZ) / 2.0
        if (shape == "L") {
            val top = components.count { it.second == minZ }
            cz += if (top == 2) -(maxZ - minZ) / 2.0 else (maxZ - minZ) / 2.0
        }

        return ((minX + maxX) / 2.0) to cz
    }

    private fun floorScale(floor: Int) = if (floor == 0) 1.5f else if (floor <= 3) 1.2f else 1f
}
