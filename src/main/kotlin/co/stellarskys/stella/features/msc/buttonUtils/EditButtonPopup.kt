package co.stellarskys.stella.features.msc.buttonUtils

import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import dev.deftu.omnicore.api.client.render.OmniResolution
import net.minecraft.client.gui.GuiGraphics
import co.stellarskys.vexel.components.core.*
import co.stellarskys.vexel.core.VexelWindow
import co.stellarskys.vexel.elements.Button
import co.stellarskys.vexel.elements.TextInput
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
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
        .setSizing(50f, Size.Percent, 50f, Size.Percent)
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
        .setPositioning(-10f, Pos.ParentPixels, 10f, Pos.ParentPixels)
        .setSizing(40f, Size.Pixels, 40f, Size.Pixels)
        .alignRight()
        .onClick { _ ->
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
        .setSizing(95f, Size.Percent, 0f, Size.Auto)
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
        .setSizing(95f, Size.Percent, 0f, Size.Auto)
        .childOf(rect)

    val saveButton = Button(
        "save",
        backgroundColor = Palette.Green.rgb,
        fontSize = 24f
    )
        .setPositioning(0f, Pos.ParentCenter, 30f, Pos.AfterSibling)
        .setSizing(130f, Size.Pixels, 0f, Size.Auto)
        .setOffset(-70f, 0f)
        .onClick { _ ->
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
        .onClick { _ ->
            delete()
            true
        }
        .childOf(rect)

    fun renderPreviewItem(context: GuiGraphics) {
        if (!shown) return

        val stack = RepoItemsAPI.getItem(itemIdInput.value)
        val scale = OmniResolution.scaleFactor.toFloat()

        val x = itemPreview.scaled.left
        val y = itemPreview.scaled.top

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