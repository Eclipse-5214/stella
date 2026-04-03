package co.stellarskys.stella.api.handlers

import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.api.zenith.player
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor
import kotlin.math.roundToInt

object Signal {
    const val LINE = "---------------------------------------------"

    @JvmStatic
    fun sendMessage(message: String) {
        player?.connection?.sendChat(message)
    }

    @JvmStatic
    fun sendCommand(command: String) {
        player?.connection?.sendCommand(command.removePrefix("/"))
    }

    @JvmStatic
    fun fakeMessage(message: Component) {
        client.gui?.chat?.addMessage(message)
    }

    @JvmStatic
    fun fakeMessage(message: String) {
        fakeMessage(Component.literal(message))
    }

    @JvmStatic
    fun width() = client.options.chatWidth().get().toInt()

    @JvmStatic
    fun getChatBreak(): String {
        val chatWidth = width()
        val textRenderer = client.font
        val dashWidth = textRenderer.width("-")

        val repeatCount = chatWidth / dashWidth
        return "-".repeat(repeatCount)
    }

    @JvmStatic
    fun getCenteredText(text: String): String {
        val chatWidth = width()
        val textRenderer = client.font
        val textWidth = textRenderer.width(text)
        if (textWidth >= chatWidth) return text
        val spaceWidth = textRenderer.width(" ")

        val padding = ((chatWidth - textWidth) / 2f / spaceWidth).roundToInt()
        return " ".repeat(padding) + text
    }

    fun MutableComponent.onHover(text: Component): MutableComponent {
        this.withStyle{ it.withHoverEvent(HoverEvent.ShowText(text)) }
        return this
    }

    fun MutableComponent.onHover(text: String): MutableComponent {
        this.withStyle{ it.withHoverEvent(HoverEvent.ShowText(Component.literal(text))) }
        return this
    }

    fun MutableComponent.color(rgba: Int): MutableComponent {
        this.withStyle{ it.withColor(rgba) }
        return this
    }

    fun MutableComponent.color(textColor: TextColor): MutableComponent {
        this.withStyle{ it.withColor(textColor) }
        return this
    }
}