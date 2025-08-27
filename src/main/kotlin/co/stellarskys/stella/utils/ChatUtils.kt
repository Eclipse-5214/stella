package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.utils.TimeUtils.millis
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

object ChatUtils {
    private var nextAvailableTime = TimeUtils.zero

    private fun schedule(action: () -> Unit) {
        val now = TimeUtils.now
        nextAvailableTime = maxOf(now, nextAvailableTime)
        val delay = nextAvailableTime.until.millis / 50.0

        TickUtils.schedule(ceil(delay).toLong()) {
            action()
        }

        nextAvailableTime += 100.milliseconds
    }

    fun chat(message: String) {
        val player = mc.player ?: return
        schedule {
            player.networkHandler?.sendChatMessage(message)
        }
    }

    fun command(command: String) {
        val player = mc.player ?: return
        val cmd = if (command.startsWith("/")) command else "/$command"
        schedule {
            player.networkHandler?.sendChatCommand(cmd.substring(1))
        }
    }

    @JvmOverloads
    fun addMessage(
        message: String,
        hover: String? = null,
        clickAction: ClickEvent.Action? = null,
        clickValue: String? = null,
        siblingText: String? = null
    ) {
        val component = Text.literal(message) as MutableText

        siblingText?.let { text ->
            val sibling = Text.literal(text).apply {
                style = createStyle(hover, clickAction, clickValue)
            }
            component.append(sibling)
        } ?: run {
            component.style = createStyle(hover, clickAction, clickValue)
        }

        mc.inGameHud.chatHud.addMessage(component)
    }

    fun createStyle(hover: String?, clickAction: ClickEvent.Action?, clickValue: String?): Style {
        var style = Style.EMPTY

        hover?.let {
            style = style.withHoverEvent(HoverEvent.ShowText(Text.literal(it)))
        }

        if (clickAction != null && clickValue != null) {
            val clickEvent = when (clickAction) {
                ClickEvent.Action.RUN_COMMAND -> ClickEvent.RunCommand(clickValue)
                ClickEvent.Action.SUGGEST_COMMAND -> ClickEvent.SuggestCommand(clickValue)
                ClickEvent.Action.OPEN_URL -> ClickEvent.OpenUrl(java.net.URI.create(clickValue))
                ClickEvent.Action.OPEN_FILE -> ClickEvent.OpenFile(clickValue)
                ClickEvent.Action.CHANGE_PAGE -> ClickEvent.ChangePage(clickValue.toIntOrNull() ?: 1)
                ClickEvent.Action.COPY_TO_CLIPBOARD -> ClickEvent.CopyToClipboard(clickValue)
                // TODO: add?
                else -> ClickEvent.RunCommand("Not Added.")
            }
            style = style.withClickEvent(clickEvent)
        }

        return style
    }

    private data class Threshold(val value: Double, val symbol: String, val precision: Int)
    private val thresholds = listOf(Threshold(1e9, "b", 1), Threshold(1e6, "m", 1), Threshold(1e3, "k", 1))

    fun formatNumber(number: String): String {
        return try {
            val num = number.replace(",", "").toDouble()
            val threshold = thresholds.find { num >= it.value }

            if (threshold != null) {
                val formatted = num / threshold.value
                val rounded = String.format("%.${threshold.precision}f", formatted).toDouble()
                "${rounded}${threshold.symbol}"
            } else {
                if (num == num.toLong().toDouble()) num.toLong().toString()
                else num.toString()
            }
        } catch (e: NumberFormatException) {
            number
        }
    }
}