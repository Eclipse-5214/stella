package co.stellarskys.stella.features.msc.profileUtils.screen

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.mc.ParentElement
import co.stellarskys.stella.api.horizon.mc.addTo
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.platform.pushPop

abstract class Page(
    val title: String,
    protected val name: String,
    private val navigate: (Page) -> Unit
) : ParentElement() {
    data class Tooltip(val x: Int, val y: Int, val width: Int, val height: Int, val comp: Style)

    abstract val icon: ItemStack

    val navBar = NavBar().addTo(this)

    var siblings: List<Page> = emptyList()
        set(value) {
            field = value
            navBar.pages = value.associateBy { it.icon }
            navBar.navigate = ::navigateTo
        }

    private val componentsTooltips = mutableListOf<Tooltip>()

    init {
        width = 300f
        height = 210f
        x = screenX
        y = screenY
    }

    protected val screenX get() = rez.scaledWidth / 2 - width / 2
    protected val screenY get() = rez.scaledHeight / 2 - height / 2

    fun navigateTo(page: Page) = navigate(page)

    fun drawComp(context: GuiGraphics, comp: MutableComponent, x: Int, y: Int) {
        ren2d.drawString(context, comp, x, y)
        comp.style.hoverEvent?.let {
            componentsTooltips.add(Tooltip(x, y, client.font.width(comp), client.font.lineHeight, comp.style))
        }
    }

    private fun renderTooltips(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val tooltip = componentsTooltips.firstOrNull {
            isAreaHovered(it.x.toFloat(), it.y.toFloat(), it.width.toFloat(), it.height.toFloat())
        } ?: return
        context.renderComponentHoverEffect(client.font, tooltip.comp, mouseX, mouseY)
    }

    open fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {}

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        x = screenX
        y = screenY
        context.pushPop {
            context.pose().translate(x, y)
            ren2d.drawRect(context, 0, 0, width.toInt(), height.toInt(), Palette.Crust.withAlpha(150))
            ren2d.drawHollowRect(context, 0, 0, width.toInt(), height.toInt(), 1, Palette.Purple)
            ren2d.drawString(context, "§d$name's $title!", 10, 10)
            navBar.render(context, mouseX, mouseY, delta)
            onRender(context, mouseX, mouseY, delta)
        }
        renderTooltips(context, mouseX.toInt(), mouseY.toInt())
        componentsTooltips.clear()
    }
}