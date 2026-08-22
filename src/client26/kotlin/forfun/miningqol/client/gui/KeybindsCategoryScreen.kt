package forfun.miningqol.client.gui

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import org.lwjgl.glfw.GLFW
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button

/**
 * Rebind the mod's hotkeys without leaving the mod GUI. Edits the same vanilla
 * KeyMappings shown under Controls -> Sybau (changes save to options.txt),
 * listing every mapping whose id starts with "key.miningqol.". Click a key box,
 * then press a key — or click the box again to bind a mouse button; Esc cancels.
 */
object KeybindsContent {
    private class Row(
        val mapping: KeyMapping,
        var keyText: Text? = null,
        var keyBox: Rectangle? = null
    )

    fun build(host: VexelMainScreen, wrapper: Rectangle, width: Float): Float {
        val rows = Minecraft.getInstance().options.keyMappings
            .filter { it.name.startsWith("key.miningqol.") }
            .map { Row(it) }

        var capturing: Row? = null

        fun restore(row: Row) {
            row.keyText?.text = keyLabel(row.mapping)
            row.keyBox?.backgroundColor = SettingsUi.alpha(SettingsUi.TRACK)
            row.keyBox?.borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER)
        }

        fun applyKey(row: Row, key: InputConstants.Key) {
            row.mapping.setKey(key)
            KeyMapping.resetMapping()
            Minecraft.getInstance().options.save()
            capturing = null
            restore(row)
        }

        var y = 0f
        for (row in rows) {
            val card = SettingsUi.inlineCard(wrapper, width, y, 60f)

            Text(I18n.get(row.mapping.name), SettingsUi.TEXT_PRIMARY, 16f, true)
                .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
                .childOf(card)

            Text("Toggle key", SettingsUi.TEXT_MUTED, 11f, false)
                .setPositioning(18f, Pos.ParentPixels, 36f, Pos.ParentPixels)
                .childOf(card)

            val keyBox = Rectangle(
                backgroundColor = SettingsUi.alpha(SettingsUi.TRACK),
                borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER),
                borderRadius = 9f,
                borderThickness = SettingsUi.EDGE_WIDTH,
                hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
            )
                .setSizing(160f, Size.Pixels, 34f, Size.Pixels)
                .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .alignRight()
                .setOffset(-66f, 0f)
                .childOf(card)

            val keyText = Text(keyLabel(row.mapping), SettingsUi.TEXT_PRIMARY, 13f, false)
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
                    keyBox.borderColor = SettingsUi.edge(SettingsUi.YELLOW)
                }
                true
            }

            // Unbind
            Button("×", SettingsUi.RED, fontSize = 18f)
                .setSizing(34f, Size.Pixels, 34f, Size.Pixels)
                .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .alignRight()
                .setOffset(-18f, 0f)
                .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
                .borderColor(SettingsUi.edge(SettingsUi.RED, 0.6f))
                .borderRadius(9f)
                .borderThickness(SettingsUi.EDGE_WIDTH)
                .hoverColors(SettingsUi.alpha(SettingsUi.CARD_HOVER), SettingsUi.RED)
                .onClick { _ ->
                    if (capturing === row) capturing = null
                    applyKey(row, InputConstants.UNKNOWN)
                    true
                }
                .childOf(card)

            y += 72f
        }

        // Capture through the vanilla key hook so we get the real GLFW key code.
        host.keyHandler = { input ->
            val row = capturing
            if (row != null) {
                if (input.key() != GLFW.GLFW_KEY_ESCAPE) {
                    applyKey(row, InputConstants.Type.KEYSYM.getOrCreate(input.key()))
                } else {
                    capturing = null
                    restore(row)
                }
                true
            } else {
                false
            }
        }
        return y
    }

    private fun keyLabel(mapping: KeyMapping): String {
        if (mapping.isUnbound) return "None"
        return mapping.translatedKeyMessage.string
    }
}
