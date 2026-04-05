package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.Bars;
import co.stellarskys.stella.features.msc.InventoryButtons;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {
    /*
     * Modified from Devonian code
     * Under GPL 3.0 License
     */
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void stella$renderEffects(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (InventoryButtons.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void stella$onRenderHealthBar(GuiGraphicsExtractor guiGraphics, Player player, int i, int j, int k, int l, float f, int m, int n, int o, boolean bl, CallbackInfo ci) {
        if (Bars.INSTANCE.getHideVanillaHealth() && Bars.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void stella$onRenderFood(GuiGraphicsExtractor context, Player player, int y, int x, CallbackInfo ci) {
        if (Bars.INSTANCE.getHideVanillaHunger() && Bars.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void stella$onRenderArmor(GuiGraphicsExtractor context, Player player, int y, int x, int z, int width, CallbackInfo ci) {
        if (Bars.INSTANCE.getHideVanillaArmor() && Bars.INSTANCE.isEnabled()) ci.cancel();
    }
}