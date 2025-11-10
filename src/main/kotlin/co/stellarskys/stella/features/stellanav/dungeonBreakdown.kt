package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.typeToColor
import co.stellarskys.stella.features.stellanav.utils.typeToName
import co.stellarskys.stella.utils.skyblock.dungeons.map.MapScanner
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayer
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.utils.skyblock.location.SkyBlockIsland
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.knit.api.text.KnitText

@Module
object dungeonBreakdown: Feature("dungeonBreakdown", island = SkyBlockIsland.THE_CATACOMBS) {

    override fun initialize() {
        register<DungeonEvent.End> { event ->
            TickScheduler.Client.schedule(3 * 20) {
                KnitChat.fakeMessage(Stella.PREFIX + " §bCleared room counts:")
                DungeonPlayerManager.players.forEach { player ->
                    if (player == null) return@forEach

                    val name = player.name
                    val secrets = player.secrets
                    val minmax = "${player.minRooms}-${player.maxRooms}"
                    val deaths = player.deaths
                    val roomLore = buildRoomLore(player)

                    val mesage = KnitText.literal("§d| §b$name §fcleared §b$minmax §frooms | §b$secrets §fsecrets | §b$deaths §fdeaths").onHover(roomLore)
                    KnitChat.fakeMessage(mesage)
                }
            }
        }
    }

    fun buildRoomLore(player: DungeonPlayer): String {
        val greenRooms = player.getGreenChecks()
        val whiteRooms = player.getWhiteChecks()

        val visitedGreenNames = mutableSetOf<String>()
        val lore = StringBuilder()

        fun formatRoomInfo(info: MapScanner.RoomClearInfo, checkColor: String, isLast: Boolean = false): String {
            val room = info.room
            val name = if (room.name == "Default") room.shape else room.name ?: room.shape
            val type = typeToName(room.type)
            val color = typeToColor(room.type)
            val time = info.time

            val stackStr = if (info.solo) "" else {
                val others = room.players
                    .filter { it.name != player.name }
                    .map { it.name }

                if (others.isEmpty()) "."
                else ", Stacked with ${others.joinToString(", ")}."
            }

            val line = "§$color$name §7(§$color$type§7) §7[§$checkColor✔§7]§$color in ${time}s$stackStr"
            return if (isLast) line else "$line\n"
        }

        val allRooms = mutableListOf<MapScanner.RoomClearInfo>()

        for ((_, info) in greenRooms) {
            allRooms += info
        }
        for ((_, info) in whiteRooms) {
            if (info.room.name !in visitedGreenNames) {
                allRooms += info
            }
        }

        allRooms.forEachIndexed { i, info ->
            if (info.solo) player.minRooms++
            player.maxRooms++

            val checkColor = if (info.room.name in visitedGreenNames) "a" else "f"
            lore.append(formatRoomInfo(info, checkColor, isLast = i == allRooms.lastIndex))
        }

        return lore.toString()
    }
}