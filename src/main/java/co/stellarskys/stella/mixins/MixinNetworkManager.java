package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.PacketEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinNetworkManager {
    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void stella$onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (EventBus.INSTANCE.onPacketReceived(packet)) ci.cancel();
    }

    @Inject(method = "channelRead0*", at = @At("TAIL"))
    private void stella$onReceivePacketPost(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PacketEvent.ReceivedPost(packet));
    }

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void stella$onPacketSend(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
        if (EventBus.INSTANCE.post(new PacketEvent.Sent(packet))) ci.cancel();
    }
}
