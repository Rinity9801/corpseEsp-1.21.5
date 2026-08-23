package forfun.miningqol.client.gui

import com.mojang.blaze3d.platform.InputConstants
import forfun.miningqol.client.CommandKeybindManager
import org.lwjgl.glfw.GLFW
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.TextInput

private data class KeybindEntry(
    var keyCode: Int,
    var command: String,
    var keyText: Text? = null,
    var keyBox: Rectangle? = null
)

/**
 * Command-keybind editor — a card per bind (key box + command field + delete)
 * in the detail panel. Key capture goes through the host's vanilla key hook so
 * we get the real GLFW key code that CommandKeybindManager matches against.
 *
 * Add/delete rebuild the detail via the host (the wrapper height changes); the
 * working entry list is kept across those rebuilds so a freshly added, not yet
 * bound row doesn't vanish. Opening the detail fresh reloads from the manager.
 */
object CommandKeybindContent {
    private var retained: MutableList<KeybindEntry>? = null

    fun build(host: VexelMainScreen, wrapper: Rectangle, width: Float): Float {
        val entries = retained ?: CommandKeybindManager.getAllKeybinds()
            .map { (keyCode, command) -> KeybindEntry(keyCode, command) }
            .toMutableList()
        retained = null

        var capturingEntry: KeybindEntry? = null
        val accent = SettingsUi.PURPLE2

        fun saveKeybinds() {
            CommandKeybindManager.getAllKeybinds().keys.toList().forEach { CommandKeybindManager.removeKeybind(it) }
            entries.forEach { entry ->
                if (entry.keyCode != -1 && entry.command.isNotBlank()) {
                    CommandKeybindManager.registerKeybind(entry.keyCode, entry.command)
                }
            }
        }

        fun refreshKeeping() {
            retained = entries
            host.refreshDetail()
        }

        var y = 0f
        entries.forEach { entry ->
            val card = SettingsUi.inlineCard(wrapper, width, y, 92f)

            Text("Key", SettingsUi.TEXT_MUTED, 12f, false)
                .setPositioning(18f, Pos.ParentPixels, 14f, Pos.ParentPixels)
                .childOf(card)

            val keyBox = Rectangle(
                backgroundColor = SettingsUi.alpha(SettingsUi.TRACK),
                borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER),
                borderRadius = 9f,
                borderThickness = SettingsUi.EDGE_WIDTH,
                hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
            )
                .setSizing(180f, Size.Pixels, 36f, Size.Pixels)
                .setPositioning(18f, Pos.ParentPixels, 38f, Pos.ParentPixels)
                .childOf(card)

            val keyText = Text(getKeyDisplayName(entry.keyCode), SettingsUi.TEXT_PRIMARY, 13f, false)
                .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
                .childOf(keyBox)

            entry.keyText = keyText
            entry.keyBox = keyBox

            keyBox.onClick { event ->
                if (capturingEntry === entry) {
                    // Already armed → this click binds the mouse button that was used.
                    val code = GLFW.GLFW_KEY_LAST + event.button + 1
                    entry.keyCode = code
                    keyText.text = getKeyDisplayName(code)
                    keyBox.borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER)
                    capturingEntry = null
                    saveKeybinds()
                } else {
                    // Arm: next keyboard key or click here (mouse button) binds it.
                    capturingEntry = entry
                    keyText.text = "Press a key or click here..."
                    keyBox.borderColor = SettingsUi.edge(SettingsUi.YELLOW)
                }
                true
            }

            Text("Command", SettingsUi.TEXT_MUTED, 12f, false)
                .setPositioning(240f, Pos.ParentPixels, 14f, Pos.ParentPixels)
                .childOf(card)

            val commandInput = TextInput(initialValue = entry.command, placeholder = "e.g. /warp forge", fontSize = 13f)
                .setSizing(width - 240f - 90f, Size.Pixels, 36f, Size.Pixels)
                .setPositioning(240f, Pos.ParentPixels, 38f, Pos.ParentPixels)
                .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
                .borderColor(SettingsUi.edge(SettingsUi.CARD_BORDER))
                .borderRadius(9f)
                .borderThickness(SettingsUi.EDGE_WIDTH)
                .childOf(card)
            commandInput.onValueChange { value ->
                entry.command = value as String
                saveKeybinds()
            }

            Button("×", SettingsUi.RED, fontSize = 20f)
                .setSizing(40f, Size.Pixels, 40f, Size.Pixels)
                .setPositioning(-18f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .alignRight()
                .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
                .borderColor(SettingsUi.edge(SettingsUi.RED, 0.6f))
                .borderRadius(9f)
                .borderThickness(SettingsUi.EDGE_WIDTH)
                .hoverColors(SettingsUi.alpha(SettingsUi.CARD_HOVER), SettingsUi.RED)
                .onClick { _ ->
                    entries.remove(entry)
                    if (capturingEntry === entry) capturingEntry = null
                    saveKeybinds()
                    refreshKeeping()
                    true
                }
                .childOf(card)

            y += 104f
        }

        Button("+ Add Keybind", SettingsUi.TEXT_PRIMARY, fontSize = 14f)
            .setSizing(200f, Size.Pixels, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, y + 4f, Pos.ParentPixels)
            .backgroundColor(SettingsUi.tint(accent, 0.14f))
            .borderColor(SettingsUi.edge(accent, 0.65f))
            .borderRadius(10f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .hoverColors(SettingsUi.tint(accent, 0.24f), SettingsUi.TEXT_PRIMARY)
            .onClick { _ ->
                entries.add(KeybindEntry(-1, ""))
                refreshKeeping()
                true
            }
            .childOf(wrapper)
        y += 56f

        host.keyHandler = { input ->
            val code = input.key()
            val entry = capturingEntry
            if (entry != null) {
                if (code != GLFW.GLFW_KEY_ESCAPE) {
                    entry.keyCode = code
                    entry.keyText?.text = getKeyDisplayName(code)
                    saveKeybinds()
                } else {
                    entry.keyText?.text = getKeyDisplayName(entry.keyCode)
                }
                entry.keyBox?.borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER)
                capturingEntry = null
                true
            } else {
                false
            }
        }
        return y
    }

    private fun getKeyDisplayName(keyCode: Int): String {
        if (keyCode == -1) return "Click to bind"
        if (keyCode > GLFW.GLFW_KEY_LAST) {
            return when (val mb = keyCode - GLFW.GLFW_KEY_LAST - 1) {
                0 -> "Mouse Left"
                1 -> "Mouse Right"
                2 -> "Mouse Middle"
                else -> "Mouse ${mb + 1}"
            }
        }
        return try {
            InputConstants.Type.KEYSYM.getOrCreate(keyCode).displayName.string
        } catch (e: Exception) {
            "Key $keyCode"
        }
    }
}
