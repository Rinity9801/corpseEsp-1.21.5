package forfun.miningqol.client.gui

import com.mojang.blaze3d.platform.InputConstants
import forfun.miningqol.client.CommandKeybindManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
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
 * 26.1.2 command-keybind editor — a card per bind (key box + command field + delete),
 * ported from the 1.21.11 screen and adapted to the newer Vexel API. Key capture goes
 * through Vexel's onKeyType hook.
 */
class CommandKeybindCategoryScreen(private val parent: Screen) : BaseCategoryScreen(parent, "Command Keybinds Settings") {
    private lateinit var scrollContainer: Rectangle
    private lateinit var entriesContainer: Rectangle
    private val entries = mutableListOf<KeybindEntry>()
    private var capturingEntry: KeybindEntry? = null

    private val cardHeight = 118f
    private val cardSpacing = 16f
    private val listHeight = 420f

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panel = SettingsUi.panel(window, 900f, 660f, "Command Keybinds", "Bind commands to keys for quick access")

        scrollContainer = Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f,
            scrollable = true
        )
            .setSizing(800f, Size.Pixels, listHeight, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 100f, Pos.ParentPixels)
            .childOf(panel)
            .padding(18f)

        entriesContainer = Rectangle(backgroundColor = 0x00000000, borderColor = 0x00000000)
            .setSizing(100f, Size.Percent, 0f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(scrollContainer)

        loadExistingKeybinds()

        Button("+ Add Keybind", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(200f, Size.Pixels, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 545f, Pos.ParentPixels)
            .backgroundColor(0xFF2A5A2A.toInt())
            .borderColor(0xFF40A040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF357035.toInt(), 0xFFFFFFFF.toInt())
            .onClick { _ ->
                addNewKeybindEntry()
                true
            }
            .childOf(panel)

        SettingsUi.backButton(panel) {
            saveKeybinds()
            saveAndClose()
        }
    }

    private fun loadExistingKeybinds() {
        CommandKeybindManager.getAllKeybinds().forEach { (keyCode, command) ->
            entries.add(KeybindEntry(keyCode, command))
        }
        rebuildCards()
    }

    private fun addNewKeybindEntry() {
        entries.add(KeybindEntry(-1, ""))
        rebuildCards()
    }

    private fun deleteKeybindEntry(entry: KeybindEntry) {
        entries.remove(entry)
        if (capturingEntry === entry) capturingEntry = null
        saveKeybinds()
        rebuildCards()
    }

    private fun rebuildCards() {
        // Recreate the entries container so cards reposition cleanly.
        scrollContainer.children.remove(entriesContainer)
        entriesContainer = Rectangle(backgroundColor = 0x00000000, borderColor = 0x00000000)
            .setSizing(100f, Size.Percent, 0f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(scrollContainer)

        entries.forEachIndexed { index, entry -> createKeybindCard(entry, index) }

        val total = if (entries.isEmpty()) 0f else entries.size * (cardHeight + cardSpacing) - cardSpacing
        entriesContainer.height = total.coerceAtLeast(listHeight)
    }

    private fun createKeybindCard(entry: KeybindEntry, index: Int) {
        val yPos = index * (cardHeight + cardSpacing)

        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(100f, Size.Percent, cardHeight, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, yPos, Pos.ParentPixels)
            .childOf(entriesContainer)

        Rectangle(backgroundColor = 0xFFA05BFF.toInt(), borderRadius = 12f)
            .setSizing(5f, Size.Pixels, 100f, Size.Percent)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(card)

        Text("Key:", 0xFFAAAAAA.toInt(), 13f, false)
            .setPositioning(30f, Pos.ParentPixels, 22f, Pos.ParentPixels)
            .childOf(card)

        val keyBox = Rectangle(
            backgroundColor = 0xFF252525.toInt(),
            borderColor = 0xFF404040.toInt(),
            borderRadius = 6f,
            borderThickness = 1f,
            hoverColor = 0xFF303030.toInt()
        )
            .setSizing(180f, Size.Pixels, 36f, Size.Pixels)
            .setPositioning(30f, Pos.ParentPixels, 48f, Pos.ParentPixels)
            .childOf(card)

        val keyText = Text(getKeyDisplayName(entry.keyCode), 0xFFFFFFFF.toInt(), 13f, false)
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
                keyBox.backgroundColor = 0xFF252525.toInt()
                keyBox.borderColor = 0xFF404040.toInt()
                capturingEntry = null
                saveKeybinds()
            } else {
                // Arm: next keyboard key (keyPressed) or click here (mouse button) binds it.
                capturingEntry = entry
                keyText.text = "Press a key or click here..."
                keyBox.backgroundColor = 0xFF3A3A1A.toInt()
                keyBox.borderColor = 0xFFAAAA40.toInt()
            }
            true
        }

        Text("Command:", 0xFFAAAAAA.toInt(), 13f, false)
            .setPositioning(260f, Pos.ParentPixels, 22f, Pos.ParentPixels)
            .childOf(card)

        val commandInput = TextInput(initialValue = entry.command, placeholder = "e.g. /warp forge", fontSize = 13f)
            .setSizing(430f, Size.Pixels, 36f, Size.Pixels)
            .setPositioning(260f, Pos.ParentPixels, 48f, Pos.ParentPixels)
            .backgroundColor(0xFF252525.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .childOf(card)
        commandInput.onValueChange { value ->
            entry.command = value as String
            saveKeybinds()
        }

        Button("×", 0xFFFF5555.toInt(), fontSize = 20f)
            .setSizing(40f, Size.Pixels, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-40f, 0f)
            .backgroundColor(0xFF3A2A2A.toInt())
            .borderColor(0xFF5A4040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(0xFF5A3535.toInt(), 0xFFFFAAAA.toInt())
            .onClick { _ ->
                deleteKeybindEntry(entry)
                true
            }
            .childOf(card)
    }

    private fun saveKeybinds() {
        CommandKeybindManager.getAllKeybinds().keys.toList().forEach { CommandKeybindManager.removeKeybind(it) }
        entries.forEach { entry ->
            if (entry.keyCode != -1 && entry.command.isNotBlank()) {
                CommandKeybindManager.registerKeybind(entry.keyCode, entry.command)
            }
        }
    }

    // Capture through the vanilla key hook so we get the real GLFW key code that
    // CommandKeybindManager matches against (Vexel's onKeyType reports a scancode).
    override fun keyPressed(input: KeyEvent): Boolean {
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
            entry.keyBox?.backgroundColor = 0xFF252525.toInt()
            entry.keyBox?.borderColor = 0xFF404040.toInt()
            capturingEntry = null
            return true
        }
        if (code == GLFW.GLFW_KEY_ESCAPE) {
            saveKeybinds()
            saveAndClose()
            return true
        }
        return super.keyPressed(input)
    }

    override fun shouldCloseOnEsc(): Boolean = false

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
