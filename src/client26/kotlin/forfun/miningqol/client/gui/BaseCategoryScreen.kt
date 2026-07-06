package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import xyz.meowing.vexel.core.VexelScreen

/** Category screens save the config and return to the parent on Back/Esc. */
abstract class BaseCategoryScreen(private val parentScreen: Screen, name: String) : VexelScreen(name) {

    protected fun saveAndClose() {
        saveConfig()
        Minecraft.getInstance().setScreen(parentScreen)
    }

    override fun onClose() {
        saveConfig()
        super.onClose()
        Minecraft.getInstance().setScreen(parentScreen)
    }

    private fun saveConfig() {
        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()
    }
}
