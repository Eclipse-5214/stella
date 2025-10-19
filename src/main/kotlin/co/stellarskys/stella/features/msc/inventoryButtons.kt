package co.stellarskys.stella.features.msc

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.GuiEvent
import co.stellarskys.stella.events.WorldEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.msc.buttonUtils.ButtonManager
import co.stellarskys.stella.utils.TimeUtils
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import kotlin.time.Duration.Companion.milliseconds

@Stella.Module
object inventoryButtons : Feature("buttonsEnabled") {
    var lastClick = TimeUtils.zero
    val clickCooldown = 200.milliseconds

    override fun initialize() {
        register<GuiEvent.AfterRender> { event ->
            if (event.screen is InventoryScreen) {
                val invX = (event.screen.width - 176) / 2
                val invY = (event.screen.height - 166) / 2

                event.context.state.goUpLayer()

                ButtonManager.renderAll(event.context, invX, invY)
            }
        }

        register<GuiEvent.Click> { event ->
            if (lastClick.since < clickCooldown) return@register

            val gui = event.screen
            if (gui !is InventoryScreen) return@register

            val mouseX = event.mx.toInt()
            val mouseY = event.my.toInt()
            val mouseButton = event.mbtn

            if (mouseButton != 0) return@register

            lastClick = TimeUtils.now
            ButtonManager.handleMouseClicked(gui, mouseX, mouseY)
        }

        register<WorldEvent.Change> {
            lastClick = TimeUtils.zero
        }
    }

    override fun onRegister() {
        lastClick = TimeUtils.zero
        super.onRegister()
    }
}