package co.stellarskys.stella.mixins;

import co.stellarskys.stella.api.handlers.Chronos;
import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.TickEvent;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinClientCommonPacketListenerImpl {
    @Inject(method = "handlePing", at = @At("HEAD"))
    private void stella$onPingStart(ClientboundPingPacket packet, CallbackInfo ci) {
        EventBus.INSTANCE.post(new TickEvent.Server());
        Chronos.Server.INSTANCE.pulse();
    }
}
