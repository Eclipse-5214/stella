package co.stellarskys.stella.utils.config.ui

import java.awt.Color

/**
 * Palette – Mutable color palette inspired by Catppuccin Mocha.
 */
object Palette {

    // === Primary color palette ===

    var Rosewater = Color.decode("#f5e0dc")
    var Flamingo  = Color.decode("#f2cdcd")
    var Pink      = Color.decode("#f5c2e7")
    var Mauve     = Color.decode("#cba6f7")
    var Purple    = Color.decode("#865dd4")
    var Red       = Color.decode("#f38ba8")
    var Maroon    = Color.decode("#eba0ac")
    var Peach     = Color.decode("#fab387")
    var Yellow    = Color.decode("#f9e2af")
    var Green     = Color.decode("#a6e3a1")
    var Teal      = Color.decode("#94e2d5")
    var Sky       = Color.decode("#89dceb")
    var Sapphire  = Color.decode("#74c7ec")
    var Blue      = Color.decode("#89b4fa")
    var Lavender  = Color.decode("#b4befe")

    // === Foreground text and overlays ===

    var Text      = Color.decode("#cdd6f4")
    var Subtext1  = Color.decode("#bac2de")
    var Subtext0  = Color.decode("#a6adc8")
    var Overlay2  = Color.decode("#9399b2")
    var Overlay1  = Color.decode("#7f849c")
    var Overlay0  = Color.decode("#6c7086")

    // === Background surfaces ===

    var Surface2  = Color.decode("#585b70")
    var Surface1  = Color.decode("#45475a")
    var Surface0  = Color.decode("#313244")
    var Base      = Color.decode("#1e1e2e")
    var Mantle    = Color.decode("#181825")
    var Crust     = Color.decode("#11111b")

    // === Extension function ===

    fun Color.withAlpha(alpha: Int): Color = Color(red, green, blue, alpha)
}
