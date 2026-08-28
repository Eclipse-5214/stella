package co.stellarskys.stella.features.msc.profileUtils.screen

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.mc.ParentElement
import co.stellarskys.stella.api.horizon.mc.addTo
import co.stellarskys.stella.api.zenith.client
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.platform.pushPop

abstract class Page(
    val title: String,
    protected val name: String,
    private val navigate: (Page) -> Unit
) : ParentElement() {
    data class ScissorBox(val x: Float, val y: Float, val width: Float, val height: Float)

    data class Tooltip(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val comp: Style,
        val scissorBox: ScissorBox? = null
    )

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
        width = 350f
        height = 220f
        x = screenX
        y = screenY
    }

    protected val screenX get() = rez.scaledWidth / 2 - width / 2
    protected val screenY get() = rez.scaledHeight / 2 - height / 2

    fun navigateTo(page: Page) = navigate(page)

    fun drawComp(
        context: GuiGraphicsExtractor,
        comp: MutableComponent,
        x: Int,
        y: Int,
        scrollOffset: Float = 0f,
        scissorX: Float = 0f,
        scissorY: Float = 0f,
        scissorW: Float = 0f,
        scissorH: Float = 0f
    ) {
        ren2d.drawString(context, comp, x, y)
        comp.style.hoverEvent?.let {
            val absX = if (scissorW > 0) (scissorX + x).toInt() else x
            val absY = if (scissorH > 0) (scissorY + y + scrollOffset).toInt() else y
            val sBox = if (scissorW > 0) ScissorBox(scissorX, scissorY, scissorW, scissorH) else null
            componentsTooltips.add(Tooltip(absX, absY, client.font.width(comp), client.font.lineHeight, comp.style, sBox))
        }
    }

    private fun renderTooltips(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val tooltip = componentsTooltips.firstOrNull {
            val isHovered = isAreaHovered(it.x.toFloat(), it.y.toFloat(), it.width.toFloat(), it.height.toFloat(), mouseX.toFloat(), mouseY.toFloat())
            if (!isHovered) return@firstOrNull false
            if (it.scissorBox != null) {
                val box = it.scissorBox
                if (!isAreaHovered(box.x, box.y, box.width, box.height, mouseX.toFloat(), mouseY.toFloat())) return@firstOrNull false
            }
            true
        } ?: return

        // Mimics GuiGraphicsExtractor L 777 - 787
        val hoverEvent = (tooltip.comp.hoverEvent as? HoverEvent.ShowText)?.value ?: return
        val text = client.font.split(hoverEvent, (context.guiWidth() / 2).coerceAtLeast(200))
        context.setTooltipForNextFrame(client.font, text, mouseX, mouseY)
    }

    open fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {}

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        x = screenX
        y = screenY
        context.pushPop {
            context.pose().translate(x, y)
            ren2d.drawRect(context, 0, 0, width.toInt(), height.toInt(), Palette.Crust.withAlpha(150))
            ren2d.drawHollowRect(context, 0, 0, width.toInt(), height.toInt(), 1, Palette.Purple)
            ren2d.drawString(context, "§d$name's $title!", 10, 10)
            navBar.render(context, mouseX, mouseY, delta)
            onRender(context, mouseX, mouseY, delta)
            elements.find { it is ProfileDropdown }?.render(context, mouseX, mouseY, delta)
        }
        renderTooltips(context, mouseX.toInt(), mouseY.toInt())
        componentsTooltips.clear()
    }
}