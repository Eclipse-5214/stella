@file:Suppress("UNUSED")

package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot

sealed class GuiEvent {
    class RenderHUD(
        val context: GuiGraphicsExtractor
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

    sealed class Container {
        class AfterContent(
            val context: GuiGraphicsExtractor,
            val mouseX: Int,
            val mouseY: Int,
            val x: Int,
            val y: Int,
            val width: Int,
            val height: Int,
        ) : Event()

        class SlotClick(
            val screen: AbstractContainerScreen<*>,
            val slot: Slot?,
            val slotId: Int,
            val buttonNum: Int,
            val type: ContainerInput
        ) : Event(true)
    }

    enum class RenderType {
        Pre,
        Post;
    }
}