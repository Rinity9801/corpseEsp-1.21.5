package forfun.miningqol.client.gui

import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Slider
import xyz.meowing.vexel.elements.TextInput

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

    /** An empty card in the detail column — base for custom rows. */
    fun inlineCard(parent: Rectangle, width: Float, y: Float, height: Float, hover: Boolean = false): Rectangle =
        Rectangle(
            backgroundColor = alpha(CARD_BG),
            borderColor = CARD_BORDER,
            borderRadius = 12f,
            borderThickness = 1f,
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
        card.borderColor = if (enabled) accent else CARD_BORDER

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
            card.borderColor = if (n) accent else CARD_BORDER
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
            .borderColor(CARD_BORDER)
            .borderRadius(8f)
            .borderThickness(1f)
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

    /** Small section label between control groups. */
    fun inlineSectionHeader(parent: Rectangle, y: Float, title: String): Float {
        Text(title.uppercase(), TEXT_MUTED, 11f, true)
            .setPositioning(4f, Pos.ParentPixels, y + 8f, Pos.ParentPixels)
            .childOf(parent)
        return y + 32f
    }

    /** An RGBA slider card with a live preview swatch. */
    fun inlineColor(
        parent: Rectangle,
        width: Float,
        y: Float,
        title: String,
        getColor: () -> FloatArray,
        setColor: (Float, Float, Float) -> Unit,
        getAlpha: () -> Float,
        setAlpha: (Float) -> Unit
    ): Float {
        val card = inlineCard(parent, width, y, 100f)

        Text(title, TEXT_PRIMARY, 15f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)

        fun swatchColor(): Int {
            val c = getColor()
            return (0xFF shl 24) or
                ((c[0] * 255).toInt().coerceIn(0, 255) shl 16) or
                ((c[1] * 255).toInt().coerceIn(0, 255) shl 8) or
                (c[2] * 255).toInt().coerceIn(0, 255)
        }

        val swatch = Rectangle(backgroundColor = swatchColor(), borderColor = CARD_BORDER,
            borderRadius = 6f, borderThickness = 1f)
            .setSizing(36f, Size.Pixels, 22f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-18f, 0f)
            .ignoreMouseEvents()
            .childOf(card)

        val channels = listOf("R", "G", "B", "A")
        val gap = 26f
        val sliderWidth = (width - 36f - 3 * gap) / 4f - 14f
        channels.forEachIndexed { i, label ->
            val x = 18f + i * (sliderWidth + 14f + gap)
            Text(label, TEXT_MUTED, 12f, false)
                .setPositioning(x, Pos.ParentPixels, 56f, Pos.ParentPixels)
                .childOf(card)

            val initial = if (i == 3) getAlpha() else getColor()[i]
            val slider = Slider(
                value = initial, minValue = 0f, maxValue = 1f, step = null,
                trackColor = alpha(TRACK),
                trackFillColor = when (i) {
                    0 -> RED; 1 -> GREEN; 2 -> BLUE; else -> TEXT_SECONDARY
                },
                thumbColor = TEXT_PRIMARY,
                trackHeight = 4f, thumbWidth = 12f, thumbHeight = 12f, thumbRadius = 6f
            )
                .setSizing(sliderWidth, Size.Pixels, 18f, Size.Pixels)
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
        return y + 112f
    }
}
