package co.stellarskys.stella.features.secrets.utils.waypoints

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.handlers.Quasar
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.LocationEvent

object SecretsRegistry {
    private val byId = mutableMapOf<Int, SecretData>()
    private val allRooms = mutableListOf<SecretData>()
    private val ROOM_DATA_URL = "${Stella.ETHER}/secretCoords.json"

    init {
        EventBus.on<LocationEvent.IslandChange> { resetSecrets() }
    }

    fun load() {
        Quasar.fetch<List<SecretData>>(ROOM_DATA_URL) { result ->
            result.onSuccess { rooms ->
                populateRooms(rooms)
                Stella.LOGGER.info("SecretsRegistry: Loaded ${rooms.size} rooms from Ether")
            }

            result.onFailure {
                Stella.LOGGER.warn("SecretsRegistry: Failed to load local room data — ${it.message}")
            }
        }
    }

    fun populateRooms(rooms: List<SecretData>) {
        allRooms += rooms
        for (room in rooms) {
            byId[room.roomID] = room
        }
    }

    fun resetSecrets() {
        allRooms.forEach { room ->
            room.redstoneKey.forEach { it.collected = false }
            room.wither.forEach { it.collected = false }
            room.bat.forEach { it.collected = false }
            room.item.forEach { it.collected = false }
            room.chest.forEach { it.collected = false }
            room.lever.forEach { it.collected = false }
        }
    }

    fun getById(id: Int): SecretData? = byId[id]
    fun getAll(): List<SecretData> = allRooms
}