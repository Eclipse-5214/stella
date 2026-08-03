package co.stellarskys.stella.features.msc

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.config.core.Keybind
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.api.zenith.player
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.events.core.PlayerEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.msc.lockUtils.*
import co.stellarskys.stella.mixins.accessors.AccessorContainer
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import java.awt.Color

@Module
object SlotLocking: Feature("slotLocking", true) {
    val lockColor by config.property<Color>("slotLocking.lockColor")
    val lockKb by config.property<Keybind.Handler>("slotLocking.lockKb")

    val protectKb by config.property<Keybind.Handler>("slotLocking.protectKb")
    val protectStarred by config.property<Boolean>("slotLocking.starred") // OMG skies.starred reference :D

    private val lockIcon = Identifier.fromNamespaceAndPath(Stella.NAMESPACE, "lock")

    override fun initialize() {
        on<GuiEvent.Container.SlotClick> { event ->
            val profile = LockRegistry.getActiveProfile()
            val slot = event.slot
            val screen = client.screen as? AbstractContainerScreen<*> ?: return@on

            if (slot != null && slot.container is Inventory) {
                val locked = profile.isSlotLocked(slot.containerSlot)
                val protected = screen.isBadMenu() && profile.isItemProtected(slot.item)

                if (locked || protected) {
                    event.cancel(); playBlockSound(); return@on
                }
            }

            if (event.type == ContainerInput.SWAP && event.buttonNum in 0..8) {
                val targetHotbarSlot = event.buttonNum
                if (profile.isSlotLocked(targetHotbarSlot)) {
                    event.cancel(); playBlockSound()
                }
            }

            if (slot != null && event.type == ContainerInput.THROW && profile.isItemProtected(slot.item)) {
                event.cancel(); playBlockSound()
            }

            if (event.slotId == -999 && profile.isItemProtected(screen.menu.carried)) {
                event.cancel(); playBlockSound()
            }
        }

        on<PlayerEvent.DropItem> { event ->
            val profile = LockRegistry.getActiveProfile()
            val playerInv = player?.inventory ?: return@on
            val itemInSlot = playerInv.getItem(event.slot)
            if (!profile.isSlotLocked(event.slot) && !profile.isItemProtected(itemInSlot)) return@on

            event.cancel()
            playBlockSound()
        }

        lockKb.onPress {
            val screen = client.screen as? AccessorContainer ?: return@onPress
            val slot = screen.hoveredSlot ?: return@onPress
            if (slot.container !is Inventory) return@onPress
            val index = slot.containerSlot

            LockRegistry.state.update {
                val active = getActive()
                if (active.isSlotLocked(index)) {
                    active.unlockSlot(index)
                    playLockSound(true)
                } else {
                    active.lockSlot(index)
                    playLockSound(false)
                }
            }
        }

        protectKb.onPress {
            val screen = client.screen as? AccessorContainer ?: return@onPress
            val slot = screen.hoveredSlot ?: return@onPress
            if (slot.container !is Inventory) return@onPress
            val item = slot.item
            if (item.isEmpty) return@onPress

            LockRegistry.state.update {
                val active = getActive()
                if (active.isItemProtected(item)) {
                    active.unprotectItem(item)
                    playLockSound(true)
                } else {
                    active.protectItem(item)
                    playLockSound(false)
                }
            }

        }

        on<GuiEvent.Container.AfterContent> { event ->
            val screen = client.screen as? AbstractContainerScreen<*> ?: return@on
            val profile = LockRegistry.getActiveProfile()

            for (slot in screen.menu.slots) {
                if (slot.container !is Inventory) continue
                val idx = slot.containerSlot

                if (profile.isSlotLocked(idx)) {
                    Render2D.drawSprite(
                        event.context,
                        lockIcon,
                        event.x + slot.x + 1,
                        event.y + slot.y + 1,
                        6, 6,
                        lockColor
                    )
                }
            }
        }
    }
}