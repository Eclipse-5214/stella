package co.stellarskys.stella.utils.config.ui.core

class ConfigInteractable(val subcategory: ConfigSubcategoryUI): ConfigElementUI() {
    override var hidden: Boolean = false
        set(value) {
            field = value
            subcategory.update()
        }
}