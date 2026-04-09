package co.stellarskys.stella.mixins.rrv;

import cc.cassian.rrv.common.recipe.ClientRecipeManager;
import co.stellarskys.stella.api.rrv.RrvCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ClientRecipeManager.class)
public class MixinClientRecipeManager {
    @Inject(method = "requestServerRrvData", at = @At("HEAD"), cancellable = true)
    private void shutup(CallbackInfo ci) {
        if (RrvCompat.INSTANCE.getEnabled()) ci.cancel();
    }
}
