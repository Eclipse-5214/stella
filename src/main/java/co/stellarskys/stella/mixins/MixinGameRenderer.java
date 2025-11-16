package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.GuiEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.meowing.knit.api.KnitClient;
import xyz.meowing.knit.api.render.KnitResolution;
import xyz.meowing.vexel.utils.render.NVGRenderer;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Final
    @Shadow
    private GuiRenderState guiRenderState;

    @Final
    @Shadow
    private GuiRenderer guiRenderer;

    /*
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void stella$afterHudRender(Gui instance, GuiGraphics guiGraphics, DeltaTracker deltaTracker, Operation<Void> original) {
        original.call(instance, guiGraphics, deltaTracker);

        EventBus.INSTANCE.post(new GuiEvent.RenderHUD(guiGraphics));
    }

     */

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void stella$afterHudRender(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphics context) {
        EventBus.INSTANCE.post(new GuiEvent.RenderHUD(context));
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void idkwhattocallthis(GuiRenderer instance, GpuBufferSlice gpuBufferSlice, Operation<Void> original) {
        NVGRenderer.INSTANCE.beginFrame(KnitResolution.getWindowWidth(), KnitResolution.getWindowHeight());
        GuiGraphics graphics = new GuiGraphics(KnitClient.getClient(), guiRenderState);

        boolean cancelled = EventBus.INSTANCE.post(new GuiEvent.NVG.Render(graphics), false);

        NVGRenderer.INSTANCE.endFrame();

        if (!cancelled) { original.call(instance, gpuBufferSlice); }
    }
}