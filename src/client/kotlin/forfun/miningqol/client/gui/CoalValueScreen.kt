package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.sacks.BazaarAPI
import forfun.miningqol.client.sacks.CoalValueCommand
import net.minecraft.client.MinecraftClient
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*
import java.text.DecimalFormat

class CoalValueScreen : VexelScreen {
    private val enchantedCoal: Long
    private var showSettings: Boolean
    private var options: List<CoalValueCommand.CraftingOption>? = null
    private var bestIndex: Int = 0

    private lateinit var mainPanel: Rectangle
    private lateinit var contentPanel: Rectangle
    private var isLoading = false

    // Settings state
    private var sellMethod: String
    private var sulphurBuy: String
    private var crudeBuy: String
    private var heavyBuy: String
    private var skipSettings: Boolean

    companion object {
        private val COIN_FORMAT = DecimalFormat("#,##0.0")
    }

    // Constructor for settings mode
    constructor(enchantedCoal: Long) : super("Coal Value Calculator") {
        this.enchantedCoal = enchantedCoal
        this.showSettings = true
        val config = MiningqolClient.getConfig()
        this.sellMethod = config.coalValueSellMethod
        this.sulphurBuy = config.coalValueSulphurBuy
        this.crudeBuy = config.coalValueCrudeBuy
        this.heavyBuy = config.coalValueHeavyBuy
        this.skipSettings = !config.coalValueShowSettings
    }

    // Constructor for results mode
    constructor(enchantedCoal: Long, options: List<CoalValueCommand.CraftingOption>, bestIndex: Int) : super("Coal Value Calculator") {
        this.enchantedCoal = enchantedCoal
        this.showSettings = false
        this.options = options
        this.bestIndex = bestIndex
        val config = MiningqolClient.getConfig()
        this.sellMethod = config.coalValueSellMethod
        this.sulphurBuy = config.coalValueSulphurBuy
        this.crudeBuy = config.coalValueCrudeBuy
        this.heavyBuy = config.coalValueHeavyBuy
        this.skipSettings = !config.coalValueShowSettings
    }

    override fun afterInitialization() {
        // Semi-transparent overlay
        Rectangle(
            backgroundColor = 0x80000000.toInt(),
            borderColor = 0x00000000,
            borderRadius = 0f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(window)
            .fadeIn(300, EasingType.EASE_OUT)

        // Main panel
        val panelHeight = if (showSettings) 400f else 520f
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(450f, Size.Pixels, panelHeight, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        mainPanel.xPositionConstraint = Pos.ScreenPixels
        mainPanel.yPositionConstraint = Pos.ScreenPixels
        mainPanel.xConstraint = (mainPanel.screenWidth - 450f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - panelHeight) / 2f
        mainPanel.fadeIn(400, EasingType.EASE_OUT)

        // Title bar
        Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 50f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusBottomLeft = 0f
                borderRadiusBottomRight = 0f
            }

        // Gold accent bar
        Rectangle(
            backgroundColor = 0xFFFFD700.toInt(),
            borderRadius = 16f
        )
            .setSizing(5f, Size.Pixels, 50f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
                borderRadiusBottomLeft = 0f
            }

        // Title
        val title = if (showSettings) "Coal Value - Settings" else "Coal Value - Results"
        Text(title, 0xFFFFD700.toInt(), 20f, true)
            .setPositioning(0f, Pos.ParentCenter, 14f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Coal amount
        Text("${COIN_FORMAT.format(enchantedCoal)} Enchanted Coal", 0xFFAAAAAA.toInt(), 12f, false)
            .setPositioning(0f, Pos.ParentCenter, 60f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Content panel
        contentPanel = Rectangle(
            backgroundColor = 0x00000000.toInt(),
            borderColor = 0x00000000
        )
            .setSizing(100f, Size.ParentPerc, panelHeight - 80f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 75f, Pos.ParentPixels)
            .childOf(mainPanel)

        if (showSettings) {
            buildSettingsPanel()
        } else {
            buildResultsPanel()
        }
    }

    private fun buildSettingsPanel() {
        var yOffset = 10f

        // Sell Method
        Text("Sell Method:", 0xFFFFFFFF.toInt(), 14f, true)
            .setPositioning(25f, Pos.ParentPixels, yOffset, Pos.ParentPixels)
            .childOf(contentPanel)

        yOffset += 25f
        createToggleButton("Insta-Sell", sellMethod == "INSTASELL", 25f, yOffset) {
            sellMethod = "INSTASELL"
            refreshSettingsPanel()
        }
        createToggleButton("Sell Offer", sellMethod == "SELLOFFER", 160f, yOffset) {
            sellMethod = "SELLOFFER"
            refreshSettingsPanel()
        }

        yOffset += 50f

        // Sulphur Buy Method
        Text("Buy Sulphur:", 0xFFFFFFFF.toInt(), 14f, true)
            .setPositioning(25f, Pos.ParentPixels, yOffset, Pos.ParentPixels)
            .childOf(contentPanel)

        yOffset += 25f
        createToggleButton("Buy Order", sulphurBuy == "BUY_ORDER", 25f, yOffset) {
            sulphurBuy = "BUY_ORDER"
            refreshSettingsPanel()
        }
        createToggleButton("Insta-Buy", sulphurBuy == "INSTA_BUY", 160f, yOffset) {
            sulphurBuy = "INSTA_BUY"
            refreshSettingsPanel()
        }

        yOffset += 50f

        // Crude Gabagool Buy Method
        Text("Buy Crude Gabagool:", 0xFFFFFFFF.toInt(), 14f, true)
            .setPositioning(25f, Pos.ParentPixels, yOffset, Pos.ParentPixels)
            .childOf(contentPanel)

        yOffset += 25f
        createToggleButton("Buy Order", crudeBuy == "BUY_ORDER", 25f, yOffset) {
            crudeBuy = "BUY_ORDER"
            refreshSettingsPanel()
        }
        createToggleButton("Insta-Buy", crudeBuy == "INSTA_BUY", 160f, yOffset) {
            crudeBuy = "INSTA_BUY"
            refreshSettingsPanel()
        }

        yOffset += 50f

        // Heavy Gabagool Buy Method
        Text("Buy Heavy Gabagool:", 0xFFFFFFFF.toInt(), 14f, true)
            .setPositioning(25f, Pos.ParentPixels, yOffset, Pos.ParentPixels)
            .childOf(contentPanel)

        yOffset += 25f
        createToggleButton("Buy Order", heavyBuy == "BUY_ORDER", 25f, yOffset) {
            heavyBuy = "BUY_ORDER"
            refreshSettingsPanel()
        }
        createToggleButton("Insta-Buy", heavyBuy == "INSTA_BUY", 160f, yOffset) {
            heavyBuy = "INSTA_BUY"
            refreshSettingsPanel()
        }

        yOffset += 55f

        // Skip settings checkbox
        val checkboxColor = if (skipSettings) 0xFF4CAF50.toInt() else 0xFF404040.toInt()
        Button(if (skipSettings) "X" else "", 0xFFFFFFFF.toInt(), fontSize = 12f)
            .setSizing(20f, Size.Pixels, 20f, Size.Pixels)
            .setPositioning(25f, Pos.ParentPixels, yOffset, Pos.ParentPixels)
            .backgroundColor(checkboxColor)
            .borderColor(0xFF606060.toInt())
            .borderRadius(4f)
            .borderThickness(1f)
            .onClick { _, _, _ ->
                skipSettings = !skipSettings
                refreshSettingsPanel()
                true
            }
            .childOf(contentPanel)

        Text("Skip settings next time", 0xFFAAAAAA.toInt(), 12f, false)
            .setPositioning(55f, Pos.ParentPixels, yOffset + 3f, Pos.ParentPixels)
            .childOf(contentPanel)

        yOffset += 40f

        // Calculate button
        Button("Calculate", 0xFFFFFFFF.toInt(), fontSize = 16f)
            .setSizing(180f, Size.Pixels, 45f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, yOffset, Pos.ParentPixels)
            .backgroundColor(0xFF4CAF50.toInt())
            .borderColor(0xFF45A049.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF45A049.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF388E3C.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                if (!isLoading) {
                    saveSettings()
                    calculateResults()
                }
                true
            }
            .childOf(contentPanel)
    }

    private fun createToggleButton(label: String, selected: Boolean, x: Float, y: Float, onClick: () -> Unit) {
        val bgColor = if (selected) 0xFF4CAF50.toInt() else 0xFF2A2A2A.toInt()
        val borderColor = if (selected) 0xFF45A049.toInt() else 0xFF404040.toInt()

        Button(label, 0xFFFFFFFF.toInt(), fontSize = 12f)
            .setSizing(120f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .backgroundColor(bgColor)
            .borderColor(borderColor)
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(if (selected) 0xFF45A049.toInt() else 0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .onClick { _, _, _ ->
                onClick()
                true
            }
            .childOf(contentPanel)
    }

    private fun refreshSettingsPanel() {
        // Clear and rebuild
        contentPanel.children.clear()
        buildSettingsPanel()
    }

    private fun saveSettings() {
        val config = MiningqolClient.getConfig()
        config.coalValueSellMethod = sellMethod
        config.coalValueSulphurBuy = sulphurBuy
        config.coalValueCrudeBuy = crudeBuy
        config.coalValueHeavyBuy = heavyBuy
        config.coalValueShowSettings = !skipSettings
        config.save()
    }

    private fun calculateResults() {
        isLoading = true

        // Show loading state
        contentPanel.children.clear()
        Text("Fetching Bazaar prices...", 0xFFFFD700.toInt(), 14f, false)
            .setPositioning(0f, Pos.ParentCenter, 100f, Pos.ParentPixels)
            .childOf(contentPanel)

        BazaarAPI.fetchPrices().thenAccept { products ->
            MinecraftClient.getInstance().execute {
                if (products.isEmpty()) {
                    contentPanel.children.clear()
                    Text("Failed to fetch prices!", 0xFFFF6B6B.toInt(), 14f, false)
                        .setPositioning(0f, Pos.ParentCenter, 100f, Pos.ParentPixels)
                        .childOf(contentPanel)
                    isLoading = false
                    return@execute
                }

                options = CoalValueCommand.calculateOptions(products, enchantedCoal)
                bestIndex = CoalValueCommand.findBestOption(options!!)
                showSettings = false
                isLoading = false

                // Rebuild the entire screen for results
                close()
                MinecraftClient.getInstance().setScreen(CoalValueScreen(enchantedCoal, options!!, bestIndex))
            }
        }
    }

    private fun buildResultsPanel() {
        val opts = options ?: return
        var yOffset = 5f

        for ((index, option) in opts.withIndex()) {
            val isBest = index == bestIndex
            val cardBg = if (isBest) 0xFF1E3A1E.toInt() else 0xFF1A1A1A.toInt()
            val cardBorder = if (isBest) 0xFF4CAF50.toInt() else 0xFF2A2A2A.toInt()

            val cardHeight = 65f + (option.costs.size * 14f)

            val card = Rectangle(
                backgroundColor = cardBg,
                borderColor = cardBorder,
                borderRadius = 8f,
                borderThickness = if (isBest) 2f else 1f
            )
                .setSizing(400f, Size.Pixels, cardHeight, Size.Pixels)
                .setPositioning(0f, Pos.ParentCenter, yOffset, Pos.ParentPixels)
                .childOf(contentPanel)

            // Option name
            val nameColor = if (isBest) 0xFF4CAF50.toInt() else 0xFFFFFFFF.toInt()
            Text(option.name, nameColor, 13f, true)
                .setPositioning(15f, Pos.ParentPixels, 10f, Pos.ParentPixels)
                .childOf(card)

            // Best badge
            if (isBest) {
                Text("BEST", 0xFF4CAF50.toInt(), 10f, true)
                    .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
                    .alignRight()
                    .setOffset(-15f, 0f)
                    .childOf(card)
            }

            // Output
            if (option.output.isNotEmpty()) {
                Text(option.output, 0xFFAAAAAA.toInt(), 11f, false)
                    .setPositioning(15f, Pos.ParentPixels, 28f, Pos.ParentPixels)
                    .childOf(card)
            }

            // Costs
            var costY = if (option.output.isEmpty()) 28f else 42f
            for (cost in option.costs) {
                Text(cost, 0xFF888888.toInt(), 10f, false)
                    .setPositioning(15f, Pos.ParentPixels, costY, Pos.ParentPixels)
                    .childOf(card)
                costY += 14f
            }

            // Profit
            val profitColor = if (option.profit >= 0) 0xFF4CAF50.toInt() else 0xFFFF6B6B.toInt()
            val profitText = if (option.profit >= 0) "+${formatCoins(option.profit)}" else formatCoins(option.profit)
            Text(profitText, profitColor, 14f, true)
                .setPositioning(0f, Pos.ParentPixels, cardHeight - 22f, Pos.ParentPixels)
                .alignRight()
                .setOffset(-15f, 0f)
                .childOf(card)

            yOffset += cardHeight + 8f
        }

        // Settings button at bottom
        Button("Settings", 0xFFAAAAAA.toInt(), fontSize = 12f)
            .setSizing(100f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, yOffset + 5f, Pos.ParentPixels)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .onClick { _, _, _ ->
                // Go back to settings - force show settings
                val config = MiningqolClient.getConfig()
                config.coalValueShowSettings = true
                close()
                MinecraftClient.getInstance().setScreen(CoalValueScreen(enchantedCoal))
                true
            }
            .childOf(contentPanel)
    }

    private fun formatCoins(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> String.format("%.2fB", amount / 1_000_000_000)
            amount >= 1_000_000 -> String.format("%.2fM", amount / 1_000_000)
            amount >= 1_000 -> String.format("%.1fK", amount / 1_000)
            else -> COIN_FORMAT.format(amount)
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun shouldCloseOnEsc(): Boolean = false
}
