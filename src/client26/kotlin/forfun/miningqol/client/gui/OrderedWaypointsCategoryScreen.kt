package forfun.miningqol.client.gui

import forfun.miningqol.client.waypoints.OrderedWaypointManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

class OrderedWaypointsCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Ordered Waypoints Settings") {
    private val accent = 0xFFAAFF88.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val rows = 12
        val panelHeight = 110f + rows * (SettingsUi.ROW_HEIGHT + SettingsUi.ROW_SPACING) + 76f
        val panel = SettingsUi.panel(window, panelWidth, panelHeight, "Ordered Waypoints", "Guided mining routes — manage with /mqo")

        var y = 110f
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Enabled", "Render and advance the loaded route", accent,
            OrderedWaypointManager.isEnabledRaw()) {
            OrderedWaypointManager.setEnabled(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Trace Line", "Line from your crosshair to the next waypoint", accent,
            OrderedWaypointManager.isTraceLineEnabled()) {
            OrderedWaypointManager.setTraceLineEnabled(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Show Number", "Waypoint #number on the label", accent,
            OrderedWaypointManager.isShowName()) {
            OrderedWaypointManager.setShowName(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Show Distance", "Distance in meters on the label", accent,
            OrderedWaypointManager.isShowDistance()) {
            OrderedWaypointManager.setShowDistance(it)
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Trigger Range", 1f, 10f, 0.5f,
            OrderedWaypointManager.getWaypointRange(), accent, { String.format("%.1f blocks", it) }) {
            OrderedWaypointManager.setWaypointRange(it)
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Upcoming Waypoints", 0f, 5f, 1f,
            OrderedWaypointManager.getNextCount().toFloat(), accent, { "${it.toInt()} shown" }) {
            OrderedWaypointManager.setNextCount(it.toInt())
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Lobby Check", "Scan upcoming waypoints for the expected block; flag wrong lobbies red", accent,
            OrderedWaypointManager.isLobbyCheckEnabled()) {
            OrderedWaypointManager.setLobbyCheckEnabled(it)
        }
        y = SettingsUi.textRow(panel, panelWidth, y, "Lobby Check Block", "Block id to expect (e.g. minecraft:coal_ore)",
            OrderedWaypointManager.getLobbyCheckBlock()) {
            OrderedWaypointManager.setLobbyCheckBlock(it.trim())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Check Interval", 2f, 30f, 1f,
            OrderedWaypointManager.getLobbyCheckInterval().toFloat(), accent, { "every ${it.toInt()} waypoints" }) {
            OrderedWaypointManager.setLobbyCheckInterval(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Check Radius", 1f, 5f, 1f,
            OrderedWaypointManager.getLobbyCheckRadius().toFloat(), accent, { "${it.toInt()} blocks" }) {
            OrderedWaypointManager.setLobbyCheckRadius(it.toInt())
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Block Outline", "Highlight matching blocks around the next waypoint", accent,
            OrderedWaypointManager.isBlockOutlineAroundWaypoint()) {
            OrderedWaypointManager.setBlockOutlineAroundWaypoint(it)
        }
        SettingsUi.linkRow(panel, panelWidth, y, "Waypoint Colors", "Colors and opacity for each waypoint type") {
            Minecraft.getInstance().setScreen(WaypointColorsScreen(this))
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
