package co.stellarskys.stella.features.msc

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Quasar
import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.features.Feature
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.FormattedCharSequence
import java.awt.Color
import java.util.WeakHashMap

@Module
object Cosmetics : Feature("cosmetics") {
    private val sequenceCache = WeakHashMap<FormattedCharSequence, FormattedCharSequence>()
    private val nameCache = mutableMapOf<String, NameData>()
    override fun initialize() { updateNames() }

    @JvmStatic
    fun handleCharSequence(seq: FormattedCharSequence): FormattedCharSequence {
        if (!isEnabled() || nameCache.isEmpty()) return seq
        return sequenceCache.computeIfAbsent(seq) { process(it) }
    }

    fun process(seq: FormattedCharSequence): FormattedCharSequence {
        val sb = StringBuilder()
        seq.accept { _, _, cp -> sb.appendCodePoint(cp); true }
        val full = sb.toString()

        val target = nameCache.keys.find { full.contains(it, true) } ?: return seq
        val data = nameCache[target.lowercase()] ?: return seq

        val parts = full.split(Regex("(?i)$target"), 2)
        val idx = parts[0].codePointCount(0, parts[0].length)
        val targetIdx = target.codePointCount(0, target.length)

        return FormattedCharSequence.composite(
            slice(seq, 0, idx),
            data.getComponent().visualOrderText,
            if (parts.size > 1 && parts[1].isNotEmpty()) process(slice(seq, idx + targetIdx, Int.MAX_VALUE))
            else FormattedCharSequence.EMPTY
        )
    }

    fun slice(source: FormattedCharSequence, start: Int, end: Int) = FormattedCharSequence { sink ->
        var current = 0
        source.accept { index, style, cp ->
            if (current in start..<end) {
                current++
                sink.accept(index, style, cp)
            } else { current++; true }
        }
    }

    fun updateNames() {
        Quasar.fetch<Map<String, NameData>>("${Stella.ETHER}/names.json") { result ->
            result.onSuccess { data ->
                data.forEach { (uuid, ndata) ->
                    HypixelApi.getName(uuid) { name ->
                        name?.let { nameCache[it.lowercase()] = ndata }
                    }
                }
            }.onFailure { Stella.LOGGER.error("Failed to fetch names: ${it.message}") }
        }
    }

    data class ExtraPart(val text: String, val color: String)
    data class NameData(val text: String, val extra: List<ExtraPart>? = null) {
        private var comp: MutableComponent? = null
        private fun toComponent(): MutableComponent = Component.literal(text).also { base ->
            extra?.forEach { part -> base.append(Component.literal(part.text).withColor(Color.decode(part.color).rgb)) }
            comp = base
        }
        fun getComponent(): MutableComponent = comp ?: toComponent()
    }
}