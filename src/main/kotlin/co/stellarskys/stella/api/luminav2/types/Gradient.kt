package co.stellarskys.stella.api.luminav2.types

class Gradient(val color1: Int, val color2: Int, val type: GradientType)
enum class GradientType {LeftToRight, TopToBottom, TopLeftToBottomRight, BottomRightToTopLeft }
