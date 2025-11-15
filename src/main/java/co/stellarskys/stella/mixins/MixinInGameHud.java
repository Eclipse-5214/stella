package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.inventoryButtons;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinInGameHud {
    /*
     * Modified from Devonian code
     * Under GPL 3.0 License
     */
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void stella$renderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (inventoryButtons.INSTANCE.isEnabled()) ci.cancel();
    }
}