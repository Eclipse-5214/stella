@file:Suppress("UNUSED")

package co.stellarskys.stella.events.core

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.SlotActionType
import xyz.meowing.knit.api.events.CancellableEvent
import xyz.meowing.knit.api.events.Event

sealed class GuiEvent {
    class RenderHUD(
        val context: DrawContext
    ) : Event()

    class Open(
        val screen: Screen
    ) : Event()

    class Close(
        val screen: Screen,
        val handler: ScreenHandler
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
            val slot: net.minecraft.screen.slot.Slot?,
            val slotId: Int,
            val button: Int,
            val actionType: SlotActionType,
            val handler: ScreenHandler,
            val screen: HandledScreen<*>
        ) : CancellableEvent()

        class Render(
            val context: DrawContext,
            val slot: net.minecraft.screen.slot.Slot,
            val screen: HandledScreen<ScreenHandler>
        ) : Event()
    }

    enum class RenderType {
        Pre,
        Post;
    }
}