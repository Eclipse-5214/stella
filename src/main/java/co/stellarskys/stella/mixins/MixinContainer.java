package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.GuiEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
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
    @Final @Shadow protected int imageHeight;;

    @Inject(method = "extractContents", at = @At("TAIL"))
    public void onRenderContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        EventBus.INSTANCE.post(new GuiEvent.Container.AfterContent(graphics, mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight));
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    public void onClickedSlot(Slot slot, int slotId, int buttonNum, ContainerInput containerInput, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        GuiEvent.Container.SlotClick event = new  GuiEvent.Container.SlotClick(screen, slot, slotId, buttonNum, containerInput);
        if (EventBus.INSTANCE.post(event)) ci.cancel();
    }
}