package forfun.miningqol.client.gui

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

class CoalValueScreen(
    private val enchantedCoal: Long,
    private val method: String,
    private val options: List<CraftingOption>,
    private val bestOptionIndex: Int
) : VexelScreen("Coal Value Calculator") {

    data class CraftingOption(
        val name: String,
        val output: String,
        val costs: List<String>,
        val profit: Double
    )

    companion object {
        private val COIN_FORMAT = DecimalFormat("#,##0.0")
        private val COUNT_FORMAT = DecimalFormat("#,###")
    }

    private lateinit var mainPanel: Rectangle

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
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(500f, Size.Pixels, 650f, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        mainPanel.xPositionConstraint = Pos.ScreenPixels
        mainPanel.yPositionConstraint = Pos.ScreenPixels
        mainPanel.xConstraint = (mainPanel.screenWidth - 500f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - 650f) / 2f
        mainPanel.fadeIn(400, EasingType.EASE_OUT)

        // Title bar
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

        // Title
        Text("Coal Value Calculator", 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 15f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Method indicator
        val methodColor = if (method == "Insta-Sell") 0xFFFF6B6B.toInt() else 0xFF6BCB77.toInt()
        Text(method, methodColor, 14f, false)
            .setPositioning(0f, Pos.ParentCenter, 45f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Enchanted Coal count
        Text("Enchanted Coal: ${COUNT_FORMAT.format(enchantedCoal)}", 0xFFAAAAAA.toInt(), 14f, false)
            .setPositioning(20f, Pos.ParentPixels, 80f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Create option cards
        var yOffset = 105f
        options.forEachIndexed { index, option ->
            createOptionCard(option, index, yOffset, index == bestOptionIndex)
            yOffset += 85f
        }

        // Close button
        Button("Close", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(100f, Size.Pixels, 36f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -15f)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                close()
                true
            }
            .childOf(mainPanel)
    }

    private fun createOptionCard(option: CraftingOption, index: Int, yPos: Float, isBest: Boolean) {
        val cardColor = if (isBest) 0xFF1E2A1E.toInt() else 0xFF1E1E1E.toInt()
        val borderColor = if (isBest) 0xFF4CAF50.toInt() else 0xFF2A2A2A.toInt()

        val card = Rectangle(
            backgroundColor = cardColor,
            borderColor = borderColor,
            borderRadius = 10f,
            borderThickness = if (isBest) 2f else 1f
        )
            .setSizing(460f, Size.Pixels, 75f, Size.Pixels)
            .setPositioning(20f, Pos.ParentPixels, yPos, Pos.ParentPixels)
            .childOf(mainPanel)

        // Option number and name
        val titleColor = if (isBest) 0xFF4CAF50.toInt() else 0xFFFFFFFF.toInt()
        Text("${index + 1}. ${option.name}", titleColor, 16f, true)
            .setPositioning(12f, Pos.ParentPixels, 8f, Pos.ParentPixels)
            .childOf(card)

        // Best badge
        if (isBest) {
            Text("BEST", 0xFF4CAF50.toInt(), 11f, true)
                .setPositioning(0f, Pos.ParentPixels, 10f, Pos.ParentPixels)
                .alignRight()
                .setOffset(-12f, 0f)
                .childOf(card)
        }

        // Output info
        if (option.output.isNotEmpty()) {
            Text(option.output, 0xFF888888.toInt(), 12f, false)
                .setPositioning(12f, Pos.ParentPixels, 28f, Pos.ParentPixels)
                .childOf(card)
        }

        // Costs
        if (option.costs.isNotEmpty()) {
            val costsText = option.costs.joinToString(" | ")
            Text(costsText, 0xFFFF6B6B.toInt(), 11f, false)
                .setPositioning(12f, Pos.ParentPixels, 44f, Pos.ParentPixels)
                .childOf(card)
        }

        // Profit
        val profitColor = if (option.profit >= 0) 0xFFFFD700.toInt() else 0xFFFF6B6B.toInt()
        Text("${COIN_FORMAT.format(option.profit)} coins", profitColor, 14f, true)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .alignRight()
            .alignBottom()
            .setOffset(-12f, -8f)
            .childOf(card)
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
