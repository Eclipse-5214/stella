package co.stellarskys.stella.utils.skyblock.dungeons.score

import co.stellarskys.stella.events.ChatEvent
import co.stellarskys.stella.events.EntityEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.utils.clearCodes
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.mob.ZombieEntity

object MimicTrigger {
    val MIMIC_PATTERN = Regex("""^Party > (?:\[[\w+]+] )?\w{1,16}: (.*)$""")

    var mimicDead = false

    val mimicMessages = listOf(
        "mimic dead",
        "mimic dead!",
        "mimic killed",
        "mimic killed!",
        "\$skytils-dungeon-score-mimic$"
    )

    val updater: EventBus.EventCall = EventBus.register<EntityEvent.Death>({ event ->
        val mcEntity = event.entity
        if (Dungeon.floorNumber !in listOf(6, 7) || mimicDead) return@register

        if (mcEntity !is ZombieEntity) return@register
        if (
            !mcEntity.isBaby ||
            EquipmentSlot.entries
                .filter { it.type == EquipmentSlot.Type.HUMANOID_ARMOR }
                .any { slot -> mcEntity.getEquippedStack(slot).isEmpty }
        ) return@register

        mimicDead = true
    }, false)

    fun init() {
        EventBus.register<ChatEvent.Receive> { event ->
            if (Dungeon.floorNumber !in listOf(6, 7) || Dungeon.floor == null || !Dungeon.inDungeon) return@register

            val msg = event.message.string.clearCodes()
            val match = MIMIC_PATTERN.matchEntire(msg) ?: return@register

            if (mimicMessages.none { it == match.groupValues[1].lowercase() }) return@register
            mimicDead = true
        }
    }

    fun reset() { mimicDead = false }
}