package co.stellarskys.stella.features.msc.lockUtils

import co.stellarskys.stella.features.msc.SlotLocking.protectStarred
import net.minecraft.world.item.ItemStack

data class LockData(
    var activeProfile: String = "default",
    val profiles: MutableMap<String, LockProfile> = mutableMapOf("default" to LockProfile())
) {
    fun getActive() = profiles.getOrPut(activeProfile) { LockProfile() }
    fun listProfiles(): List<String> = profiles.keys.toList()

    data class LockProfile (
        val lockedOverworld: MutableSet<Int> = mutableSetOf(),
        val lockedRift: MutableSet<Int> = mutableSetOf(),
        val boundOverworld: MutableMap<Int, MutableList<Int>> = mutableMapOf(),
        val boundRift: MutableMap<Int, MutableList<Int>> = mutableMapOf(),
        val lockedUUIDs: MutableSet<String> = mutableSetOf(),
        val lockedItemIds: MutableSet<String> = mutableSetOf()
    ) {
        val locked get() = if (isRift) lockedRift else lockedOverworld
        val bound get() = if (isRift) boundRift else boundOverworld

        fun lockSlot(index: Int) = locked.add(index)
        fun unlockSlot(index: Int) = locked.remove(index)
        fun isSlotLocked(index: Int) = locked.contains(index)

        fun protectItem(item: ItemStack) {
            val key = item.getKey() ?: return
            if (key.isUuid) lockedUUIDs.add(key.id) else lockedItemIds.add(key.id)
        }

        fun unprotectItem(item: ItemStack) {
            val key = item.getKey() ?: return
            if (key.isUuid) lockedUUIDs.remove(key.id) else lockedItemIds.remove(key.id)
        }

        fun isItemProtected(item: ItemStack): Boolean {
            if (item.isEmpty) return false
            if (item.isStarred() && protectStarred) return true
            val key = item.getKey() ?: return false
            return if (key.isUuid) lockedUUIDs.contains(key.id) else lockedItemIds.contains(key.id)
        }

        fun isSlotBound(index: Int): Boolean = bound[index]?.isNotEmpty() == true

        fun addBind(src: Int, dst: Int) {
            if (src == dst) return
            val srcList = bound.getOrPut(src) { mutableListOf() }
            val dstList = bound.getOrPut(dst) { mutableListOf() }
            if (dst !in srcList) srcList.add(dst)
            if (src !in dstList) dstList.add(src)
        }

        fun clearBinds(index: Int) {
            val connections = bound.remove(index) ?: return
            connections.forEach { other ->
                val otherList = bound[other]
                otherList?.remove(index)
                if (otherList?.isEmpty() == true) bound.remove(other)
            }
        }

        fun promoteTargetToFront(src: Int, target: Int) {
            fun swapToFront(list: MutableList<Int>?, element: Int) {
                if (list == null) return
                val idx = list.indexOf(element)
                if (idx > 0) list[0] = list[idx].also { list[idx] = list[0] }
            }
            swapToFront(bound[src], target)
            swapToFront(bound[target], src)
        }
    }
}