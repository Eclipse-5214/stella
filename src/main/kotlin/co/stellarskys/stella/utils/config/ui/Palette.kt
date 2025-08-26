package co.stellarskys.stella.utils.config.ui

import java.awt.Color

/**
 * Palette – Mutable color palette inspired by Catppuccin.
 */
object Palette {

    // === Primary color palette ===

    var Rosewater = Color.decode("#f2d5cf")
    var Flamingo  = Color.decode("#eebebe")
    var Pink      = Color.decode("#f4b8e4")
    var Mauve     = Color.decode("#ca9ee6")
    var Purple    = Color.decode("#865dd4")
    var Red       = Color.decode("#e78284")
    var Maroon    = Color.decode("#ea999c")
    var Peach     = Color.decode("#ef9f76")
    var Yellow    = Color.decode("#e5c890")
    var Green     = Color.decode("#a6d189")
    var Teal      = Color.decode("#81c8be")
    var Sky       = Color.decode("#99d1db")
    var Sapphire  = Color.decode("#85c1dc")
    var Blue      = Color.decode("#8caaee")
    var Lavender  = Color.decode("#babbf1")

    // === Foreground text and overlays ===

    var Text      = Color.decode("#c6d0f5")
    var Subtext1  = Color.decode("#b5bfe2")
    var Subtext0  = Color.decode("#a5adce")
    var Overlay2  = Color.decode("#949cbb")
    var Overlay1  = Color.decode("#838ba7")
    var Overlay0  = Color.decode("#737994")

    // === Background surfaces ===

    var Surface2  = Color.decode("#626880")
    var Surface1  = Color.decode("#51576d")
    var Surface0  = Color.decode("#414559")
    var Base      = Color.decode("#303446")
    var Mantle    = Color.decode("#292c3c")
    var Crust     = Color.decode("#232634")

    // === Extension function ===

    fun Color.withAlpha(alpha: Int): Color = Color(red, green, blue, alpha)
}
