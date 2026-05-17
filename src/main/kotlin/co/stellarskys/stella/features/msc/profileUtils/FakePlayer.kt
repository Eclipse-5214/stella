package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.zenith.client
import com.mojang.authlib.GameProfile
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ResolvableProfile
import java.util.UUID
import java.util.concurrent.CompletableFuture

class FakePlayer(val profile: GameProfile, val armor: List<ItemStack> = listOf()): RemotePlayer(client.level!!, profile) {
    init {
        equipment.set(EquipmentSlot.HEAD, armor[3])
        equipment.set(EquipmentSlot.CHEST, armor[2])
        equipment.set(EquipmentSlot.LEGS, armor[1])
        equipment.set(EquipmentSlot.FEET, armor[0])
    }

    override fun getSkin(): PlayerSkin = PlayerInfo(profile, false).skin
    override fun isModelPartShown(part: PlayerModelPart): Boolean = part != PlayerModelPart.CAPE
    override fun isInvisibleTo(player: Player): Boolean = false

    companion object {
        fun fromUUID(id: UUID, armor: List<ItemStack> = listOf()): CompletableFuture<FakePlayer?> {
            val resolvable = ResolvableProfile.createUnresolved(id)

            return client.playerSkinRenderCache().lookup(resolvable).thenApply { optionalEntry ->
                if (optionalEntry.isPresent) {
                    val entry = optionalEntry.get()
                    FakePlayer(entry.gameProfile(), armor)
                } else null
            }
        }
    }
}