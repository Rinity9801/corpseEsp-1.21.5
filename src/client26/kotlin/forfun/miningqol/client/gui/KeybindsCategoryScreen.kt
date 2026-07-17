package forfun.miningqol.client.gui

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.resources.language.I18n
import org.lwjgl.glfw.GLFW
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button

/**
 * Rebind the mod's hotkeys without leaving the mod GUI. Edits the same vanilla
 * KeyMappings shown under Controls -> MiningQOL (changes save to options.txt),
 * listing every mapping whose id starts with "key.miningqol.". Click a key box,
 * then press a key — or click the box again to bind a mouse button; Esc cancels.
 */
class KeybindsCategoryScreen(parent: Screen) : BaseCategoryScreen(parent, "Keybinds Settings") {
    private class Row(
        val mapping: KeyMapping,
        var keyText: Text? = null,
        var keyBox: Rectangle? = null
    )

    private val rows = mutableListOf<Row>()
    private var capturing: Row? = null

    override fun afterInitialization() {
        SettingsUi.overlay(window)

        rows.clear()
        Minecraft.getInstance().options.keyMappings
            .filter { it.name.startsWith("key.miningqol.") }
            .forEach { rows.add(Row(it)) }

        val panelWidth = 600f
        val panelHeight = 130f + rows.size * (SettingsUi.ROW_HEIGHT + SettingsUi.ROW_SPACING) + 60f
        val panel = SettingsUi.panel(window, panelWidth, panelHeight,
            "Keybinds", "Click a key box, then press a key (click the box again for a mouse button) — also in Controls")

        var y = 110f
        for (row in rows) {
            createRow(panel, panelWidth, y, row)
            y += SettingsUi.ROW_HEIGHT + SettingsUi.ROW_SPACING
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }

    private fun createRow(panel: Rectangle, panelWidth: Float, y: Float, row: Row) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(SettingsUi.ROW_WIDTH, Size.Pixels, SettingsUi.ROW_HEIGHT, Size.Pixels)
            .setPositioning((panelWidth - SettingsUi.ROW_WIDTH) / 2f, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(panel)

        Text(I18n.get(row.mapping.name), 0xFFFFFFFF.toInt(), 17f, true)
            .setPositioning(16f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .childOf(card)

        Text("Toggle key", 0xFF888888.toInt(), 12f, false)
            .setPositioning(16f, Pos.ParentPixels, 33f, Pos.ParentPixels)
            .childOf(card)

        val keyBox = Rectangle(
            backgroundColor = 0xFF252525.toInt(),
            borderColor = 0xFF404040.toInt(),
            borderRadius = 6f,
            borderThickness = 1f,
            hoverColor = 0xFF303030.toInt()
        )
            .setSizing(150f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-64f, 0f)
            .childOf(card)

        val keyText = Text(keyLabel(row.mapping), 0xFFFFFFFF.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
            .childOf(keyBox)

        row.keyBox = keyBox
        row.keyText = keyText

        keyBox.onClick { event ->
            if (capturing === row) {
                // Armed → this click binds the mouse button that was used.
                applyKey(row, InputConstants.Type.MOUSE.getOrCreate(event.button))
            } else {
                capturing?.let { restore(it) } // un-arm any other row
                capturing = row
                keyText.text = "Press a key..."
                keyBox.backgroundColor = 0xFF3A3A1A.toInt()
                keyBox.borderColor = 0xFFAAAA40.toInt()
            }
            true
        }

        // Unbind
        Button("×", 0xFFFF5555.toInt(), fontSize = 18f)
            .setSizing(32f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-16f, 0f)
            .backgroundColor(0xFF3A2A2A.toInt())
            .borderColor(0xFF5A4040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(0xFF5A3535.toInt(), 0xFFFFAAAA.toInt())
            .onClick { _ ->
                if (capturing === row) capturing = null
                applyKey(row, InputConstants.UNKNOWN)
                true
            }
            .childOf(card)
    }

    private fun applyKey(row: Row, key: InputConstants.Key) {
        row.mapping.setKey(key)
        KeyMapping.resetMapping()
        Minecraft.getInstance().options.save()
        capturing = null
        restore(row)
    }

    private fun restore(row: Row) {
        row.keyText?.text = keyLabel(row.mapping)
        row.keyBox?.backgroundColor = 0xFF252525.toInt()
        row.keyBox?.borderColor = 0xFF404040.toInt()
    }

    private fun keyLabel(mapping: KeyMapping): String {
        if (mapping.isUnbound) return "None"
        return mapping.translatedKeyMessage.string
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        val row = capturing
        if (row != null) {
            if (input.key() != GLFW.GLFW_KEY_ESCAPE) {
                applyKey(row, InputConstants.Type.KEYSYM.getOrCreate(input.key()))
            } else {
                capturing = null
                restore(row)
            }
            return true
        }
        return super.keyPressed(input)
    }
}
