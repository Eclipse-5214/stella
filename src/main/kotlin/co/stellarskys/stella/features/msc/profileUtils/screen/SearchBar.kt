package co.stellarskys.stella.features.msc.profileUtils.screen

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.mc.ParentElement
import co.stellarskys.stella.api.horizon.mc.TextHandler
import co.stellarskys.stella.api.horizon.mc.addTo
import net.minecraft.client.gui.GuiGraphicsExtractor
import co.stellarskys.stella.api.zenith.Zenith.Keys
import co.stellarskys.stella.api.zenith.Zenith.Keys.isCtrlDown
import tech.thatgravyboat.skyblockapi.platform.pushPop

class SearchBar : ParentElement() {
    var query = ""
        private set

    val textHandler = TextHandler(
        textProvider = { query },
        textSetter = { query = it },
        scale = 1f,
        textColor = java.awt.Color.WHITE,
        centerIfSmall = false
    ).addTo(this)

    init {
        width = 120f
        height = 15f
        textHandler.width = width - 8f
        textHandler.height = height
        textHandler.x = 4f
        textHandler.y = 0f
        textHandler.textSidePadding = 0f
    }

    override fun keyPressed(keyCode: Int, modifiers: Int): Boolean {
        if (modifiers.isCtrlDown && keyCode == Keys.F) {
            textHandler.isFocused = !textHandler.isFocused
            return true
        }
        return super.keyPressed(keyCode, modifiers)
    }

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        val hovered = isAreaHovered(0f, 0f, width, height, mouseX, mouseY) || textHandler.isFocused

        context.pushPop {
            context.pose().translate(x, y)
            
            ren2d.drawRect(context, 0, 0, width.toInt(), height.toInt(), Palette.Crust.withAlpha(if (hovered) 200 else 150))
            ren2d.drawHollowRect(context, 0, 0, width.toInt(), height.toInt(), 1, Palette.Purple)

            if (query.isEmpty() && !textHandler.isFocused) {
                ren2d.drawString(context, "Search...", 4, 3, 1f, Palette.Overlay1)
            }

            context.pushPop {
                context.pose().translate(textHandler.x, textHandler.y)
                textHandler.render(context, mouseX, mouseY, delta)
            }
        }
    }
}
