package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.Stella
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
            if (uuid == null) {
                Signal.fakeMessage("${Stella.PREFIX} §cNo UUID found for $name")
                return@getUuid
            }
            HypixelApi.fetchSkyblockProfile(uuid, 600_000L) { profile ->
                if (profile == null) {
                    Signal.fakeMessage("${Stella.PREFIX} §cNo Profile found for $name")
                    return@fetchSkyblockProfile
                }
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
                        + "§dCatacomb stats for§8: §b$name\n\n"
                        + "§6Cata ${"%.1f".format(cata)} §8| §e$secrets Secrets §8(§b${"%.1f".format(averageSecrets)}§8)\n"
            )
                .append("§b§l${Signal.LINE}")
                .let { Signal.fakeMessage(it) }
        }
    }
}