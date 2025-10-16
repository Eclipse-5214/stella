package co.stellarskys.stella.utils.skyblock

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.NetworkUtils
import com.google.gson.Gson

object HypixelApi {
    private val gson = Gson()

    fun fetchElectionData(
        apiUrl: String = "https://api.hypixel.net/resources/skyblock/election",
        onResult: (ElectionData?) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        NetworkUtils.getJson(
            url = apiUrl,
            onSuccess = { json ->
                try {
                    val electionData = gson.fromJson(json.toString(), ElectionWrapper::class.java)?.toElectionData()
                    onResult(electionData)
                } catch (e: Exception) {
                    onError(e)
                }
            },
            onError = onError
        )
    }

    data class ElectionData(
        val mayorName: String,
        val mayorPerks: List<Pair<String, String>>,
        val ministerName: String,
        val ministerPerk: String,
        val currentYear: Int
    )

    private data class ElectionWrapper(
        val current: CurrentYear?,
        val mayor: Mayor?
    ) {
        fun toElectionData(): ElectionData {
            val perks = mayor?.perks?.mapNotNull {
                if (it.name != null && it.description != null) it.name to it.description else null
            } ?: emptyList()

            return ElectionData(
                mayorName = mayor?.name ?: "Unknown",
                mayorPerks = perks,
                ministerName = mayor?.minister?.name ?: "None",
                ministerPerk = mayor?.minister?.perk?.name ?: "None",
                currentYear = current?.year ?: -1
            )
        }
    }

    private data class CurrentYear(val year: Int?)
    private data class Mayor(
        val name: String?,
        val perks: List<Perk>?,
        val minister: Minister?
    )
    private data class Perk(val name: String?, val description: String?)
    private data class Minister(val name: String?, val perk: MinisterPerk?)
    private data class MinisterPerk(val name: String?)

    fun fetchSecrets(uuid: String, cacheMs: Long, onResult: (Int) -> Unit) {
        if (SecretsCache.isFresh(uuid, cacheMs)) {
            SecretsCache.get(uuid)?.let(onResult)
            return
        }

        NetworkUtils.fetchJson<Int>(
            url = "https://api.tenios.dev/secrets/$uuid",
            headers = mapOf("User-Agent" to "Stella"),
            onSuccess = { secrets ->
                SecretsCache.put(uuid, secrets)
                onResult(secrets)
            },
            onError = { error ->
                Stella.LOGGER.error("Failed to fetch secrets for $uuid: ${error.message}")
                onResult(0)
            }
        )
    }
}

object SecretsCache {
    private val data = mutableMapOf<String, Pair<Long, Int>>() // UUID → (timestamp, secrets)
    private const val EXPIRY_MS = 5 * 60 * 1000L

    fun cleanup() {
        val now = System.currentTimeMillis()
        data.entries.removeIf { now - it.value.first > EXPIRY_MS }
    }

    fun get(uuid: String): Int? = data[uuid]?.second

    fun put(uuid: String, secrets: Int) {
        data[uuid] = System.currentTimeMillis() to secrets
    }

    fun isFresh(uuid: String, cacheMs: Long): Boolean {
        val timestamp = data[uuid]?.first ?: return false
        return System.currentTimeMillis() - timestamp < cacheMs
    }
}
