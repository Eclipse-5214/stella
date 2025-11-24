package co.stellarskys.stella.mixins;

import co.stellarskys.stella.utils.render.layers.GuiRendererHook;
import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC > 1.21.9
//$$ import com.mojang.blaze3d.platform.Window;
//#endif

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @Inject(method = "flipFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/Tesselator;clear()V"))
    private static void stella$clearChromaUniforms(
            //#if MC > 1.21.9
            //$$ Window window,
            //#else
            long l,
            //#endif
            TracyFrameCapture tracyFrameCapture,
            CallbackInfo ci
    ) {
        GuiRendererHook.INSTANCE.getChromaUniform().endFrame();
    }
}