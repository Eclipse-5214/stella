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
            "Purse" to getCurrencyNetworth(member),
            "Sacks" to getSacksNetworth(member),
            "Pets" to getPetsNetworth(member)
        ).apply {
            with(member.inventory) {
                put("Inventory", invContents.getValue())
                put("E-Chest", eChestContents.getValue())
                put("Backpacks", backpackContents.values.sumOf { it.getValue() })
                put("Armor", invArmor.getValue())
                put("Equipment", equipment.getValue())
                put("Wardrobe", wardrobeContents.getValue())
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
        (m.currencies.purse + m.soloBank + (m.profile?.banking?.balance ?: 0.0)).toLong()

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

    fun SkyblockResponse.Pet.getValue() = (LowestBinAPI.getLowestPrice("pet:${this.type}:${this.tier}") ?: 0L) + Pricing.getPrice(this.heldItem)
    fun SkyblockResponse.InventoryContents.getValue() = this.items().filterNot { it.isEmpty }.sumOf { it.getItemValue().price }
}