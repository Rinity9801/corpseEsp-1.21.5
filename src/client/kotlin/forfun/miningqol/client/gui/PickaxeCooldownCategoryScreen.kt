package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.PickaxeCooldownHUD
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.KeyInput
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.Slider
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*

class PickaxeCooldownCategoryScreen(private val parentScreen: Screen) : VexelScreen("Pickaxe Cooldown Settings") {
    private lateinit var overlay: Rectangle
    private lateinit var mainPanel: Rectangle

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
            .setSizing(550f, Size.Pixels, 500f, Size.Pixels)
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
        mainPanel.yConstraint = (mainPanel.screenHeight - 500f) / 2f
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
        Text("Pickaxe Cooldown", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        Text("Display pickaxe ability cooldown from tab list", 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Toggles
        val toggles = listOf(
            Triple("Enable Pickaxe Cooldown Display", 0xFF4A90E2.toInt()) { PickaxeCooldownHUD.isEnabled() },
            Triple("Enable \"Almost Ready\" Title", 0xFF4A90E2.toInt()) { PickaxeCooldownHUD.isTitleEnabled() }
        )

        val toggleActions = listOf<() -> Unit>(
            { PickaxeCooldownHUD.setEnabled(!PickaxeCooldownHUD.isEnabled()) },
            { PickaxeCooldownHUD.setTitleEnabled(!PickaxeCooldownHUD.isTitleEnabled()) }
        )

        val startY = 110f
        val toggleHeight = 65f
        val toggleSpacing = 15f
        val toggleWidth = 480f

        toggles.forEachIndexed { index, (label, accentColor, getEnabled) ->
            val delay = 200L + (index * 100L)
            createToggleCard(
                label,
                accentColor,
                getEnabled,
                toggleActions[index],
                (mainPanel.width - toggleWidth) / 2f,
                startY + (index * (toggleHeight + toggleSpacing)),
                toggleWidth,
                toggleHeight,
                mainPanel,
                delay
            )
        }

        // Title threshold slider
        createSliderCard(
            "Title Threshold",
            0f,
            15f,
            PickaxeCooldownHUD.getTitleThreshold().toFloat(),
            { value -> PickaxeCooldownHUD.setTitleThreshold(value.toInt()) },
            "s",
            (mainPanel.width - toggleWidth) / 2f,
            275f,
            toggleWidth,
            75f,
            mainPanel,
            400L
        )

        // Position button
        Button("Set HUD Position", 0xFFFFFFFF.toInt(), fontSize = 16f)
            .setSizing(480f, Size.Pixels, 50f, Size.Pixels)
            .setPositioning((mainPanel.width - 480f) / 2f, Pos.ParentPixels, 370f, Pos.ParentPixels)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF4A90E2.toInt())
            .borderRadius(12f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                MinecraftClient.getInstance().setScreen(PickaxeCooldownPositionScreen(this@PickaxeCooldownCategoryScreen))
                true
            }
            .childOf(mainPanel)
            .apply {
                visible = false
                Thread {
                    Thread.sleep(400L)
                    MinecraftClient.getInstance().execute {
                        fadeIn(400, EasingType.EASE_OUT)
                    }
                }.start()
            }

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

        Text(label, 0xFFFFFFFF.toInt(), 20f, true)
            .setPositioning(20f, Pos.ParentPixels, 18f, Pos.ParentPixels)
            .childOf(card)

        val statusTextStr = if (enabled) "ON" else "OFF"
        val statusColor = if (enabled) accentColor else 0xFF606060.toInt()
        val statusText = Text(statusTextStr, statusColor, 16f, true)
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
            MinecraftClient.getInstance().execute {
                card.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun createSliderCard(
        label: String,
        min: Float,
        max: Float,
        initialValue: Float,
        onValueChange: (Float) -> Unit,
        suffix: String,
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
            backgroundColor = 0xFF4A90E2.toInt(),
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

        Text(label, 0xFFFFFFFF.toInt(), 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)

        val valueText = Text("${initialValue.toInt()}$suffix", 0xFF4A90E2.toInt(), 14f, true)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-20f, 0f)
            .childOf(card)

        Slider(
            value = initialValue,
            minValue = min,
            maxValue = max,
            trackColor = 0xFF1A1A1A.toInt(),
            trackFillColor = 0xFF4A90E2.toInt(),
            thumbColor = 0xFF4A90E2.toInt(),
            trackHeight = 4f,
            thumbWidth = 16f,
            thumbHeight = 16f,
            thumbRadius = 8f,
            trackRadius = 2f
        )
            .setSizing(width - 40f, Size.Pixels, 25f, Size.Pixels)
            .setPositioning(20f, Pos.ParentPixels, 40f, Pos.ParentPixels)
            .onValueChange { newValue ->
                val floatValue = (newValue as? Float) ?: initialValue
                onValueChange(floatValue)
                valueText.text = "${floatValue.toInt()}$suffix"
            }
            .childOf(card)

        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            MinecraftClient.getInstance().execute {
                card.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun closeWithAnimation() {
        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()

        MinecraftClient.getInstance().setScreen(parentScreen)
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
