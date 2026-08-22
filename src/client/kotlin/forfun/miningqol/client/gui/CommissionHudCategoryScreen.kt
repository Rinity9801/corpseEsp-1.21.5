package forfun.miningqol.client.gui

import forfun.miningqol.client.CommissionHUD
import forfun.miningqol.client.MiningqolClient
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.KeyInput
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.animations.EasingType
import xyz.meowing.vexel.animations.fadeIn
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.Slider

class CommissionHudCategoryScreen(private val parentScreen: Screen) : VexelScreen("Commission HUD Settings") {
    private lateinit var overlay: Rectangle
    private lateinit var mainPanel: Rectangle

    override fun afterInitialization() {
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

        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(600f, Size.Pixels, 560f, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        mainPanel.xPositionConstraint = Pos.ScreenPixels
        mainPanel.yPositionConstraint = Pos.ScreenPixels
        mainPanel.xConstraint = (mainPanel.screenWidth - 600f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - 560f) / 2f
        mainPanel.fadeIn(500, EasingType.EASE_OUT)

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

        Text("Commission HUD", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        Text("Layout, background and positioning", 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        val startY = 115f
        val cardHeight = 72f
        val cardSpacing = 14f
        val cardWidth = 530f
        val cardX = (mainPanel.width - cardWidth) / 2f

        createToggleCard(
            "Commission HUD",
            "Enable or disable the overlay entirely",
            0xFF60A5FA.toInt(),
            { CommissionHUD.isEnabled() },
            { CommissionHUD.setEnabled(!CommissionHUD.isEnabled()) },
            cardX, startY, cardWidth, cardHeight, mainPanel, 200L
        )

        createToggleCard(
            "Background",
            "Show the frosted panel behind the text",
            0xFF4ADE80.toInt(),
            { CommissionHUD.isBackgroundEnabled() },
            { CommissionHUD.setBackgroundEnabled(!CommissionHUD.isBackgroundEnabled()) },
            cardX, startY + (cardHeight + cardSpacing), cardWidth, cardHeight, mainPanel, 300L
        )

        createLayoutCard(
            cardX,
            startY + (cardHeight + cardSpacing) * 2,
            cardWidth,
            cardHeight,
            mainPanel,
            400L
        )

        createScaleCard(
            cardX,
            startY + (cardHeight + cardSpacing) * 3,
            cardWidth,
            86f,
            mainPanel,
            500L
        )

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
                saveAndClose()
                true
            }
            .childOf(mainPanel)
            .fadeIn(1000, EasingType.EASE_OUT)
    }

    private fun createToggleCard(
        label: String,
        description: String,
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

        val accentBar = Rectangle(
            backgroundColor = if (getEnabled()) accentColor else 0xFF303030.toInt(),
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

        Text(label, 0xFFFFFFFF.toInt(), 18f, true)
            .setPositioning(20f, Pos.ParentPixels, 14f, Pos.ParentPixels)
            .childOf(card)

        Text(description, 0xFF888888.toInt(), 12f, false)
            .setPositioning(20f, Pos.ParentPixels, 39f, Pos.ParentPixels)
            .childOf(card)

        val statusText = Text(
            if (getEnabled()) "ON" else "OFF",
            if (getEnabled()) accentColor else 0xFF606060.toInt(),
            14f,
            true
        )
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-15f, 0f)
            .childOf(card)

        card.onClick { _, _, _ ->
            toggleAction()
            val enabled = getEnabled()
            statusText.text = if (enabled) "ON" else "OFF"
            statusText.textColor = if (enabled) accentColor else 0xFF606060.toInt()
            accentBar.backgroundColor = if (enabled) accentColor else 0xFF303030.toInt()
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

    private fun createLayoutCard(
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

        val accentBar = Rectangle(
            backgroundColor = 0xFFF59E0B.toInt(),
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

        Text("Layout", 0xFFFFFFFF.toInt(), 18f, true)
            .setPositioning(20f, Pos.ParentPixels, 14f, Pos.ParentPixels)
            .childOf(card)

        Text("Choose between the current grid and a single vertical column", 0xFF888888.toInt(), 12f, false)
            .setPositioning(20f, Pos.ParentPixels, 39f, Pos.ParentPixels)
            .childOf(card)

        val layoutText = Text(
            if (CommissionHUD.getLayoutMode() == CommissionHUD.LayoutMode.GRID) "2x2" else "1x4",
            0xFFF59E0B.toInt(),
            14f,
            true
        )
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-15f, 0f)
            .childOf(card)

        card.onClick { _, _, _ ->
            val next = if (CommissionHUD.getLayoutMode() == CommissionHUD.LayoutMode.GRID) {
                CommissionHUD.LayoutMode.COLUMN
            } else {
                CommissionHUD.LayoutMode.GRID
            }
            CommissionHUD.setLayoutMode(next)
            layoutText.text = if (next == CommissionHUD.LayoutMode.GRID) "2x2" else "1x4"
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

    private fun createScaleCard(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        parent: Rectangle,
        animDelay: Long
    ) {
        val accentColor = 0xFF8B5CF6.toInt()
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

        Rectangle(
            backgroundColor = accentColor,
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

        Text("Commission HUD Scale", 0xFFFFFFFF.toInt(), 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)

        val valueText = Text(String.format("%.1fx", CommissionHUD.getScale()), accentColor, 14f, true)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-20f, 0f)
            .childOf(card)

        Slider(
            value = CommissionHUD.getScale(),
            minValue = 0.5f,
            maxValue = 2.0f,
            trackColor = 0xFF1A1A1A.toInt(),
            trackFillColor = accentColor,
            thumbColor = accentColor,
            trackHeight = 4f,
            thumbWidth = 16f,
            thumbHeight = 16f,
            thumbRadius = 8f,
            trackRadius = 2f
        )
            .setSizing(width - 40f, Size.Pixels, 25f, Size.Pixels)
            .setPositioning(20f, Pos.ParentPixels, 44f, Pos.ParentPixels)
            .onValueChange { newValue ->
                val value = (newValue as? Float) ?: CommissionHUD.getScale()
                CommissionHUD.setScale(value)
                valueText.text = String.format("%.1fx", value)
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

    private fun saveAndClose() {
        MiningqolClient.getConfig().loadFromGame()
        MiningqolClient.getConfig().save()
        close()
        MinecraftClient.getInstance().setScreen(parentScreen)
    }

    override fun keyPressed(input: KeyInput?): Boolean {
        if (input?.key() == KnitKeys.KEY_ESCAPE.code) {
            saveAndClose()
            return true
        }
        return super.keyPressed(input)
    }

    override fun shouldCloseOnEsc(): Boolean = false
}
