package co.stellarskys.stella.mixins.rrv;

import cc.cassian.rrv.common.config.Configs;
import cc.cassian.rrv.common.overlay.AbstractRrvOverlay;
import cc.cassian.rrv.common.overlay.itemlist.AbstractRrvItemListOverlay;
import cc.cassian.rrv.common.overlay.itemlist.view.ItemViewOverlay;
import cc.cassian.rrv.common.overlay.itemlist.view.ReliableSpriteIconButton;
import co.stellarskys.stella.api.rrv.RrvCompat;
import co.stellarskys.stella.features.msc.InventoryButtons;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemViewOverlay.class)
public abstract class MixinItemViewOverlay extends AbstractRrvItemListOverlay {
    public MixinItemViewOverlay(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Inject(method = "initForScreen", at = @At("TAIL"))
    private void modifyWidth(AbstractContainerScreen<? extends AbstractContainerMenu> screen, AbstractRrvOverlay.InventoryPositionInfo invInfo, CallbackInfo ci) {
        if(!RrvCompat.INSTANCE.getEnabled()) return;

        double percentage = RrvCompat.INSTANCE.getWidth() / 100.0;

        int newWidth = (int) (this.width * percentage);
        newWidth -= (newWidth - 4) % ITEM_ENTRY_SIZE;
        this.width = Math.max(ITEM_ENTRY_SIZE + 4, newWidth);

        if (Configs.CLIENT_SETTINGS.isRightIndex()) {
            this.x = invInfo.screenWidth() - this.width;
        }

        this.itemStartX = this.x + 2;
        this.itemEndX = this.x + this.width - 2;
    }

    @Redirect(method = "placeWidgets", at = @At(value = "INVOKE", target = "Lcc/cassian/rrv/common/overlay/AbstractRrvOverlay$ScreenContext;addRenderable(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V"))
    private void cancelSettingsButton(ScreenContext instance, GuiEventListener renderable) {
        if (renderable instanceof ReliableSpriteIconButton && InventoryButtons.INSTANCE.isEnabled()) return;

        if (renderable instanceof Renderable widget && widget instanceof NarratableEntry) {
            instance.addRenderable((GuiEventListener & Renderable & NarratableEntry) widget);
        }
    }
}
