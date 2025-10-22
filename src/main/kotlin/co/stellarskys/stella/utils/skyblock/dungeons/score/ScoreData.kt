package co.stellarskys.stella.utils.skyblock.dungeons.score

data class ScoreData(
    // Raw stats
    var secretsFound: Int = 0,
    var secretsFoundPercent: Double = 0.0,
    var crypts: Int = 0,
    var milestone: String = "⓿",
    var completedRooms: Int = 0,
    var puzzleCount: Int = 0,
    var teamDeaths: Int = 0,
    var openedRooms: Int = 0,
    var clearedRooms: Int = 0,
    var puzzlesDone: Int = 0,
    var dungeonSeconds: Int = 0,
    var clearedPercent: Int = 0,
    var secretsPercentNeeded: Double = 1.0,
    var hasSpiritPet: Boolean = false,

    // Derived score data
    var totalSecrets: Int = 0,
    var secretsRemaining: Int = 0,
    var totalRooms: Int = 0,
    var adjustedRooms: Int = 0,
    var deathPenalty: Int = 0,
    var completionRatio: Double = 0.0,
    var roomsScore: Double = 0.0,
    var skillScore: Double = 0.0,
    var secretsScore: Double = 0.0,
    var exploreScore: Double = 0.0,
    var bonusScore: Int = 0,
    var score: Int = 0,
    var maxSecrets: Int = 0,
    var minSecrets: Int = 0
)
