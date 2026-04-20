package co.stellarskys.stella.api.zenith

import com.mojang.blaze3d.platform.Window
import net.minecraft.SharedConstants
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.server.packs.resources.ResourceManager
import org.lwjgl.glfw.GLFW.*
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
    @JvmStatic val cam: Camera get() = client.gameRenderer.mainCamera

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
        fun isMouseButton(code: Int): Boolean = code in GLFW_MOUSE_BUTTON_1..GLFW_MOUSE_BUTTON_8

        @JvmStatic
        fun isPressed(code: Int): Boolean {
            if (!isMouseButton(code)) {
                return false
            }

            val state = glfwGetMouseButton(windowHandle, code)
            return state == GLFW_PRESS || state == GLFW_REPEAT
        }

        const val LEFT = GLFW_MOUSE_BUTTON_LEFT
        const val RIGHT = GLFW_MOUSE_BUTTON_RIGHT
        const val MIDDLE = GLFW_MOUSE_BUTTON_MIDDLE

    }

    object Keys {
        const val A = GLFW_KEY_A
        const val B = GLFW_KEY_B
        const val C = GLFW_KEY_C
        const val D = GLFW_KEY_D
        const val E = GLFW_KEY_E
        const val F = GLFW_KEY_F
        const val G = GLFW_KEY_G
        const val H = GLFW_KEY_H
        const val I = GLFW_KEY_I
        const val J = GLFW_KEY_J
        const val K = GLFW_KEY_K
        const val L = GLFW_KEY_L
        const val M = GLFW_KEY_M
        const val N = GLFW_KEY_N
        const val O = GLFW_KEY_O
        const val P = GLFW_KEY_P
        const val Q = GLFW_KEY_Q
        const val R = GLFW_KEY_R
        const val S = GLFW_KEY_S
        const val T = GLFW_KEY_T
        const val U = GLFW_KEY_U
        const val V = GLFW_KEY_V
        const val W = GLFW_KEY_W
        const val X = GLFW_KEY_X
        const val Y = GLFW_KEY_Y
        const val Z = GLFW_KEY_Z

        // Numbers (Top Row)
        const val N_0 = GLFW_KEY_0
        const val N_1 = GLFW_KEY_1
        const val N_2 = GLFW_KEY_2
        const val N_3 = GLFW_KEY_3
        const val N_4 = GLFW_KEY_4
        const val N_5 = GLFW_KEY_5
        const val N_6 = GLFW_KEY_6
        const val N_7 = GLFW_KEY_7
        const val N_8 = GLFW_KEY_8
        const val N_9 = GLFW_KEY_9

        // Function Keys
        const val F1 = GLFW_KEY_F1
        const val F2 = GLFW_KEY_F2
        const val F3 = GLFW_KEY_F3
        const val F4 = GLFW_KEY_F4
        const val F5 = GLFW_KEY_F5
        const val F6 = GLFW_KEY_F6
        const val F7 = GLFW_KEY_F7
        const val F8 = GLFW_KEY_F8
        const val F9 = GLFW_KEY_F9
        const val F10 = GLFW_KEY_F10
        const val F11 = GLFW_KEY_F11
        const val F12 = GLFW_KEY_F12

        // Navigation & Editing
        const val ESCAPE = GLFW_KEY_ESCAPE
        const val ENTER = GLFW_KEY_ENTER
        const val TAB = GLFW_KEY_TAB
        const val BACKSPACE = GLFW_KEY_BACKSPACE
        const val INSERT = GLFW_KEY_INSERT
        const val DELETE = GLFW_KEY_DELETE
        const val RIGHT = GLFW_KEY_RIGHT
        const val LEFT = GLFW_KEY_LEFT
        const val DOWN = GLFW_KEY_DOWN
        const val UP = GLFW_KEY_UP
        const val PAGE_UP = GLFW_KEY_PAGE_UP
        const val PAGE_DOWN = GLFW_KEY_PAGE_DOWN
        const val HOME = GLFW_KEY_HOME
        const val END = GLFW_KEY_END
        const val CAPS_LOCK = GLFW_KEY_CAPS_LOCK
        const val SCROLL_LOCK = GLFW_KEY_SCROLL_LOCK
        const val NUM_LOCK = GLFW_KEY_NUM_LOCK
        const val PRINT_SCREEN = GLFW_KEY_PRINT_SCREEN
        const val PAUSE = GLFW_KEY_PAUSE

        // Keypad
        const val KP_0 = GLFW_KEY_KP_0
        const val KP_1 = GLFW_KEY_KP_1
        const val KP_2 = GLFW_KEY_KP_2
        const val KP_3 = GLFW_KEY_KP_3
        const val KP_4 = GLFW_KEY_KP_4
        const val KP_5 = GLFW_KEY_KP_5
        const val KP_6 = GLFW_KEY_KP_6
        const val KP_7 = GLFW_KEY_KP_7
        const val KP_8 = GLFW_KEY_KP_8
        const val KP_9 = GLFW_KEY_KP_9
        const val KP_DECIMAL = GLFW_KEY_KP_DECIMAL
        const val KP_DIVIDE = GLFW_KEY_KP_DIVIDE
        const val KP_MULTIPLY = GLFW_KEY_KP_MULTIPLY
        const val KP_SUBTRACT = GLFW_KEY_KP_SUBTRACT
        const val KP_ADD = GLFW_KEY_KP_ADD
        const val KP_ENTER = GLFW_KEY_KP_ENTER
        const val KP_EQUAL = GLFW_KEY_KP_EQUAL

        // Modifiers
        const val L_SHIFT = GLFW_KEY_LEFT_SHIFT
        const val L_CONTROL = GLFW_KEY_LEFT_CONTROL
        const val L_ALT = GLFW_KEY_LEFT_ALT
        const val L_SUPER = GLFW_KEY_LEFT_SUPER
        const val R_SHIFT = GLFW_KEY_RIGHT_SHIFT
        const val R_CONTROL = GLFW_KEY_RIGHT_CONTROL
        const val R_ALT = GLFW_KEY_RIGHT_ALT
        const val R_SUPER = GLFW_KEY_RIGHT_SUPER
        const val MENU = GLFW_KEY_MENU

        // Punctuation & Misc
        const val SPACE = GLFW_KEY_SPACE
        const val APOSTROPHE = GLFW_KEY_APOSTROPHE   /* ' */
        const val COMMA = GLFW_KEY_COMMA             /* , */
        const val MINUS = GLFW_KEY_MINUS             /* - */
        const val PERIOD = GLFW_KEY_PERIOD           /* . */
        const val SLASH = GLFW_KEY_SLASH             /* / */
        const val SEMICOLON = GLFW_KEY_SEMICOLON     /* ; */
        const val EQUAL = GLFW_KEY_EQUAL             /* = */
        const val L_BRACKET = GLFW_KEY_LEFT_BRACKET  /* [ */
        const val BACKSLASH = GLFW_KEY_BACKSLASH     /* \ */
        const val R_BRACKET = GLFW_KEY_RIGHT_BRACKET /* ] */
        const val GRAVE_ACCENT = GLFW_KEY_GRAVE_ACCENT /* ` */
        const val WORLD_1 = GLFW_KEY_WORLD_1         /* non-US #1 */
        const val WORLD_2 = GLFW_KEY_WORLD_2         /* non-US #2 */

        // Null Key
        const val NONE = GLFW_KEY_UNKNOWN

        val Int.isShiftDown get() = (this and GLFW_MOD_SHIFT) != 0
        val Int.isCtrlDown get() = (this and GLFW_MOD_CONTROL) != 0
        val Int.isAltDown get() = (this and GLFW_MOD_ALT) != 0
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