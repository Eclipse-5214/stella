@file:Suppress("UNUSED")

package co.stellarskys.stella.events.core

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import xyz.meowing.knit.api.events.CancellableEvent
import xyz.meowing.knit.api.events.Event

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
    ) : CancellableEvent()

    class Click(
        val mouseX: Double,
        val mouseY: Double,
        val mouseButton: Int,
        val buttonState: Boolean,
        val screen: Screen
    ) : CancellableEvent()

    class Key(
        val keyName: String?,
        val key: Int,
        val character: Char,
        val scanCode: Int,
        val screen: Screen
    ) : CancellableEvent()

    sealed class Slot {
        class Click(
            val slot: net.minecraft.world.inventory.Slot?,
            val slotId: Int,
            val button: Int,
            val actionType: ClickType,
            val handler: AbstractContainerMenu,
            val screen: AbstractContainerScreen<*>
        ) : CancellableEvent()

        class Render(
            val context: GuiGraphics,
            val slot: net.minecraft.world.inventory.Slot,
            val screen: AbstractContainerScreen<AbstractContainerMenu>
        ) : Event()
    }

    enum class RenderType {
        Pre,
        Post;
    }
}