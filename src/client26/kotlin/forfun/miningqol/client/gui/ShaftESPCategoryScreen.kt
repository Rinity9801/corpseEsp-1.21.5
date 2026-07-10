package forfun.miningqol.client.gui

import forfun.miningqol.client.ShaftESP
import net.minecraft.client.gui.screens.Screen

class ShaftESPCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Shaft ESP Settings") {
    private val accent = 0xFF88DDFF.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panelHeight = 110f + 3 * (SettingsUi.ROW_HEIGHT + SettingsUi.ROW_SPACING) +
            (SettingsUi.COLOR_ROW_HEIGHT + SettingsUi.ROW_SPACING) + 76f
        val panel = SettingsUi.panel(window, panelWidth, panelHeight, "Shaft ESP", "Highlights inside mineshafts")

        var y = 110f
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Littlefoot ESP", "Highlight the Littlefoot in mineshafts", accent,
            ShaftESP.isLittlefootEnabled()) {
            ShaftESP.setLittlefootEnabled(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Littlefoot Tracer", "Draw a line from your crosshair to the Littlefoot", accent,
            ShaftESP.isLittlefootTracer()) {
            ShaftESP.setLittlefootTracer(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Mob ESP", "Highlight mineshaft mobs", accent,
            ShaftESP.isMobsEnabled()) {
            ShaftESP.setMobsEnabled(it)
        }
        SettingsUi.colorRow(panel, panelWidth, y, "Mob ESP Color",
            { ShaftESP.getMobColor() },
            { r, g, b -> ShaftESP.setMobColor(r, g, b) },
            { ShaftESP.getMobAlpha() },
            { ShaftESP.setMobAlpha(it) })

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
