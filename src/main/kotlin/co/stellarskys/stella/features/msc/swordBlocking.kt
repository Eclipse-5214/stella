package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.features.Feature
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.utils.extentions.pushPop

@Module
object swordBlocking: Feature("swordBlocking", true) {
    fun interface ArmTransform {
        fun apply(poseStack: PoseStack, arm: HumanoidArm, height: Float)
    }

    fun isBlocking() = this.isEnabled()
            && player?.mainHandItem?.`is`(ItemTags.SWORDS) == true
            && client.options.keyUse.isDown

    fun handleBlock(
        renderer: ItemInHandRenderer, player: AbstractClientPlayer,
        heldItem: ItemStack, i: Float, poseStack: PoseStack,
        collector: SubmitNodeCollector, j: Int,
        transform: ArmTransform
    ): Boolean {
        if (!isBlocking()) return false

        poseStack.pushPop {
            val arm = player.mainArm
            val side = if (arm == HumanoidArm.RIGHT) 1f else -1f
            val context =
                if (arm == HumanoidArm.RIGHT) ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                else ItemDisplayContext.FIRST_PERSON_LEFT_HAND

            transform.apply(poseStack, arm, i)
            poseStack.translate(side * -0.14142136f, 0.08f, 0.14142136f)
            poseStack.mulPose(Axis.XP.rotationDegrees(-102.25f))
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 13.365f))
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 78.05f))

            renderer.renderItem(player, heldItem, context, poseStack, collector, j)
        }
        return true
    }
}