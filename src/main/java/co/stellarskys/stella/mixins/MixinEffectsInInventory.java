package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.InventoryButtons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * Modified from Devonian code
 * Under GPL 3.0 License
 */
@Mixin(EffectsInInventory.class)
public class MixinEffectsInInventory {
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    //?if > 1.21.10 {
    /*private void stella$onDrawEffect(GuiGraphics guiGraphics, Collection<MobEffectInstance> collection, int i, int j, int k, int l, int m, CallbackInfo ci) {
    *///?} else {
     private void stella$onInventoryEffects(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
    //?}
        if (InventoryButtons.INSTANCE.isEnabled()) ci.cancel();
    }
}