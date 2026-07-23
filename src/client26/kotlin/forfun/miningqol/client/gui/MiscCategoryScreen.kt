package forfun.miningqol.client.gui

import forfun.miningqol.client.EfficientMinerOverlay
import forfun.miningqol.client.FiletWarning
import forfun.miningqol.client.MiningqolClient
import net.minecraft.client.gui.screens.Screen

class MiscCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Misc Settings") {
    private val accent = 0xFFFFAA55.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panelHeight = 468f + ExtraMiscRows.toggles.size * (SettingsUi.ROW_HEIGHT + SettingsUi.ROW_SPACING)
        val panel = SettingsUi.panel(window, panelWidth, panelHeight, "Misc", "Everything without its own category")

        var y = 110f
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Auto-skip /sho load", "Automatically runs /sho skipto 1 after /sho load", accent,
            MiningqolClient.getConfig()?.autoSkipShoLoad ?: false) {
            MiningqolClient.getConfig()?.autoSkipShoLoad = it
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Filet O' Fortune Warning", "Warn when your Filet O' Fortune cake expires", accent,
            FiletWarning.isEnabled()) {
            FiletWarning.setEnabled(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Efficient Miner Overlay", "Heatmap the best clay / red sandstone to mine (Glacite)", accent,
            EfficientMinerOverlay.isEnabled()) {
            EfficientMinerOverlay.setEnabled(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Old Heatmap Colors", "Use the legacy 8-colour heatmap palette", accent,
            EfficientMinerOverlay.isUsingOldHeatmap()) {
            EfficientMinerOverlay.setUseOldHeatmap(it)
        }
        for (row in ExtraMiscRows.toggles) {
            y = SettingsUi.toggleRow(panel, panelWidth, y, row.title, row.description, accent, row.get()) {
                row.set(it)
            }
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
