package forfun.miningqol.client.gui

import forfun.miningqol.client.EfficientMinerOverlay
import forfun.miningqol.client.FiletWarning
import net.minecraft.client.gui.screens.Screen

class MiscCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Misc Settings") {
    private val accent = 0xFFFFAA55.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 400f, "Misc", "Everything without its own category")

        var y = 110f
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Filet O' Fortune Warning", "Warn when your Filet O' Fortune cake expires", accent,
            FiletWarning.isEnabled()) {
            FiletWarning.setEnabled(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Efficient Miner Overlay", "Heatmap the best clay / red sandstone to mine (Glacite)", accent,
            EfficientMinerOverlay.isEnabled()) {
            EfficientMinerOverlay.setEnabled(it)
        }
        SettingsUi.toggleRow(panel, panelWidth, y, "Old Heatmap Colors", "Use the legacy 8-colour heatmap palette", accent,
            EfficientMinerOverlay.isUsingOldHeatmap()) {
            EfficientMinerOverlay.setUseOldHeatmap(it)
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
