package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.hypixel.SkyblockResponse
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockPetsRepo

object PetUtils {
    data class PetLevel(val level: Int, val levelCap: Int, val progressToNextLevel: Float, val progressToMax: Float)

    fun SkyblockResponse.Pet.item() = SkyBlockPetsRepo.getItemStackOrDefault {
        val (lvl, _, _) = getPetLevel(this@item)
        id = type
        rarity = this@item.rarity
        level = lvl
        skin = this@item.skin
        heldItem = this@item.heldItem
    }

    private val xpCurve = listOf(
        100, 110, 120, 130, 145, 160, 175, 190, 210, 230, 250, 275, 300, 330, 360, 400, 440, 490, 540, 600,
        660, 730, 800, 880, 960, 1050, 1150, 1260, 1380, 1510, 1650, 1800, 1960, 2130, 2310, 2500, 2700, 2920,
        3160, 3420, 3700, 4000, 4350, 4750, 5200, 5700, 6300, 7000, 7800, 8700, 9700, 10800, 12000, 13300,
        14700, 16200, 17800, 19500, 21300, 23200, 25200, 27400, 29800, 32400, 35200, 38200, 41400, 44800,
        48400, 52200, 56200, 60400, 64800, 69400, 74200, 79200, 84700, 90700, 97200, 104200, 111700, 119700,
        128200, 137200, 146700, 156700, 167700, 179700, 192700, 206700, 221700, 237700, 254700, 272700, 291700,
        311700, 333700, 357700, 383700, 411700, 441700, 476700, 516700, 561700, 611700, 666700, 726700, 791700,
        861700, 936700, 1016700, 1101700, 1191700, 1286700, 1386700, 1496700, 1616700, 1746700, 1886700
    )

    private val rarityOffsets = listOf(0, 6, 11, 16, 20, 20)
    private val dragonPetTypes = setOf("GOLDEN_DRAGON", "JADE_DRAGON", "ROSE_DRAGON")

    fun getCurveForPet(type: String, rarity: SkyBlockRarity): List<Int> = when (val cleanType = type.uppercase()) {
        "BINGO" -> xpCurve.take(99)
        in dragonPetTypes -> List(199) { i ->
            when {
                i < 99 -> xpCurve[i + 20]
                i == 99 -> 0
                i == 100 -> 5555
                else -> 1886700
            }
        }
        else -> xpCurve.drop(rarityOffsets.getOrElse(rarity.ordinal) { 20 }).take(99)
    }

    fun getPetLevel(apiPet: SkyblockResponse.Pet): PetLevel {
        val isDragon = apiPet.type.uppercase() in dragonPetTypes
        val curve = getCurveForPet(apiPet.type, apiPet.rarity)
        val levelCap = if (isDragon) 200 else 100
        val cumulativeXp = curve.scan(0L) { acc, xp -> acc + xp }
        val foundIndex = cumulativeXp.indexOfLast { it <= apiPet.exp }
        val finalLevel = (if (foundIndex != -1) foundIndex + 1 else 1).coerceAtMost(levelCap)

        val progressToNextLevel = if (finalLevel >= levelCap) 1.0f else {
            val currentLevelMinXp = cumulativeXp[finalLevel - 1]
            val nextLevelMaxXp = cumulativeXp[finalLevel]
            val range = nextLevelMaxXp - currentLevelMinXp
            if (range <= 0L) 0.0f else ((apiPet.exp - currentLevelMinXp).toFloat() / range).coerceIn(0f, 1f)
        }

        val progressToMax = (apiPet.exp / (cumulativeXp.lastOrNull() ?: 1L).toDouble()).toFloat().coerceIn(0f, 1f)

        return PetLevel(finalLevel, levelCap, progressToNextLevel, progressToMax)
    }
}