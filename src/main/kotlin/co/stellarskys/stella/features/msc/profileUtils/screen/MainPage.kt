package co.stellarskys.stella.features.msc.profileUtils.screen

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.hypixel.HypixelApi
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.profileUtils.FakePlayer
import co.stellarskys.stella.features.msc.profileUtils.NetworthUtils
import co.stellarskys.stella.features.msc.profileUtils.NetworthUtils.toReadable
import com.mojang.util.UndashedUuid
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import java.awt.Color
import java.util.UUID

class MainPage(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
) : Page("profile", name, navigate) {
    private var entity: FakePlayer? = null
    val networth = NetworthUtils.getProfileNetworth(member)
    val nwComp = Component.literal("§dNW§7: ")
        .append("§6" + networth.total.toReadable())
        .onHover(networth.getFormatted())

    init {
        HypixelApi.getUuid(name) { id ->
            val uuid = UndashedUuid.fromStringLenient(id)
            client.playerSkinRenderCache().lookup(ResolvableProfile.createUnresolved(uuid))
                .thenAccept { profile ->
                    entity = FakePlayer(profile.get().gameProfile(), member.inventory.invArmor.items())
                }
        }
    }

    override val icon: ItemStack = Items.IRON_SWORD.defaultInstance

    override fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        // Paper Doll
        ren2d.drawRect(context, 10, 25, 80, 100, Color.BLACK)
        entity?.let { fakePlayer ->
            // Use the exact parameters from your Skyblocker example
            // i, j: Top Left (10, 25)
            // k, l: Bottom Right (90, 125)
            // m: Scale (42)
            // f: Eye offset (0.0625f)

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                context,
                10, 25, 90, 125, // Bounds
                42,              // Scale
                0.0625f,         // Offset
                mouseX,          // Mouse X
                mouseY,          // Mouse Y
                fakePlayer       // Your entity
            )
        }
        // Quick Stats
        ren2d.drawHollowRect(context, 10, 135, 80, 65, 1, Palette.Purple)
        ren2d.drawString(context, "§b§n${member.profile?.cuteName ?: ""}", 15, 140)
        ren2d.drawString(context, "§bLevel§7: §6${member.leveling.getLevel().first}", 15, 150)
        ren2d.drawString(context, "§dPurse§7: §6${member.currencies.purse.toLong().toReadable()}", 15, 160)
        ren2d.drawString(context, "§dBank§7: §6${member.profile?.banking?.balance?.toLong()?.toReadable() ?: ""}", 15, 170)
        drawComp(context, nwComp, 15, 180)
        ren2d.drawString(context, "§dMP§7: §6${member.assumedMagicalPower}", 15, 190)

    }
}