package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.PlayerEvent;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void onDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer)(Object)this;
        PlayerEvent.DropItem event = new PlayerEvent.DropItem(player.getInventory().getSelectedSlot(), all);
        if (EventBus.INSTANCE.post(event)) cir.setReturnValue(false);
    }
}
