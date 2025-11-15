package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.inventoryButtons;
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
public class MixinStatusEffectsDisplay {
    @Inject(method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
    private void stella$onDrawEffect(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (inventoryButtons.INSTANCE.isEnabled()) ci.cancel();
    }
}