package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitClient.world
import xyz.meowing.knit.api.KnitPlayer.player

object ScoreboardUtils {
    // Modified from Skyblocker https://github.com/SkyblockerMod/Skyblocker
    fun getSidebarLines(): List<String> {
        return try {
            //#if MC >= 1.21.9
            //$$ val scoreboard = world?.scoreboard ?: return emptyList()
            //#else
            val scoreboard = player?.scoreboard ?: return emptyList()
            //#endif

            val objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) ?: return emptyList()

            val stringLines = mutableListOf<String>()
            val scoreHolders = scoreboard.knownScoreHolders.toList()

            // Loop over all known scoreboard entries
            for (scoreHolder in scoreHolders) {
                if (!scoreboard.getScoreHolderObjectives(scoreHolder).containsKey(objective)) continue
                // Only include entries that are part of the current objective
                val objectivesForEntry = scoreboard.getScoreHolderObjectives(scoreHolder)
                if (!objectivesForEntry.containsKey(objective)) continue

                val team = scoreboard.getScoreHolderTeam(scoreHolder.nameForScoreboard)

                if (team != null) {
                    val strLine = team.prefix.string + team.suffix.string
                    if (strLine.trim().isNotEmpty()) stringLines.add(strLine)
                }
            }

            // Add the objective title at the end (top of sidebar)
            val objectiveTitle = objective.displayName
            stringLines.add(objectiveTitle.string)

            // Reverse so the sidebar order is correct
            return stringLines.reversed()
        } catch (e: Exception) {
            Stella.LOGGER.warn("Error in getSidebarLines: $e")
            emptyList()
        }
    }


    fun getScoreboardTitle(cleanColor: Boolean = true): String? {
        val scoreboard = world?.scoreboard ?: return null
        val objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) ?: return null

        return objective.displayName?.string?.let {
            if (cleanColor) it.clearCodes() else it
        }
    }

    /**
     * This code is modified
     * @Author: nea98
     * @Source: https://moddev.nea.moe
     **/
    private fun stripAlienCharacters(text: String): String {
        return text.filter {
            client.textRenderer.getWidth(it.toString()) > 0 || it == '§'
        }
    }

    fun getTabListEntries(): List<String> {
        val playerList = client.networkHandler?.playerList ?: return emptyList()
        return playerList.map { playerInfo ->
            playerInfo.displayName?.string ?: playerInfo.profile.name
        }
    }
}