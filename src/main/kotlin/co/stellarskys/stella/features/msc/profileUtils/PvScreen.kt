package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.Aperture
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics

class PvScreen(member: SkyblockResponse.SkyblockMember): Aperture("Profile Viewer") {
    val networth = NetworthUtils.getProfileNetworth(member)

    override fun onRender(context: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {
        Render2D.drawString(context, "Networth: $networth", 10, 10)
    }
}