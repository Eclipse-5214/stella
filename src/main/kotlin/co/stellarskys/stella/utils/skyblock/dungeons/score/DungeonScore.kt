package co.stellarskys.stella.utils.skyblock.dungeons.score

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.SidebarUpdateEvent
import co.stellarskys.stella.events.TablistEvent
import co.stellarskys.stella.utils.clearCodes
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import kotlin.math.ceil
import kotlin.math.floor

object DungeonScore {
    // enums
    val puzzleEnums = mapOf(
        "✦" to 0,
        "✔" to 1,
        "✖" to 2
    )

    val milestones = listOf("⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾")

    val floorSecrets = mapOf(
        "F1" to 0.3,
        "F2" to 0.4,
        "F3" to 0.5,
        "F4" to 0.6,
        "F5" to 0.7,
        "F6" to 0.85
    )

    val floorTimes = mapOf(
        "F3" to 120,
        "F4" to 240,
        "F5" to 120,
        "F6" to 240,
        "F7" to 360,
        "M1" to 0,
        "M2" to 0,
        "M3" to 0,
        "M4" to 0,
        "M5" to 0,
        "M6" to 120,
        "M7" to 360
    )

    // Regexes
    val SECRETS_FOUND_PATTERN = Regex("""^Secrets Found: ([\d,.]+)$""")
    val SECRETS_FOUND_PERCENT_PATTERN = Regex("""^Secrets Found: ([\d,.]+)%$""")
    val MILESTONES_PATTERN = Regex("""^Your Milestone: .(.)$""")
    val COMPLEATED_ROOMS_PATTERN =  Regex("""^Completed Rooms: (\d+)$""")
    val TEAM_DEATHS_PATTERN = Regex("""^Team Deaths: (\d+)$""")
    val PUZZLE_COUNT_PATTERN = Regex("""^Puzzles: \((\d+)\)$""")
    val CRYPTS_PATTERN = Regex("""^Crypts: (\d+)$""")
    val PUZZLE_STATE_PATTERN = Regex("""^([\w ]+): \[([✦✔✖])]\s?\(?(\w{1,16})?\)?$""")
    val OPENED_ROOMS_PATTERN = Regex("""^Opened Rooms: (\d+)$""")
    val CLEARED_ROOMS_PATTERN = Regex("""^Completed Rooms: (\d+)$""")
    val CLEAR_PERCENT_PATTERN =  Regex("""^Cleared: (\d+)% \(\d+\)$""")
    val DUNGEON_TIME_PATTERN =  Regex("""^Time: (?:(\d+)h)?\s?(?:(\d+)m)?\s?(?:(\d+)s)?$""")

    // Score stuff
    val score get() = scoreData.score
    var scoreData = ScoreData()
    var secretsFound: Int = 0
    var secretsFoundPercent: Double = 0.0
    var crypts: Int = 0
    var milestone: String = "⓿"
    var completedRooms: Int = 0
    var puzzleCount: Int = 0
    var teamDeaths: Int = 0
    var openedRooms: Int = 0
    var clearedRooms: Int = 0
    var puzzlesDone: Int = 0
    var dungeonSeconds: Int = 0
    var hasSpiritPet: Boolean = false
    var clearedPercent: Int = 0
    var secretsPercentNeeded: Double = 1.0
    var has270Triggered: Boolean = false
    var has300Triggered: Boolean = false
    var hasPaul = false

    fun init() {
        EventBus.register<TablistEvent.Update> { event->
            if (!Dungeon.inDungeon) return@register

            event.packet.entries.forEach { entry ->
                val msg = entry.displayName?.string?.clearCodes()?.trim() ?: return@forEach

                val timeMatch = DUNGEON_TIME_PATTERN.find(msg)
                if (timeMatch != null) {
                    val hours = timeMatch.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
                    val minutes = timeMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
                    val seconds = timeMatch.groupValues.getOrNull(3)?.toIntOrNull() ?: 0

                    dungeonSeconds = seconds + (minutes * 60) + (hours * 60 * 60)
                }

                secretsFound        = extractInt(SECRETS_FOUND_PATTERN, msg, secretsFound)
                secretsFoundPercent = extractDouble(SECRETS_FOUND_PERCENT_PATTERN, msg, secretsFoundPercent)
                crypts              = extractInt(CRYPTS_PATTERN, msg, crypts)
                milestone           = extractString(MILESTONES_PATTERN, msg, milestone)
                completedRooms      = extractInt(COMPLEATED_ROOMS_PATTERN, msg, completedRooms)
                puzzleCount         = extractInt(PUZZLE_COUNT_PATTERN, msg, puzzleCount)
                teamDeaths          = extractInt(TEAM_DEATHS_PATTERN, msg, teamDeaths)
                openedRooms         = extractInt(OPENED_ROOMS_PATTERN, msg, openedRooms)
                clearedRooms        = extractInt(CLEARED_ROOMS_PATTERN, msg, clearedRooms)
                calculateScore()

                val puzzleMatch = PUZZLE_STATE_PATTERN.find(msg)
                if (puzzleMatch != null) {
                    val puzzleName = puzzleMatch.groupValues[1]
                    val puzzleState = puzzleMatch.groupValues[2]
                    val failedBy = puzzleMatch.groupValues.getOrNull(3)
                    val puzzleEnum = puzzleEnums[puzzleState]
                    if (puzzleEnum == 1) puzzlesDone++
                }
            }
        }

        EventBus.register<SidebarUpdateEvent> { event ->
            if (!Dungeon.inDungeon) return@register

            event.lines.forEach { line ->
                val msg = line.clearCodes().trim()

                val percentMatch = CLEAR_PERCENT_PATTERN.find(msg)
                if ( percentMatch != null){
                    val percentStr = percentMatch.groupValues[1]
                    clearedPercent = percentStr.toInt()
                    return@register
                }

                secretsPercentNeeded = floorSecrets[Dungeon.floor] ?: 1.0
            }
        }


    }

    fun reset(){
        scoreData = ScoreData()
        secretsFound = 0
        secretsFoundPercent = 0.0
        crypts = 0
        milestone = "⓿"
        completedRooms = 0
        puzzleCount = 0
        teamDeaths = 0
        openedRooms = 0
        clearedRooms = 0
        puzzlesDone = 0
        dungeonSeconds = 0
        hasSpiritPet = false
        clearedPercent = 0
        secretsPercentNeeded = 1.0
        has270Triggered = false
        has300Triggered = false
    }

    private fun calculateScore() {
        if (Dungeon.floor == null) return

        val missingPuzzles = puzzleCount - puzzlesDone

        scoreData.totalSecrets = ((100.0 / secretsFoundPercent) * secretsFound + 0.5).toInt()
        scoreData.secretsRemaining = scoreData.totalSecrets - secretsFound

        val estimatedTotal = ((100.0 / clearedPercent) * completedRooms + 0.4)
        val totalRooms = estimatedTotal.toInt().takeIf { it > 0 } ?: 36
        scoreData.totalRooms = totalRooms
        scoreData.adjustedRooms = completedRooms

        if (!Dungeon.bloodClear || !Dungeon.inBoss) {
            scoreData.adjustedRooms++
        }
        if (completedRooms <= scoreData.totalRooms - 1 && !Dungeon.bloodClear) scoreData.adjustedRooms++

        scoreData.deathPenalty = (teamDeaths * -2) + if (hasSpiritPet && teamDeaths > 0) 1 else 0

        scoreData.completionRatio = scoreData.adjustedRooms.toDouble() / scoreData.totalRooms
        scoreData.roomsScore = (80 * scoreData.completionRatio).coerceIn(0.0, 80.0)
        scoreData.skillScore = (20 + scoreData.roomsScore - 10 * missingPuzzles + scoreData.deathPenalty).coerceIn(20.0, 100.0)

        scoreData.secretsScore = (40 * ((secretsFoundPercent / 100.0) / secretsPercentNeeded)).coerceIn(0.0, 40.0)
        scoreData.exploreScore = (60 * scoreData.completionRatio + scoreData.secretsScore).coerceIn(0.0, 100.0)

        if (clearedPercent == 0) scoreData.exploreScore = 0.0

        val cryptScore = crypts.coerceAtMost(5)
        val mimicScore = if (MimicTrigger.mimicDead) 2 else 0
        val paulScore = if (hasPaul) 10 else 0

        scoreData.bonusScore = cryptScore + mimicScore + paulScore

        val totalTime = dungeonSeconds - (floorTimes[Dungeon.floor] ?: 0)
        val speedScore = calculateSpeedScore(totalTime, if (Dungeon.floor == "E") 0.7 else 1.0)
        scoreData.score = (scoreData.skillScore + scoreData.exploreScore + speedScore + scoreData.bonusScore).toInt()
        scoreData.maxSecrets = ceil(scoreData.totalSecrets * secretsPercentNeeded).toInt()
        scoreData.minSecrets = floor(scoreData.maxSecrets * ((40.0 - scoreData.bonusScore + scoreData.deathPenalty) / 40.0)).toInt()

        /*
        if (scoreData.score >= 300 && !_has300Triggered) {
            _on300Listeners.forEach { it() }
            _has300Triggered = true
            return
        }
        if (scoreData.score < 270 || _has270Triggered) return

        _on270Listeners.forEach { it() }
        _has270Triggered = true
        */
    }

    private fun extractInt(regex: Regex, msg: String, fallback: Int): Int {
        val match = regex.find(msg) ?: return fallback
        return match.groupValues.getOrNull(1)?.replace(",", "")?.toIntOrNull() ?: fallback
    }

    private fun extractDouble(regex: Regex, msg: String, fallback: Double): Double {
        val match = regex.find(msg) ?: return fallback
        return match.groupValues.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: fallback
    }

    private fun extractString(regex: Regex, msg: String, fallback: String): String {
        val match = regex.find(msg) ?: return fallback
        return match.groupValues.getOrNull(1) ?: fallback
    }

    private fun calculateSpeedScore(time: Int, scale: Double = 1.0): Int = when {
        time < 492.0 -> 100.0 * scale
        time < 600.0 -> (140 - time / 12.0) * scale
        time < 840.0 -> (115 - time / 24.0) * scale
        time < 1140.0 -> (108 - time / 30.0) * scale
        time < 3570.0 -> (98.5 - time / 40.0) * scale
        else -> 0.0
    }.toInt()

    fun getMilestone(asIndex: Boolean = false): Any =
        if (asIndex) milestones.indexOf(milestone) else milestone

    fun setSpiritPet(value: Boolean) {
        hasSpiritPet = value
    }
}