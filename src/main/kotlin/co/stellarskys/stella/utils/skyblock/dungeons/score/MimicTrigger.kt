package co.stellarskys.stella.utils.skyblock.dungeons.score

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.EntityEvent
import co.stellarskys.stella.utils.clearCodes
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.Zombie

/**
 * Tracks whether the Mimic miniboss has been killed in F6/F7.
 * Updates via chat messages or entity death detection.
 */
object MimicTrigger {
    val MIMIC_PATTERN = Regex("""^Party > (?:\[[\w+]+] )?\w{1,16}: (.*)$""")

    var mimicDead = false

    val mimicMessages = listOf("mimic dead", "mimic dead!", "mimic killed", "mimic killed!", "\$skytils-dungeon-score-mimic$")

    fun init() {
        EventBus.on<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (Dungeon.floorNumber !in listOf(6, 7) || Dungeon.floor == null) return@on

            val msg = event.message.string.clearCodes()
            val match = MIMIC_PATTERN.matchEntire(msg) ?: return@on

            if (mimicMessages.none { it == match.groupValues[1].lowercase() }) return@on
            mimicDead = true
        }

        EventBus.on<EntityEvent.Death>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (Dungeon.floor?.floorNumber !in listOf(6, 7) || mimicDead) return@on
            val mcEntity = event.entity

            if (mcEntity !is Zombie) return@on
            if (
                !mcEntity.isBaby ||
                EquipmentSlot.entries
                    .filter { it.type == EquipmentSlot.Type.HUMANOID_ARMOR }
                    .any { slot -> mcEntity.getItemBySlot(slot).isEmpty }
            ) return@on

            mimicDead = true
        }
    }

    fun reset() { mimicDead = false }
}