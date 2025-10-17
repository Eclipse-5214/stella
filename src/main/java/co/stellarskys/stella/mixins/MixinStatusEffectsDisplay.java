package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.inventoryButtons;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * Modified from Devonian code
 * Under GPL 3.0 License
 */
@Mixin(StatusEffectsDisplay.class)
public class MixinStatusEffectsDisplay {
    @Inject(method = "drawStatusEffects(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("HEAD"), cancellable = true)
    private void stella$onDrawEffect(DrawContext context, int mx, int my, CallbackInfo callbackInfo) {
        if (inventoryButtons.INSTANCE.isEnabled()) callbackInfo.cancel();
    }
}