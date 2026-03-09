package co.stellarskys.stella.events.core

import co.stellarskys.stella.events.api.Event
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3

sealed class SoundEvent {
    class Play(
        val pos: Vec3,
        val sound: SoundEvent,
        val source: SoundSource,
        val volume: Float,
        val pitch: Float
    ) : Event(cancelable = true)
}