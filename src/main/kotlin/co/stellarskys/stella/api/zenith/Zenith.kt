package co.stellarskys.stella.api.zenith

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.Window
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.server.packs.resources.ResourceManager
import kotlin.math.max

@Suppress("UNUSED")
object Zenith {
    @JvmStatic val client: Minecraft get() = Minecraft.getInstance()
    @JvmStatic val player: LocalPlayer? get() = client.player
    @JvmStatic val world: ClientLevel? get() = client.level
    @JvmStatic val textureManager: TextureManager get() = client.textureManager
    @JvmStatic val resourceManager: ResourceManager get() = client.resourceManager
    @JvmStatic val window: Window get() = client.window
    @JvmStatic val windowHandle: Long get() = window.handle()
    @JvmStatic val cam: Camera get() = client.gameRenderer /*? if < 26.2 { */ .mainCamera /*? } else { */ /*.mainCamera() *//*? } */

    object Mouse {
        @JvmStatic val rawX: Double get() = client.mouseHandler.xpos()
        @JvmStatic val rawY: Double get() = client.mouseHandler.ypos()
        @JvmStatic val scaledX: Double get() = rawX * Res.scaledWidth / max(1, Res.windowWidth)
        @JvmStatic val scaledY: Double get() = rawY * Res.scaledHeight / max(1, Res.windowHeight)

        @JvmStatic
        var isCursorGrabbed: Boolean
            get() = client.mouseHandler.isMouseGrabbed
            set(value) = if (value) client.mouseHandler.grabMouse() else client.mouseHandler.releaseMouse()

        @JvmStatic
        fun isMouseButton(code: Int): Boolean = code in LEFT..(LEFT + 7)

        // Only the three primary buttons expose queryable state; 4-8 always report false.
        @JvmStatic
        fun isPressed(code: Int): Boolean = when (code) {
            LEFT -> client.mouseHandler.isLeftPressed
            RIGHT -> client.mouseHandler.isRightPressed
            MIDDLE -> client.mouseHandler.isMiddlePressed
            else -> false
        }

        const val LEFT = InputConstants.MOUSE_BUTTON_LEFT
        const val RIGHT = InputConstants.MOUSE_BUTTON_RIGHT
        const val MIDDLE = InputConstants.MOUSE_BUTTON_MIDDLE

    }

    object Keys {
        const val A = InputConstants.KEY_A
        const val B = InputConstants.KEY_B
        const val C = InputConstants.KEY_C
        const val D = InputConstants.KEY_D
        const val E = InputConstants.KEY_E
        const val F = InputConstants.KEY_F
        const val G = InputConstants.KEY_G
        const val H = InputConstants.KEY_H
        const val I = InputConstants.KEY_I
        const val J = InputConstants.KEY_J
        const val K = InputConstants.KEY_K
        const val L = InputConstants.KEY_L
        const val M = InputConstants.KEY_M
        const val N = InputConstants.KEY_N
        const val O = InputConstants.KEY_O
        const val P = InputConstants.KEY_P
        const val Q = InputConstants.KEY_Q
        const val R = InputConstants.KEY_R
        const val S = InputConstants.KEY_S
        const val T = InputConstants.KEY_T
        const val U = InputConstants.KEY_U
        const val V = InputConstants.KEY_V
        const val W = InputConstants.KEY_W
        const val X = InputConstants.KEY_X
        const val Y = InputConstants.KEY_Y
        const val Z = InputConstants.KEY_Z

        // Numbers (Top Row)
        const val N_0 = InputConstants.KEY_0
        const val N_1 = InputConstants.KEY_1
        const val N_2 = InputConstants.KEY_2
        const val N_3 = InputConstants.KEY_3
        const val N_4 = InputConstants.KEY_4
        const val N_5 = InputConstants.KEY_5
        const val N_6 = InputConstants.KEY_6
        const val N_7 = InputConstants.KEY_7
        const val N_8 = InputConstants.KEY_8
        const val N_9 = InputConstants.KEY_9

        // Function Keys
        const val F1 = InputConstants.KEY_F1
        const val F2 = InputConstants.KEY_F2
        const val F3 = InputConstants.KEY_F3
        const val F4 = InputConstants.KEY_F4
        const val F5 = InputConstants.KEY_F5
        const val F6 = InputConstants.KEY_F6
        const val F7 = InputConstants.KEY_F7
        const val F8 = InputConstants.KEY_F8
        const val F9 = InputConstants.KEY_F9
        const val F10 = InputConstants.KEY_F10
        const val F11 = InputConstants.KEY_F11
        const val F12 = InputConstants.KEY_F12

        // Navigation & Editing
        const val ESCAPE = InputConstants.KEY_ESCAPE
        const val ENTER = InputConstants.KEY_RETURN
        const val TAB = InputConstants.KEY_TAB
        const val BACKSPACE = InputConstants.KEY_BACKSPACE
        const val INSERT = InputConstants.KEY_INSERT
        const val DELETE = InputConstants.KEY_DELETE
        const val RIGHT = InputConstants.KEY_RIGHT
        const val LEFT = InputConstants.KEY_LEFT
        const val DOWN = InputConstants.KEY_DOWN
        const val UP = InputConstants.KEY_UP
        const val PAGE_UP = InputConstants.KEY_PAGEUP
        const val PAGE_DOWN = InputConstants.KEY_PAGEDOWN
        const val HOME = InputConstants.KEY_HOME
        const val END = InputConstants.KEY_END
        const val CAPS_LOCK = InputConstants.KEY_CAPSLOCK
        const val SCROLL_LOCK = InputConstants.KEY_SCROLLLOCK
        const val NUM_LOCK = InputConstants.KEY_NUMLOCK
        const val PRINT_SCREEN = InputConstants.KEY_PRINTSCREEN
        const val PAUSE = InputConstants.KEY_PAUSE

        // Keypad
        const val KP_0 = InputConstants.KEY_NUMPAD0
        const val KP_1 = InputConstants.KEY_NUMPAD1
        const val KP_2 = InputConstants.KEY_NUMPAD2
        const val KP_3 = InputConstants.KEY_NUMPAD3
        const val KP_4 = InputConstants.KEY_NUMPAD4
        const val KP_5 = InputConstants.KEY_NUMPAD5
        const val KP_6 = InputConstants.KEY_NUMPAD6
        const val KP_7 = InputConstants.KEY_NUMPAD7
        const val KP_8 = InputConstants.KEY_NUMPAD8
        const val KP_9 = InputConstants.KEY_NUMPAD9
        const val KP_MULTIPLY = InputConstants.KEY_MULTIPLY
        const val KP_ADD = InputConstants.KEY_ADD
        const val KP_ENTER = InputConstants.KEY_NUMPADENTER
        const val KP_EQUAL = InputConstants.KEY_NUMPADEQUALS

        // Modifiers
        const val L_SHIFT = InputConstants.KEY_LSHIFT
        const val L_CONTROL = InputConstants.KEY_LCONTROL
        const val L_ALT = InputConstants.KEY_LALT
        const val R_SHIFT = InputConstants.KEY_RSHIFT
        const val R_CONTROL = InputConstants.KEY_RCONTROL
        const val R_ALT = InputConstants.KEY_RALT

        // Punctuation & Misc
        const val SPACE = InputConstants.KEY_SPACE
        const val APOSTROPHE = InputConstants.KEY_APOSTROPHE   /* ' */
        const val COMMA = InputConstants.KEY_COMMA             /* , */
        const val MINUS = InputConstants.KEY_MINUS             /* - */
        const val PERIOD = InputConstants.KEY_PERIOD           /* . */
        const val SLASH = InputConstants.KEY_SLASH             /* / */
        const val SEMICOLON = InputConstants.KEY_SEMICOLON     /* ; */
        const val EQUAL = InputConstants.KEY_EQUALS            /* = */
        const val L_BRACKET = InputConstants.KEY_LBRACKET      /* [ */
        const val BACKSLASH = InputConstants.KEY_BACKSLASH     /* \ */
        const val R_BRACKET = InputConstants.KEY_RBRACKET      /* ] */
        const val GRAVE_ACCENT = InputConstants.KEY_GRAVE      /* ` */

        val Int.isShiftDown get() = (this and InputConstants.MOD_SHIFT) != 0
        val Int.isCtrlDown get() = (this and InputConstants.MOD_CONTROL) != 0
        val Int.isAltDown get() = (this and InputConstants.MOD_ALT) != 0

        fun isDown(key: Int) = InputConstants.isKeyDown(/*? if < 26.3 {*/client.window,/*?}*/ key)
        fun name(key: Int) = InputConstants.Type/*? if < 26.3 { */ .KEYSYM /*? } else { *//*.KEYBOARD*//*? } */.getOrCreate(key).displayName.string
    }

    object Res {
        @JvmStatic val windowWidth: Int get() = client.window.screenWidth
        @JvmStatic val windowHeight: Int get() = client.window.screenHeight
        @JvmStatic val viewportWidth: Int get() = client.window.width
        @JvmStatic val viewportHeight: Int get() = client.window.height
        @JvmStatic val scaledWidth: Int get() = client.window.guiScaledWidth
        @JvmStatic val scaledHeight: Int get() = client.window.guiScaledHeight
        @JvmStatic val scaleFactor: Double get() = client.window.guiScale.toDouble()
    }
}
