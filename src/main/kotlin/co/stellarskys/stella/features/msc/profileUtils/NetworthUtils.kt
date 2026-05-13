package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.hypixel.SkyblockResponse
import tech.thatgravyboat.skyblockapi.api.item.calculator.getItemValue
import tech.thatgravyboat.skyblockapi.api.remote.hypixel.pricing.LowestBinAPI
import tech.thatgravyboat.skyblockapi.api.remote.hypixel.pricing.Pricing

object NetworthUtils {
    fun getProfileNetworth(member: SkyblockResponse.SkyblockMember) = getItemsNetworth(member) + getCurrencyNetworth(member) + getPetsNetworth(member)

    fun getItemsNetworth(member: SkyblockResponse.SkyblockMember) = member.allStacks.sumOf { it.getItemValue().price }

    fun getCurrencyNetworth(member: SkyblockResponse.SkyblockMember) = member.currencies.purse.toLong() +
            member.soloBank.toLong() +
            (member.profileBank?.balance?.toLong() ?: 0L)

    fun getPetsNetworth(member: SkyblockResponse.SkyblockMember) = member.petsData.pets.sumOf { it.getValue() }

    fun SkyblockResponse.Pet.getValue() = (LowestBinAPI.getLowestPrice("pet${this.type}:${this.tier}") ?: 0L) +
            Pricing.getPrice(this.heldItem)
}