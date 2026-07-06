package forfun.miningqol.client.gui

import forfun.miningqol.client.CorpseESP
import net.minecraft.client.gui.screens.Screen

class CorpseESPCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Corpse ESP Settings") {
    private val accent = 0xFFFFCC66.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 480f, "Corpse ESP", "Frozen corpse waypoints in the Glacite Mineshafts")

        var y = 110f
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Lapis Corpses", "Track Lapis armor corpses", accent,
            CorpseESP.isLapisEnabled()) {
            if (CorpseESP.isLapisEnabled() != it) CorpseESP.toggleLapis()
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Tungsten Corpses", "Track Tungsten armor corpses", accent,
            CorpseESP.isTungstenEnabled()) {
            if (CorpseESP.isTungstenEnabled() != it) CorpseESP.toggleTungsten()
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Umber Corpses", "Track Umber armor corpses", accent,
            CorpseESP.isUmberEnabled()) {
            if (CorpseESP.isUmberEnabled() != it) CorpseESP.toggleUmber()
        }
        SettingsUi.toggleRow(panel, panelWidth, y, "Vanguard Corpses", "Track Vanguard armor corpses", accent,
            CorpseESP.isVanguardEnabled()) {
            if (CorpseESP.isVanguardEnabled() != it) CorpseESP.toggleVanguard()
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
