package co.stellarskys.stella.utils.skyblock.dungeons.players

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.ChatEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.WorldEvent
import co.stellarskys.stella.utils.PlayerListUtils
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DungeonClass
import net.minecraft.text.Text
import java.util.regex.Pattern

object DungeonPlayerManager {
    /**
     * Match a player entry.
     * Group 1: name
     * Group 2: class (or literal "EMPTY" pre map start)
     * Group 3: level (or nothing, if pre map start)
     * This regex filters out the ironman icon as well as rank prefixes and emblems
     * \[\d+\] (?:\[[A-Za-z]+\] )?(?&lt;name&gt;[A-Za-z0-9_]+) (?:.+ )?\((?&lt;class&gt;\S+) ?(?&lt;level&gt;[LXVI0]+)?\)
     *
     * Taken from Skyblocker
     */
    val PLAYER_TAB_PATTERN: Pattern = Pattern.compile("\\[\\d+] (?:\\[[A-Za-z]+] )?(?<name>[A-Za-z0-9_]+) (?:.+ )?\\((?<class>\\S+) ?(?<level>[LXVI0]+)?\\)")
    val PLAYER_GHOST_PATTERN: Pattern = Pattern.compile(" ☠ (?<name>[A-Za-z0-9_]+) .+ became a ghost\\.")

    val players = Array<DungeonPlayer?>(5) { null }

    fun init() {
        TickUtils.loop(1) { update() }
        EventBus.register<ChatEvent.Receive> { onDeath(it.message) }
    }

    fun update() {
        if (!Dungeon.inDungeon) return

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

    private fun onDeath(text: Text) {
        val matcher = PLAYER_GHOST_PATTERN.matcher(text.string)
        if (!matcher.find()) return

        var name = matcher.group("name")
        if (name == "You") Stella.mc.player?.let { name = it.name.string }

        val player = getPlayer(name)
        if (player != null) {
            player.dclass = DungeonClass.DEAD
        } else {
            Stella.LOGGER.error(
                "[Dungeon Player Manager] Received ghost message for player '{}' but player was not found in the player list: {}",
                matcher.group("name"),
                players.contentToString()
            )
        }
    }

    fun getPlayer(name: String): DungeonPlayer? {
        return players
            .asSequence()
            .filterNotNull()
            .firstOrNull { it.name == name }
    }

    fun reset() {
        players.fill(null)
    }
}