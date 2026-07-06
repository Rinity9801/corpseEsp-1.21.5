package forfun.miningqol.client.gui

import forfun.miningqol.client.FiletWarning
import net.minecraft.client.gui.screens.Screen

class MiscCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Misc Settings") {
    private val accent = 0xFFFFAA55.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 280f, "Misc", "Everything without its own category")

        SettingsUi.toggleRow(panel, panelWidth, 110f, "Filet O' Fortune Warning", "Warn when your Filet O' Fortune cake expires", accent,
            FiletWarning.isEnabled()) {
            FiletWarning.setEnabled(it)
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
