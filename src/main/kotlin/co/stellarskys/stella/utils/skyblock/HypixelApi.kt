package co.stellarskys.stella.utils.skyblock

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
}
