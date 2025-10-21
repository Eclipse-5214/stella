package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.mixins.accessors.AccessorPlayerList
import net.minecraft.client.network.PlayerListEntry
import java.util.regex.Matcher
import java.util.regex.Pattern

object PlayerListUtils {
    var playerList = emptyList<PlayerListEntry>()

    init {
        TickUtils.loop(20){
            update()
        }
    }

    fun update() {
        val nethandler = Stella.mc.networkHandler

        if (nethandler != null) {
            playerList = nethandler.playerList.stream().sorted(AccessorPlayerList.getOrdering()).toList()
        }
    }

    fun strAt(idx: Int): String? {
        if (playerList.isEmpty() || playerList.size < idx) return null
        val txt = playerList[idx].displayName ?: return null
        val str = txt.string.trim()

        return str.ifEmpty { null }
    }

    fun regexAt(idx: Int, pattern: Pattern): Matcher? {
        val str = strAt(idx) ?: return null
        val match = pattern.matcher(str)

        return  if (!match.matches()) null else match
    }
}