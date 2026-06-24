package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.hypixel.SkyblockResponse
import tech.thatgravyboat.skyblockapi.api.item.calculator.getItemValue
import tech.thatgravyboat.skyblockapi.api.remote.hypixel.pricing.LowestBinAPI
import tech.thatgravyboat.skyblockapi.api.remote.hypixel.pricing.Pricing

object NetworthUtils {
    data class NetworthResult(val total: Long, val breakdown: Map<String, Long>) {
        fun getFormatted() = breakdown.filter { it.value > 0 }.entries.joinToString("\n") { "§2${it.key}§7: §6%,d".format(it.value) }
    }

    fun getProfileNetworth(member: SkyblockResponse.SkyblockMember): NetworthResult {
        val breakdown = mutableMapOf(
            "Currency" to getCurrencyNetworth(member),
            "Sacks" to getSacksNetworth(member),
            "Pets" to getPetsNetworth(member),
            "Museum" to member.museumValue
        ).apply {
            with(member.inventory) {
                put("Inventory", invContents.getValue())
                put("E-Chest", eChestContents.getValue())
                put("Backpacks", backpackContents.values.sumOf { it.getValue() })
                put("Armors", invArmor.getValue() + fullWardrobe.filterNot { it.isEmpty }.sumOf { it.getItemValue().price })
                put("Equipment", equipment.getValue())
                put("Talisman Bag", bags.talismanBag.getValue())
                put("Fishing Bag", bags.fishingBag.getValue())
                put("Quiver", bags.quiver.getValue())
                put("Personal Vault", personalVault.getValue())
            }
        }
        return NetworthResult(breakdown.values.sum(), breakdown)
    }

    fun getPetsNetworth(m: SkyblockResponse.SkyblockMember) = m.petsData.pets.sumOf { it.getValue() }

    fun getCurrencyNetworth(m: SkyblockResponse.SkyblockMember) =
        (m.currencies.purse + (m.memberProfile?.personalBank ?: 0.0) + (m.profile?.banking?.balance ?: 0.0)).toLong()

    fun getSacksNetworth(m: SkyblockResponse.SkyblockMember) =
        m.inventory.sacks.entries.sumOf { (id, amt) -> Pricing.getPrice(id) * amt }

    fun Long.toReadable(): String {
        return when {
            this >= 1_000_000_000 -> String.format("%.2fB", this / 1_000_000_000.0)
            this >= 1_000_000 -> String.format("%.2fM", this / 1_000_000.0)
            this >= 1_000 -> String.format("%.1fk", this / 1_000.0)
            else -> this.toString()
        }
    }

    fun SkyblockResponse.Pet.getValue(): Long {
        val petKey = "pet:${this.type.uppercase()}:${this.tier.uppercase()}"
        val petBase = LowestBinAPI.getLowestPrice(petKey) ?: 0L
        val heldItemVal = this.heldItem?.let {
            val key = if (it.uppercase().startsWith("PET_ITEM_")) it.uppercase() else "PET_ITEM_${it.uppercase()}"
            LowestBinAPI.getLowestPrice(key) ?: Pricing.getPrice(it)
        } ?: 0L
        val skinVal = this.skin?.let {
            val id = it.uppercase()
            val queryId = if (id.startsWith("PET_SKIN_")) id else "PET_SKIN_$id"
            LowestBinAPI.getLowestPrice(queryId) ?: LowestBinAPI.getLowestPrice(it) ?: Pricing.getPrice(it)
        } ?: 0L
        return petBase + heldItemVal + skinVal
    }
    fun SkyblockResponse.InventoryContents.getValue() = this.items().filterNot { it.isEmpty }.sumOf { it.getItemValue().price }
}