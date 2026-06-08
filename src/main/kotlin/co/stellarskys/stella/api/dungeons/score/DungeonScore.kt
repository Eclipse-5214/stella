package co.stellarskys.stella.api.dungeons.score

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.*
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.utils.Checkmark
import co.stellarskys.stella.api.handlers.Flare
import co.stellarskys.stella.api.handlers.Spark
import co.stellarskys.stella.utils.config
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.data.MayorPerks
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.extentions.stripColor
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Tracks and calculates dungeon score metrics dynamically.
 */
object DungeonScore {
    private val milestones = listOf("⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾")
    private val floorSecrets = mapOf("F1" to 0.3, "F2" to 0.4, "F3" to 0.5, "F4" to 0.6, "F5" to 0.7, "F6" to 0.85)
    private val floorTimes = mapOf("F3" to 120, "F4" to 240, "F5" to 120, "F6" to 240, "F7" to 360, "M6" to 120, "M7" to 360)

    private val SECRETS_FOUND_PATTERN = Regex("""^Secrets Found: ([\d,.]+)$""")
    private val SECRETS_FOUND_PERCENT_PATTERN = Regex("""^Secrets Found: ([\d,.]+)%$""")
    private val MILESTONES_PATTERN = Regex("""^Your Milestone: .(.)$""")
    private val COMPLETED_ROOMS_PATTERN = Regex("""^Completed Rooms: (\d+)$""")
    private val TEAM_DEATHS_PATTERN = Regex("""^Team Deaths: (\d+)$""")
    private val CRYPTS_PATTERN = Regex("""^Crypts: (\d+)$""")
    private val OPENED_ROOMS_PATTERN = Regex("""^Opened Rooms: (\d+)$""")
    private val CLEAR_PERCENT_PATTERN = Regex("""^Cleared: (\d+)% \(\d+\)$""")
    private val DUNGEON_TIME_PATTERN = Regex("""^Time: (?:(\d+)h)?\s?(?:(\d+)m)?\s?(?:(\d+)s)?$""")

    private var had270 = false
        set(value) { field = value; if (value) EventBus.post(DungeonEvent.Score.On270()) }
    private var had300 = false
        set(value) { field = value; if (value) EventBus.post(DungeonEvent.Score.On300()) }
    private var hadCrypts = false
        set(value) { field = value; if (value) EventBus.post(DungeonEvent.Score.AllCrypts()) }

    val forcePaul by config.property<Boolean>("forcePaul")
    val hasPaul get() = MayorPerks.EZPZ.active || forcePaul
    var hasSpiritPet = true

    var dungeonSeconds by Spark(0)
    var secretsFound by Spark(0)
    var secretsFoundPercent by Spark(0.0)
    var crypts by Spark(0)
    var completedRooms by Spark(0)
    var teamDeaths by Spark(0)
    var clearedPercent by Spark(0)
    var secretsPercentNeeded by Spark(1.0)
    var openedRooms by Spark(0)
    var milestone by Spark("⓿")
    var currentFloor by Spark<DungeonFloor?>(null)
    var puzzlePenalty by Spark(0)

    val totalRooms by Flare(36) { if (clearedPercent == 0) 36 else ((100.0 / clearedPercent) * completedRooms + 0.4).toInt() }
    val roomRatio by Flare(0.0) {
        val projectedRooms = (completedRooms + (if (!Dungeon.bloodClear) 1 else 0) + (if (!Dungeon.inBoss) 1 else 0)).coerceAtMost(totalRooms)
        projectedRooms.toDouble() / totalRooms.coerceAtLeast(1)
    }

    val skillScore by Flare(20.0) {
        val actualDeathPenalty = ((teamDeaths * 2) - (if (hasSpiritPet) 1 else 0)).coerceAtLeast(0)
        (20 + (80 * roomRatio) - puzzlePenalty - actualDeathPenalty).coerceIn(20.0, 100.0)
    }

    val secretsScore by Flare(0.0) { (40 * ((secretsFoundPercent / 100.0) / secretsPercentNeeded)).coerceIn(0.0, 40.0) }
    val exploreScore by Flare(0.0) { ((60 * roomRatio) + secretsScore).coerceIn(0.0, 100.0) }

    val bonusScore by Flare(0) { crypts.coerceAtMost(5) + (if (MimicTrigger.mimicDead) 2 else 0) + (if (MimicTrigger.princeDead) 1 else 0) + (if (hasPaul) 10 else 0) }

    val speedScore by Flare(0) {
        currentFloor?.let { f ->
            val offset = dungeonSeconds - (floorTimes[f.name] ?: 0)
            calculateSpeedScore(offset, if (f.name == "E") 0.7 else 1.0)
        } ?: 0
    }

    val totalSecrets by Flare(0) { if (secretsFoundPercent == 0.0) 0 else floor(100.0 / secretsFoundPercent * secretsFound + 0.5).toInt() }
    val maxSecrets by Flare(0) { ceil(totalSecrets * secretsPercentNeeded).toInt() }
    val minSecrets by Flare(0) {
        val actualDeathPenalty = ((teamDeaths * 2) - (if (hasSpiritPet) 1 else 0)).coerceAtLeast(0)
        ceil(totalSecrets * secretsPercentNeeded * (40.0 - bonusScore + actualDeathPenalty) / 40.0).toInt()
    }
    val secretsRemaining by Flare(0) { (minSecrets - secretsFound).coerceAtLeast(0) }

    val score by Flare(0) { (skillScore + exploreScore + speedScore + bonusScore).toInt() }

    fun reset() {
        had270 = false; had300 = false; hadCrypts = false; MimicTrigger.reset()
        dungeonSeconds = 0; secretsFound = 0; secretsFoundPercent = 0.0; crypts = 0
        completedRooms = 0; teamDeaths = 0; clearedPercent = 0; secretsPercentNeeded = 1.0
        openedRooms = 0; milestone = "⓿"; hasSpiritPet = true; puzzlePenalty = 0; currentFloor = null
    }

    fun init() {
        EventBus.on<TablistEvent.Change>(SkyBlockIsland.THE_CATACOMBS) { it.new.flatten().forEach { l -> parseTablist(l.stripped.trim()) } }
        EventBus.on<ScoreboardEvent.Update>(SkyBlockIsland.THE_CATACOMBS) { it.new.forEach { l -> parseSidebar(l.stripColor().trim()) } }
        EventBus.on<DungeonEvent.Enter>(SkyBlockIsland.THE_CATACOMBS) { currentFloor = it.floor }
        MimicTrigger.init()
    }

    private fun parseTablist(msg: String) {
        msg.match(DUNGEON_TIME_PATTERN)?.let {
            val (h, m, s) = it.destructured
            dungeonSeconds = (h.toIntOrNull() ?: 0) * 3600 + (m.toIntOrNull() ?: 0) * 60 + (s.toIntOrNull() ?: 0)
        }

        secretsFound = msg.extractInt(SECRETS_FOUND_PATTERN, secretsFound)
        secretsFoundPercent = msg.extractDouble(SECRETS_FOUND_PERCENT_PATTERN, secretsFoundPercent)
        crypts = msg.extractInt(CRYPTS_PATTERN, crypts)
        completedRooms = msg.extractInt(COMPLETED_ROOMS_PATTERN, completedRooms)
        teamDeaths = msg.extractInt(TEAM_DEATHS_PATTERN, teamDeaths)
        openedRooms = msg.extractInt(OPENED_ROOMS_PATTERN, openedRooms)
        milestone = msg.extractString(MILESTONES_PATTERN, milestone)
        puzzlePenalty = Dungeon.puzzles.count { it.checkmark !in setOf(Checkmark.GREEN, Checkmark.WHITE) } * 10
        checkMilestones()
    }

    private fun parseSidebar(msg: String) {
        msg.match(CLEAR_PERCENT_PATTERN)?.let { clearedPercent = it.groupValues[1].toIntOrNull() ?: clearedPercent }
        secretsPercentNeeded = floorSecrets[Dungeon.floor?.name] ?: 1.0
        checkMilestones()
    }

    private fun checkMilestones() {
        val currentScore = score
        if (currentScore >= 270 && !had270) had270 = true
        if (currentScore >= 300 && !had300) had300 = true
        if (crypts >= 5 && !hadCrypts) hadCrypts = true
    }

    private fun calculateSpeedScore(time: Int, scale: Double): Int = when {
        time < 492 -> 100.0 * scale
        time < 600 -> (140 - time / 12.0) * scale
        time < 840 -> (115 - time / 24.0) * scale
        time < 1140 -> (108 - time / 30.0) * scale
        time < 3570 -> (98.5 - time / 40.0) * scale
        else -> 0.0
    }.toInt()

    fun getMilestone(asIndex: Boolean = false): Any = if (asIndex) milestones.indexOf(milestone) else milestone
    private fun String.match(regex: Regex) = regex.find(this)
    private fun String.extractInt(regex: Regex, fallback: Int) = regex.find(this)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull() ?: fallback
    private fun String.extractDouble(regex: Regex, fallback: Double) = regex.find(this)?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: fallback
    private fun String.extractString(regex: Regex, fallback: String) = regex.find(this)?.groupValues?.getOrNull(1) ?: fallback
}