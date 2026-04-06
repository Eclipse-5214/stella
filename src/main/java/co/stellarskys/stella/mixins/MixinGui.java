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
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void stella$renderEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (InventoryButtons.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "extractHearts", at = @At("HEAD"), cancellable = true)
    private void stella$onRenderHealthBar(GuiGraphicsExtractor graphics, Player player, int xLeft, int yLineBase, int healthRowHeight, int heartOffsetIndex, float maxHealth, int currentHealth, int oldHealth, int absorption, boolean blink, CallbackInfo ci) {
        if (Bars.INSTANCE.getHideVanillaHealth() && Bars.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void stella$onRenderFood(GuiGraphicsExtractor graphics, Player player, int yLineBase, int xRight, CallbackInfo ci) {
        if (Bars.INSTANCE.getHideVanillaHunger() && Bars.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
    private static void stella$onRenderArmor(GuiGraphicsExtractor graphics, Player player, int yLineBase, int numHealthRows, int healthRowHeight, int xLeft, CallbackInfo ci) {
        if (Bars.INSTANCE.getHideVanillaArmor() && Bars.INSTANCE.isEnabled()) ci.cancel();
    }
}