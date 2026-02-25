package co.stellarskys.stella.utils.skyblock.dungeons.score

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.ScoreboardEvent
import co.stellarskys.stella.events.core.TablistEvent
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.utils.Checkmark
import tech.thatgravyboat.skyblockapi.api.data.Perk
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.extentions.stripColor
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Tracks and calculates dungeon score metrics based on tablist and scoreboard data.
 */
object DungeonScore {
    // Enums
    val milestones = listOf("⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾")
    val floorSecrets = mapOf("F1" to 0.3, "F2" to 0.4, "F3" to 0.5, "F4" to 0.6, "F5" to 0.7, "F6" to 0.85)
    val floorTimes = mapOf("F3" to 120, "F4" to 240, "F5" to 120, "F6" to 240, "F7" to 360, "M6" to 120, "M7" to 360)

    // Regex patterns for parsing tablist and scoreboard lines
    val SECRETS_FOUND_PATTERN = Regex("""^Secrets Found: ([\d,.]+)$""")
    val SECRETS_FOUND_PERCENT_PATTERN = Regex("""^Secrets Found: ([\d,.]+)%$""")
    val MILESTONES_PATTERN = Regex("""^Your Milestone: .(.)$""")
    val COMPLETED_ROOMS_PATTERN =  Regex("""^Completed Rooms: (\d+)$""")
    val TEAM_DEATHS_PATTERN = Regex("""^Team Deaths: (\d+)$""")
    val CRYPTS_PATTERN = Regex("""^Crypts: (\d+)$""")
    val OPENED_ROOMS_PATTERN = Regex("""^Opened Rooms: (\d+)$""")
    val CLEAR_PERCENT_PATTERN =  Regex("""^Cleared: (\d+)% \(\d+\)$""")
    val DUNGEON_TIME_PATTERN =  Regex("""^Time: (?:(\d+)h)?\s?(?:(\d+)m)?\s?(?:(\d+)s)?$""")

    // Current dungeon score state and accessor
    var data = ScoreData()
    val forcePaul by config.property<Boolean>("forcePaul")
    val hasPaul get() = Perk.EZPZ.active || forcePaul
    val score get() = data.score

    private var lastScore = 0
    private var lastCrypts = 0

    /** Resets all score data to default values */
    fun reset() {
        data = ScoreData()
        MimicTrigger.reset()
    }

    /** Registers event listeners for tablist and scoreboard updates */
    fun init() {
        EventBus.on<TablistEvent.Change>(SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.flatten().forEach { parseTablist(it.stripped.trim()) }
        }

        EventBus.on<ScoreboardEvent.Update>(SkyBlockIsland.THE_CATACOMBS) { event ->
            event.new.forEach { parseSidebar(it.stripColor().trim()) }
        }
    }

    /** Parses a single tablist line and updates score data */
    private fun parseTablist(msg: String) = with(data) {
        msg.match(DUNGEON_TIME_PATTERN)?.let {
            val (h, m, s) = it.destructured
            dungeonSeconds = (h.toIntOrNull() ?: 0) * 3600 + (m.toIntOrNull() ?: 0) * 60 + (s.toIntOrNull() ?: 0)
        }

        secretsFound        = msg.extractInt(SECRETS_FOUND_PATTERN, secretsFound)
        secretsFoundPercent = msg.extractDouble(SECRETS_FOUND_PERCENT_PATTERN, secretsFoundPercent)
        crypts              = msg.extractInt(CRYPTS_PATTERN, crypts)
        milestone           = msg.extractString(MILESTONES_PATTERN, milestone)
        completedRooms      = msg.extractInt(COMPLETED_ROOMS_PATTERN, completedRooms)
        teamDeaths          = msg.extractInt(TEAM_DEATHS_PATTERN, teamDeaths)
        openedRooms         = msg.extractInt(OPENED_ROOMS_PATTERN, openedRooms)
        calculateScore2()
    }

    /** Parses a single sidebar line and updates percent-based metrics */
    private fun parseSidebar(msg: String) = with(data) {
        msg.match(CLEAR_PERCENT_PATTERN)?.let {
            clearedPercent = it.groupValues[1].toIntOrNull() ?: clearedPercent
        }
        secretsPercentNeeded = floorSecrets[Dungeon.floor?.name] ?: 1.0
    }

    /** Computes final score and all derived metrics */
    private fun calculateScore2() = with(data) {
        val currentFloor = Dungeon.floor ?: return@with

        totalRooms = if (clearedPercent == 0) 36 else ((100.0 / clearedPercent) * completedRooms + 0.4).toInt()
        val projectedRooms = (completedRooms + (if (!Dungeon.bloodClear) 1 else 0) + (if (!Dungeon.inBoss) 1 else 0)).coerceAtMost(totalRooms)
        val roomRatio = projectedRooms.toDouble() / totalRooms.coerceAtLeast(1)
        val actualDeathPenalty = ((teamDeaths * 2) - (if (hasSpiritPet) 1 else 0)).coerceAtLeast(0)
        val puzzlePenalty = Dungeon.puzzles.count { it.checkmark !in setOf(Checkmark.GREEN, Checkmark.WHITE) } * 10

        skillScore = (20 + (80 * roomRatio) - puzzlePenalty - actualDeathPenalty).coerceIn(20.0, 100.0)
        secretsScore = (40 * ((secretsFoundPercent / 100.0) / secretsPercentNeeded)).coerceIn(0.0, 40.0)
        exploreScore = ( (60 * roomRatio) + secretsScore ).coerceIn(0.0, 100.0)

        val mimicBonus = if (MimicTrigger.mimicDead) 2 else 0
        val princeBonus = if (MimicTrigger.princeDead) 1 else 0
        val paulBonus = if (hasPaul || forcePaul) 10 else 0
        bonusScore = crypts.coerceAtMost(5) + mimicBonus + princeBonus + paulBonus

        val timeOffset = dungeonSeconds - (floorTimes[currentFloor.name] ?: 0)
        speedScore = calculateSpeedScore(timeOffset, if (currentFloor.name == "E") 0.7 else 1.0)

        score = (skillScore + exploreScore + speedScore + bonusScore).toInt()
        totalSecrets = if (secretsFoundPercent == 0.0) 0 else floor(100.0 / secretsFoundPercent * secretsFound + 0.5).toInt()
        maxSecrets = ceil(totalSecrets * secretsPercentNeeded).toInt()
        minSecrets = ceil(totalSecrets * secretsPercentNeeded * (40.0 - bonusScore + actualDeathPenalty) / 40.0).toInt()
        secretsRemaining = (minSecrets - secretsFound).coerceAtLeast(0)

        if (score >= 270 && lastScore < 270) EventBus.post(DungeonEvent.Score.On270())
        if (score >= 300 && lastScore < 300) EventBus.post(DungeonEvent.Score.On300())
        if (crypts >= 5 && lastCrypts < 5) EventBus.post(DungeonEvent.Score.AllCrypts())

        lastScore = maxOf(lastScore, score)
        lastCrypts = maxOf(lastCrypts, crypts)
    }


    /** Calculates speed score based on time offset and scaling factor */
    private fun calculateSpeedScore(time: Int, scale: Double): Int = when {
        time < 492 -> 100.0 * scale
        time < 600 -> (140 - time / 12.0) * scale
        time < 840 -> (115 - time / 24.0) * scale
        time < 1140 -> (108 - time / 30.0) * scale
        time < 3570 -> (98.5 - time / 40.0) * scale
        else -> 0.0
    }.toInt()

    /** Returns milestone symbol or index */
    fun getMilestone(asIndex: Boolean = false): Any = if (asIndex) milestones.indexOf(data.milestone) else data.milestone

    // Regex helpers for parsing
    private fun String.match(regex: Regex) = regex.find(this)
    private fun String.extractInt(regex: Regex, fallback: Int) = regex.find(this)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull() ?: fallback
    private fun String.extractDouble(regex: Regex, fallback: Double) = regex.find(this)?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: fallback
    private fun String.extractString(regex: Regex, fallback: String) = regex.find(this)?.groupValues?.getOrNull(1) ?: fallback
}
