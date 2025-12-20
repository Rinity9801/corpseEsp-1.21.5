package forfun.miningqol.client.gui

import forfun.miningqol.client.CommandKeybindManager
import forfun.miningqol.client.MiningqolClient
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.InputUtil
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.TextInput
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*
import org.lwjgl.glfw.GLFW

data class KeybindEntry(
    var keyCode: Int,
    var command: String,
    var card: Rectangle? = null,
    var keyText: Text? = null,
    var keyBox: Rectangle? = null
)

class CommandKeybindCategoryScreen(private val parentScreen: Screen) : VexelScreen("Command Keybinds") {
    private lateinit var overlay: Rectangle
    private lateinit var mainPanel: Rectangle
    private lateinit var scrollContainer: Rectangle
    private lateinit var entriesContainer: Rectangle
    private val entries = mutableListOf<KeybindEntry>()
    private var capturingEntry: KeybindEntry? = null
    private val mc = MinecraftClient.getInstance()

    override fun afterInitialization() {
        // Semi-transparent dark overlay background
        overlay = Rectangle(
            backgroundColor = 0x80000000.toInt(),
            borderColor = 0x00000000,
            borderRadius = 0f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(window)
            .fadeIn(400, EasingType.EASE_OUT)

        // Main panel - wider for more comfortable layout
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(900f, Size.Pixels, 650f, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        // Center the panel manually
        mainPanel.xPositionConstraint = Pos.ScreenPixels
        mainPanel.yPositionConstraint = Pos.ScreenPixels
        mainPanel.xConstraint = (mainPanel.screenWidth - 900f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - 650f) / 2f
        mainPanel.fadeIn(500, EasingType.EASE_OUT)

        // Title bar background
        Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 80f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusBottomLeft = 0f
                borderRadiusBottomRight = 0f
            }
            .fadeIn(600, EasingType.EASE_OUT)

        // Title
        Text("Command Keybinds", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        Text("Bind commands to keys for quick access", 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Scroll container for keybind entries (scrollable Rectangle)
        scrollContainer = Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f,
            scrollable = true
        )
            .setSizing(800f, Size.Pixels, 450f, Size.Pixels)
            .setPositioning(50f, Pos.ParentPixels, 110f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                padding(25f)
            }

        // Container for all entries
        entriesContainer = Rectangle(
            backgroundColor = 0x00000000,
            borderColor = 0x00000000
        )
            .setSizing(100f, Size.ParentPerc, 0f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(scrollContainer)

        // Load existing keybinds
        loadExistingKeybinds()

        // Add Keybind button
        Button("+ Add Keybind", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(200f, Size.Pixels, 42f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 575f, Pos.ParentPixels)
            .backgroundColor(0xFF2A5A2A.toInt())
            .borderColor(0xFF40A040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF357035.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A3A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                addNewKeybindEntry()
                true
            }
            .childOf(mainPanel)
            .fadeIn(900, EasingType.EASE_OUT)

        // Back button at bottom
        Button("Back", 0xFFFFFFFF.toInt(), fontSize = 15f)
            .setSizing(140f, Size.Pixels, 42f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -40f)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                closeWithAnimation()
                true
            }
            .childOf(mainPanel)
            .fadeIn(1000, EasingType.EASE_OUT)

        scrollContainer.visible = false
        Thread {
            Thread.sleep(200L)
            MinecraftClient.getInstance().execute {
                scrollContainer.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun loadExistingKeybinds() {
        val keybinds = CommandKeybindManager.getAllKeybinds()
        keybinds.forEach { (keyCode, command) ->
            val entry = KeybindEntry(keyCode, command)
            entries.add(entry)
            createKeybindCard(entry, entries.size - 1)
        }
        updateEntriesContainerHeight()
    }

    private fun addNewKeybindEntry() {
        val entry = KeybindEntry(-1, "")
        entries.add(entry)
        createKeybindCard(entry, entries.size - 1)
        updateEntriesContainerHeight()
    }

    private fun createKeybindCard(entry: KeybindEntry, index: Int) {
        val cardHeight = 120f
        val cardSpacing = 20f
        val yPos = index * (cardHeight + cardSpacing)

        // Card
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(100f, Size.ParentPerc, cardHeight, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, yPos, Pos.ParentPixels)
            .childOf(entriesContainer)
            .apply {
                dropShadow = true
                shadowBlur = 10f
                shadowSpread = 1f
                shadowColor = 0x30000000.toInt()
            }

        entry.card = card

        // Accent bar
        Rectangle(
            backgroundColor = 0xFFA05BFF.toInt(),
            borderRadius = 12f
        )
            .setSizing(5f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(card)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        // "Key:" label
        Text("Key:", 0xFFAAAAAA.toInt(), 13f, false)
            .setPositioning(30f, Pos.ParentPixels, 22f, Pos.ParentPixels)
            .childOf(card)

        // Custom keybind box (Rectangle + Text for dynamic updates)
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

        keyBox.onClick { _, _, mouseButton ->
            if (capturingEntry == entry) {
                // Already capturing for this entry - this click is the new binding
                val mouseCode = GLFW.GLFW_KEY_LAST + mouseButton + 1
                entry.keyCode = mouseCode
                keyText.text = getKeyDisplayName(mouseCode)
                keyBox.backgroundColor = 0xFF252525.toInt()
                keyBox.borderColor = 0xFF404040.toInt()
                capturingEntry = null
                saveKeybinds()
            } else {
                // Start capturing
                capturingEntry = entry
                keyText.text = "Press a key..."
                keyBox.backgroundColor = 0xFF3A3A1A.toInt()
                keyBox.borderColor = 0xFFAAAA40.toInt()
            }
            true
        }

        // "Command:" label
        Text("Command:", 0xFFAAAAAA.toInt(), 13f, false)
            .setPositioning(260f, Pos.ParentPixels, 22f, Pos.ParentPixels)
            .childOf(card)

        // Command text input
        val commandInput = TextInput(
            entry.command,
            "e.g. /warp hub",
            fontSize = 13f,
            textColor = 0xFFFFFFFF.toInt()
        )
            .setSizing(400f, Size.Pixels, 36f, Size.Pixels)
            .setPositioning(260f, Pos.ParentPixels, 48f, Pos.ParentPixels)
            .backgroundColor(0xFF252525.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .childOf(card)

        commandInput.onValueChange { newValue ->
            entry.command = newValue as String
            // Save immediately after command entry
            Thread {
                Thread.sleep(100) // Small delay to ensure value is set
                MinecraftClient.getInstance().execute {
                    saveKeybinds()
                }
            }.start()
        }

        // Delete button
        Button("×", 0xFFFF5555.toInt(), fontSize = 20f)
            .setSizing(40f, Size.Pixels, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-50f, 0f)
            .backgroundColor(0xFF3A2A2A.toInt())
            .borderColor(0xFF5A4040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(0xFF5A3535.toInt(), 0xFFFFAAAA.toInt())
            .pressedColors(0xFF2A1A1A.toInt(), 0xFFFF8888.toInt())
            .onClick { _, _, _ ->
                deleteKeybindEntry(entry)
                true
            }
            .childOf(card)
    }

    private fun deleteKeybindEntry(entry: KeybindEntry) {
        // Remove from CommandKeybindManager
        if (entry.keyCode != -1) {
            CommandKeybindManager.removeKeybind(entry.keyCode)
        }

        // Remove from entries list
        entries.remove(entry)

        // Rebuild all cards to fix positioning
        rebuildAllCards()
    }

    private fun rebuildAllCards() {
        // Clear the entries container by recreating it
        val oldContainer = entriesContainer
        scrollContainer.children.remove(oldContainer)

        entriesContainer = Rectangle(
            backgroundColor = 0x00000000,
            borderColor = 0x00000000
        )
            .setSizing(100f, Size.ParentPerc, 0f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(scrollContainer)

        // Recreate all cards with updated positions
        entries.forEachIndexed { index, entry ->
            createKeybindCard(entry, index)
        }

        updateEntriesContainerHeight()
    }

    private fun updateEntriesContainerHeight() {
        val cardHeight = 120f
        val cardSpacing = 20f
        val totalHeight = if (entries.isEmpty()) 0f else entries.size * (cardHeight + cardSpacing) - cardSpacing
        entriesContainer.height = totalHeight.coerceAtLeast(450f)
    }

    private fun saveKeybinds() {
        // Clear all existing keybinds first
        val allKeybinds = CommandKeybindManager.getAllKeybinds()
        allKeybinds.keys.forEach { keyCode ->
            CommandKeybindManager.removeKeybind(keyCode)
        }

        // Register all current keybinds
        entries.forEach { entry ->
            if (entry.keyCode != -1 && entry.command.isNotBlank()) {
                CommandKeybindManager.registerKeybind(entry.keyCode, entry.command)
            }
        }
    }

    private fun closeWithAnimation() {
        // Save all keybinds before closing
        saveKeybinds()

        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()

        MinecraftClient.getInstance().setScreen(parentScreen)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // If we're capturing a key for a keybind
        val entry = capturingEntry
        if (entry != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                // Cancel capturing on ESC
                entry.keyText?.text = getKeyDisplayName(entry.keyCode)
                entry.keyBox?.backgroundColor = 0xFF252525.toInt()
                entry.keyBox?.borderColor = 0xFF404040.toInt()
                capturingEntry = null
                return true
            }
            // Bind this key
            entry.keyCode = keyCode
            entry.keyText?.text = getKeyDisplayName(keyCode)
            entry.keyBox?.backgroundColor = 0xFF252525.toInt()
            entry.keyBox?.borderColor = 0xFF404040.toInt()
            saveKeybinds()
            capturingEntry = null
            return true
        }

        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int) {
        if (capturingEntry != null) {
            return // Don't process key types while capturing
        }
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
        } else {
            super.onKeyType(typedChar, keyCode, scanCode)
        }
    }

    private fun getKeyDisplayName(keyCode: Int): String {
        if (keyCode == -1) return "Click to bind"
        if (keyCode == CommandKeybindManager.SCROLL_UP) return "Scroll Up"
        if (keyCode == CommandKeybindManager.SCROLL_DOWN) return "Scroll Down"
        if (keyCode > GLFW.GLFW_KEY_LAST) {
            val mouseButton = keyCode - GLFW.GLFW_KEY_LAST - 1
            return when (mouseButton) {
                0 -> "Mouse Left"
                1 -> "Mouse Right"
                2 -> "Mouse Middle"
                else -> "Mouse ${mouseButton + 1}"
            }
        }
        return try {
            val key = InputUtil.Type.KEYSYM.createFromCode(keyCode)
            key.localizedText.string
        } catch (e: Exception) {
            "Key $keyCode"
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val entry = capturingEntry
        if (entry != null) {
            val scrollCode = if (verticalAmount > 0) CommandKeybindManager.SCROLL_UP else CommandKeybindManager.SCROLL_DOWN
            entry.keyCode = scrollCode
            entry.keyText?.text = getKeyDisplayName(scrollCode)
            entry.keyBox?.backgroundColor = 0xFF252525.toInt()
            entry.keyBox?.borderColor = 0xFF404040.toInt()
            capturingEntry = null
            saveKeybinds()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun shouldCloseOnEsc(): Boolean = false
}
