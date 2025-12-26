package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.bars;
import co.stellarskys.stella.features.msc.inventoryButtons;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
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

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void stella$onRenderHealthBar(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, float f, int m, int n, int o, boolean bl, CallbackInfo ci) {
        if (bars.INSTANCE.getHideVanillaHealth() && bars.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void stella$onRenderFood(GuiGraphics context, Player player, int y, int x, CallbackInfo ci) {
        if (bars.INSTANCE.getHideVanillaHunger() && bars.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void stella$onRenderArmor(GuiGraphics context, Player player, int y, int x, int z, int width, CallbackInfo ci) {
        if (bars.INSTANCE.getHideVanillaArmor() && bars.INSTANCE.isEnabled()) ci.cancel();
    }
}