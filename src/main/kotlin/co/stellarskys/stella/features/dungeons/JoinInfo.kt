package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import dev.deftu.textile.Text
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object JoinInfo: Feature("joinInfo", island = SkyBlockIsland.DUNGEON_HUB) {
    private val JoinRegex = "Party Finder > (\\w+) joined the dungeon group!".toRegex()

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            val person = JoinRegex.find(event.message.stripped)?.groups?.get(1)?.value ?: return@on
            fetchAndDisplayStats(person)
        }
    }

    fun fetchAndDisplayStats(name:String) {
        HypixelApi.getUuid(name) { uuid ->
            if (uuid == null) return@getUuid
            HypixelApi.fetchSkyblockProfile(uuid, 600_000L) { profile ->
                if (profile == null) return@fetchSkyblockProfile
                displayCataStats(profile, name)
            }
        }
    }

    fun displayCataStats(player: SkyblockResponse.SkyblockMember, name: String) {
        with(player.dungeons) {
            val normal = dungeonTypes.catacombs
            val master = dungeonTypes.mastermode
            val cata = Dungeon.calculateDungeonLevel(normal.experience)
            val classes = DungeonClass.entries.map {
                if (it !in setOf(DungeonClass.DEAD, DungeonClass.UNKNOWN)) Dungeon.calculateDungeonLevel(
                    classes[it.name.lowercase()]?.experience ?: 0.0
                )
            }

            Text.literal(
                "§b§l${Signal.LINE}\n"
                        + "§dCatacomb stats for: §b$name\n\n"
                        + "§6Cata $cata §8| §e$secrets Secrets §8(§b${averageSecrets.toString().format(1)}§8)\n"
            )
        }
    }
}