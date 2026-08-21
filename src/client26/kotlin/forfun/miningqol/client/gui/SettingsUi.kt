package forfun.miningqol.client.gui

import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.ColorPicker
import xyz.meowing.vexel.elements.Dropdown
import xyz.meowing.vexel.elements.Slider
import xyz.meowing.vexel.elements.TextInput
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Prisma-style UI kit for the 26.1.2 settings GUI: near-black floating panels,
 * card-based inline controls that thread a y cursor through a detail wrapper,
 * and a global GUI transparency applied to every surface color.
 */
object SettingsUi {
    // Prisma palette
    val PANEL_BG = 0xFF0D0D0F.toInt()
    val PANEL_BORDER = 0xFF2A2A2E.toInt()
    val CARD_BG = 0xFF161618.toInt()
    val CARD_BORDER = 0xFF252528.toInt()
    val CARD_HOVER = 0xFF1E1E20.toInt()
    val NAV_SELECTED = 0xFF1E1E20.toInt()
    val NAV_HOVER = 0xFF151517.toInt()
    val TRACK = 0xFF1A1A1A.toInt()
    val TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    val TEXT_SECONDARY = 0xFFBBBBBB.toInt()
    val TEXT_MUTED = 0xFF888888.toInt()
    val TEXT_DIM = 0xFF606060.toInt()
    val EDGE_WIDTH = 0.5f

    // Category/feature accents
    val BLUE = 0xFF7AA2F7.toInt()
    val GREEN = 0xFF9ECE6A.toInt()
    val CYAN = 0xFF7DCFFF.toInt()
    val PURPLE = 0xFFBB9AF7.toInt()
    val PURPLE2 = 0xFF9D7CD8.toInt()
    val SKY = 0xFF89DDFF.toInt()
    val YELLOW = 0xFFE0AF68.toInt()
    val ORANGE = 0xFFFF9E64.toInt()
    val RED = 0xFFF7768E.toInt()
    val TEAL = 0xFF73DACA.toInt()

    /** Whole-GUI opacity (0.3..1.0). Applied to every surface color via [alpha]. */
    @JvmStatic
    var guiOpacity = 0.8f
        set(value) {
            field = value.coerceIn(0.3f, 1.0f)
        }

    /** Scales a color's alpha channel by the configured GUI opacity. */
    fun alpha(color: Int): Int {
        val a = ((color ushr 24) and 0xFF) * guiOpacity
        return (a.toInt().coerceIn(0, 255) shl 24) or (color and 0xFFFFFF)
    }

    /** The accent color with its alpha replaced (0f..1f) — for tinted chips and pills. */
    fun tint(accent: Int, alphaFrac: Float): Int =
        ((alphaFrac * 255f).toInt().coerceIn(0, 255) shl 24) or (accent and 0xFFFFFF)

    /** Softer opacity-aware color for thin NanoVG control outlines. */
    fun edge(color: Int, strength: Float = 0.72f): Int {
        val sourceAlpha = (color ushr 24) and 0xFF
        val a = (sourceAlpha * guiOpacity * strength).toInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0xFFFFFF)
    }

    /** An empty card in the detail column — base for custom rows. */
    fun inlineCard(parent: Rectangle, width: Float, y: Float, height: Float, hover: Boolean = false): Rectangle =
        Rectangle(
            backgroundColor = alpha(CARD_BG),
            borderColor = edge(CARD_BORDER),
            borderRadius = 12f,
            borderThickness = EDGE_WIDTH,
            hoverColor = if (hover) alpha(CARD_HOVER) else null
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, y, Pos.ParentPixels)
            .childOf(parent)

    /** Prisma-style toggle card: click anywhere, ON/OFF status, accent border + dot when on. */
    fun inlineToggle(
        parent: Rectangle,
        width: Float,
        y: Float,
        label: String,
        description: String?,
        accent: Int,
        get: () -> Boolean,
        set: (Boolean) -> Unit
    ): Float {
        val enabled = get()
        val card = inlineCard(parent, width, y, 60f, hover = true)
        card.borderColor = if (enabled) edge(accent) else edge(CARD_BORDER)

        val labelText = Text(label, if (enabled) TEXT_PRIMARY else TEXT_SECONDARY, 16f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)
        val statusText = Text(if (enabled) "ON" else "OFF", if (enabled) accent else TEXT_DIM, 13f, true)
            .setPositioning(18f, Pos.ParentPixels, 34f, Pos.ParentPixels)
            .childOf(card)
        if (description != null) {
            Text(description, TEXT_MUTED, 11f, false)
                .setPositioning(60f, Pos.ParentPixels, 36f, Pos.ParentPixels)
                .childOf(card)
        }
        val indicator = Rectangle(backgroundColor = accent, borderColor = 0x00000000, borderRadius = 4f)
            .setSizing(8f, Size.Pixels, 8f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-14f, 14f)
            .ignoreMouseEvents()
            .childOf(card)
        indicator.visible = enabled

        card.onClick { _ ->
            val n = !get()
            set(n)
            card.borderColor = if (n) edge(accent) else edge(CARD_BORDER)
            labelText.textColor = if (n) TEXT_PRIMARY else TEXT_SECONDARY
            statusText.text = if (n) "ON" else "OFF"
            statusText.textColor = if (n) accent else TEXT_DIM
            indicator.visible = n
            true
        }
        return y + 72f
    }

    /** Prisma-style slider card with live value readout. */
    fun inlineSlider(
        parent: Rectangle,
        width: Float,
        y: Float,
        label: String,
        min: Float,
        max: Float,
        step: Float?,
        initial: Float,
        accent: Int,
        format: (Float) -> String,
        onChange: (Float) -> Unit
    ): Float {
        val card = inlineCard(parent, width, y, 75f)

        Text(label, TEXT_PRIMARY, 15f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)
        val valueText = Text(format(initial), TEXT_SECONDARY, 13f, true)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-18f, 0f)
            .childOf(card)

        val slider = Slider(
            value = initial, minValue = min, maxValue = max, step = step,
            trackColor = alpha(TRACK), trackFillColor = accent, thumbColor = accent,
            trackHeight = 4f, thumbWidth = 16f, thumbHeight = 16f, thumbRadius = 8f
        )
            .setSizing(width - 36f, Size.Pixels, 24f, Size.Pixels)
            .setPositioning(18f, Pos.ParentPixels, 40f, Pos.ParentPixels)
            .childOf(card)
        slider.onValueChange { value ->
            val v = value as Float
            valueText.text = format(v)
            onChange(v)
        }
        return y + 87f
    }

    /** A labelled free-text card (e.g. a block id). */
    fun inlineTextInput(
        parent: Rectangle,
        width: Float,
        y: Float,
        label: String,
        description: String?,
        initial: String,
        onChange: (String) -> Unit
    ): Float {
        val card = inlineCard(parent, width, y, 60f)

        Text(label, TEXT_PRIMARY, 16f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)
        if (description != null) {
            Text(description, TEXT_MUTED, 11f, false)
                .setPositioning(18f, Pos.ParentPixels, 36f, Pos.ParentPixels)
                .childOf(card)
        }
        val input = TextInput(initialValue = initial, fontSize = 13f)
            .setSizing(220f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-16f, 0f)
            .backgroundColor(alpha(TRACK))
            .borderColor(edge(CARD_BORDER))
            .borderRadius(8f)
            .borderThickness(EDGE_WIDTH)
            .childOf(card)
        input.onValueChange { value -> onChange(value as String) }
        return y + 72f
    }

    /** A clickable row that opens something (a mover screen, an external GUI). */
    fun inlineLink(
        parent: Rectangle,
        width: Float,
        y: Float,
        label: String,
        description: String?,
        onOpen: () -> Unit
    ): Float {
        val card = inlineCard(parent, width, y, 60f, hover = true)

        Text(label, TEXT_PRIMARY, 16f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)
        if (description != null) {
            Text(description, TEXT_MUTED, 11f, false)
                .setPositioning(18f, Pos.ParentPixels, 36f, Pos.ParentPixels)
                .childOf(card)
        }
        Text("›", TEXT_DIM, 24f, false)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-18f, 0f)
            .childOf(card)

        card.onClick { _ ->
            onOpen()
            true
        }
        return y + 72f
    }

    /** A clickable option row that cycles through a small fixed set of values. */
    fun inlineChoice(
        parent: Rectangle,
        width: Float,
        y: Float,
        label: String,
        description: String?,
        accent: Int,
        get: () -> String,
        next: () -> Unit
    ): Float {
        val card = inlineCard(parent, width, y, 60f, hover = true)

        Text(label, TEXT_PRIMARY, 16f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)
        if (description != null) {
            Text(description, TEXT_MUTED, 11f, false)
                .setPositioning(18f, Pos.ParentPixels, 36f, Pos.ParentPixels)
                .childOf(card)
        }
        val valueText = Text(get(), accent, 13f, true)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-18f, 0f)
            .childOf(card)

        card.onClick { _ ->
            next()
            valueText.text = get()
            true
        }
        return y + 72f
    }

    /** Popup selector for a small fixed set of values. */
    fun inlineDropdown(
        parent: Rectangle,
        width: Float,
        y: Float,
        label: String,
        description: String?,
        accent: Int,
        options: List<String>,
        selectedIndex: Int,
        onChange: (Int) -> Unit
    ): Float {
        val card = inlineCard(parent, width, y, 60f)

        Text(label, TEXT_PRIMARY, 16f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)
        if (description != null) {
            Text(description, TEXT_MUTED, 11f, false)
                .setPositioning(18f, Pos.ParentPixels, 36f, Pos.ParentPixels)
                .childOf(card)
        }

        val dropdown = Dropdown(
            options = options,
            selectedIndex = selectedIndex.coerceIn(0, options.lastIndex),
            backgroundColor = alpha(TRACK),
            iconColor = accent,
            borderColor = edge(accent, 0.82f),
            borderRadius = 9f,
            borderThickness = EDGE_WIDTH,
            padding = floatArrayOf(10f, 6f, 10f, 6f),
            hoverColor = alpha(CARD_HOVER),
            pressedColor = alpha(NAV_SELECTED)
        )
            .fontSize(13f)
            .setSizing(190f, Size.Pixels, 34f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-16f, 0f)
            .childOf(card)
        val dropdownEdge = edge(accent, 0.82f)
        dropdown.setBorderGradient(dropdownEdge, dropdownEdge)
        dropdown.onValueChange { value -> onChange(value as Int) }
        return y + 72f
    }

    /** Small section label between control groups. */
    fun inlineSectionHeader(parent: Rectangle, y: Float, title: String): Float {
        Text(title.uppercase(), TEXT_MUTED, 11f, true)
            .setPositioning(4f, Pos.ParentPixels, y + 8f, Pos.ParentPixels)
            .childOf(parent)
        return y + 32f
    }

    private data class ParsedColor(val red: Int, val green: Int, val blue: Int, val alpha: Int?)

    private fun parseColor(value: String, allowAlpha: Boolean): ParsedColor? {
        val raw = value.trim()
        val argb = raw.startsWith("0x", ignoreCase = true)
        var hex = when {
            argb -> raw.substring(2)
            raw.startsWith("#") -> raw.substring(1)
            else -> raw
        }

        if (hex.length == 3 || hex.length == 4 && allowAlpha) {
            hex = buildString(hex.length * 2) {
                hex.forEach { append(it).append(it) }
            }
        }
        if (hex.length != 6 && (!allowAlpha || hex.length != 8)) return null

        return try {
            if (hex.length == 8 && argb) {
                ParsedColor(
                    hex.substring(2, 4).toInt(16),
                    hex.substring(4, 6).toInt(16),
                    hex.substring(6, 8).toInt(16),
                    hex.substring(0, 2).toInt(16)
                )
            } else {
                ParsedColor(
                    hex.substring(0, 2).toInt(16),
                    hex.substring(2, 4).toInt(16),
                    hex.substring(4, 6).toInt(16),
                    if (hex.length == 8) hex.substring(6, 8).toInt(16) else null
                )
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** Full color card: popup HSV picker, hex/ARGB input, and separated channel rows. */
    fun inlineColor(
        parent: Rectangle,
        width: Float,
        y: Float,
        title: String,
        getColor: () -> FloatArray,
        setColor: (Float, Float, Float) -> Unit,
        getAlpha: () -> Float,
        setAlpha: (Float) -> Unit,
        showAlpha: Boolean = true,
        getHex: (() -> String)? = null,
        setHex: ((String) -> Boolean)? = null
    ): Float {
        val channels = if (showAlpha) listOf("Red", "Green", "Blue", "Alpha") else listOf("Red", "Green", "Blue")
        val cardHeight = 54f + channels.size * 34f
        val card = inlineCard(parent, width, y, cardHeight)

        Text(title, TEXT_PRIMARY, 15f, true)
            .setPositioning(18f, Pos.ParentPixels, 14f, Pos.ParentPixels)
            .childOf(card)

        fun currentColor(): Color {
            val c = getColor()
            return Color(
                (c[0] * 255f).roundToInt().coerceIn(0, 255),
                (c[1] * 255f).roundToInt().coerceIn(0, 255),
                (c[2] * 255f).roundToInt().coerceIn(0, 255),
                if (showAlpha) (getAlpha() * 255f).roundToInt().coerceIn(0, 255) else 255
            )
        }

        fun formattedColor(): String {
            val color = currentColor()
            return if (showAlpha) {
                String.format("0x%02X%02X%02X%02X", color.alpha, color.red, color.green, color.blue)
            } else {
                String.format("#%02X%02X%02X", color.red, color.green, color.blue)
            }
        }

        val readHex = getHex ?: ::formattedColor
        val picker = ColorPicker(
            initialColor = currentColor(),
            backgroundColor = alpha(PANEL_BG),
            borderColor = edge(CARD_BORDER),
            borderRadius = 7f,
            borderThickness = EDGE_WIDTH,
            hoverColor = alpha(CARD_HOVER),
            pressedColor = alpha(NAV_SELECTED)
        )
            .setSizing(42f, Size.Pixels, 28f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 9f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-176f, 0f)
            .childOf(card)

        val hexInput = TextInput(
            initialValue = readHex(),
            placeholder = if (showAlpha) "0xAARRGGBB" else "#RRGGBB",
            fontSize = 12f
        )
            .setSizing(146f, Size.Pixels, 28f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 9f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-18f, 0f)
            .backgroundColor(alpha(TRACK))
            .borderColor(edge(CARD_BORDER))
            .borderRadius(7f)
            .borderThickness(EDGE_WIDTH)
            .childOf(card)

        var syncing = false
        val channelInputs = mutableListOf<TextInput>()
        val channelSliders = mutableListOf<Slider>()

        fun syncControls(updateHex: Boolean = true, skipChannelInput: Int = -1) {
            syncing = true
            val color = currentColor()
            val values = intArrayOf(color.red, color.green, color.blue, color.alpha)
            picker.setColor(color, silent = true)
            picker.pickerPanel?.let { panel ->
                val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
                panel.currentHue = hsb[0]
                panel.currentSaturation = hsb[1]
                panel.currentBrightness = hsb[2]
                panel.currentAlpha = color.alpha / 255f
                panel.currentColor = color
                panel.pickerArea.currentHue = hsb[0]
                panel.alphaSlider.currentColor = Color(color.red, color.green, color.blue)
            }
            channelInputs.forEachIndexed { index, input ->
                if (index != skipChannelInput) input.value = values[index].toString()
                input.background.borderColor = edge(CARD_BORDER)
            }
            channelSliders.forEachIndexed { index, slider ->
                slider.setValue(values[index].toFloat(), animated = false, silent = true)
            }
            if (updateHex) hexInput.value = readHex()
            hexInput.background.borderColor = edge(CARD_BORDER)
            syncing = false
        }

        picker.onValueChange { value ->
            if (!syncing) {
                val color = value as Color
                setColor(color.red / 255f, color.green / 255f, color.blue / 255f)
                if (showAlpha) setAlpha(color.alpha / 255f)
                syncControls()
            }
        }

        hexInput.onValueChange { value ->
            if (!syncing) {
                val text = value as String
                val parsed = parseColor(text, showAlpha)
                val valid = if (getHex != null && setHex != null) {
                    setHex(text)
                } else if (parsed != null) {
                    setColor(parsed.red / 255f, parsed.green / 255f, parsed.blue / 255f)
                    if (showAlpha && parsed.alpha != null) setAlpha(parsed.alpha / 255f)
                    true
                } else {
                    false
                }
                hexInput.background.borderColor = edge(if (valid) CARD_BORDER else RED)
                if (valid) syncControls(updateHex = false)
            }
        }

        val sliderX = 154f
        val sliderWidth = width - sliderX - 18f
        channels.forEachIndexed { i, label ->
            val rowY = 52f + i * 34f
            Text(label, TEXT_MUTED, 12f, false)
                .setPositioning(18f, Pos.ParentPixels, rowY + 7f, Pos.ParentPixels)
                .childOf(card)

            val initial = if (i == 3) {
                (getAlpha() * 255f).roundToInt()
            } else {
                (getColor()[i] * 255f).roundToInt()
            }.coerceIn(0, 255)

            val input = TextInput(initialValue = initial.toString(), fontSize = 12f)
                .setSizing(62f, Size.Pixels, 28f, Size.Pixels)
                .setPositioning(72f, Pos.ParentPixels, rowY, Pos.ParentPixels)
                .backgroundColor(alpha(TRACK))
                .borderColor(edge(CARD_BORDER))
                .borderRadius(7f)
                .borderThickness(EDGE_WIDTH)
                .childOf(card)
            channelInputs += input

            val slider = Slider(
                value = initial.toFloat(), minValue = 0f, maxValue = 255f, step = 1f,
                trackColor = alpha(TRACK),
                trackFillColor = when (i) {
                    0 -> RED; 1 -> GREEN; 2 -> BLUE; else -> TEXT_SECONDARY
                },
                thumbColor = TEXT_PRIMARY,
                trackHeight = 4f, thumbWidth = 12f, thumbHeight = 12f, thumbRadius = 6f
            )
                .setSizing(sliderWidth, Size.Pixels, 22f, Size.Pixels)
                .setPositioning(sliderX, Pos.ParentPixels, rowY + 3f, Pos.ParentPixels)
                .childOf(card)
            channelSliders += slider

            input.onValueChange { value ->
                if (!syncing) {
                    val channel = (value as String).trim().toIntOrNull()
                    val valid = channel != null && channel in 0..255
                    input.background.borderColor = edge(if (valid) CARD_BORDER else RED)
                    if (valid) {
                        val color = currentColor()
                        val values = intArrayOf(color.red, color.green, color.blue, color.alpha)
                        values[i] = channel
                        setColor(values[0] / 255f, values[1] / 255f, values[2] / 255f)
                        if (i == 3) setAlpha(values[3] / 255f)
                        syncControls(skipChannelInput = i)
                    }
                }
            }
            slider.onValueChange { value ->
                if (!syncing) {
                    val channel = (value as Float).roundToInt().coerceIn(0, 255)
                    val color = currentColor()
                    val values = intArrayOf(color.red, color.green, color.blue, color.alpha)
                    values[i] = channel
                    setColor(values[0] / 255f, values[1] / 255f, values[2] / 255f)
                    if (i == 3) setAlpha(values[3] / 255f)
                    syncControls()
                }
            }
        }
        return y + cardHeight + 12f
    }

    fun inlineRgb(
        parent: Rectangle,
        width: Float,
        y: Float,
        title: String,
        getColor: () -> FloatArray,
        setColor: (Float, Float, Float) -> Unit,
        getHex: (() -> String)? = null,
        setHex: ((String) -> Boolean)? = null
    ): Float = inlineColor(parent, width, y, title, getColor, setColor,
        { 1f }, { _ -> }, false, getHex, setHex)
}
