package forfun.miningqol.client.gui

import com.mojang.blaze3d.platform.InputConstants
import forfun.miningqol.client.CommandKeybindManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * 26.1.2 command-keybind editor. Existing binds are click-to-remove rows; to add
 * one, type a command and click "Bind Key", then press the key to bind it to.
 * Built on SettingsUi so it matches the other 26 category screens; key capture
 * goes through Vexel's onKeyType hook.
 */
class CommandKeybindCategoryScreen(private val parent: Screen) : BaseCategoryScreen(parent, "Command Keybinds Settings") {
    private var pendingCommand = ""
    private var capturing = false

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 640f
        val binds = CommandKeybindManager.getAllKeybinds().toList()
        val panelHeight = 110f + (binds.size + 2) * 68f + 80f
        val panel = SettingsUi.panel(window, panelWidth, panelHeight, "Command Keybinds", "Bind commands to keys for quick access")

        var y = 110f
        for ((key, cmd) in binds) {
            val k = key
            y = SettingsUi.linkRow(panel, panelWidth, y, "${keyName(k)}   →   $cmd", "Click to remove this keybind") {
                CommandKeybindManager.removeKeybind(k)
                reopen()
            }
        }

        y = SettingsUi.textRow(panel, panelWidth, y, "New command", "e.g. /warp forge", pendingCommand) {
            pendingCommand = it
        }
        SettingsUi.linkRow(panel, panelWidth, y,
            "Bind Key", "Type a command above, then click and press the key to bind") {
            if (pendingCommand.isBlank()) {
                msg("§cType a command first.")
            } else {
                capturing = true
                msg("§ePress a key to bind §f$pendingCommand§e (Esc cancels)")
            }
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }

    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int): Boolean {
        if (capturing) {
            capturing = false
            if (keyCode != GLFW.GLFW_KEY_ESCAPE && pendingCommand.isNotBlank()) {
                CommandKeybindManager.registerKeybind(keyCode, pendingCommand)
                pendingCommand = ""
            }
            reopen()
            return true
        }
        return super.onKeyType(typedChar, keyCode, scanCode)
    }

    /** Rebuild from the manager (which holds the live binds) after add/remove. */
    private fun reopen() {
        Minecraft.getInstance().setScreen(CommandKeybindCategoryScreen(parent))
    }

    private fun keyName(keyCode: Int): String = try {
        InputConstants.Type.KEYSYM.getOrCreate(keyCode).displayName.string
    } catch (e: Exception) {
        "Key $keyCode"
    }

    private fun msg(s: String) {
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal("§6[MQO] $s"))
    }
}
