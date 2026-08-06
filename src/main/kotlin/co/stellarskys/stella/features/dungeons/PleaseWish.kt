package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object PleaseWish: Feature("pleaseWish", island = SkyBlockIsland.THE_CATACOMBS) {
    private val message by config.property<String>("pleaseWish.message")

    private val wishMessages = setOf(
        "⚠ Maxor is enraged! ⚠",
        "[BOSS] Goldor: You have done it, you destroyed the factory..."
    )


    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            if (DungeonPlayerManager.you?.dclass != DungeonClass.HEALER || Dungeon.floorNumber != 7) return@on
            if (event.stripped !in wishMessages) return@on

            Chronos.Tick after 10 run {
                Utils.alert(message.replace("&", "§"), SoundEvents.NOTE_BLOCK_PLING.value())
            }
        }
    }
}