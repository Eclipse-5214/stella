package co.stellarskys.stella.utils.skyblock.dungeons.score

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.EntityEvent
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

/**
 * Tracks whether the Mimic miniboss has been killed in F6/F7.
 * Updates via chat messages or entity death detection.
 */
object MimicTrigger {
    val MIMIC_PATTERN = Regex("""^Party > (?:\[[\w+]+] )?\w{1,16}: (.*)$""")

    var mimicDead = false
    var princeDead = false

    val mimicMessages = listOf("mimic dead", "mimic dead!", "mimic killed", "mimic killed!", "\$skytils-dungeon-score-mimic$")

    fun init() {
        EventBus.on<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (Dungeon.floorNumber !in 6..7 || Dungeon.floor == null) return@on
            val msg = event.message.stripped.lowercase()

            when {
                MIMIC_PATTERN.matches(msg) && mimicMessages.any { msg.contains(it) } -> mimicDead = true
                msg == "a prince falls. +1 bonus score" -> princeDead = true
            }
        }

        EventBus.on<EntityEvent.Death>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (Dungeon.floor?.floorNumber !in listOf(6, 7) || mimicDead || Dungeon.inBoss) return@on
            val entity = event.entity as? Zombie ?: return@on
            if (entity.isBaby && (0 .. 3).all { entity.getArmor(it).isEmpty }) {
                mimicDead = true
            }
        }
    }

    fun reset() {
        mimicDead = false
        princeDead = false
    }

    fun LivingEntity.getArmor(index: Int): ItemStack =
        when (index) {
            0 -> getItemBySlot(EquipmentSlot.FEET)
            1 -> getItemBySlot(EquipmentSlot.LEGS)
            2 -> getItemBySlot(EquipmentSlot.CHEST)
            3 -> getItemBySlot(EquipmentSlot.HEAD)
            else -> ItemStack.EMPTY
        }
}