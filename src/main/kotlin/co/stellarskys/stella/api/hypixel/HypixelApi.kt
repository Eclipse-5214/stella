package co.stellarskys.stella.api.hypixel

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.api.handlers.Quasar
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.millis
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.fabric.event.HypixelModAPICallback
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import kotlin.jvm.optionals.getOrNull

@Module
object HypixelApi {
    private val UUID2NameCache = object : LinkedHashMap<String, String>(64, 0.75f, true) { override fun removeEldestEntry(eldest: Map.Entry<String, String>) = size > 512 }
    private val Name2UUIDCache = object : LinkedHashMap<String, String>(64, 0.75f, true) { override fun removeEldestEntry(eldest: Map.Entry<String, String>) = size > 512 }

    data class MojangProfile(val name: String, val id: String)

    init {
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket::class.java)
        HypixelModAPICallback.EVENT.register { event ->
            when (event) {
                is ClientboundLocationPacket -> {
                    EventBus.post(
                        LocationEvent.ServerChange(
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

        val apiUrl = "${Stella.API}/skyblock/profiles?uuid=$uuid"
        val url = if (force) "$apiUrl&t=${System.currentTimeMillis()}" else apiUrl

        Quasar.fetch<SkyblockResponse>(url) { result ->
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

    fun fetchSecrets(
        uuid: String,
        cacheMs: Long = 300_000L, // Default 5 minutes
        force: Boolean = false,
        onResult: (Int?) -> Unit
    ) {
        if (!force) {
            SecretsCache.get(uuid, cacheMs)?.let {
                onResult(it)
                return
            }
        }

        val apiUrl = "${Stella.API}/secrets?uuid=$uuid"
        val url = if (force) "$apiUrl&t=${System.currentTimeMillis()}" else apiUrl

        Quasar.fetch<Int>(url) { result ->
            result.onSuccess { text ->
                if (text != -1) {
                    SecretsCache.put(uuid, text)
                }
                onResult(text)
            }.onFailure {
                onResult(null)
            }
        }
    }

    fun getName(uuid: String, onResult: (String?) -> Unit) {
        val clean = uuid.replace("-", "")
        UUID2NameCache[clean]?.let { return onResult(it) }
        val url = "https://sessionserver.mojang.com/session/minecraft/profile/$clean"
        Quasar.fetch<MojangProfile>(url) { result ->
            val name = result.getOrNull()?.name
            if (name != null) {
                UUID2NameCache[clean] = name
                Name2UUIDCache[name] = clean
            }
            onResult(name)
        }
    }

    fun getUuid(name: String, onResult: (String?) -> Unit) {
        val lowerName = name.lowercase()
        Name2UUIDCache[lowerName]?.let { return onResult(it) }
        val url = "https://api.mojang.com/users/profiles/minecraft/$name"
        Quasar.fetch<MojangProfile>(url) { result ->
            result.onSuccess { profile ->
                val uuid = profile.id
                val officialName = profile.name
                Name2UUIDCache[officialName.lowercase()] = uuid
                UUID2NameCache[uuid] = officialName
                onResult(uuid)
            }.onFailure { onResult(null) }
        }
    }

    object ProfileCache {
        private val data = mutableMapOf<String, Pair<Chronos.SimpleTimeMark, SkyblockResponse.SkyblockMember>>()
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
            data[uuid] = Chronos.now to member
        }

        fun cleanup() {
            data.entries.removeIf { it.value.first.since.millis > EXPIRY_MS }
        }
    }

    object SecretsCache {
        private val data = mutableMapOf<String, Pair<Chronos.SimpleTimeMark, Int>>()
        private const val DEFAULT_EXPIRY = 5 * 60 * 1000L

        fun get(uuid: String, cacheMs: Long): Int? {
            val entry = data[uuid] ?: return null
            if (entry.first.since.millis > cacheMs) {
                data.remove(uuid)
                return null
            }
            return entry.second
        }

        fun put(uuid: String, count: Int) {
            data[uuid] = Chronos.now to count
        }

        fun cleanup() {
            data.entries.removeIf { it.value.first.since.millis > DEFAULT_EXPIRY }
        }
    }
}