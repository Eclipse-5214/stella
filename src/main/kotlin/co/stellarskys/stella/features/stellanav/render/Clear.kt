package co.stellarskys.stella.features.stellanav.render

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
        Dungeon.discoveredRooms.values.forEach {
            drawIcon(context, it.x.toFloat() * SPACING + HALF, it.z.toFloat() * SPACING + HALF, scale, Checkmark.UNEXPLORED.texture!!, 10, 12, -5f)
        }

        Dungeon.uniqueRooms.forEach { room ->
            if (!room.explored || room.type == RoomType.ENTRANCE) return@forEach
            val show = if (room.type.isNormal) map.roomCheck else if (room.type.isPuzzle) map.puzzleCheck else true
            if (!show) return@forEach
            val tex = room.checkmark.texture ?: return@forEach
            val anchor = Anchor.fromInt(map.checkAnchor)
            val coords = room.getAnchorPos(anchor)
            val cx = coords.first.toFloat() * SPACING + HALF
            val cz = coords.second.toFloat() * SPACING + HALF

            drawIcon(context, cx, cz, scale, tex, 12, 12, -6f)
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
        Dungeon.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            if (!room.type.isNormal && !room.type.isPuzzle) return@forEach
            if (map.replaceText && room.checkmark == Checkmark.GREEN) return@forEach
            val posGroups = mutableMapOf<Pair<Double, Double>, MutableList<Pair<String, Float>>>()
            val isNormal = room.type.isNormal

            if (if (isNormal) map.roomName else map.puzzleName) {
                val anchor = Anchor.fromInt(map.nameAnchor)
                val pos = room.getAnchorPos(anchor)
                room.name?.split(" ")?.forEach {
                    posGroups.getOrPut(pos) { mutableListOf() }.add(it to 0.75f * map.nameScale)
                }
            }

            if (room.secrets != 0 && (if (isNormal) map.roomSecrets else map.puzzleSecrets)) {
                val anchor = Anchor.fromInt(map.secretsAnchor)
                val pos = room.getAnchorPos(anchor)
                val count = if (room.checkmark == Checkmark.GREEN) room.secrets else room.secretsFound
                posGroups.getOrPut(pos) { mutableListOf() }.add("$count/${room.secrets}" to 0.75f * map.secretScale)
            }

            posGroups.forEach { (coords, lines) ->
                val x = (coords.first * SPACING + HALF).toFloat()
                val y = (coords.second * SPACING + HALF).toFloat()
                renderStack(context, lines, x, y, room.checkmark.colorCode)
            }
        }
    }

    private fun renderStack(context: GuiGraphics, lines: List<Pair<String, Float>>, x: Float, y: Float, color: String) {
        val visualHeight = lines.sumOf { (it.second * 9).toDouble() }.toFloat()
        var currentY = -visualHeight / 2f
        lines.forEach { (text, scale) ->
            val tw = text.width()
            context.pushPop {
                context.pose().translate(x, y + currentY)
                context.pose().scale(scale, scale)
                context.pose().translate(-tw / 2f, 0f)
                MapRenderer.drawShadowedText(context, text, 0, 0, scale)
                Render2D.drawString(context, color + text, 0, 0)
            }
            currentY += scale * 9f
        }
    }

    private fun renderPlayers(context: GuiGraphics) {
        if (Dungeon.inBoss) return
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

    private fun Room.getAnchorPos(anchor: Anchor): Pair<Double, Double> {
        val sorted = components
            .sortedWith(compareBy({ it.second }, { it.first }))
            .map { it.toDouble()}

        val size = sorted.size

        return when (anchor) {
            Anchor.FIRST -> sorted.first()
            Anchor.MIDDLE -> if (size > 1) sorted[1] else sorted.first()
            Anchor.LAST -> sorted.last()
            Anchor.CENTER -> {
                val isIrregular = shape in setOf("L", "1x2")
                if (isIrregular && map.prioMiddle) if (size > 1) sorted[1] else sorted.first()
                else this.center()
            }
        }
    }

    private fun floorScale(floor: Int) = if (floor == 0) 1.5f else if (floor <= 3) 1.2f else 1f
    private fun Pair<Int, Int>.toDouble() = Pair(first.toDouble(), second.toDouble())

    enum class Anchor(val id: Int) {
        FIRST(0),
        MIDDLE(1),
        LAST(2),
        CENTER(3);

        companion object {
            fun fromInt(value: Int) = entries.find { it.id == value } ?: FIRST
        }
    }
}
