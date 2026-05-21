package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.Aperture
import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.features.msc.profileUtils.screen.pages.*
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import net.minecraft.client.gui.GuiGraphics

class PvScreen private constructor(
    val name: String,
    val member: SkyblockResponse.SkyblockMember
) : Aperture("Profile Viewer") {
    private val pages: List<Page>
    private var currentPage: Page

    init {
        pages = listOf(
            Main(name, member, ::switchTo),
            Inventory(name, member, ::switchTo),
            Cata(name, member, ::switchTo),
            Slayer(name, member, ::switchTo),
            Collection(name, member, ::switchTo),
        )
        pages.forEach { it.siblings = pages }
        currentPage = pages.first()
    }

    private fun switchTo(page: Page) { currentPage = page }

    override fun onRender(context: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) =
        currentPage.render(context, mouseX.toFloat(), mouseY.toFloat(), tickDelta)

    override fun onMouseClick(button: Int, x: Double, y: Double, modifiers: Int) =
        currentPage.mouseClicked(x.toFloat(), y.toFloat(), button)

    override fun onMouseRelease(button: Int, x: Double, y: Double, modifiers: Int): Boolean {
        currentPage.mouseReleased(x.toFloat(), y.toFloat(), button)
        return false
    }

    override fun onMouseScroll(x: Double, y: Double, amount: Double, horizontalAmount: Double) =
        currentPage.mouseScrolled(x.toFloat(), y.toFloat(), amount.toFloat(), horizontalAmount.toFloat())

    override fun onKeyPress(key: Int, scanCode: Int, modifiers: Int) =
        currentPage.keyPressed(key, modifiers)

    override fun onCharTyped(char: Char) =
        currentPage.charTyped(char)

    companion object {
        fun open(name: String, member: SkyblockResponse.SkyblockMember) =
            Zenith.client.setScreen(PvScreen(name, member))
    }
}