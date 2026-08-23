package forfun.miningqol.client.gui

import xyz.meowing.vexel.Vexel.renderer
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
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
/** One coloured run of text inside a HUD preview line. [color] is 0xRRGGBB. */
data class HudSegment(val text: String, val color: Int)

/** A single previewed HUD state: a muted caption plus the line the HUD would print. */
data class HudRow(val caption: String, val segments: List<HudSegment>)

/**
 * Live HUD preview. Rows are pulled every frame rather than snapshotted at build time,
 * so colour edits and the toggles that change what the HUD prints show up immediately
 * instead of only after closing the settings screen.
 */
class HudPreviewSurface(private val rows: () -> List<HudRow>) : VexelElement<HudPreviewSurface>() {
    override fun onRender(mouseX: Float, mouseY: Float) {
        renderer.rect(x, y, width, height, SettingsUi.alpha(0xFF0A0A0B.toInt()), 8f)
        renderer.hollowRect(x, y, width, height, 1f, SettingsUi.edge(SettingsUi.CARD_BORDER), 8f)

        var lineY = y + 12f
        for (row in rows()) {
            renderer.text(row.caption, x + 14f, lineY + 1f, 10f, SettingsUi.alpha(SettingsUi.TEXT_DIM))
            var lineX = x + 118f
            for (segment in row.segments) {
                renderer.shadowedText(segment.text, lineX, lineY, 13f, segment.color or (0xFF shl 24))
                lineX += renderer.textWidth(segment.text, 13f)
            }
            lineY += 22f
        }
    }
}

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
            .setPositioning(-14f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .alignRight()
            .setOffset(0f, 14f)
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
            .setPositioning(-18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .alignRight()
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
            .setPositioning(-16f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
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
            .setPositioning(-18f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
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
            .setPositioning(-18f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
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
            .setPositioning(-16f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
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

    /** Packs a HUD's 0..1 colour triple into the 0xRRGGBB the preview draws with. */
    fun rgbOf(color: FloatArray): Int =
        ((color[0] * 255f).roundToInt().coerceIn(0, 255) shl 16) or
        ((color[1] * 255f).roundToInt().coerceIn(0, 255) shl 8) or
        (color[2] * 255f).roundToInt().coerceIn(0, 255)

    /**
     * Compact colour row: a live preview swatch on the right that opens the
     * MoulConfig-style [ColorEditor] modal when the row is clicked.
     */
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
        description: String? = null,
        previewRowCount: Int = 0,
        preview: (() -> List<HudRow>)? = null
    ): Float {
        val card = inlineCard(parent, width, y, 60f, hover = true)

        Text(title, TEXT_PRIMARY, 16f, true)
            .setPositioning(18f, Pos.ParentPixels, if (description == null) 0f else 12f,
                if (description == null) Pos.ParentCenter else Pos.ParentPixels)
            .childOf(card)
        if (description != null) {
            Text(description, TEXT_MUTED, 11f, false)
                .setPositioning(18f, Pos.ParentPixels, 36f, Pos.ParentPixels)
                .childOf(card)
        }

        fun current(): Color {
            val c = getColor()
            return Color(
                (c[0] * 255f).roundToInt().coerceIn(0, 255),
                (c[1] * 255f).roundToInt().coerceIn(0, 255),
                (c[2] * 255f).roundToInt().coerceIn(0, 255),
                if (showAlpha) (getAlpha() * 255f).roundToInt().coerceIn(0, 255) else 255
            )
        }

        val swatch = ColorSwatch(current().rgb)
            .setSizing(44f, Size.Pixels, 22f, Size.Pixels)
            .setPositioning(-18f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .ignoreMouseEvents()
            .childOf(card)

        card.onClick { _ ->
            ColorEditor.open(card, title, current(), showAlpha, previewRowCount, preview) { color ->
                setColor(color.red / 255f, color.green / 255f, color.blue / 255f)
                if (showAlpha) setAlpha(color.alpha / 255f)
                swatch.color = color.rgb
            }
            true
        }
        return y + 72f
    }

    fun inlineRgb(
        parent: Rectangle,
        width: Float,
        y: Float,
        title: String,
        getColor: () -> FloatArray,
        setColor: (Float, Float, Float) -> Unit,
        description: String? = null,
        previewRowCount: Int = 0,
        preview: (() -> List<HudRow>)? = null
    ): Float = inlineColor(parent, width, y, title, getColor, setColor,
        { 1f }, { _ -> }, false, description, previewRowCount, preview)
}
