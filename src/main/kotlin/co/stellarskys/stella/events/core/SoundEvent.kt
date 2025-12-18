package co.stellarskys.stella.events.core

import co.stellarskys.stella.events.api.Event
import net.minecraft.client.resources.sounds.SoundInstance

sealed class SoundEvent {
    class Play(
        val sound: SoundInstance
    ) : Event(cancelable = true)
}