package co.stellarskys.stella.utils.config.ui.base

abstract class ParentElement: BaseElement() {
    val elements = mutableListOf<BaseElement>()
    open val visibleElements get() = elements.filter { it.visible }

    override var isAnimating: Boolean = false
        get() = super.isAnimating || areElementsAnimating() || field

    abstract fun update()

    open fun getEH() = visibleElements.fold(0f) { acc, e -> acc + e.height }
    open fun updateElements(offset: Float) = visibleElements.fold(offset) { ey, e -> e.also {it.y = ey }.height + ey }
    open fun areElementsAnimating() = visibleElements.any { it.isAnimating }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int) = visibleElements.any { it.mouseClicked(mouseX, mouseY, button) }
    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) { for (element in elements) element.mouseReleased(mouseX, mouseY, button) }

    override fun charTyped(char: Char) = elements.any { it.charTyped(char) }
    override fun keyPressed(keyCode: Int) = elements.any { it.keyPressed(keyCode) }
}