package co.stellarskys.stella.features.msc.profileUtils.screen

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.profileUtils.FakePlayer
import co.stellarskys.stella.features.msc.profileUtils.NetworthUtils
import co.stellarskys.stella.features.msc.profileUtils.NetworthUtils.toReadable
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.platform.pushPop
import java.awt.Color

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
        member.uuid?.let {
            FakePlayer.fromUUID(it, member.inventory.invArmor.items() ).thenAccept { plr ->
                client.execute { entity = plr }
            }
        }

    }

    override val icon: ItemStack = Items.IRON_SWORD.defaultInstance

    override fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        // Paper Doll
        drawPlayer(context, 10, 25, 80, 100)

        // Quick Stats
        ren2d.drawHollowRect(context, 10, 135, 80, 65, 1, Palette.Purple)
        ren2d.drawString(context, "§b§n${member.profile?.cuteName ?: ""}", 15, 140)
        ren2d.drawString(context, "§bLevel§7: §6${member.leveling.getLevel().first}", 15, 150)
        ren2d.drawString(context, "§dPurse§7: §6${member.currencies.purse.toLong().toReadable()}", 15, 160)
        ren2d.drawString(context, "§dBank§7: §6${member.profile?.banking?.balance?.toLong()?.toReadable() ?: ""}", 15, 170)
        drawComp(context, nwComp, 15, 180)
        ren2d.drawString(context, "§dMP§7: §6${member.assumedMagicalPower}", 15, 190)


    }

    private fun drawPlayer(context: GuiGraphics, x: Int, y: Int, width: Int, height: Int) {
        ren2d.drawRect(context, 10, 25, width, height, Color.BLACK)
        val player = entity ?: return ren2d.drawString(context, "§cLoading Model...", x + 5, y + 5)
        val x0 = (absoluteX + x).toInt()
        val y0 = (absoluteY + y).toInt()

        context.pushPop {
            context.pose().identity()
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                context, x0, y0, x0 + width, y0 + height, 42, 0.0625f,
                mouse.scaledX.toFloat(), mouse.scaledY.toFloat(), player
            )
        }
    }
}