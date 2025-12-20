package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.msc.buttonUtils.ButtonManager
import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.events.core.GuiEvent
import dev.deftu.omnicore.api.client.client
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import kotlin.time.Duration.Companion.milliseconds

@Module
object inventoryButtons : Feature("buttonsEnabled",true) {
    var lastClick = TimeUtils.zero
    val clickCooldown = 200.milliseconds

    override fun initialize() {
        on<GuiEvent.Container.Content> { event ->
            val screen = client.screen ?: return@on
            if (screen is InventoryScreen) {
                val invX = (screen.width - 176) / 2
                val invY = (screen.height - 166) / 2
                val width = screen.width.toFloat()
                val height = screen.height.toFloat()

                ButtonManager.renderAll(event.context, invX, invY, width, height)
            }
        }


        on<GuiEvent.Click> { event ->
            if (lastClick.since < clickCooldown) return@on

            val gui = event.screen
            if (gui !is InventoryScreen) return@on

            val mouseX = event.mouseX.toInt()
            val mouseY = event.mouseY.toInt()
            val mouseButton = event.mouseButton

            if (mouseButton != 0) return@on

            lastClick = TimeUtils.now
            ButtonManager.handleMouseClicked(gui, mouseX, mouseY)
        }

        on<LocationEvent.IslandChange> {
            lastClick = TimeUtils.zero
        }
    }

    override fun onRegister() {
        lastClick = TimeUtils.zero
        super.onRegister()
    }
}