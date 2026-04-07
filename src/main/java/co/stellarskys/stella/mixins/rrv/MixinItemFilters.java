package co.stellarskys.stella.mixins.rrv;

import cc.cassian.rrv.common.overlay.itemlist.view.ItemFilters;
import co.stellarskys.stella.api.rrv.RrvCompat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemFilters.class)
public class MixinItemFilters {
    @Inject(method = "fullStackList", at = @At("HEAD"), cancellable = true)
    private static void stella$bypassIndexing(CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!RrvCompat.INSTANCE.getConfigEnabled()) return;
        List<ItemStack> prebuilt = RrvCompat.INSTANCE.getCachedStacks();
        cir.setReturnValue(prebuilt);
    }
}
