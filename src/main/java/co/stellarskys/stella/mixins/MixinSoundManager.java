package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.SoundEvent;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class MixinSoundManager {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if(EventBus.INSTANCE.post(new SoundEvent.Play(soundInstance))) cir.cancel();
    }
}