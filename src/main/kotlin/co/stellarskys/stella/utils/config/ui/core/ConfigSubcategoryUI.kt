package co.stellarskys.stella.utils.config.ui.core

import kotlin.collections.plusAssign


class ConfigSubcategoryUI(val category: ConfigCategoryUI): ConfigElementUI() {
    var elements: MutableList<ConfigInteractable> = mutableListOf()

    init {
        width = 100f
    }


    fun update() {
        var lasty = y
        for (element in elements) {
            element.x = x
            element.y = lasty
            if (!element.hidden) lasty += element.getAHeight()
        }

        height = getAHeight()
        category.update()
    }

    override fun getAHeight(): Float = elements.fold(40f) { acc, e -> acc + e.getAHeight() }

    override fun render() {
        for (element in elements) {
            if (element.hidden) continue
            element.render()
        }
    }
}