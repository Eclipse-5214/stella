package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.msc.buttonUtils.ButtonManager
import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.events.core.GuiEvent
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import kotlin.time.Duration.Companion.milliseconds

@Module
object inventoryButtons : Feature("buttonsEnabled",true) {
    var lastClick = TimeUtils.zero
    val clickCooldown = 200.milliseconds

    override fun initialize() {
        /*
        register<GuiEvent.RenderHUD> { event ->
            if (event.screen is InventoryScreen) {
                val invX = (event.screen.width - 176) / 2
                val invY = (event.screen.height - 166) / 2
                val width = event.screen.width.toFloat()
                val height = event.screen.height.toFloat()

                ButtonManager.renderAll(event.context, invX, invY, width, height)
            }
        }

         */

        register<GuiEvent.Click> { event ->
            if (lastClick.since < clickCooldown) return@register

            val gui = event.screen
            if (gui !is InventoryScreen) return@register

            val mouseX = event.mouseX.toInt()
            val mouseY = event.mouseY.toInt()
            val mouseButton = event.mouseButton

            if (mouseButton != 0) return@register

            lastClick = TimeUtils.now
            ButtonManager.handleMouseClicked(gui, mouseX, mouseY)
        }

        register<LocationEvent.IslandChange> {
            lastClick = TimeUtils.zero
        }
    }

    override fun onRegister() {
        lastClick = TimeUtils.zero
        super.onRegister()
    }
}