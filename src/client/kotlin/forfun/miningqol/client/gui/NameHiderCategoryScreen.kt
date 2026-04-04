package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.NameHider
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text as VText
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.ColorPicker
import xyz.meowing.vexel.elements.TextInput
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*

class NameHiderCategoryScreen(private val parentScreen: Screen) : VexelScreen("Name Hider Settings") {
    private lateinit var overlay: Rectangle
    private lateinit var mainPanel: Rectangle
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

        // Main panel - darker and more modern
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(550f, Size.Pixels, 620f, Size.Pixels)
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
        mainPanel.xConstraint = (mainPanel.screenWidth - 550f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - 620f) / 2f
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
        VText("Name Hider Settings", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        VText("Customize player name replacement and colors", 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Enable toggle
        createToggleCard(
            "Enable Name Hider",
            0xFFFF9944.toInt(),
            { NameHider.isEnabled() },
            { NameHider.setEnabled(!NameHider.isEnabled()) },
            35f,
            100f,
            480f,
            65f,
            mainPanel,
            200L
        )

        // Replacement name text input
        createTextInputCard(
            "Replacement Name",
            NameHider.getReplacementName(),
            { text -> NameHider.setReplacementName(text) },
            35f,
            180f,
            480f,
            75f,
            mainPanel,
            300L
        )

        // Gradient toggle
        createToggleCard(
            "Use Gradient",
            0xFFFF9944.toInt(),
            { NameHider.isUsingGradient() },
            { NameHider.setUseGradient(!NameHider.isUsingGradient()) },
            35f,
            270f,
            480f,
            65f,
            mainPanel,
            400L
        )

        // Color 1 picker
        createColorPickerCard(
            "Color 1",
            (NameHider.getRed1() * 255).toInt(),
            (NameHider.getGreen1() * 255).toInt(),
            (NameHider.getBlue1() * 255).toInt(),
            { r, g, b -> NameHider.setColor1(r / 255f, g / 255f, b / 255f) },
            35f,
            350f,
            480f,
            75f,
            mainPanel,
            500L
        )

        // Color 2 picker (for gradient)
        createColorPickerCard(
            "Color 2 (Gradient)",
            (NameHider.getRed2() * 255).toInt(),
            (NameHider.getGreen2() * 255).toInt(),
            (NameHider.getBlue2() * 255).toInt(),
            { r, g, b -> NameHider.setColor2(r / 255f, g / 255f, b / 255f) },
            35f,
            440f,
            480f,
            75f,
            mainPanel,
            600L
        )

        // Back button at bottom
        Button("Back", 0xFFFFFFFF.toInt(), fontSize = 15f)
            .setSizing(140f, Size.Pixels, 42f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -25f)
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
    }

    private fun createToggleCard(
        label: String,
        accentColor: Int,
        getEnabled: () -> Boolean,
        toggleAction: () -> Unit,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        parent: Rectangle,
        animDelay: Long
    ) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f,
            hoverColor = 0xF0252525.toInt()
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(parent)
            .apply {
                dropShadow = true
                shadowBlur = 15f
                shadowSpread = 1f
                shadowColor = 0x40000000.toInt()
            }

        val enabled = getEnabled()
        val accentBar = Rectangle(
            backgroundColor = if (enabled) accentColor else 0xFF303030.toInt(),
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

        VText(label, 0xFFFFFFFF.toInt(), 20f, true)
            .setPositioning(20f, Pos.ParentPixels, 18f, Pos.ParentPixels)
            .childOf(card)

        val statusTextStr = if (enabled) "ON" else "OFF"
        val statusColor = if (enabled) accentColor else 0xFF606060.toInt()
        val statusText = VText(statusTextStr, statusColor, 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 43f, Pos.ParentPixels)
            .childOf(card)

        card.onClick { _, _, _ ->
            toggleAction()
            val newEnabled = getEnabled()
            statusText.textColor = if (newEnabled) accentColor else 0xFF606060.toInt()
            statusText.text = if (newEnabled) "ON" else "OFF"
            accentBar.backgroundColor = if (newEnabled) accentColor else 0xFF303030.toInt()
            true
        }

        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            mc.execute {
                card.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun createTextInputCard(
        label: String,
        initialText: String,
        onTextChange: (String) -> Unit,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        parent: Rectangle,
        animDelay: Long
    ) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(parent)
            .apply {
                dropShadow = true
                shadowBlur = 15f
                shadowSpread = 1f
                shadowColor = 0x40000000.toInt()
            }

        // Accent bar
        Rectangle(
            backgroundColor = 0xFFFF9944.toInt(),
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

        VText(label, 0xFFFFFFFF.toInt(), 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)

        TextInput(
            initialValue = initialText,
            placeholder = "Enter name...",
            fontSize = 14f,
            textColor = 0xFFFFFFFF.toInt(),
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 6f,
            borderThickness = 1f,
            padding = floatArrayOf(8f, 6f, 8f, 6f)
        )
            .setSizing(width - 40f, Size.Pixels, 30f, Size.Pixels)
            .setPositioning(20f, Pos.ParentPixels, 37f, Pos.ParentPixels)
            .onValueChange { newValue ->
                val textValue = (newValue as? String) ?: initialText
                onTextChange(textValue)
            }
            .childOf(card)

        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            mc.execute {
                card.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun createColorPickerCard(
        label: String,
        initialR: Int,
        initialG: Int,
        initialB: Int,
        onColorChange: (Int, Int, Int) -> Unit,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        parent: Rectangle,
        animDelay: Long
    ) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(parent)
            .apply {
                dropShadow = true
                shadowBlur = 15f
                shadowSpread = 1f
                shadowColor = 0x40000000.toInt()
            }

        // Accent bar
        Rectangle(
            backgroundColor = 0xFFFF9944.toInt(),
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

        VText(label, 0xFFFFFFFF.toInt(), 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)

        ColorPicker(
            initialColor = java.awt.Color(initialR, initialG, initialB),
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 6f,
            borderThickness = 1f,
            padding = floatArrayOf(4f, 4f, 4f, 4f)
        )
            .setSizing(width - 40f, Size.Pixels, 35f, Size.Pixels)
            .setPositioning(20f, Pos.ParentPixels, 32f, Pos.ParentPixels)
            .onValueChange { newValue ->
                val color = (newValue as? java.awt.Color) ?: java.awt.Color(initialR, initialG, initialB)
                onColorChange(color.red, color.green, color.blue)
            }
            .childOf(card)

        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            mc.execute {
                card.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun closeWithAnimation() {
        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()

        mc.setScreen(parentScreen)
    }

    override fun keyPressed(input: KeyInput?): Boolean {
        if (input?.key() == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
            return true  // Consume the event to prevent pause menu
        }
        return super.keyPressed(input)
    }

    //? if is1_21_11 {
    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int): Boolean {
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
            return true
        } else {
            return super.onKeyType(typedChar, keyCode, scanCode)
        }
    }
    //?} else {
    /*override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int) {
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
        } else {
            super.onKeyType(typedChar, keyCode, scanCode)
        }
    }
    *///?}

    override fun shouldCloseOnEsc(): Boolean = false
}
