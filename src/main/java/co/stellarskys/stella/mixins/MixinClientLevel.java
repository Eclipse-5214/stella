package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.SoundEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel  {
    @Inject(method = "playSound", at = @At("TAIL"), cancellable = true)
    private void stella$onPlaySound(double x, double y, double z, net.minecraft.sounds.SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay, long seed, CallbackInfo ci) {
        Vec3 pos = new Vec3(x, y, z);
        if(EventBus.INSTANCE.post(new SoundEvent.Play(pos, sound, source, volume, pitch))) ci.cancel();
    }
}
