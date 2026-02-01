package co.stellarskys.stella.utils.animation

enum class AnimType {
    LINEAR,        // linear
    EASE_OUT,      // slower finish
    EASE_IN,       // slower start
    EASE_IN_OUT,   // smooth both ends
    SMOOTH,        // cosine smoothstep
    SPRING         // overshoot + settle
}
