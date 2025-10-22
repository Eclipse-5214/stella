package co.stellarskys.stella.utils.skyblock.dungeons.players

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.skyblock.HypixelApi
import co.stellarskys.stella.utils.skyblock.dungeons.map.MapScanner.RoomClearInfo
import co.stellarskys.stella.utils.skyblock.dungeons.map.Room
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DungeonClass
import net.minecraft.entity.player.PlayerEntity
import java.util.*
import java.util.concurrent.CompletableFuture

class DungeonPlayer(val name: String) {
    // position
    var iconX: Double? = null
    var iconZ: Double? = null
    var realX: Double? = null
    var realZ: Double? = null
    var yaw: Float? = null

    // score stuff
    var deaths = 0
    var minRooms = 0
    var maxRooms = 0
    var dclass = DungeonClass.UNKNOWN
    val alive get() = dclass != DungeonClass.DEAD;

    private var initSecrets: Int? = null
    private var currSecrets: Int? = null

    // api
    var uuid: UUID? = null
    var inRender = false

    var currRoom: Room? = null
    var lastRoom: Room? = null

    val clearedRooms = mutableMapOf(
        "WHITE" to mutableMapOf<String, RoomClearInfo>(),
        "GREEN" to mutableMapOf<String, RoomClearInfo>()
    )

    init {
        uuid = findPlayerUUID(name)

        HypixelApi.fetchSecrets(uuid.toString(), 120_000) { secrets ->
            initSecrets = secrets
            currSecrets = secrets
        }

        CompletableFuture.runAsync { Stella.mc.sessionService.fetchProfile(uuid, false) }
    }

    private fun findPlayerUUID(name: String): UUID? {
        val world = Stella.mc.world ?: return null
        return world.entities
            .asSequence()
            .filterIsInstance<PlayerEntity>()
            .find { it.gameProfile.name == name }
            ?.uuid
    }

    fun getSecrets(): Int {
        if (uuid == null || initSecrets == null) return -1

        HypixelApi.fetchSecrets(uuid.toString(), 0) { secrets->
            currSecrets = secrets
        }

        return currSecrets!! - initSecrets!!
    }

    fun getGreenChecks(): MutableMap<String, RoomClearInfo> = clearedRooms["GREEN"] ?: mutableMapOf()
    fun getWhiteChecks(): MutableMap<String, RoomClearInfo> = clearedRooms["WHITE"] ?: mutableMapOf()
}