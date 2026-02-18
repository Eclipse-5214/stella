package co.stellarskys.stella.utils.skyblock.api

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.utils.NetworkUtils
import co.stellarskys.stella.utils.SimpleTimeMark
import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.utils.TimeUtils.millis
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.fabric.event.HypixelModAPICallback
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import kotlin.jvm.optionals.getOrNull

@Module
object HypixelApi {
    init {
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket::class.java)
        HypixelModAPICallback.EVENT.register { event ->
            when (event) {
                is ClientboundLocationPacket -> {
                    EventBus.post(LocationEvent.ServerChange(
                        event.serverName,
                        event.serverType.getOrNull(),
                        event.lobbyName.getOrNull(),
                        event.mode.getOrNull(),
                        event.map.getOrNull(),
                    ))
                }

                is ClientboundHelloPacket -> {
                    EventBus.post(LocationEvent.HypixelJoin(event.environment))
                }
            }
        }
    }

    fun fetchSkyblockProfile(
        uuid: String,
        cacheMs: Long = 300_000L,
        force: Boolean = false, // New parameter
        onResult: (SkyblockResponse.SkyblockMember?) -> Unit
    ) {
        if (!force) {
            ProfileCache.get(uuid, cacheMs)?.let {
                onResult(it)
                return
            }
        }

        val apiUrl = "https://api.stellarskys.co/profiles?uuid=$uuid"
        val url = if (force) "$apiUrl&t=${System.currentTimeMillis()}" else apiUrl

        NetworkUtils.fetch<SkyblockResponse>(url) { result ->
            result.onSuccess { response ->
                val member = response.getActiveMember(uuid)
                if (member != null) {
                    ProfileCache.put(uuid, member)
                }
                onResult(member)
            }.onFailure {
                onResult(null)
            }
        }
    }

    object ProfileCache {
        private val data = mutableMapOf<String, Pair<SimpleTimeMark, SkyblockResponse.SkyblockMember>>()
        private const val EXPIRY_MS = 5 * 60 * 1000L

        fun get(uuid: String, cacheMs: Long): SkyblockResponse.SkyblockMember? {
            val entry = data[uuid] ?: return null
            if (entry.first.since.millis > cacheMs) {
                data.remove(uuid)
                return null
            }
            return entry.second
        }

        fun put(uuid: String, member: SkyblockResponse.SkyblockMember) {
            data[uuid] = TimeUtils.now to member
        }

        fun cleanup() {
            data.entries.removeIf { it.value.first.since.millis > EXPIRY_MS }
        }
    }
}