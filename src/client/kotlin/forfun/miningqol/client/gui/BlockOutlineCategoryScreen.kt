package forfun.miningqol.client.gui

import forfun.miningqol.client.BlockOutlineRenderer
import forfun.miningqol.client.MiningqolClient
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.KeyInput
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*

class BlockOutlineCategoryScreen(private val parentScreen: Screen) : VexelScreen("Block Outline Settings") {
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
            .setSizing(550f, Size.Pixels, 380f, Size.Pixels)
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
        mainPanel.yConstraint = (mainPanel.screenHeight - 380f) / 2f
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
        Text("Block Outline Settings", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        Text("Customize mining block highlights", 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Block outline toggle
        val toggles = listOf(
            Triple("Enable Block Outline", 0xFF9966FF.toInt()) { BlockOutlineRenderer.isEnabled() }
        )

        val toggleActions = listOf<() -> Unit>(
            { BlockOutlineRenderer.setEnabled(!BlockOutlineRenderer.isEnabled()) }
        )

        val startY = 120f
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

        // Note about advanced settings
        Text("Note: Use the old Java screen for color and mode settings", 0xFF666666.toInt(), 11f, false)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -75f)
            .childOf(mainPanel)
            .fadeIn(900, EasingType.EASE_OUT)

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
        // Create card first
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

        // Glowing accent bar on left
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

        // Title
        Text(label, 0xFFFFFFFF.toInt(), 20f, true)
            .setPositioning(20f, Pos.ParentPixels, 18f, Pos.ParentPixels)
            .childOf(card)

        // Status text
        val statusTextStr = if (enabled) "ON" else "OFF"
        val statusColor = if (enabled) accentColor else 0xFF606060.toInt()
        val statusText = Text(statusTextStr, statusColor, 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 43f, Pos.ParentPixels)
            .childOf(card)

        // Set up onClick handler now that all components exist
        card.onClick { _, _, _ ->
            toggleAction()
            // Update the status text color based on new state
            val newEnabled = getEnabled()
            statusText.textColor = if (newEnabled) accentColor else 0xFF606060.toInt()
            statusText.text = if (newEnabled) "ON" else "OFF"
            accentBar.backgroundColor = if (newEnabled) accentColor else 0xFF303030.toInt()
            true
        }

        // Fade in card with delay
        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            MinecraftClient.getInstance().execute {
                card.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun closeWithAnimation() {
        // Save config before closing
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
