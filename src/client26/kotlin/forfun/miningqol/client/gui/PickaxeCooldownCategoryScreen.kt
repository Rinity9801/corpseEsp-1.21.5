package forfun.miningqol.client.gui

import forfun.miningqol.client.PickaxeCooldownHUD
import net.minecraft.client.gui.screens.Screen

class PickaxeCooldownCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Pickaxe Cooldown Settings") {
    private val accent = 0xFFCC88FF.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 480f, "Pickaxe Cooldown", "Ability cooldown HUD and ready alert")

        var y = 110f
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Enabled", "Show the cooldown HUD", accent,
            PickaxeCooldownHUD.isEnabled()) {
            PickaxeCooldownHUD.setEnabled(it)
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Scale", 0.5f, 2.0f, 0.05f,
            PickaxeCooldownHUD.getScale(), accent, { String.format("%.2fx", it) }) {
            PickaxeCooldownHUD.setScale(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Ready Title", "Flash a title when the ability comes off cooldown", accent,
            PickaxeCooldownHUD.isTitleEnabled()) {
            PickaxeCooldownHUD.setTitleEnabled(it)
        }
        SettingsUi.sliderRow(panel, panelWidth, y, "Title Threshold", 0f, 30f, 1f,
            PickaxeCooldownHUD.getTitleThreshold().toFloat(), accent, { "${it.toInt()}s left" }) {
            PickaxeCooldownHUD.setTitleThreshold(it.toInt())
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
