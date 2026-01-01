package co.stellarskys.stella.utils.config.ui.core

class ConfigCategoryUI: ConfigElementUI() {
    val column1: MutableList<ConfigSubcategoryUI> = mutableListOf()
    val column2: MutableList<ConfigSubcategoryUI> = mutableListOf()

    fun update() {
        var lastY = 10f
        for (subcategory in column1) {
            subcategory.x = 10f
            subcategory.y = lastY
            lastY += subcategory.getAHeight()
        }

        lastY = 10f
        for (subcategory in column2) {
            subcategory.x = 120f
            subcategory.y = lastY
            lastY += subcategory.getAHeight()
        }
    }

    override fun render() {
        for (subcategory in column1) subcategory.render()
        for (subcategory in column2) subcategory.render()
    }
}