package co.stellarskys.stella.api.dungeons.score

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.EntityEvent
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.handlers.Spark
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import net.minecraft.world.entity.EquipmentSlot

//? if <= 1.21.10 {
import net.minecraft.world.entity.monster.Zombie
//?} else {
/*import net.minecraft.world.entity.monster.zombie.Zombie
*///?}

/**
 * Tracks whether the Mimic miniboss has been killed in F6/F7.
 * Updates via chat messages or entity death detection.
 */
object MimicTrigger {
    var mimicDead by Spark(false)
    var princeDead by Spark(false)

    val mimicMessages = listOf("mimic dead", "mimic dead!", "mimic killed", "mimic killed!", "\$skytils-dungeon-score-mimic$")

    fun init() {
        EventBus.on<ChatEvent.Channel.Party>(SkyBlockIsland.THE_CATACOMBS) { event ->
            println("Party message found from ${event.name}: ${event.contents}")
            if (Dungeon.floorNumber !in 6..7 || Dungeon.floor == null) return@on
            if (mimicMessages.any { event.contents.lowercase().contains(it) }) mimicDead = true
        }

        EventBus.on<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if(event.stripped.lowercase() != "a prince falls. +1 bonus score") return@on
            princeDead = true; EventBus.post(DungeonEvent.Score.PrinceDead())
        }

        EventBus.on<EntityEvent.Death>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (Dungeon.floorNumber !in listOf(6, 7) || mimicDead || Dungeon.inBoss) return@on
            val entity = event.entity as? Zombie ?: return@on
            if (!entity.isBaby || entity.hasItemInSlot(EquipmentSlot.HEAD)) return@on
            mimicDead = true
            EventBus.post(DungeonEvent.Score.MimicDead())
        }
    }

    fun reset() {
        mimicDead = false
        princeDead = false
    }
}