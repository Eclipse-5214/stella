package co.stellarskys.stella.api.dungeons.players

import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.api.dungeons.map.MapScanner.RoomClearInfo
import co.stellarskys.stella.api.dungeons.map.Room
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.zenith.world
import net.minecraft.world.entity.player.Player
import java.util.*

class DungeonPlayer(val name: String) {
    var pos = DungeonPlayerPosition()
    var deaths = 0
    var minRooms = 0
    var maxRooms = 0
    var dclass = DungeonClass.UNKNOWN
    val alive get() = dclass != DungeonClass.DEAD

    private var initSecrets: Int = 0
    private var currSecrets: Int = 0
    val secrets get() = currSecrets - initSecrets

    val entity: Player? = world?.entitiesForRendering()
        ?.filterIsInstance<Player>()
        ?.find { it.gameProfile.name == name }

    val uuid: UUID? get() = entity?.uuid

    var inRender = false

    var currRoom: Room? = null
    var lastRoom: Room? = null

    val clearedRooms = mutableMapOf(
        "WHITE" to mutableMapOf<String, RoomClearInfo>(),
        "GREEN" to mutableMapOf<String, RoomClearInfo>()
    )

    init {
        HypixelApi.fetchSecrets(uuid.toString(), 120_000) { secrets ->
            secrets?.let { initSecrets = it; currSecrets = it }
        }
    }

    fun updateSecrets() {
        if (uuid == null) return
        HypixelApi.fetchSecrets(uuid.toString(), 0) { secrets ->
            secrets?.let { currSecrets = it }
        }
    }

    fun getGreenChecks() = clearedRooms["GREEN"] ?: mutableMapOf()
    fun getWhiteChecks() = clearedRooms["WHITE"] ?: mutableMapOf()
}
