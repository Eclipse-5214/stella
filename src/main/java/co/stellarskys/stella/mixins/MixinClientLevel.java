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
    private void stella$onPlaySound(double d, double e, double f, net.minecraft.sounds.SoundEvent soundEvent, SoundSource soundSource, float g, float h, boolean bl, long l, CallbackInfo ci) {
        Vec3 pos = new Vec3(d,e,f);
        if(EventBus.INSTANCE.post(new SoundEvent.Play(pos, soundEvent, soundSource, g, h))) ci.cancel();
    }
}
