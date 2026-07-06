package forfun.miningqol.client.gui

import forfun.miningqol.client.CommClaimManager
import forfun.miningqol.client.MiningqolClient
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

class CommClaimCategoryScreen(private val parentScreen: Screen) : VexelScreen("Comm Claim Settings") {
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

        // Main panel
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(550f, Size.Pixels, 750f, Size.Pixels)
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
        mainPanel.yConstraint = (mainPanel.screenHeight - 750f) / 2f
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

        // Accent bar
        Rectangle(
            backgroundColor = 0xFFFFD700.toInt(),
            borderRadius = 16f
        )
            .setSizing(5f, Size.Pixels, 80f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
                borderRadiusBottomLeft = 0f
            }

        // Title
        Text("Comm Claim", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        Text("Auto-claim commissions with wardrobe swap", 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        val sliderWidth = 480f
        val sliderHeight = 75f
        val sliderX = (550f - sliderWidth) / 2f
        var yOffset = 100f

        // Description
        Text("Press G to start commission claim sequence", 0xFFAAAAAA.toInt(), 12f, false)
            .setPositioning(0f, Pos.ParentCenter, yOffset, Pos.ParentPixels)
            .childOf(mainPanel)
        yOffset += 25f

        // Auto-trigger toggle card
        val autoTriggerCard = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 8f,
            borderThickness = 1f,
            hoverColor = 0xF0252525.toInt()
        )
            .setSizing(220f, Size.Pixels, 36f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, yOffset, Pos.ParentPixels)
            .childOf(mainPanel)

        var autoTriggerEnabled = CommClaimManager.isAutoTrigger()
        val autoTriggerAccent = Rectangle(
            backgroundColor = if (autoTriggerEnabled) 0xFF4CAF50.toInt() else 0xFF424242.toInt(),
            borderRadius = 8f
        )
            .setSizing(4f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(autoTriggerCard)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        Text("Auto-Trigger:", 0xFFFFFFFF.toInt(), 13f, false)
            .setPositioning(14f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(autoTriggerCard)

        val autoTriggerStatus = Text(if (autoTriggerEnabled) "ON" else "OFF",
            if (autoTriggerEnabled) 0xFF4CAF50.toInt() else 0xFF888888.toInt(), 13f, true)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-14f, 0f)
            .childOf(autoTriggerCard)

        autoTriggerCard.onClick { _, _, _ ->
            autoTriggerEnabled = !autoTriggerEnabled
            CommClaimManager.setAutoTrigger(autoTriggerEnabled)
            autoTriggerStatus.text = if (autoTriggerEnabled) "ON" else "OFF"
            autoTriggerStatus.textColor = if (autoTriggerEnabled) 0xFF4CAF50.toInt() else 0xFF888888.toInt()
            autoTriggerAccent.backgroundColor = if (autoTriggerEnabled) 0xFF4CAF50.toInt() else 0xFF424242.toInt()
            true
        }
        yOffset += 42f

        // Wardrobe Swap toggle card
        val wardrobeSwapCard = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 8f,
            borderThickness = 1f,
            hoverColor = 0xF0252525.toInt()
        )
            .setSizing(220f, Size.Pixels, 36f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, yOffset, Pos.ParentPixels)
            .childOf(mainPanel)

        var wardrobeSwapEnabled = CommClaimManager.isWardrobeSwap()
        val wardrobeSwapAccent = Rectangle(
            backgroundColor = if (wardrobeSwapEnabled) 0xFFFFD700.toInt() else 0xFF424242.toInt(),
            borderRadius = 8f
        )
            .setSizing(4f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(wardrobeSwapCard)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        Text("Wardrobe Swap:", 0xFFFFFFFF.toInt(), 13f, false)
            .setPositioning(14f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(wardrobeSwapCard)

        val wardrobeSwapStatus = Text(if (wardrobeSwapEnabled) "ON" else "OFF",
            if (wardrobeSwapEnabled) 0xFFFFD700.toInt() else 0xFF888888.toInt(), 13f, true)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-14f, 0f)
            .childOf(wardrobeSwapCard)

        wardrobeSwapCard.onClick { _, _, _ ->
            wardrobeSwapEnabled = !wardrobeSwapEnabled
            CommClaimManager.setWardrobeSwap(wardrobeSwapEnabled)
            wardrobeSwapStatus.text = if (wardrobeSwapEnabled) "ON" else "OFF"
            wardrobeSwapStatus.textColor = if (wardrobeSwapEnabled) 0xFFFFD700.toInt() else 0xFF888888.toInt()
            wardrobeSwapAccent.backgroundColor = if (wardrobeSwapEnabled) 0xFFFFD700.toInt() else 0xFF424242.toInt()
            true
        }
        yOffset += 40f

        // Bat Person Armor Slot slider card
        createSliderCard(
            "Bat Person Armor Slot (Wardrobe)",
            1f, 9f,
            CommClaimManager.getBatPersonSlot().toFloat(),
            { value -> CommClaimManager.setBatPersonSlot(value.toInt()) },
            0xFFFFD700.toInt(),
            sliderX, yOffset, sliderWidth, sliderHeight,
            mainPanel, 300L
        )
        yOffset += sliderHeight + 10f

        // Divan Armor Slot slider card
        createSliderCard(
            "Divan Armor Slot (Wardrobe)",
            1f, 9f,
            CommClaimManager.getDivanSlot().toFloat(),
            { value -> CommClaimManager.setDivanSlot(value.toInt()) },
            0xFF4CAF50.toInt(),
            sliderX, yOffset, sliderWidth, sliderHeight,
            mainPanel, 400L
        )
        yOffset += sliderHeight + 10f

        // Refined Tool Slot slider card
        createSliderCard(
            "Refined Tool Slot (Hotbar)",
            1f, 9f,
            (CommClaimManager.getRefinedToolSlot() + 1).toFloat(),
            { value -> CommClaimManager.setRefinedToolSlot(value.toInt() - 1) },
            0xFF2196F3.toInt(),
            sliderX, yOffset, sliderWidth, sliderHeight,
            mainPanel, 500L
        )
        yOffset += sliderHeight + 10f

        // Action Delay slider card
        createSliderCard(
            "Action Delay (Ticks)",
            1f, 10f,
            CommClaimManager.getTickDelay().toFloat(),
            { value -> CommClaimManager.setTickDelay(value.toInt()) },
            0xFFE91E63.toInt(),
            sliderX, yOffset, sliderWidth, sliderHeight,
            mainPanel, 600L
        )
        yOffset += sliderHeight + 10f

        // GUI Wait Delay slider card
        createSliderCard(
            "GUI Wait Delay (Ticks)",
            1f, 10f,
            CommClaimManager.getGuiWaitDelay().toFloat(),
            { value -> CommClaimManager.setGuiWaitDelay(value.toInt()) },
            0xFF9C27B0.toInt(),
            sliderX, yOffset, sliderWidth, sliderHeight,
            mainPanel, 700L
        )
        yOffset += sliderHeight + 20f

        yOffset += 20f

        // Test button
        Button("Test Commission Claim", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(200f, Size.Pixels, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, yOffset, Pos.ParentPixels)
            .backgroundColor(0xFF4CAF50.toInt())
            .borderColor(0xFF45A049.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF45A049.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF388E3C.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                close()
                MinecraftClient.getInstance().send {
                    CommClaimManager.start()
                }
                true
            }
            .childOf(mainPanel)

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
                saveAndClose()
                true
            }
            .childOf(mainPanel)
            .fadeIn(1000, EasingType.EASE_OUT)
    }

    private fun createSliderCard(
        label: String,
        min: Float,
        max: Float,
        initialValue: Float,
        onValueChange: (Float) -> Unit,
        accentColor: Int,
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

        Text(label, 0xFFFFFFFF.toInt(), 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)

        val valueText = Text("${initialValue.toInt()}", accentColor, 14f, true)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-20f, 0f)
            .childOf(card)

        Slider(
            value = initialValue,
            minValue = min,
            maxValue = max,
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
            .setPositioning(20f, Pos.ParentPixels, 40f, Pos.ParentPixels)
            .onValueChange { newValue ->
                val floatValue = (newValue as? Float) ?: initialValue
                onValueChange(floatValue)
                valueText.text = "${floatValue.toInt()}"
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

    private fun createFloatSliderCard(
        label: String,
        min: Float,
        max: Float,
        initialValue: Float,
        onValueChange: (Float) -> Unit,
        accentColor: Int,
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

        Text(label, 0xFFFFFFFF.toInt(), 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)

        val valueText = Text(String.format("%.1fx", initialValue), accentColor, 14f, true)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-20f, 0f)
            .childOf(card)

        Slider(
            value = initialValue,
            minValue = min,
            maxValue = max,
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
            .setPositioning(20f, Pos.ParentPixels, 40f, Pos.ParentPixels)
            .onValueChange { newValue ->
                val floatValue = ((newValue as? Float) ?: initialValue)
                onValueChange(floatValue)
                valueText.text = String.format("%.1fx", floatValue)
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
