package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.inventoryButtons;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    /*
     * Modified from Devonian code
     * Under GPL 3.0 License
     */
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void stella$renderStatusOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (inventoryButtons.INSTANCE.isEnabled()) ci.cancel();
    }
}