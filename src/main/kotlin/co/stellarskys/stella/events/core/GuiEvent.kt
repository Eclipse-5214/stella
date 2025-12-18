@file:Suppress("UNUSED")

package co.stellarskys.stella.events.core

import co.stellarskys.stella.events.api.Event
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType


sealed class GuiEvent {
    class RenderHUD(
        val context: GuiGraphics
    ) : Event()

    class Open(
        val screen: Screen
    ) : Event()

    class Close(
        val screen: Screen,
        val handler: AbstractContainerMenu
    ) : Event(cancelable = true)

    class Click(
        val mouseX: Double,
        val mouseY: Double,
        val mouseButton: Int,
        val buttonState: Boolean,
        val screen: Screen
    ) : Event(cancelable = true)

    class Key(
        val keyName: String?,
        val key: Int,
        val character: Char,
        val scanCode: Int,
        val screen: Screen
    ) : Event(cancelable = true)

    sealed class Slot {
        class Click(
            val slot: net.minecraft.world.inventory.Slot?,
            val slotId: Int,
            val button: Int,
            val actionType: ClickType,
            val handler: AbstractContainerMenu,
            val screen: AbstractContainerScreen<*>
        ) : Event(cancelable = true)

        class Render(
            val context: GuiGraphics,
            val slot: net.minecraft.world.inventory.Slot,
            val screen: AbstractContainerScreen<AbstractContainerMenu>
        ) : Event()
    }

    sealed class Container {
        class Content(
            val context: GuiGraphics,
            val mouseX: Int,
            val mouseY: Int
        ) : Event()
    }

    enum class RenderType {
        Pre,
        Post;
    }
}