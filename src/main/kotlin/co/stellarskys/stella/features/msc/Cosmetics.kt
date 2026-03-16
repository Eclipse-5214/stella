package co.stellarskys.stella.features.msc

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Quasar
import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.features.Feature
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.util.FormattedCharSequence
import java.awt.Color

@Module
object Cosmetics : Feature("cosmetics") {
    private val nameCache = mutableMapOf<String, NameData>()
    override fun initialize() { updateNames() }

    @JvmStatic
    fun handleCharSequence(seq: FormattedCharSequence): FormattedCharSequence {
        if (!isEnabled() || nameCache.isEmpty()) return seq
        val rebuilt = Component.literal("")
        var currentStyle: Style? = null
        val buffer = StringBuilder()

        seq.accept { _, style, codePoint ->
            if (style != currentStyle) {
                if (buffer.isNotEmpty()) rebuilt.append(Component.literal(buffer.toString()).withStyle(currentStyle)).also { buffer.clear() }
                currentStyle = style
            }
            buffer.append(codePoint.toChar())
            true
        }

        if (buffer.isNotEmpty()) rebuilt.append(Component.literal(buffer.toString()).withStyle(currentStyle))
        if (nameCache.keys.none { rebuilt.string.contains(it, ignoreCase = true) }) return seq
        return rebuildComponent(rebuilt).visualOrderText
    }

    private fun rebuildComponent(comp: Component): MutableComponent {
        val contents = comp.contents
        return (if (contents is PlainTextContents) {
            val text = contents.text()
            val target = nameCache.keys.find { text.contains(it, ignoreCase = true) }
            if (target != null) injectReplacement(text, target, nameCache[target.lowercase()]!!, comp.style)
            else (comp.copy() as MutableComponent).apply { siblings.clear() }
        } else (comp.copy() as MutableComponent).apply { siblings.clear() } )
            .apply { comp.siblings.forEach { append(rebuildComponent(it)) } }
    }

    private fun injectReplacement(text: String, target: String, data: NameData, style: Style): MutableComponent {
        val parts = text.split(Regex("(?i)$target"), limit = 2)
        return Component.literal("").apply {
            if (parts[0].isNotEmpty()) append(Component.literal(parts[0]).withStyle(style))
            append(data.getComponent())
            if (parts.size > 1 && parts[1].isNotEmpty()) {
                val next = nameCache.keys.find { parts[1].contains(it, ignoreCase = true) }
                append(if (next != null) injectReplacement(parts[1], next, nameCache[next.lowercase()]!!, style)
                else Component.literal(parts[1]).withStyle(style))
            }
        }
    }

    fun updateNames() {
        Quasar.fetch<Map<String, NameData>>("https://ether.stellarskys.co/names.json") { result ->
            result.onSuccess { data ->
                data.forEach { (uuid, ndata) ->
                    HypixelApi.getName(uuid) { name ->
                        name?.let { nameCache[it.lowercase()] = ndata }
                    }
                }
            }.onFailure { Stella.LOGGER.error("Failed to fetch names: ${it.message}") }
        }
    }

    data class NameData(val text: String, val extra: List<ExtraPart>? = null) {
        private var comp: MutableComponent? = null
        private fun toComponent(): MutableComponent = Component.literal(text).also { base ->
            extra?.forEach { part -> base.append(Component.literal(part.text).withColor(Color.decode(part.color).rgb)) }
            comp = base
        }
        fun getComponent(): MutableComponent = comp ?: toComponent()
    }

    data class ExtraPart(val text: String, val color: String)
}