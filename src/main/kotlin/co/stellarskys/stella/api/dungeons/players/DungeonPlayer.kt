package co.stellarskys.stella.api.dungeons.players

import co.stellarskys.stella.api.dungeons.map.MapScanner.RoomClearInfo
import co.stellarskys.stella.api.dungeons.map.Room
import co.stellarskys.stella.api.dungeons.utils.Checkmark
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.zenith.world
import net.minecraft.world.entity.player.Player
import java.util.*

class DungeonPlayer(val name: String) {
    private var cachedUuid: UUID? = null

    var pos = DungeonPlayerPosition()
    var deaths = 0
    var minRooms = 0
    var maxRooms = 0
    var dclass = DungeonClass.UNKNOWN
    val alive get() = dclass != DungeonClass.DEAD

    init { cachedUuid = entity?.uuid }

    val entity: Player? get() = world?.entitiesForRendering()
        ?.filterIsInstance<Player>()
        ?.find { it.gameProfile.name == name }

    val uuid: UUID? get() = entity?.uuid?.also { cachedUuid = it } ?: cachedUuid

    var inRender = false
    var currRoom: Room? = null
    var lastRoom: Room? = null

    val clearedRooms = mutableMapOf(
        Checkmark.WHITE to mutableMapOf<String, RoomClearInfo>(),
        Checkmark.GREEN to mutableMapOf<String, RoomClearInfo>()
    )

    fun getGreenChecks() = clearedRooms[Checkmark.GREEN] ?: mutableMapOf()
    fun getWhiteChecks() = clearedRooms[Checkmark.WHITE] ?: mutableMapOf()
}
