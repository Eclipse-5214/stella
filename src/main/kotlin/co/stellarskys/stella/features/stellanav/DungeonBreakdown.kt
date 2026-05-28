package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.dungeons.players.DungeonPlayer
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.events.core.LocationEvent
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import kotlin.collections.MutableMap
import kotlin.let

@Module
object DungeonBreakdown: Feature("dungeonBreakdown", island = SkyBlockIsland.THE_CATACOMBS) {
    private val initSecrets: MutableMap<String, Int> = mutableMapOf()
    private val deltaSecrets: MutableMap<String, Int> = mutableMapOf()

    override fun initialize() {
        on<DungeonEvent.Start> {
            Dungeon.players.forEach { player ->
                player.uuid?.toString()?.let { uuid ->
                    HypixelApi.fetchSecrets(uuid, 120_000) { it?.let { count -> initSecrets[player.name] = count } }
                }
            }
        }

        on<DungeonEvent.End> {
            Dungeon.players.forEach { player ->
                player.uuid?.toString()?.let { uuid ->
                    HypixelApi.fetchSecrets(uuid, 0) { secrets ->
                        secrets?.let { count -> deltaSecrets[player.name] = count - (initSecrets[player.name] ?: -1) }
                    }
                }
            }

            Chronos.Tick.after(3 * 20) run {
                Signal.fakeMessage(Stella.PREFIX + " §bCleared room counts:")
                Dungeon.players.forEach { player ->
                    val secrets = deltaSecrets[player.name]?.let { "§b$it" } ?: "§cAPI Err"
                    val lore = buildRoomLore(player)
                    val msg = "§d| §b${player.name} §fcleared §b${player.minRooms}-${player.maxRooms} §frooms | $secrets §fsecrets | §b${player.deaths} §fdeaths"
                    Signal.fakeMessage(Component.literal(msg).onHover(lore))
                }
            }
        }

        on<LocationEvent.ServerChange> {
            initSecrets.clear()
            deltaSecrets.clear()
        }
    }

    fun buildRoomLore(player: DungeonPlayer): String {
        val greenNames = player.getGreenChecks().values.mapNotNull { it.room.name }.toSet()
        val allRooms = (player.getGreenChecks().values + player.getWhiteChecks().values.filter { it.room.name !in greenNames })

        return allRooms.joinToString("\n") { info ->
            val room = info.room
            val name = if (room.name == "Default") room.shape else room.name ?: room.shape
            val stack = if (info.solo) "" else {
                val others = room.players.filter { it.name != player.name }.map { it.name }
                if (others.isEmpty()) "." else ", Stacked with ${others.joinToString(", ")}."
            }

            val check = if (room.name in greenNames) "§a✔" else "§f✔"
            "§${room.type.colorCode}$name §7(§${room.type.colorCode}${room.type.name}§7) §7[$check§7]§${room.type.colorCode} in ${info.time}$stack"
        }
    }
}