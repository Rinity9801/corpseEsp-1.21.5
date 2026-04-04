package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.profit.BazaarPriceManager
import forfun.miningqol.client.profit.BlockTracker
import forfun.miningqol.client.profit.GemstoneTracker
import forfun.miningqol.client.profit.ProfitTrackerHUD
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

class MiningProfitCategoryScreen(private val parentScreen: Screen) : VexelScreen("Mining Profit Settings") {
    private lateinit var overlay: Rectangle
    private lateinit var mainPanel: Rectangle
    private lateinit var contentPanel: Rectangle
    private var isBlockMode = ProfitTrackerHUD.isBlockMode()
    private var dropdownOpen = false
    private var dropdownPanel: Rectangle? = null

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
            .setSizing(550f, Size.Pixels, 630f, Size.Pixels)
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
        mainPanel.yConstraint = (mainPanel.screenHeight - 630f) / 2f
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
        Text("Mining Profit", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        val subtitle = if (isBlockMode) "Track your block mining profits" else "Track your gemstone profits"
        Text(subtitle, 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Content panel for mode-specific options
        contentPanel = Rectangle(
            backgroundColor = 0x00000000.toInt(),
            borderColor = 0x00000000
        )
            .setSizing(100f, Size.ParentPerc, 520f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 90f, Pos.ParentPixels)
            .childOf(mainPanel)

        buildContent()

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

    private fun buildContent() {
        val toggleWidth = 480f
        val toggleHeight = 65f
        val toggleSpacing = 10f
        var yOffset = 10f

        // Enable toggle
        createToggleCard(
            "Enable Profit Tracker",
            0xFF44FF44.toInt(),
            { ProfitTrackerHUD.isEnabled() },
            { ProfitTrackerHUD.setEnabled(!ProfitTrackerHUD.isEnabled()) },
            (mainPanel.width - toggleWidth) / 2f,
            yOffset,
            toggleWidth,
            toggleHeight,
            contentPanel,
            200L
        )
        yOffset += toggleHeight + toggleSpacing

        // Mode selector
        createModeSelector(
            (mainPanel.width - toggleWidth) / 2f,
            yOffset,
            toggleWidth,
            toggleHeight,
            contentPanel,
            300L
        )
        yOffset += toggleHeight + toggleSpacing

        if (isBlockMode) {
            buildBlockModeOptions(yOffset, toggleWidth, toggleHeight, toggleSpacing)
        } else {
            buildGemstoneModeOptions(yOffset, toggleWidth, toggleHeight, toggleSpacing)
        }
    }

    private fun buildGemstoneModeOptions(startY: Float, toggleWidth: Float, toggleHeight: Float, spacing: Float) {
        var yOffset = startY

        // Include rough gemstones
        createToggleCard(
            "Include Rough Gemstones",
            0xFF44FF44.toInt(),
            { GemstoneTracker.isIncludingRough() },
            { GemstoneTracker.setIncludeRough(!GemstoneTracker.isIncludingRough()) },
            (mainPanel.width - toggleWidth) / 2f,
            yOffset,
            toggleWidth,
            toggleHeight,
            contentPanel,
            400L
        )
        yOffset += toggleHeight + spacing

        // Use NPC prices
        createToggleCard(
            "Use NPC Prices Instead of Bazaar",
            0xFF44FF44.toInt(),
            { BazaarPriceManager.isUsingNPCPrices() },
            { BazaarPriceManager.setUseNPCPrices(!BazaarPriceManager.isUsingNPCPrices()) },
            (mainPanel.width - toggleWidth) / 2f,
            yOffset,
            toggleWidth,
            toggleHeight,
            contentPanel,
            450L
        )
        yOffset += toggleHeight + spacing

        // Gem tier selector
        createSelectorCard(
            "Gem Tier",
            { GemstoneTracker.getGemTierName() },
            {
                val currentTier = GemstoneTracker.getGemTier()
                val nextTier = if (currentTier >= 3) 1 else currentTier + 1
                GemstoneTracker.setGemTier(nextTier)
            },
            (mainPanel.width - toggleWidth) / 2f,
            yOffset,
            toggleWidth,
            toggleHeight,
            contentPanel,
            500L
        )
        yOffset += toggleHeight + spacing

        // Pristine chance slider
        createSliderCard(
            "Pristine Chance",
            0f,
            100f,
            GemstoneTracker.getPristineChance().toFloat(),
            { value -> GemstoneTracker.setPristineChance(value.toInt()) },
            "%",
            (mainPanel.width - toggleWidth) / 2f,
            yOffset,
            toggleWidth,
            75f,
            contentPanel,
            550L
        )
        yOffset += 75f + spacing

        // Position button
        Button("Set HUD Position", 0xFFFFFFFF.toInt(), fontSize = 16f)
            .setSizing(toggleWidth, Size.Pixels, 50f, Size.Pixels)
            .setPositioning((mainPanel.width - toggleWidth) / 2f, Pos.ParentPixels, yOffset, Pos.ParentPixels)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF44FF44.toInt())
            .borderRadius(12f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                MinecraftClient.getInstance().setScreen(ProfitPositionScreen(this@MiningProfitCategoryScreen))
                true
            }
            .childOf(contentPanel)
            .fadeIn(600, EasingType.EASE_OUT)
    }

    private fun buildBlockModeOptions(startY: Float, toggleWidth: Float, toggleHeight: Float, spacing: Float) {
        var yOffset = startY

        // Material dropdown selector
        createMaterialDropdown(
            (mainPanel.width - toggleWidth) / 2f,
            yOffset,
            toggleWidth,
            toggleHeight,
            contentPanel,
            400L
        )
        yOffset += toggleHeight + spacing

        // Info text
        Text("Tracks [Sacks] messages for the selected material.", 0xFF888888.toInt(), 12f, false)
            .setPositioning(0f, Pos.ParentCenter, yOffset + 10f, Pos.ParentPixels)
            .childOf(contentPanel)

        Text("Calculates value using enchanted prices (160 raw = 1 enchanted).", 0xFF888888.toInt(), 12f, false)
            .setPositioning(0f, Pos.ParentCenter, yOffset + 26f, Pos.ParentPixels)
            .childOf(contentPanel)

        yOffset += 60f

        // Reset button
        Button("Reset Session", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(200f, Size.Pixels, 45f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, yOffset, Pos.ParentPixels)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFFFF6B6B.toInt())
            .borderRadius(10f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                BlockTracker.reset()
                true
            }
            .childOf(contentPanel)
            .fadeIn(450, EasingType.EASE_OUT)

        yOffset += 60f

        // Position button
        Button("Set HUD Position", 0xFFFFFFFF.toInt(), fontSize = 16f)
            .setSizing(toggleWidth, Size.Pixels, 50f, Size.Pixels)
            .setPositioning((mainPanel.width - toggleWidth) / 2f, Pos.ParentPixels, yOffset, Pos.ParentPixels)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF44FF44.toInt())
            .borderRadius(12f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                MinecraftClient.getInstance().setScreen(ProfitPositionScreen(this@MiningProfitCategoryScreen))
                true
            }
            .childOf(contentPanel)
            .fadeIn(500, EasingType.EASE_OUT)
    }

    private fun createMaterialDropdown(x: Float, y: Float, width: Float, height: Float, parent: Rectangle, animDelay: Long) {
        // Display mode options (SEPARATE shows each material, COMBINED shows totals)
        val displayModes = listOf("SEPARATE", "COMBINED")
        val displayModeNames = mapOf(
            "SEPARATE" to "Separate",
            "COMBINED" to "Combined"
        )

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

        // Accent bar
        Rectangle(
            backgroundColor = 0xFFFFAA00.toInt(),
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

        Text("Display Mode", 0xFFFFFFFF.toInt(), 20f, true)
            .setPositioning(20f, Pos.ParentPixels, 18f, Pos.ParentPixels)
            .childOf(card)

        val currentMode = BlockTracker.getDisplayMode().name
        val valueText = Text(displayModeNames[currentMode] ?: currentMode, 0xFFFFAA00.toInt(), 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 43f, Pos.ParentPixels)
            .childOf(card)

        Text("▼", 0xFF888888.toInt(), 14f, false)
            .setPositioning(0f, Pos.ParentPixels, 25f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-20f, 0f)
            .childOf(card)

        // Dropdown panel (hidden initially) - added to mainPanel for proper z-order
        val dropdownHeight = displayModes.size * 35f + 10f
        val contentPanelOffset = 90f // contentPanel starts at y=90 in mainPanel
        val dropdown = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF3A3A3A.toInt(),
            borderRadius = 8f,
            borderThickness = 1f
        )
            .setSizing(width - 20f, Size.Pixels, dropdownHeight, Size.Pixels)
            .setPositioning(x + 10f, Pos.ParentPixels, contentPanelOffset + y + height + 5f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                dropShadow = true
                shadowBlur = 20f
                shadowSpread = 2f
                shadowColor = 0x80000000.toInt()
                visible = false
            }
        dropdownPanel = dropdown

        // Add display mode options
        var optionY = 5f
        for (mode in displayModes) {
            val displayName = displayModeNames[mode] ?: mode
            val isSelected = mode == BlockTracker.getDisplayMode().name
            val bgColor = if (isSelected) 0xFF3A3A3A.toInt() else 0x00000000.toInt()

            val option = Rectangle(
                backgroundColor = bgColor,
                borderColor = 0x00000000,
                borderRadius = 6f,
                hoverColor = 0xFF2A2A2A.toInt()
            )
                .setSizing(width - 30f, Size.Pixels, 30f, Size.Pixels)
                .setPositioning(5f, Pos.ParentPixels, optionY, Pos.ParentPixels)
                .childOf(dropdown)

            Text(displayName, if (isSelected) 0xFFFFAA00.toInt() else 0xFFFFFFFF.toInt(), 14f, false)
                .setPositioning(10f, Pos.ParentPixels, 8f, Pos.ParentPixels)
                .childOf(option)

            option.onClick { _, _, _ ->
                BlockTracker.setDisplayMode(BlockTracker.DisplayMode.valueOf(mode))
                dropdown.visible = false
                dropdownOpen = false
                valueText.text = displayModeNames[mode] ?: mode
                true
            }

            optionY += 35f
        }

        card.onClick { _, _, _ ->
            dropdownOpen = !dropdownOpen
            dropdown.visible = dropdownOpen
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

    private fun createModeSelector(x: Float, y: Float, width: Float, height: Float, parent: Rectangle, animDelay: Long) {
        // Card is just a visual background - ignores mouse events
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(parent)
            .apply {
                dropShadow = true
                shadowBlur = 15f
                shadowSpread = 1f
                shadowColor = 0x40000000.toInt()
            }

        // Accent bar
        Rectangle(
            backgroundColor = 0xFF6B6BFF.toInt(),
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

        Text("Tracking Mode", 0xFFFFFFFF.toInt(), 20f, true)
            .setPositioning(x + 20f, Pos.ParentPixels, y + 18f, Pos.ParentPixels)
            .childOf(parent)

        // Mode buttons - added directly to parent for proper click handling
        val gemBg = if (!isBlockMode) 0xFF4CAF50.toInt() else 0xFF2A2A2A.toInt()
        val blockBg = if (isBlockMode) 0xFF4CAF50.toInt() else 0xFF2A2A2A.toInt()
        val gemHover = if (!isBlockMode) 0xFF45A049.toInt() else 0xFF353535.toInt()
        val blockHover = if (isBlockMode) 0xFF45A049.toInt() else 0xFF353535.toInt()

        // Calculate button positions relative to parent
        val buttonY = y + 17f
        val gemstonesX = x + width - 130f - 100f  // 130f offset from right, 100f button width
        val blocksX = x + width - 20f - 100f      // 20f offset from right, 100f button width

        Button("Gemstones", 0xFFFFFFFF.toInt(), fontSize = 12f)
            .setSizing(100f, Size.Pixels, 30f, Size.Pixels)
            .setPositioning(gemstonesX, Pos.ParentPixels, buttonY, Pos.ParentPixels)
            .backgroundColor(gemBg)
            .borderColor(0xFF45A049.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(gemHover, 0xFFFFFFFF.toInt())
            .pressedColors(0xFF388E3C.toInt(), 0xFFFFFFFF.toInt())
            .onClick { _, _, _ ->
                if (isBlockMode) {
                    ProfitTrackerHUD.setMode("GEMSTONES")
                    MinecraftClient.getInstance().setScreen(MiningProfitCategoryScreen(parentScreen))
                }
                true
            }
            .childOf(parent)

        Button("Blocks", 0xFFFFFFFF.toInt(), fontSize = 12f)
            .setSizing(100f, Size.Pixels, 30f, Size.Pixels)
            .setPositioning(blocksX, Pos.ParentPixels, buttonY, Pos.ParentPixels)
            .backgroundColor(blockBg)
            .borderColor(0xFF45A049.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(blockHover, 0xFFFFFFFF.toInt())
            .pressedColors(0xFF388E3C.toInt(), 0xFFFFFFFF.toInt())
            .onClick { _, _, _ ->
                if (!isBlockMode) {
                    ProfitTrackerHUD.setMode("BLOCKS")
                    MinecraftClient.getInstance().setScreen(MiningProfitCategoryScreen(parentScreen))
                }
                true
            }
            .childOf(parent)

        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            MinecraftClient.getInstance().execute {
                card.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()
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

    private fun createSelectorCard(
        label: String,
        getValue: () -> String,
        cycleAction: () -> Unit,
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

        // Accent bar
        Rectangle(
            backgroundColor = 0xFFFFAA00.toInt(),
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

        val valueText = Text(getValue(), 0xFFFFAA00.toInt(), 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 43f, Pos.ParentPixels)
            .childOf(card)

        Text("Click to cycle >", 0xFF888888.toInt(), 12f, false)
            .setPositioning(0f, Pos.ParentPixels, 43f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-20f, 0f)
            .childOf(card)

        card.onClick { _, _, _ ->
            cycleAction()
            valueText.text = getValue()
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
            backgroundColor = 0xFF44FF44.toInt(),
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

        val valueText = Text("${initialValue.toInt()}$suffix", 0xFF44FF44.toInt(), 14f, true)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-20f, 0f)
            .childOf(card)

        Slider(
            value = initialValue,
            minValue = min,
            maxValue = max,
            trackColor = 0xFF1A1A1A.toInt(),
            trackFillColor = 0xFF44FF44.toInt(),
            thumbColor = 0xFF44FF44.toInt(),
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
