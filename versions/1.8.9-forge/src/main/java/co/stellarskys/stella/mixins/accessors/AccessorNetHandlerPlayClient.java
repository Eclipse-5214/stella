package co.stellarskys.stella.mixins.accessors;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.INetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(NetHandlerPlayClient.class)
public interface AccessorNetHandlerPlayClient extends INetHandlerPlayClient {
    @Accessor("playerInfoMap")
    Map<UUID, NetworkPlayerInfo> getUUIDToPlayerInfo();
}