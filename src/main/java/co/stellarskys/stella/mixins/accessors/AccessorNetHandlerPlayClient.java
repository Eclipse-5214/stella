package co.stellarskys.stella.mixins.accessors;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.listener.ClientPlayPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public interface AccessorNetHandlerPlayClient extends ClientPlayPacketListener {
    @Accessor("playerListEntries")
    Map<UUID, PlayerListEntry> getUUIDToPlayerInfo();
}