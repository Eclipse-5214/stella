package co.stellarskys.stella.hud

import co.stellarskys.stella.utils.DataUtils
import com.google.gson.reflect.TypeToken
import net.minecraft.client.gui.GuiGraphics
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object HUDManager {
    val elements = mutableMapOf<String, HUDElement>()
    val customRenderers = mutableMapOf<String, (GuiGraphics) -> Unit>()
    val customSizes = mutableMapOf<String, Pair<Int, Int>>()

    data class HudLayoutData(
        var x: Float,
        var y: Float,
        var scale: Float = 1f
    )

    private val layoutStore = DataUtils(
        fileName = "hud_positions",
        defaultObject = mutableMapOf<String, HudLayoutData>(),
        typeToken = object : TypeToken<MutableMap<String, HudLayoutData>>() {}
    )

    fun register(id: String, text: String, configKey: String? = null) {
        elements[id] = HUDElement(id, 20f, 20f, 0, 0, text = text, configKey = configKey)
        loadLayout(id)
    }

    fun registerCustom(
        id: String,
        width: Int,
        height: Int,
        renderer: (GuiGraphics) -> Unit,
        configKey: String? =  null
    ) {
        customRenderers[id] = renderer
        customSizes[id] = width to height
        elements[id] = HUDElement(id, 20f, 20f, width, height, configKey = configKey)
        loadLayout(id)
    }

    fun saveAllLayouts() {
        layoutStore.updateAndSave {
            elements.forEach { (id, element) ->
                this[id] = HudLayoutData(element.x, element.y, element.scale)
            }
        }
    }

    fun loadAllLayouts() { layoutStore.getData().keys.forEach { loadLayout(it) } }

    fun loadLayout(id: String) {
        layoutStore.getData()[id]?.let {
            elements[id]?.apply {
                x = it.x
                y = it.y
                scale = it.scale
            }
        }
    }

    fun getX(id: String): Float = elements[id]?.x ?: 0f
    fun getY(id: String): Float = elements[id]?.y ?: 0f
    fun getScale(id: String): Float = elements[id]?.scale ?: 1f
}