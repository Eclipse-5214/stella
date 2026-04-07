package co.stellarskys.stella.mixins.rrv;

import cc.cassian.rrv.common.config.Configs;
import cc.cassian.rrv.common.overlay.AbstractRrvOverlay;
import cc.cassian.rrv.common.overlay.itemlist.AbstractRrvItemListOverlay;
import cc.cassian.rrv.common.overlay.itemlist.view.ItemViewOverlay;
import co.stellarskys.stella.api.rrv.RrvCompat;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemViewOverlay.class)
public abstract class MixinItemVieweOverlay extends AbstractRrvItemListOverlay {
    public MixinItemVieweOverlay(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Inject(method = "initForScreen", at = @At("TAIL"))
    private void modifyWidth(AbstractContainerScreen<? extends AbstractContainerMenu> screen, AbstractRrvOverlay.InventoryPositionInfo invInfo, CallbackInfo ci) {
        if(RrvCompat.INSTANCE.getEnabled()) return;

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
}
