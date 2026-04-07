package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.msc.buttonUtils.ButtonManager
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.utils.config
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import kotlin.time.Duration.Companion.milliseconds

@Module
object InventoryButtons : Feature("buttons",true) {
    private var lastClick = Chronos.zero
    private val clickCooldown = 200.milliseconds

    private val invOnly by config.property<Boolean>("buttons.invOnly")
    private val hideInTerms by config.property<Boolean>("buttons.hideInTerms")

    private val menueBlacklist = setOf("RecipeViewScreen")

    private val dungeonMenus = setOf(
        "Spirit Leap", "Revive A Teammate", "Click in order!",
        "Click the button on time!", "Correct all the panes!", "Change all to same color!"
    )

    private val dungeonMenuPrefixes = listOf("What starts with", "Select all the")

    override fun initialize() {
        on<GuiEvent.Container.Content> { event ->
            val screen = client.screen ?: return@on
            if (!validScreen(screen)) return@on
            val sw = screen.width.toFloat()
            val sh = screen.height.toFloat()

            ButtonManager.renderAll(
                event.context,
                event.x, event.y,
                event.width, event.height,
                sw, sh
            )
        }

        on<GuiEvent.Click> { event ->
            if (lastClick.since < clickCooldown) return@on

            val gui = event.screen
            if (!validScreen(gui)) return@on

            val mouseX = event.mouseX.toInt()
            val mouseY = event.mouseY.toInt()
            val mouseButton = event.mouseButton

            if (mouseButton != 0) return@on

            lastClick = Chronos.now
            ButtonManager.handleMouseClicked(gui, mouseX, mouseY)
        }

        on<LocationEvent.IslandChange> {
            lastClick = Chronos.zero
        }
    }

    override fun onRegister() {
        lastClick = Chronos.zero
        super.onRegister()
    }

    private fun isTerm(title: String): Boolean = title in dungeonMenus ||
            dungeonMenuPrefixes.any { title.startsWith(it) }

    private fun validScreen(screen: Screen): Boolean {
        if (screen !is InventoryScreen && invOnly) return false
        if (screen.javaClass.simpleName in menueBlacklist) return false
        if (isTerm(screen.title.stripped) && hideInTerms) return false
        return true
    }
}