package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.PlayerEvent;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//? if > 26.2 {
/*import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///? } else {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
 //? }

//? if > 26.2 {
/*@Mixin(MultiPlayerGameMode.class)
*///? } else {
@Mixin(LocalPlayer.class)
 //? }
public class MixinLocalPlayer {
    //? if > 26.2 {
    /*@Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void onDrop(LocalPlayer player, boolean all, CallbackInfo ci) {
        if (EventBus.INSTANCE.post(new PlayerEvent.DropItem(player.getInventory().getSelectedSlot(), all))) ci.cancel();
    }
    *///? } else {
      @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
      private void onDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
          LocalPlayer player = (LocalPlayer)(Object)this;
          if (EventBus.INSTANCE.post(new PlayerEvent.DropItem(player.getInventory().getSelectedSlot(), all))) cir.setReturnValue(false);
      }
      //? }
}