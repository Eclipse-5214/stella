package co.stellarskys.stella.features.msc.buttonUtils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.skyblock.NEUApi
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.core.*
import xyz.meowing.vexel.core.VexelWindow
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.TextInput
import java.awt.Color

class EditButtonPopup(window: VexelWindow) {
    var activeAnchor: AnchorType? = null
    var activeIndex: Int = 0
    var shown = false

    private var itemId: String = ""
    private var command: String = ""

    //gui
    val rect = Rectangle(
        Color.black.rgb,
        Palette.Purple.rgb,
        10f, 1f
    )
        .setPositioning(Pos.ScreenCenter, Pos.ScreenCenter)
        .setSizing(50f, Size.ParentPerc, 50f, Size.ParentPerc)
        .childOf(window)
        .hide()

    val popupTitle = Text("Edit Inventory Button", fontSize = 40f)
        .setPositioning(20f, Pos.ParentPixels, 20f, Pos.ParentPixels)
        .childOf(rect)

    val closeX = Button(
        "X",
        backgroundColor = Color.black.rgb,
        borderColor = Palette.Purple.rgb,
        fontSize = 24f
    )
        .setPositioning(5f, Pos.ParentPixels, 5f, Pos.ParentPixels)
        .setSizing(40f, Size.Pixels, 40f, Size.Pixels)
        .alignRight()
        .onClick { _, _, _ ->
            close()
            true
        }
        .childOf(rect)


    val itemPreview = Rectangle(
        Color.black.rgb,
        Palette.Purple.rgb,
        10f, 1f
    )
        .setPositioning(20f, Pos.ParentPixels, 20f, Pos.AfterSibling)
        .setSizing(130f, Size.Pixels, 130f, Size.Pixels)
        .childOf(rect)

    val itemIdInput = TextInput(
        placeholder = "EX: ENDER_PEARL",
        fontSize = 24f,
        backgroundColor = Color.black.rgb,
        borderColor = Palette.Purple.rgb,
        borderRadius = 10f,
        borderThickness = 1f
    )
        .setPositioning(0f, Pos.ParentCenter, 30f, Pos.AfterSibling)
        .setSizing(95f, Size.ParentPerc, 0f, Size.Auto)
        .childOf(rect)

    val commandInput = TextInput(
        placeholder = "EX: /hub",
        fontSize = 24f,
        backgroundColor = Color.black.rgb,
        borderColor = Palette.Purple.rgb,
        borderRadius = 10f,
        borderThickness = 1f
    )
        .setPositioning(0f, Pos.ParentCenter, 30f, Pos.AfterSibling)
        .setSizing(95f, Size.ParentPerc, 0f, Size.Auto)
        .childOf(rect)

    val saveButton = Button(
        "save",
        backgroundColor = Palette.Green.rgb,
        fontSize = 24f
    )
        .setPositioning(0f, Pos.ParentCenter, 30f, Pos.AfterSibling)
        .setSizing(130f, Size.Pixels, 0f, Size.Auto)
        .setOffset(-70f, 0f)
        .onClick { _, _, _ ->
            save()
            true
        }
        .childOf(rect)

    val deleteButton = Button(
        "delete",
        backgroundColor = Palette.Red.rgb,
        fontSize = 24f
    )
        .setPositioning(0f, Pos.ParentCenter, 0f, Pos.MatchSibling)
        .setSizing(130f, Size.Pixels, 0f, Size.Auto)
        .setOffset(70f, 0f)
        .onClick { _, _, _ ->
            delete()
            true
        }
        .childOf(rect)

    fun renderPreviewItem(context: DrawContext) {
        if (!shown) return

        val item = NEUApi.getItemBySkyblockId(itemIdInput.value)

        var stack = ItemStack(Items.BARRIER)

        if (item != null) {
            stack = NEUApi.createDummyStack(item)
        }

        val res = Stella.mc.window
        val scale = res.scaleFactor.toFloat() / 2

        val x = itemPreview.scaled.left * 2
        val y = itemPreview.scaled.top * 2

        Render2D.renderItem( context,stack, x + 5, y + 5, 7f / scale)
    }

    fun open(anchor: AnchorType, index: Int) {
        activeAnchor = anchor
        activeIndex = index

        val existing = ButtonManager.getButtonAt(anchor, index)

        itemId = existing?.iconId.orEmpty()
        command = existing?.command.orEmpty()

        itemIdInput.value = itemId.takeIf { it.isNotBlank() } ?: itemIdInput.value
        commandInput.value = command.takeIf { it.isNotBlank() } ?: commandInput.value

        rect.show()
        shown = true
    }

    fun close() {
        activeAnchor = null
        activeIndex = 0
        itemIdInput.value = ""
        commandInput.value = ""
        rect.hide()
        shown = false
    }

    fun save() {
        val anchor = activeAnchor ?: return
        val index = activeIndex

        if (itemIdInput.value.isEmpty() || commandInput.value.isEmpty()) {
            ButtonManager.remove(anchor, index)
            close()
            return
        }

        val noBackgroundAnchors = setOf(
            AnchorType.PLAYER_MODEL_TOP_LEFT,
            AnchorType.PLAYER_MODEL_TOP_RIGHT,
            AnchorType.PLAYER_MODEL_BOTTOM_LEFT,
            AnchorType.PLAYER_MODEL_BOTTOM_RIGHT
        )

        val background = anchor !in noBackgroundAnchors

        val existing = ButtonManager.getButtonAt(anchor, index)

        if (existing != null) {
            existing.iconId = itemIdInput.value
            existing.command = commandInput.value
        } else {
            val newButton = StellaButton(
                anchor = anchor,
                index = index,
                iconId = itemIdInput.value,
                command = commandInput.value,
                background = background
            )
            ButtonManager.add(newButton)
        }

        close()
    }

    fun delete() {
        val anchor = activeAnchor ?: return
        val index = activeIndex
        ButtonManager.remove(anchor, index)

        close()
    }
}