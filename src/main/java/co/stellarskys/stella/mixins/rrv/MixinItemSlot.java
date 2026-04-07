package co.stellarskys.stella.mixins.rrv;

import cc.cassian.rrv.common.overlay.ItemSlot;
import co.stellarskys.stella.api.rrv.RrvCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ItemSlot.class)
public class MixinItemSlot {
    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;getTooltipFromItem(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
    private List<Component> stella$getCachedTooltip(Minecraft minecraft, ItemStack itemStack) {
        return RrvCompat.getCachedTooltip(minecraft, itemStack);
    }
}
