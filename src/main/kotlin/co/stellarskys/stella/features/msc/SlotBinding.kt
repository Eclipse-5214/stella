package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.config.core.Keybind
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.zenith.*
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.msc.lockUtils.*
import co.stellarskys.stella.mixins.accessors.AccessorContainer
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import java.awt.Color
import kotlin.collections.first
import kotlin.collections.isNullOrEmpty

//? if >= 26.2 {
/*import co.stellarskys.stella.api.zenith.screen
*///? }

@Module
object SlotBinding: Feature("slotLocking.bind", true) {
    val bindKb by config.property<Keybind.Handler>("slotLocking.bindKb")
    val bindColor by config.property<Color>("slotLocking.bindColor")
    val perSlotColor by config.property<Boolean>("slotLocking.perSlotColor")
    val showLines by config.property<Int>("slotLocking.bindLines")

    private var currentHeldSlot: Slot? = null
    private var isBinding = false

    private val slotColors = arrayOf(
        Palette.Green,
        Palette.Peach,
        Palette.Yellow,
        Palette.Red,
        Palette.Pink,
        Palette.Mauve,
        Palette.Purple,
        Palette.Blue,
        Palette.Teal
    )

    override fun initialize() {
        on<GuiEvent.Container.SlotClick> { event ->
            val profile = LockRegistry.getActiveProfile()
            val slot = event.slot
            val screen = client.screen as? AbstractContainerScreen<*> ?: return@on
            if (event.type != ContainerInput.QUICK_MOVE || slot == null || slot.container !is Inventory) return@on

            val srcIdx = slot.containerSlot
            val targets = profile.bound[srcIdx]

            if (targets.isNullOrEmpty()) return@on
            val targetIdx = targets.first()

            profile.promoteTargetToFront(srcIdx, targetIdx)

            val menu = screen.menu
            val containerId = menu.containerId
            val player = player ?: return@on

            if (srcIdx < 9 && targetIdx >= 9) {
                val dstSlot = menu.slots.find { it.container is Inventory && it.containerSlot == targetIdx }
                if (dstSlot != null) client.gameMode?.handleContainerInput(containerId, dstSlot.index, srcIdx, ContainerInput.SWAP, player)
            } else {
                client.gameMode?.handleContainerInput(containerId, slot.index, targetIdx, ContainerInput.SWAP, player)
            }

            event.cancel()
            return@on
        }

        on<GuiEvent.Container.AfterContent> { event ->
            val screen = client.screen as? AbstractContainerScreen<*> ?: return@on
            val profile = LockRegistry.getActiveProfile()
            val hovered = (screen as AccessorContainer).hoveredSlot
            val slotPosMap = mutableMapOf<Int, Pair<Int, Int>>()

            for (slot in screen.menu.slots) {
                if (slot.container is Inventory) {
                    slotPosMap[slot.containerSlot] = Pair(event.x + slot.x, event.y + slot.y)
                }
            }

            for (slot in screen.menu.slots) {
                if (slot.container !is Inventory) continue
                val idx = slot.containerSlot
                val targets = profile.bound[idx] ?: continue
                if (targets.isEmpty()) continue

                val isHoveringBound = hovered != null && hovered.container is Inventory && profile.isSlotBound(hovered.containerSlot)

                targets.forEachIndexed { i, other ->
                    val color = getColorForBind(idx, other)

                    Render2D.drawHollowRect(
                        event.context,
                        event.x + slot.x,
                        event.y + slot.y,
                        16, 16,
                        1,
                        color
                    )

                    val targetPos = slotPosMap[other] ?: return@forEachIndexed

                    if (isHoveringBound) {
                        if (hovered != slot || i > 0) return@forEachIndexed
                    } else if (idx < other) return@forEachIndexed

                    val isShiftDown = InputConstants.isKeyDown(client.window, Zenith.Keys.L_SHIFT)
                    if (showLines == 2 || (showLines == 1 && !isShiftDown)) return@forEachIndexed

                    Render2D.drawLine(
                        event.context,
                        event.x + slot.x + 8f, event.y + slot.y + 8f,
                        targetPos.first + 8f, targetPos.second + 8f,
                        color
                    )
                }
            }

            val source = currentHeldSlot
            if (isBinding && source != null) {
                Render2D.drawLine(
                    event.context,
                    event.x + source.x + 8f, event.y + source.y + 8f,
                    event.mouseX.toFloat(),
                    event.mouseY.toFloat(),
                    Palette.Green
                )
            }
        }


        bindKb.onPress {
            if (!isEnabled()) return@onPress
            isBinding = true
            val screen = client.screen as? AccessorContainer ?: return@onPress
            val slot = screen.hoveredSlot ?: return@onPress
            if (slot.container !is Inventory) return@onPress

            currentHeldSlot = slot
            playLockSound(false)
        }

        bindKb.onRelease {
            if (!isEnabled() || !isBinding) return@onRelease
            isBinding = false

            val sourceSlot = currentHeldSlot ?: return@onRelease
            currentHeldSlot = null

            val screen = client.screen as? AccessorContainer ?: return@onRelease
            val targetSlot = screen.hoveredSlot ?: return@onRelease
            if (targetSlot.container !is Inventory) return@onRelease

            val srcIdx = sourceSlot.containerSlot
            val dstIdx = targetSlot.containerSlot

            LockRegistry.state.update {
                val active = getActive()
                if (srcIdx == dstIdx) {
                    active.clearBinds(srcIdx)
                } else {
                    if (srcIdx >= 9 && dstIdx >= 9) return@update
                    active.addBind(srcIdx, dstIdx)
                }
            }
        }
    }

    private fun getColorForBind(idx: Int, other: Int): Color {
        if (!perSlotColor) return bindColor
        val primaryIndex = minOf(idx, other)
        return slotColors.getOrElse(primaryIndex) { bindColor }
    }
}