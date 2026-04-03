package forfun.miningqol.client.gui

import forfun.miningqol.client.hotm.HotmChestScreen
import forfun.miningqol.client.hotm.HotmManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.input.KeyInput
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.TextInput
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*

class HotmSaveScreen : VexelScreen("Save HOTM Preset") {
    private lateinit var nameInput: TextInput
    private var currentName = ""

    override fun afterInitialization() {
        // Dark overlay
        Rectangle(
            backgroundColor = 0x80000000.toInt(),
            borderColor = 0x00000000,
            borderRadius = 0f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(window)

        // Small panel
        val panel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(300f, Size.Pixels, 150f, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 30f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        panel.xPositionConstraint = Pos.ScreenPixels
        panel.yPositionConstraint = Pos.ScreenPixels
        panel.xConstraint = (panel.screenWidth - 300f) / 2f
        panel.yConstraint = (panel.screenHeight - 150f) / 2f

        Text("Save Preset", 0xFFFFFFFF.toInt(), 20f, true)
            .setPositioning(0f, Pos.ParentCenter, 15f, Pos.ParentPixels)
            .childOf(panel)

        Text("Enter preset name:", 0xFF888888.toInt(), 12f, false)
            .setPositioning(0f, Pos.ParentCenter, 42f, Pos.ParentPixels)
            .childOf(panel)

        nameInput = TextInput(
            "",
            "preset name",
            fontSize = 14f,
            textColor = 0xFFFFFFFF.toInt()
        )
            .setSizing(240f, Size.Pixels, 30f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 60f, Pos.ParentPixels)
            .backgroundColor(0xFF1E1E1E.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .childOf(panel)
            .apply {
                onValueChange { value ->
                    currentName = (value as? String) ?: ""
                }
            }

        Button("Save", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(100f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(30f, Pos.ParentPixels, 105f, Pos.ParentPixels)
            .backgroundColor(0xFF1A5A1A.toInt())
            .borderColor(0xFF2A8A2A.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(0xFF2A7A2A.toInt(), 0xFFFFFFFF.toInt())
            .onClick { _, _, _ ->
                val name = currentName.trim()
                if (name.isNotEmpty()) {
                    HotmManager.savePreset(name)
                    HotmChestScreen.open()
                }
                true
            }
            .childOf(panel)

        Button("Cancel", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(100f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(170f, Pos.ParentPixels, 105f, Pos.ParentPixels)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .onClick { _, _, _ ->
                HotmChestScreen.open()
                true
            }
            .childOf(panel)
    }

    override fun keyPressed(input: KeyInput?): Boolean {
        if (input?.key() == KnitKeys.KEY_ESCAPE.code) {
            HotmChestScreen.open()
            return true
        }
        return super.keyPressed(input)
    }

    override fun shouldCloseOnEsc(): Boolean = false
}
