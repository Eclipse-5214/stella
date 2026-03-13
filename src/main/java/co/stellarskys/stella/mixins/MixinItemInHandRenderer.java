package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.SwordBlocking;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {
    @Shadow private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight) {}

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void stella$applySwordBlock(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
         if(SwordBlocking.INSTANCE.handleBlock((ItemInHandRenderer) (Object) this, abstractClientPlayer, itemStack, i, poseStack, submitNodeCollector, j, this::applyItemArmTransform)) ci.cancel();
    }
}
