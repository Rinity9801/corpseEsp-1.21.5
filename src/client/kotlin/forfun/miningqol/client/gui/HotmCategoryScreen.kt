package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.hotm.HotmManager
import forfun.miningqol.client.hotm.HotmNode
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

class HotmCategoryScreen(private val parentScreen: Screen) : VexelScreen("HOTM Config") {
    private lateinit var overlay: Rectangle
    private lateinit var mainPanel: Rectangle
    private lateinit var tokenText: Text
    private val cellRects = mutableMapOf<HotmNode, Rectangle>()
    private val cellLabels = mutableMapOf<HotmNode, Text>()
    private val cellStateTexts = mutableMapOf<HotmNode, Text>()
    private var scrollOffset = 0

    private val tree get() = HotmManager.getTree()

    // Colors for states
    companion object {
        const val COLOR_NOT_CLICKED_PERK = 0xFF333333.toInt()    // dark gray (coal)
        const val COLOR_NOT_CLICKED_ABILITY = 0xFF1A1A1A.toInt() // darker (coal block)
        const val COLOR_DISABLED = 0xFFAA0000.toInt()            // red (redstone)
        const val COLOR_LEVEL_1 = 0xFF00AA00.toInt()             // green (emerald)
        const val COLOR_MAXED = 0xFF55FFFF.toInt()               // cyan (diamond)
        const val COLOR_CHOSEN = 0xFF00FF00.toInt()              // bright green (emerald block)
        const val COLOR_EMPTY = 0xFF0A0A0A.toInt()               // very dark (empty slot)

        const val BORDER_PERK = 0xFF555555.toInt()
        const val BORDER_ABILITY = 0xFF8855FF.toInt()
        const val BORDER_ACTIVE = 0xFF55FF55.toInt()
    }

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
            .setSizing(620f, Size.Pixels, 620f, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        mainPanel.xPositionConstraint = Pos.ScreenPixels
        mainPanel.yPositionConstraint = Pos.ScreenPixels
        mainPanel.xConstraint = (mainPanel.screenWidth - 620f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - 620f) / 2f
        mainPanel.fadeIn(500, EasingType.EASE_OUT)

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

        Text("Heart of the Mountain", 0xFFFFFFFF.toInt(), 24f, true)
            .setPositioning(0f, Pos.ParentCenter, 12f, Pos.ParentPixels)
            .childOf(mainPanel)

        tokenText = Text(getTokenString(), 0xFF888888.toInt(), 14f, false)
            .setPositioning(0f, Pos.ParentCenter, 42f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Grid area
        buildGrid()

        // Save button
        Button("Save", 0xFFFFFFFF.toInt(), fontSize = 13f)
            .setSizing(80f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(20f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -25f)
            .backgroundColor(0xFF1A5A1A.toInt())
            .borderColor(0xFF2A8A2A.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF2A7A2A.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF0A3A0A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                HotmManager.save()
                true
            }
            .childOf(mainPanel)

        // Back button
        Button("Back", 0xFFFFFFFF.toInt(), fontSize = 13f)
            .setSizing(80f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .alignBottom()
            .alignRight()
            .setOffset(-20f, -25f)
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
    }

    private fun buildGrid() {
        val cellSize = 42f
        val cellGap = 4f
        val gridStartX = (620f - (9 * (cellSize + cellGap) - cellGap)) / 2f
        val gridStartY = 75f

        // All 10 rows, 9 columns - no scrolling
        for (visRow in 0 until 10) {
            for (col in 0 until 9) {
                val treeRow = visRow
                val node = tree.getNodeAt(treeRow, col)

                val x = gridStartX + col * (cellSize + cellGap)
                val y = gridStartY + visRow * (cellSize + cellGap)

                if (node != null) {
                    val state = tree.getState(node)
                    val bgColor = getColorForState(node, state)
                    val borderColor = getBorderColor(node, state)

                    val cell = Rectangle(
                        backgroundColor = bgColor,
                        borderColor = borderColor,
                        borderRadius = 6f,
                        borderThickness = 2f,
                        hoverColor = lighten(bgColor, 30)
                    )
                        .setSizing(cellSize, Size.Pixels, cellSize, Size.Pixels)
                        .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
                        .childOf(mainPanel)
                        .onClick { _, _, _ ->
                            cycleState(node)
                            true
                        }

                    // Node name (short)
                    val nameText = Text(
                        shortenName(node.displayName),
                        0xFFFFFFFF.toInt(),
                        7f,
                        false
                    )
                        .setPositioning(0f, Pos.ParentCenter, 5f, Pos.ParentPixels)
                        .childOf(cell)

                    // Type indicator
                    val typeStr = if (node.type == HotmNode.Type.ABILITY) "★" else ""
                    Text(typeStr, 0xFFFFDD55.toInt(), 9f, true)
                        .setPositioning(0f, Pos.ParentCenter, 17f, Pos.ParentPixels)
                        .childOf(cell)

                    // State text
                    val stateText = Text(
                        getStateLabel(state),
                        getStateLabelColor(state),
                        8f,
                        true
                    )
                        .setPositioning(0f, Pos.ParentCenter, 29f, Pos.ParentPixels)
                        .childOf(cell)

                    cellRects[node] = cell
                    cellLabels[node] = nameText
                    cellStateTexts[node] = stateText
                } else {
                    // Empty cell
                    Rectangle(
                        backgroundColor = COLOR_EMPTY,
                        borderColor = 0xFF1A1A1A.toInt(),
                        borderRadius = 6f,
                        borderThickness = 1f
                    )
                        .setSizing(cellSize, Size.Pixels, cellSize, Size.Pixels)
                        .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
                        .ignoreMouseEvents()
                        .childOf(mainPanel)
                }
            }
        }
    }

    private fun refreshGrid() {
        // Rebuild the screen with new scroll offset
        MinecraftClient.getInstance().setScreen(HotmCategoryScreen(parentScreen).also {
            it.scrollOffset = this.scrollOffset
        })
    }

    private fun cycleState(node: HotmNode) {
        val currentState = tree.getState(node)
        val nextState = getNextState(node, currentState)
        if (tree.setState(node, nextState)) {
            updateCell(node, nextState)
            tokenText.text = getTokenString()
        }
    }

    private fun getNextState(node: HotmNode, current: HotmNode.State): HotmNode.State {
        return if (node.type == HotmNode.Type.PERK) {
            when (current) {
                HotmNode.State.NOT_CLICKED -> HotmNode.State.LEVEL_1
                HotmNode.State.LEVEL_1 -> HotmNode.State.MAXED
                HotmNode.State.MAXED -> HotmNode.State.DISABLED
                HotmNode.State.DISABLED -> HotmNode.State.NOT_CLICKED
                else -> HotmNode.State.NOT_CLICKED
            }
        } else {
            when (current) {
                HotmNode.State.NOT_CLICKED -> HotmNode.State.CHOSEN
                HotmNode.State.CHOSEN -> HotmNode.State.DISABLED
                HotmNode.State.DISABLED -> HotmNode.State.NOT_CLICKED
                else -> HotmNode.State.NOT_CLICKED
            }
        }
    }

    private fun updateCell(node: HotmNode, state: HotmNode.State) {
        cellRects[node]?.let { cell ->
            cell.backgroundColor = getColorForState(node, state)
            cell.borderColor = getBorderColor(node, state)
            cell.hoverColor = lighten(getColorForState(node, state), 30)
        }
        cellStateTexts[node]?.let { text ->
            text.text = getStateLabel(state)
            text.textColor = getStateLabelColor(state)
        }
    }

    private fun getColorForState(node: HotmNode, state: HotmNode.State): Int {
        return when (state) {
            HotmNode.State.NOT_CLICKED -> if (node.type == HotmNode.Type.PERK) COLOR_NOT_CLICKED_PERK else COLOR_NOT_CLICKED_ABILITY
            HotmNode.State.DISABLED -> COLOR_DISABLED
            HotmNode.State.LEVEL_1 -> COLOR_LEVEL_1
            HotmNode.State.MAXED -> COLOR_MAXED
            HotmNode.State.CHOSEN -> COLOR_CHOSEN
        }
    }

    private fun getBorderColor(node: HotmNode, state: HotmNode.State): Int {
        if (state != HotmNode.State.NOT_CLICKED) return BORDER_ACTIVE
        return if (node.type == HotmNode.Type.ABILITY) BORDER_ABILITY else BORDER_PERK
    }

    private fun getStateLabel(state: HotmNode.State): String {
        return when (state) {
            HotmNode.State.NOT_CLICKED -> "OFF"
            HotmNode.State.DISABLED -> "DIS"
            HotmNode.State.LEVEL_1 -> "LV1"
            HotmNode.State.MAXED -> "MAX"
            HotmNode.State.CHOSEN -> "ON"
        }
    }

    private fun getStateLabelColor(state: HotmNode.State): Int {
        return when (state) {
            HotmNode.State.NOT_CLICKED -> 0xFF666666.toInt()
            HotmNode.State.DISABLED -> 0xFFFF5555.toInt()
            HotmNode.State.LEVEL_1 -> 0xFF55FF55.toInt()
            HotmNode.State.MAXED -> 0xFF55FFFF.toInt()
            HotmNode.State.CHOSEN -> 0xFF55FF55.toInt()
        }
    }

    private fun shortenName(name: String): String {
        if (name.length <= 8) return name
        // Take first word or abbreviate
        val words = name.split(" ")
        return if (words.size > 1) {
            words.joinToString("") { it.take(1) }.uppercase()
        } else {
            name.take(7) + "."
        }
    }

    private fun lighten(color: Int, amount: Int): Int {
        val a = (color shr 24) and 0xFF
        val r = minOf(255, ((color shr 16) and 0xFF) + amount)
        val g = minOf(255, ((color shr 8) and 0xFF) + amount)
        val b = minOf(255, (color and 0xFF) + amount)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun getTokenString(): String {
        return "Tokens: ${tree.usedTokens}/${tree.totalTokens} (${tree.remainingTokens} remaining)"
    }

    private fun closeWithAnimation() {
        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()
        MinecraftClient.getInstance().setScreen(parentScreen)
    }

    override fun keyPressed(input: KeyInput?): Boolean {
        if (input?.key() == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
            return true
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

    // Allow setting scroll offset before screen opens
    var initialScroll: Int
        get() = scrollOffset
        set(value) { scrollOffset = value }
}
