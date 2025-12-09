package co.stellarskys.stella.utils

import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import dev.deftu.textile.MutableText
import dev.deftu.textile.Text
import dev.deftu.textile.minecraft.HoverEvent
import dev.deftu.textile.minecraft.MCText
import dev.deftu.textile.minecraft.MCTextStyle
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

object ChatUtils {
    @JvmStatic
    fun sendMessage(message: String) {
        player?.connection?.sendChat(message)
    }

    @JvmStatic
    fun sendCommand(command: String) {
        player?.connection?.sendCommand(command.removePrefix("/"))
    }

    @JvmStatic
    fun fakeMessage(message: Text) {
        client.gui?.chat?.addMessage(MCText.convert(message))
    }

    @JvmStatic
    fun fakeMessage(message: Component) {
        client.gui?.chat?.addMessage(message)
    }

    @JvmStatic
    fun fakeMessage(message: String) {
        fakeMessage(Text.literal(message))
    }

    @JvmStatic
    fun getChatBreak(): String {
        val chatWidth = client.gui?.chat?.width ?: return ""
        val textRenderer = client.font
        val dashWidth = textRenderer.width("-")

        val repeatCount = chatWidth / dashWidth
        return "-".repeat(repeatCount)
    }

    @JvmStatic
    fun getCenteredText(text: String): String {
        val chatWidth = client.gui?.chat?.width ?: return text
        val textRenderer = client.font
        val textWidth = textRenderer.width(text)
        if (textWidth >= chatWidth) return text
        val spaceWidth = textRenderer.width(" ")

        val padding = ((chatWidth - textWidth) / 2f / spaceWidth).roundToInt()
        return " ".repeat(padding) + text
    }

    fun MutableText.onHover(text: Text): MutableText {
        this.setStyle(MCTextStyle(hoverEvent = HoverEvent.ShowText(text)))
        return this
    }

    fun MutableText.onHover(text: String): MutableText {
        this.setStyle(MCTextStyle(hoverEvent = HoverEvent.ShowText(Text.literal(text))))
        return this
    }
}