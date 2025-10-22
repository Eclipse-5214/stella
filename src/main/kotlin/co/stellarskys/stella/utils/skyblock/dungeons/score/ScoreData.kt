package co.stellarskys.stella.utils.skyblock.dungeons.score

data class ScoreData(
    var totalSecrets: Int = 0,
    var secretsRemaining: Int = 0,
    var totalRooms: Int = 0,
    var deathPenalty: Int = 0,
    var completionRatio: Double = 0.0,
    var adjustedRooms: Int = 0,
    var roomsScore: Double = 0.0,
    var skillScore: Double = 0.0,
    var secretsScore: Double = 0.0,
    var exploreScore: Double = 0.0,
    var bonusScore: Int = 0,
    var score: Int = 0,
    var maxSecrets: Int = 0,
    var minSecrets: Int = 0
)