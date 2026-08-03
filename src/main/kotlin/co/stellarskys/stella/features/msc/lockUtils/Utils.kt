package co.stellarskys.stella.features.msc.lockUtils

import co.stellarskys.stella.api.zenith.player
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.extentions.getApiId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

val isRift get() = SkyBlockIsland.THE_RIFT.inIsland()

fun playBlockSound() {
    player?.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f)
}

fun playLockSound(locked: Boolean) {
    player?.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, if (locked) 0.1f else 1f)
}

data class ItemKey(val id: String, val isUuid: Boolean)

fun ItemStack.isStarred() = this.getData(DataTypes.STAR_COUNT) != null
fun ItemStack.getKey(): ItemKey? {
    if (this.isEmpty) return null
    val uuid = this.getData(DataTypes.UUID)?.toString()
    if(!uuid.isNullOrBlank()) return ItemKey(uuid, true)

    val apiId = this.getApiId()
    if (!apiId.isNullOrBlank()) return ItemKey(apiId, false)
    return null
}

fun AbstractContainerScreen<*>.isBadMenu(): Boolean {
    val title = this.title.string
    if (title == "Salvage Items" || title.startsWith("You ")) return true
    return this.isSellMenu()
}

fun AbstractContainerScreen<*>.isSellMenu(): Boolean {
    val sellSlot = this.menu.items.getOrNull(49) ?: return false
    val first = sellSlot.getLore().firstOrNull()?.stripped
    val last = sellSlot.getLore().lastOrNull()?.stripped
    return first == "Click items in your inventory to sell" || last == "Click to buyback!"
}