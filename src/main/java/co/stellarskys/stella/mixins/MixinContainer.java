package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.GuiEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class MixinContainer {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Final @Shadow protected int imageWidth;
    @Final @Shadow protected int imageHeight;

    @Inject(method = "extractContents", at = @At("TAIL"))
    public void onRenderContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci){
        EventBus.INSTANCE.post(new GuiEvent.Container.Content(graphics, mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight));
    }
}
