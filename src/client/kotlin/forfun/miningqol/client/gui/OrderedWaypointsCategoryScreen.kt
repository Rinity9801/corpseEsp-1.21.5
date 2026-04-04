package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.waypoints.OrderedWaypointManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.KeyInput
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.Slider
import xyz.meowing.vexel.elements.ColorPicker
import xyz.meowing.vexel.elements.TextInput
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*
import java.awt.Color

class OrderedWaypointsCategoryScreen(private val parentScreen: Screen) : VexelScreen("Ordered Waypoints Settings") {
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

        // Main panel - wider for 2 columns
        val panelWidth = 900f
        val panelHeight = 650f
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(panelWidth, Size.Pixels, panelHeight, Size.Pixels)
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
        mainPanel.xConstraint = (mainPanel.screenWidth - panelWidth) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - panelHeight) / 2f
        mainPanel.fadeIn(500, EasingType.EASE_OUT)

        // Title bar background
        Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 70f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusBottomLeft = 0f
                borderRadiusBottomRight = 0f
            }
            .fadeIn(600, EasingType.EASE_OUT)

        // Title
        Text("Ordered Waypoints", 0xFFFFFFFF.toInt(), 26f, true)
            .setPositioning(0f, Pos.ParentCenter, 15f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        Text("Use /mqo commands to manage routes", 0xFF888888.toInt(), 12f, false)
            .setPositioning(0f, Pos.ParentCenter, 42f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Two-column layout
        val cardWidth = 400f
        val toggleHeight = 45f
        val sliderHeight = 60f
        val colorHeight = 60f
        val spacing = 8f
        val leftX = 25f
        val rightX = panelWidth / 2 + 12f
        val startY = 85f

        // ===== LEFT COLUMN =====
        var leftY = startY

        // Main toggle
        createToggleCard(
            "Enable Ordered Waypoints",
            0xFF55FF55.toInt(),
            { OrderedWaypointManager.isEnabledRaw() },
            { OrderedWaypointManager.setEnabled(!OrderedWaypointManager.isEnabledRaw()) },
            leftX, leftY, cardWidth, toggleHeight, 200L
        )
        leftY += toggleHeight + spacing

        // Current waypoint color
        createColorPickerCard(
            "Current Waypoint Color",
            OrderedWaypointManager.getCurrentWaypointColor(),
            OrderedWaypointManager.getCurrentWaypointAlpha(),
            { r, g, b, a ->
                OrderedWaypointManager.setCurrentWaypointColor(r / 255f, g / 255f, b / 255f)
                OrderedWaypointManager.setCurrentWaypointAlpha(a / 255f)
            },
            leftX, leftY, cardWidth, colorHeight, 250L
        )
        leftY += colorHeight + spacing

        // Next waypoint color
        createColorPickerCard(
            "Next Waypoint Color",
            OrderedWaypointManager.getNextWaypointColor(),
            OrderedWaypointManager.getNextWaypointAlpha(),
            { r, g, b, a ->
                OrderedWaypointManager.setNextWaypointColor(r / 255f, g / 255f, b / 255f)
                OrderedWaypointManager.setNextWaypointAlpha(a / 255f)
            },
            leftX, leftY, cardWidth, colorHeight, 300L
        )
        leftY += colorHeight + spacing

        // Previous waypoint color
        createColorPickerCard(
            "Previous Waypoint Color",
            OrderedWaypointManager.getPreviousWaypointColor(),
            OrderedWaypointManager.getPreviousWaypointAlpha(),
            { r, g, b, a ->
                OrderedWaypointManager.setPreviousWaypointColor(r / 255f, g / 255f, b / 255f)
                OrderedWaypointManager.setPreviousWaypointAlpha(a / 255f)
            },
            leftX, leftY, cardWidth, colorHeight, 350L
        )
        leftY += colorHeight + spacing

        // Next count slider
        createSliderCard(
            "Next Waypoints to Show",
            1f, 10f,
            OrderedWaypointManager.getNextCount().toFloat(),
            { value -> OrderedWaypointManager.setNextCount(value.toInt()) },
            "",
            leftX, leftY, cardWidth, sliderHeight, 400L
        )
        leftY += sliderHeight + spacing

        // Waypoint reach radius slider
        createSliderCard(
            "Reach Radius (blocks)",
            1f, 5f,
            OrderedWaypointManager.getWaypointRange(),
            { value -> OrderedWaypointManager.setWaypointRange(value) },
            "",
            leftX, leftY, cardWidth, sliderHeight, 450L,
            step = 0.1f
        )
        leftY += sliderHeight + spacing

        // Trace line toggle
        createToggleCard(
            "Enable Trace Line",
            0xFF55FFFF.toInt(),
            { OrderedWaypointManager.isTraceLineEnabled() },
            { OrderedWaypointManager.setTraceLineEnabled(!OrderedWaypointManager.isTraceLineEnabled()) },
            leftX, leftY, cardWidth, toggleHeight, 500L
        )

        // ===== RIGHT COLUMN =====
        var rightY = startY

        // Show distance toggle
        createToggleCard(
            "Show Distance",
            0xFF55FF55.toInt(),
            { OrderedWaypointManager.isShowDistance() },
            { OrderedWaypointManager.setShowDistance(!OrderedWaypointManager.isShowDistance()) },
            rightX, rightY, cardWidth, toggleHeight, 200L
        )
        rightY += toggleHeight + spacing

        // Show name toggle
        createToggleCard(
            "Show Waypoint Number",
            0xFF55FF55.toInt(),
            { OrderedWaypointManager.isShowName() },
            { OrderedWaypointManager.setShowName(!OrderedWaypointManager.isShowName()) },
            rightX, rightY, cardWidth, toggleHeight, 250L
        )
        rightY += toggleHeight + spacing

        // Section header for Lobby Check
        Text("Lobby Check", 0xFFFF5555.toInt(), 14f, true)
            .setPositioning(rightX + 10f, Pos.ParentPixels, rightY + 5f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(300, EasingType.EASE_OUT)
        rightY += 25f

        // Lobby check toggle
        createToggleCard(
            "Enable Lobby Check",
            0xFFFF5555.toInt(),
            { OrderedWaypointManager.isLobbyCheckEnabled() },
            { OrderedWaypointManager.setLobbyCheckEnabled(!OrderedWaypointManager.isLobbyCheckEnabled()) },
            rightX, rightY, cardWidth, toggleHeight, 300L
        )
        rightY += toggleHeight + spacing

        // Lobby check block input
        createTextInputCard(
            "Expected Block",
            OrderedWaypointManager.getLobbyCheckBlock(),
            { value -> OrderedWaypointManager.setLobbyCheckBlock(value) },
            rightX, rightY, cardWidth, sliderHeight, 350L
        )
        rightY += sliderHeight + spacing

        // Lobby check interval slider (renamed for clarity)
        createSliderCard(
            "Waypoints to Scan",
            1f, 50f,
            OrderedWaypointManager.getLobbyCheckInterval().toFloat(),
            { value -> OrderedWaypointManager.setLobbyCheckInterval(value.toInt()) },
            "",
            rightX, rightY, cardWidth, sliderHeight, 400L
        )
        rightY += sliderHeight + spacing

        // Lobby check radius slider
        createSliderCard(
            "Scan Radius (blocks)",
            1f, 5f,
            OrderedWaypointManager.getLobbyCheckRadius().toFloat(),
            { value -> OrderedWaypointManager.setLobbyCheckRadius(value.toInt()) },
            "",
            rightX, rightY, cardWidth, sliderHeight, 450L
        )
        rightY += sliderHeight + spacing

        // Block outline toggle
        createToggleCard(
            "Outline Blocks at Waypoint",
            0xFFFFFFFF.toInt(),
            { OrderedWaypointManager.isBlockOutlineAroundWaypoint() },
            { OrderedWaypointManager.setBlockOutlineAroundWaypoint(!OrderedWaypointManager.isBlockOutlineAroundWaypoint()) },
            rightX, rightY, cardWidth, toggleHeight, 500L
        )

        // Back button at bottom
        Button("Back", 0xFFFFFFFF.toInt(), fontSize = 15f)
            .setSizing(140f, Size.Pixels, 42f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -20f)
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
            .fadeIn(800, EasingType.EASE_OUT)
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
        animDelay: Long
    ) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 10f,
            borderThickness = 1f,
            hoverColor = 0xF0252525.toInt()
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                dropShadow = true
                shadowBlur = 10f
                shadowSpread = 1f
                shadowColor = 0x30000000.toInt()
            }

        val enabled = getEnabled()
        val accentBar = Rectangle(
            backgroundColor = if (enabled) accentColor else 0xFF303030.toInt(),
            borderRadius = 10f
        )
            .setSizing(4f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(card)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        Text(label, 0xFFFFFFFF.toInt(), 15f, true)
            .setPositioning(18f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(card)

        val statusTextStr = if (enabled) "ON" else "OFF"
        val statusColor = if (enabled) accentColor else 0xFF606060.toInt()
        val statusText = Text(statusTextStr, statusColor, 14f, true)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-15f, 0f)
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
                card.fadeIn(300, EasingType.EASE_OUT)
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
        animDelay: Long,
        step: Float = 1f
    ) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 10f,
            borderThickness = 1f
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                dropShadow = true
                shadowBlur = 10f
                shadowSpread = 1f
                shadowColor = 0x30000000.toInt()
            }

        Rectangle(
            backgroundColor = 0xFF55FF55.toInt(),
            borderRadius = 10f
        )
            .setSizing(4f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(card)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        Text(label, 0xFFFFFFFF.toInt(), 14f, true)
            .setPositioning(18f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .childOf(card)

        val displayValue = if (step < 1f) String.format("%.1f", initialValue) else initialValue.toInt().toString()
        val valueText = Text("$displayValue$suffix", 0xFF55FF55.toInt(), 13f, true)
            .setPositioning(0f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-15f, 0f)
            .childOf(card)

        Slider(
            value = initialValue,
            minValue = min,
            maxValue = max,
            trackColor = 0xFF1A1A1A.toInt(),
            trackFillColor = 0xFF55FF55.toInt(),
            thumbColor = 0xFF55FF55.toInt(),
            trackHeight = 4f,
            thumbWidth = 14f,
            thumbHeight = 14f,
            thumbRadius = 7f,
            trackRadius = 2f
        )
            .setSizing(width - 36f, Size.Pixels, 22f, Size.Pixels)
            .setPositioning(18f, Pos.ParentPixels, 35f, Pos.ParentPixels)
            .onValueChange { newValue ->
                val floatValue = (newValue as? Float) ?: initialValue
                onValueChange(floatValue)
                val newDisplayValue = if (step < 1f) String.format("%.1f", floatValue) else floatValue.toInt().toString()
                valueText.text = "$newDisplayValue$suffix"
            }
            .childOf(card)

        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            mc.execute {
                card.fadeIn(300, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun createColorPickerCard(
        label: String,
        initialColor: FloatArray,
        initialAlpha: Float,
        onColorChange: (Int, Int, Int, Int) -> Unit,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        animDelay: Long
    ) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 10f,
            borderThickness = 1f
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                dropShadow = true
                shadowBlur = 10f
                shadowSpread = 1f
                shadowColor = 0x30000000.toInt()
            }

        Rectangle(
            backgroundColor = 0xFFFFAA00.toInt(),
            borderRadius = 10f
        )
            .setSizing(4f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(card)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        Text(label, 0xFFFFFFFF.toInt(), 14f, true)
            .setPositioning(18f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .childOf(card)

        val r = (initialColor[0] * 255).toInt()
        val g = (initialColor[1] * 255).toInt()
        val b = (initialColor[2] * 255).toInt()
        val a = (initialAlpha * 255).toInt()

        // Hex display text (shows current hex value)
        val hexText = Text(String.format("#%02X%02X%02X", r, g, b), 0xFF888888.toInt(), 11f, false)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-15f, 0f)
            .childOf(card)

        val colorPicker = ColorPicker(
            initialColor = Color(r, g, b, a),
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 6f,
            borderThickness = 1f,
            padding = floatArrayOf(4f, 4f, 4f, 4f)
        )
            .setSizing(width - 36f, Size.Pixels, 28f, Size.Pixels)
            .setPositioning(18f, Pos.ParentPixels, 30f, Pos.ParentPixels)
            .childOf(card)

        // Update hex display and call callback when color picker changes
        colorPicker.onValueChange { newValue ->
            val color = (newValue as? Color) ?: Color(r, g, b, a)
            onColorChange(color.red, color.green, color.blue, color.alpha)
            hexText.text = String.format("#%02X%02X%02X", color.red, color.green, color.blue)
        }

        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            mc.execute {
                card.fadeIn(300, EasingType.EASE_OUT)
            }
        }.start()
    }

    private fun createTextInputCard(
        label: String,
        initialValue: String,
        onValueChange: (String) -> Unit,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        animDelay: Long
    ) {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 10f,
            borderThickness = 1f
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                dropShadow = true
                shadowBlur = 10f
                shadowSpread = 1f
                shadowColor = 0x30000000.toInt()
            }

        Rectangle(
            backgroundColor = 0xFFFF5555.toInt(),
            borderRadius = 10f
        )
            .setSizing(4f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(card)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        Text(label, 0xFFFFFFFF.toInt(), 14f, true)
            .setPositioning(18f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .childOf(card)

        val textInput = TextInput(
            initialValue,
            "e.g. minecraft:coal_ore",
            fontSize = 12f,
            textColor = 0xFFFFFFFF.toInt()
        )
            .setSizing(width - 36f, Size.Pixels, 28f, Size.Pixels)
            .setPositioning(18f, Pos.ParentPixels, 32f, Pos.ParentPixels)
            .backgroundColor(0xFF252525.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .childOf(card)

        textInput.onValueChange { newValue ->
            onValueChange(newValue as String)
        }

        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            mc.execute {
                card.fadeIn(300, EasingType.EASE_OUT)
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
            return true
        }
        return super.keyPressed(input)
    }

    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int): Boolean {
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
            return true
        } else {
            return super.onKeyType(typedChar, keyCode, scanCode)
        }
    }

    override fun shouldCloseOnEsc(): Boolean = false
}
