package forfun.miningqol.client.gui

import forfun.miningqol.client.waypoints.OrderedWaypointManager
import net.minecraft.client.gui.screens.Screen

class WaypointColorsScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Waypoint Colors") {

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panelHeight = 110f + 4 * (SettingsUi.COLOR_ROW_HEIGHT + SettingsUi.ROW_SPACING) + 76f
        val panel = SettingsUi.panel(window, panelWidth, panelHeight, "Waypoint Colors", "RGBA per waypoint type — applies live")

        var y = 110f
        y = SettingsUi.colorRow(panel, panelWidth, y, "Current Waypoint",
            { OrderedWaypointManager.getCurrentWaypointColor() },
            { r, g, b -> OrderedWaypointManager.setCurrentWaypointColor(r, g, b) },
            { OrderedWaypointManager.getCurrentWaypointAlpha() },
            { OrderedWaypointManager.setCurrentWaypointAlpha(it) })
        y = SettingsUi.colorRow(panel, panelWidth, y, "Next Waypoints",
            { OrderedWaypointManager.getNextWaypointColor() },
            { r, g, b -> OrderedWaypointManager.setNextWaypointColor(r, g, b) },
            { OrderedWaypointManager.getNextWaypointAlpha() },
            { OrderedWaypointManager.setNextWaypointAlpha(it) })
        y = SettingsUi.colorRow(panel, panelWidth, y, "Previous Waypoint",
            { OrderedWaypointManager.getPreviousWaypointColor() },
            { r, g, b -> OrderedWaypointManager.setPreviousWaypointColor(r, g, b) },
            { OrderedWaypointManager.getPreviousWaypointAlpha() },
            { OrderedWaypointManager.setPreviousWaypointAlpha(it) })
        SettingsUi.colorRow(panel, panelWidth, y, "Trace Line",
            { OrderedWaypointManager.getTraceLineColor() },
            { r, g, b -> OrderedWaypointManager.setTraceLineColor(r, g, b) },
            { OrderedWaypointManager.getTraceLineAlpha() },
            { OrderedWaypointManager.setTraceLineAlpha(it) })

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
