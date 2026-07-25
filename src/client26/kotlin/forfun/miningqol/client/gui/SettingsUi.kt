package forfun.miningqol.client.gui

import xyz.meowing.vexel.animations.presets.fadeIn
import xyz.meowing.vexel.animations.types.EasingType
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelWindow
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.Slider
import xyz.meowing.vexel.elements.Switch

/**
 * Shared scaffolding for the 26.1.2 settings screens (dark overlay, centered panel,
 * toggle/slider rows) in the same visual style as the 1.21 Vexel screens.
 */
object SettingsUi {
    const val ROW_WIDTH = 530f
    const val ROW_HEIGHT = 56f
    const val ROW_SPACING = 12f

    fun overlay(window: VexelWindow): Rectangle =
        Rectangle(
            backgroundColor = 0x80000000.toInt(),
            borderColor = 0x00000000,
            borderRadius = 0f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.Percent, 100f, Size.Percent)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(window)
            .fadeIn(400, EasingType.EASE_OUT)

    fun panel(window: VexelWindow, width: Float, height: Float, title: String, subtitle: String): Rectangle {
        val panel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        // ScreenCenter recomputes on every layout pass, so the panel stays centered across
        // window resizes / fullscreen toggles / monitor moves (ScreenPixels bakes the position
        // at open time and can leave the panel entirely off-screen after a resize).
        panel.xPositionConstraint = Pos.ScreenCenter
        panel.yPositionConstraint = Pos.ScreenCenter
        panel.xConstraint = 0f
        panel.yConstraint = 0f
        panel.fadeIn(500, EasingType.EASE_OUT)

        Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.Percent, 80f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(panel)
            .apply {
                borderRadiusBottomLeft = 0f
                borderRadiusBottomRight = 0f
            }

        Text(title, 0xFFFFFFFF.toInt(), 28f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(panel)

        Text(subtitle, 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 50f, Pos.ParentPixels)
            .childOf(panel)

        return panel
    }

    /** A labelled on/off row. Returns the y of the next row. */
    fun toggleRow(
        parent: Rectangle,
        panelWidth: Float,
        y: Float,
        label: String,
        description: String,
        accentColor: Int,
        initial: Boolean,
        onChange: (Boolean) -> Unit
    ): Float {
        val card = row(parent, panelWidth, y)

        Text(label, 0xFFFFFFFF.toInt(), 17f, true)
            .setPositioning(16f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .childOf(card)

        Text(description, 0xFF888888.toInt(), 12f, false)
            .setPositioning(16f, Pos.ParentPixels, 33f, Pos.ParentPixels)
            .childOf(card)

        val switch = Switch(trackEnabledColor = accentColor)
            .setSizing(46f, Size.Pixels, 24f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-16f, 0f)
            .childOf(card)
        switch.setEnabled(initial, animated = false, silent = true)
        switch.onValueChange { value -> onChange(value as Boolean) }

        return y + ROW_HEIGHT + ROW_SPACING
    }

    /** A labelled slider row with a live value readout. Returns the y of the next row. */
    fun sliderRow(
        parent: Rectangle,
        panelWidth: Float,
        y: Float,
        label: String,
        min: Float,
        max: Float,
        step: Float?,
        initial: Float,
        accentColor: Int,
        format: (Float) -> String,
        onChange: (Float) -> Unit
    ): Float {
        val card = row(parent, panelWidth, y)

        Text(label, 0xFFFFFFFF.toInt(), 17f, true)
            .setPositioning(16f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .childOf(card)

        val valueText = Text(format(initial), 0xFF888888.toInt(), 13f, false)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-16f, 0f)
            .childOf(card)

        val slider = Slider(
            value = initial,
            minValue = min,
            maxValue = max,
            step = step,
            trackFillColor = accentColor,
            thumbWidth = 14f,
            thumbHeight = 14f,
            thumbRadius = 7f
        )
            .setSizing(ROW_WIDTH - 32f, Size.Pixels, 14f, Size.Pixels)
            .setPositioning(16f, Pos.ParentPixels, 34f, Pos.ParentPixels)
            .childOf(card)
        slider.onValueChange { value ->
            val v = value as Float
            valueText.text = format(v)
            onChange(v)
        }

        return y + ROW_HEIGHT + ROW_SPACING
    }

    /** A labelled free-text row (e.g. a block id). Returns the y of the next row. */
    fun textRow(
        parent: Rectangle,
        panelWidth: Float,
        y: Float,
        label: String,
        description: String,
        initial: String,
        onChange: (String) -> Unit
    ): Float {
        val card = row(parent, panelWidth, y)

        Text(label, 0xFFFFFFFF.toInt(), 17f, true)
            .setPositioning(16f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .childOf(card)

        Text(description, 0xFF888888.toInt(), 12f, false)
            .setPositioning(16f, Pos.ParentPixels, 33f, Pos.ParentPixels)
            .childOf(card)

        val input = xyz.meowing.vexel.elements.TextInput(initialValue = initial, fontSize = 12f)
            .setSizing(200f, Size.Pixels, 30f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-16f, 0f)
            .childOf(card)
        input.onValueChange { value -> onChange(value as String) }

        return y + ROW_HEIGHT + ROW_SPACING
    }

    /** A row that just opens a sub-screen. Returns the y of the next row. */
    fun linkRow(
        parent: Rectangle,
        panelWidth: Float,
        y: Float,
        label: String,
        description: String,
        onOpen: () -> Unit
    ): Float {
        val card = row(parent, panelWidth, y)

        Text(label, 0xFFFFFFFF.toInt(), 17f, true)
            .setPositioning(16f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .childOf(card)

        Text(description, 0xFF888888.toInt(), 12f, false)
            .setPositioning(16f, Pos.ParentPixels, 33f, Pos.ParentPixels)
            .childOf(card)

        Text("›", 0xFF666666.toInt(), 26f, false)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-18f, 0f)
            .childOf(card)

        card.onClick { _ ->
            onOpen()
            true
        }

        return y + ROW_HEIGHT + ROW_SPACING
    }

    const val COLOR_ROW_HEIGHT = 96f

    /** An RGBA slider group with a live preview swatch. Returns the y of the next row. */
    fun colorRow(
        parent: Rectangle,
        panelWidth: Float,
        y: Float,
        title: String,
        getColor: () -> FloatArray,
        setColor: (Float, Float, Float) -> Unit,
        getAlpha: () -> Float,
        setAlpha: (Float) -> Unit
    ): Float {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(ROW_WIDTH, Size.Pixels, COLOR_ROW_HEIGHT, Size.Pixels)
            .setPositioning((panelWidth - ROW_WIDTH) / 2f, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(parent)

        Text(title, 0xFFFFFFFF.toInt(), 16f, true)
            .setPositioning(16f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)

        fun swatchColor(): Int {
            val c = getColor()
            return (0xFF shl 24) or
                ((c[0] * 255).toInt().coerceIn(0, 255) shl 16) or
                ((c[1] * 255).toInt().coerceIn(0, 255) shl 8) or
                (c[2] * 255).toInt().coerceIn(0, 255)
        }

        val swatch = Rectangle(backgroundColor = swatchColor(), borderColor = 0xFF444444.toInt(),
            borderRadius = 6f, borderThickness = 1f)
            .setSizing(34f, Size.Pixels, 22f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 10f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-16f, 0f)
            .ignoreMouseEvents()
            .childOf(card)

        val channels = listOf("R", "G", "B", "A")
        val sliderWidth = 92f
        channels.forEachIndexed { i, label ->
            val x = 16f + i * (sliderWidth + 34f)
            Text(label, 0xFF888888.toInt(), 12f, false)
                .setPositioning(x, Pos.ParentPixels, 52f, Pos.ParentPixels)
                .childOf(card)

            val initial = if (i == 3) getAlpha() else getColor()[i]
            val slider = Slider(
                value = initial, minValue = 0f, maxValue = 1f, step = null,
                trackFillColor = when (i) {
                    0 -> 0xFFFF6666.toInt(); 1 -> 0xFF66FF66.toInt(); 2 -> 0xFF6699FF.toInt(); else -> 0xFFCCCCCC.toInt()
                },
                thumbWidth = 12f, thumbHeight = 12f, thumbRadius = 6f
            )
                .setSizing(sliderWidth, Size.Pixels, 12f, Size.Pixels)
                .setPositioning(x + 14f, Pos.ParentPixels, 52f, Pos.ParentPixels)
                .childOf(card)
            slider.onValueChange { value ->
                val v = value as Float
                if (i == 3) {
                    setAlpha(v)
                } else {
                    val c = getColor()
                    val r = if (i == 0) v else c[0]
                    val g = if (i == 1) v else c[1]
                    val b = if (i == 2) v else c[2]
                    setColor(r, g, b)
                    swatch.backgroundColor = swatchColor()
                }
            }
        }

        return y + COLOR_ROW_HEIGHT + ROW_SPACING
    }

    fun backButton(parent: Rectangle, onBack: () -> Unit) {
        Button("Back", 0xFFFFFFFF.toInt(), fontSize = 15f)
            .setSizing(140f, Size.Pixels, 42f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -22f)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _ ->
                onBack()
                true
            }
            .childOf(parent)
    }

    private fun row(parent: Rectangle, panelWidth: Float, y: Float): Rectangle =
        Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(ROW_WIDTH, Size.Pixels, ROW_HEIGHT, Size.Pixels)
            .setPositioning((panelWidth - ROW_WIDTH) / 2f, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(parent)
}
