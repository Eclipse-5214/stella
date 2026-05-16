package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.api.zenith.world
import com.mojang.authlib.GameProfile
import net.minecraft.client.entity.ClientMannequin
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ResolvableProfile

class FakePlayer(
    val profile: GameProfile,
    val armor: List<ItemStack>,
    val customDisplayName: Component? = null
): ClientMannequin(world!!, client.playerSkinRenderCache()) {
    private var _skin: PlayerSkin = DefaultPlayerSkin.get(profile)
    private val resolvable = ResolvableProfile.createResolved(profile)
        .apply { resolveProfile(client.services().profileResolver) }

    init {
        EquipmentSlot.entries.filter { it.isArmor }.reversed().forEachIndexed { i, slot ->
            equipment.set(slot, armor.getOrNull(i) ?: ItemStack.EMPTY)
        }

       client.playerSkinRenderCache().lookup(resolvable).thenAccept {
            _skin = it.get().playerSkin()
        }
    }

    override fun getSkin() = _skin
    override fun getDisplayName() = customName
    override fun shouldShowName() = true
    override fun isModelPartShown(part: PlayerModelPart) = part != PlayerModelPart.CAPE
}