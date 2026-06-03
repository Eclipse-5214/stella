package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.Utils
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextUtils.substring

@Module
object AutopetTitles: Feature("autopetMessages", true) {
    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            val autoMatch = PetDisplay.autoPet.find(event.stripped)?.groups[2] ?: return@on
            val formattedPetName = event.message.substring(autoMatch.range.first, autoMatch.range.last + 1)
            Utils.alert(Component.literal("§aEquipped: ").append(formattedPetName))
        }
    }
}