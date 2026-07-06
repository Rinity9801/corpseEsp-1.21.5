package forfun.miningqol.client.gui

import forfun.miningqol.client.CommissionHUD
import forfun.miningqol.client.MiningqolClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import xyz.meowing.vexel.core.VexelScreen

class CommissionHudCategoryScreen(private val parentScreen: Screen) : VexelScreen("Commission HUD Settings") {
    private val accent = 0xFF88AAFF.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panelHeight = 480f
        val panel = SettingsUi.panel(window, panelWidth, panelHeight, "Commission HUD", "On-screen commission progress tracker")

        var y = 110f
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Enabled", "Show the commission HUD while in the mines", accent,
            CommissionHUD.isEnabled()) {
            CommissionHUD.setEnabled(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Background", "Dark backdrop behind the HUD", accent,
            CommissionHUD.isBackgroundEnabled()) {
            CommissionHUD.setBackgroundEnabled(it)
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Scale", 0.5f, 2.0f, 0.05f,
            CommissionHUD.getScale(), accent, { String.format("%.2fx", it) }) {
            CommissionHUD.setScale(it)
        }

        SettingsUi.linkRow(panel, panelWidth, y, "Move HUD", "Drag the panel to wherever you want it") {
            Minecraft.getInstance().setScreen(CommissionHudPositionScreen(this))
        }

        SettingsUi.backButton(panel) { close() }
    }

    private fun close() {
        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()
        Minecraft.getInstance().setScreen(parentScreen)
    }

    override fun onClose() {
        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()
        super.onClose()
        Minecraft.getInstance().setScreen(parentScreen)
    }
}
