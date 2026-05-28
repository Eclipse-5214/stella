package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.profileUtils.FakePlayer
import co.stellarskys.stella.features.msc.profileUtils.NetworthUtils
import co.stellarskys.stella.features.msc.profileUtils.NetworthUtils.toReadable
import co.stellarskys.stella.features.msc.profileUtils.SkillUtils
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockItemsRepo
import tech.thatgravyboat.skyblockapi.platform.pushPop

class Main(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
) : Page("profile", name, navigate) {
    private var entity: FakePlayer? = null
    val networth = NetworthUtils.getProfileNetworth(member)
    val nwComp = Component.literal("§dNW§7: ")
        .append("§6" + networth.total.toReadable())
        .onHover(networth.getFormatted())

    val skillAverage = SkillUtils.getCappedSkillAverage(member, false) to SkillUtils.getCappedSkillAverage(member)
    val saComp = Component.literal("§dSA§7: ")
        .append("§6" + skillAverage.first)
        .onHover("§bWith Progress§7: §6${String.format("%.2f", skillAverage.second)}")

    init {
        member.uuid?.let {
            FakePlayer.fromUUID(it, member.inventory.invArmor.items() ).thenAccept { plr ->
                client.execute { entity = plr }
            }
        }

    }

    override val icon: ItemStack = SkyBlockItemsRepo.getItemStackOrDefault("HYPERION")

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        // Paper Doll
        drawPlayer(context, 10, 25, 80, 100)

        // Quick Stats
        ren2d.drawHollowRect(context, 10, 135, 80, 75, 1, Palette.Purple)
        ren2d.drawString(context, "§b§n${member.profile?.cuteName ?: ""}", 15, 140)
        ren2d.drawString(context, "§dLevel§7: §6${member.sbLevel}", 15, 150)
        ren2d.drawString(context, "§dPurse§7: §6${member.currencies.purse.toLong().toReadable()}", 15, 160)
        ren2d.drawString(context, "§dBank§7: §6${member.profile?.banking?.balance?.toLong()?.toReadable() ?: ""}", 15, 170)
        drawComp(context, nwComp, 15, 180)
        ren2d.drawString(context, "§dMP§7: §6${member.assumedMagicalPower}", 15, 190)
        drawComp(context, saComp, 15, 200)

        // Skills
        ren2d.drawHollowRect(context, 100, 25, 240, 185, 1, Palette.Purple)

        drawSkill(context, 107, 30, SkillUtils.SkillType.COMBAT)
        drawSkill(context, 107, 60, SkillUtils.SkillType.MINING)
        drawSkill(context, 107, 90, SkillUtils.SkillType.FARMING)
        drawSkill(context, 107, 120, SkillUtils.SkillType.FORAGING)
        drawSkill(context, 107, 150, SkillUtils.SkillType.FISHING)
        drawSkill(context, 107, 180, SkillUtils.SkillType.ENCHANTING)

        drawSkill(context, 222, 30, SkillUtils.SkillType.ALCHEMY)
        drawSkill(context, 222, 60, SkillUtils.SkillType.TAMING)
        drawSkill(context, 222, 90, SkillUtils.SkillType.CARPENTRY)
        drawSkill(context, 222, 120, SkillUtils.SkillType.HUNTING)
        drawSkill(context, 222, 150, SkillUtils.SkillType.RUNECRAFTING)
        drawSkill(context, 222, 180, SkillUtils.SkillType.SOCIAL)

    }

    private fun drawPlayer(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int) {
        ren2d.drawHollowRect(context, 10, 25, width, height, 1, Palette.Purple)
        val player = entity ?: return ren2d.drawString(context, "§cLoading Model...", x + 5, y + 5)
        val x0 = (absoluteX + x).toInt()
        val y0 = (absoluteY + y).toInt()

        context.pushPop {
            context.pose().identity()
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                context, x0, y0, x0 + width, y0 + height, 42, 0.0625f,
                mouse.scaledX.toFloat(), mouse.scaledY.toFloat(), player
            )
        }
    }

    private fun drawSkill(context: GuiGraphicsExtractor, x: Int, y: Int, skilltype: SkillUtils.SkillType) {
        val skill = SkillUtils.getSkill(skilltype, member)
        val skillComp = Component.literal("§d${skilltype.displayName}§7: §6${skill.level.toInt()}")
            .onHover("§b${skilltype.displayName}\n§dXP§7: §6" + "%,.3f".format(skill.xp))

        ren2d.drawHollowRect(context, x, y, 110, 25, 1, Palette.Purple)
        ren2d.renderItem(context, skilltype.icon(), x.toFloat() + 5f, y.toFloat() + 5f, 1f)
        drawComp(context, skillComp, x + 25, y + 5)
        ren2d.drawRect(context, x + 25, y + 15, 80, 5, Palette.Crust)
        ren2d.drawRect(context, x + 25, y + 15, (80f * skill.progress).toInt(), 5, if (skill.level >= skill.cap.toDouble()) Palette.Sapphire else Palette.Green)
    }
}