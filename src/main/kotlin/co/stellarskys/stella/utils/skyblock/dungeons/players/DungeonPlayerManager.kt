package co.stellarskys.stella.utils.skyblock.dungeons.players

import co.stellarskys.stella.utils.PlayerListUtils
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonClass
import java.util.regex.Pattern

class DungeonPlayerManager {
    /**
     * Match a player entry.
     * Group 1: name
     * Group 2: class (or literal "EMPTY" pre dungeon start)
     * Group 3: level (or nothing, if pre dungeon start)
     * This regex filters out the ironman icon as well as rank prefixes and emblems
     * \[\d+\] (?:\[[A-Za-z]+\] )?(?&lt;name&gt;[A-Za-z0-9_]+) (?:.+ )?\((?&lt;class&gt;\S+) ?(?&lt;level&gt;[LXVI0]+)?\)
     *
     * Taken from Skyblocker
     */
    val PLAYER_TAB_PATTERN: Pattern = Pattern.compile("\\[\\d+] (?:\\[[A-Za-z]+] )?(?<name>[A-Za-z0-9_]+) (?:.+ )?\\((?<class>\\S+) ?(?<level>[LXVI0]+)?\\)")
    val PLAYER_GHOST_PATTERN: Pattern = Pattern.compile(" ☠ (?<name>[A-Za-z0-9_]+) .+ became a ghost\\.")

    val players = Array<DungeonPlayer?>(5) { null }

    fun update() {
        for (i in 0 until 5) {
            val matcher = PlayerListUtils.regexAt(1 + i * 4, PLAYER_TAB_PATTERN)
            if (matcher == null ) {
                players[i] == null
                continue
            }

            val name = matcher.group("name")
            val clazz = DungeonClass.from(matcher.group("class"))

            if (players[i] != null && players[i]!!.name == name) {
                players[i]!!.dclass = clazz
            } else {
                players[i] = DungeonPlayer(name).apply { dclass = clazz }
            }
        }
    }
}