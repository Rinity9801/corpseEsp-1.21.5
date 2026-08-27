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

/**
 * A full GUI palette. Surfaces set the mood, accents colour the per-feature highlights.
 */
class GuiTheme(
    val name: String,
    val panelBg: Int,
    val panelBorder: Int,
    val cardBg: Int,
    val cardBorder: Int,
    val cardHover: Int,
    val navSelected: Int,
    val navHover: Int,
    val track: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textMuted: Int,
    val textDim: Int,
    val blue: Int,
    val green: Int,
    val cyan: Int,
    val purple: Int,
    val purple2: Int,
    val sky: Int,
    val yellow: Int,
    val orange: Int,
    val red: Int,
    val teal: Int
)

/** Panel proportions. The grid column count is what really changes the feel. */
class GuiLayout(
    val name: String,
    val sidebarWidth: Float,
    val mainWidth: Float,
    val panelHeight: Float,
    val gridColumns: Int
)

object SettingsUi {
    // Prisma palette
    var PANEL_BG = 0xFF0D0D0F.toInt()
    var PANEL_BORDER = 0xFF2A2A2E.toInt()
    var CARD_BG = 0xFF161618.toInt()
    var CARD_BORDER = 0xFF252528.toInt()
    var CARD_HOVER = 0xFF1E1E20.toInt()
    var NAV_SELECTED = 0xFF1E1E20.toInt()
    var NAV_HOVER = 0xFF151517.toInt()
    var TRACK = 0xFF1A1A1A.toInt()
    var TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    var TEXT_SECONDARY = 0xFFBBBBBB.toInt()
    var TEXT_MUTED = 0xFF888888.toInt()
    var TEXT_DIM = 0xFF606060.toInt()
    val EDGE_WIDTH = 0.5f

    // Category/feature accents
    var BLUE = 0xFF7AA2F7.toInt()
    var GREEN = 0xFF9ECE6A.toInt()
    var CYAN = 0xFF7DCFFF.toInt()
    var PURPLE = 0xFFBB9AF7.toInt()
    var PURPLE2 = 0xFF9D7CD8.toInt()
    var SKY = 0xFF89DDFF.toInt()
    var YELLOW = 0xFFE0AF68.toInt()
    var ORANGE = 0xFFFF9E64.toInt()
    var RED = 0xFFF7768E.toInt()
    var TEAL = 0xFF73DACA.toInt()

    val THEMES = listOf(
        GuiTheme(
            "Prisma", 0xFF0D0D0F.toInt(), 0xFF2A2A2E.toInt(), 0xFF161618.toInt(), 0xFF252528.toInt(),
            0xFF1E1E20.toInt(), 0xFF1E1E20.toInt(), 0xFF151517.toInt(), 0xFF1A1A1A.toInt(),
            0xFFFFFFFF.toInt(), 0xFFBBBBBB.toInt(), 0xFF888888.toInt(), 0xFF606060.toInt(),
            0xFF7AA2F7.toInt(), 0xFF9ECE6A.toInt(), 0xFF7DCFFF.toInt(), 0xFFBB9AF7.toInt(),
            0xFF9D7CD8.toInt(), 0xFF89DDFF.toInt(), 0xFFE0AF68.toInt(), 0xFFFF9E64.toInt(),
            0xFFF7768E.toInt(), 0xFF73DACA.toInt()
        ),
        GuiTheme(
            "Tokyo Night", 0xFF1A1B26.toInt(), 0xFF3B4261.toInt(), 0xFF24283B.toInt(), 0xFF32384F.toInt(),
            0xFF2F3549.toInt(), 0xFF343A55.toInt(), 0xFF232739.toInt(), 0xFF1F2335.toInt(),
            0xFFC0CAF5.toInt(), 0xFFA9B1D6.toInt(), 0xFF787C99.toInt(), 0xFF565F89.toInt(),
            0xFF7AA2F7.toInt(), 0xFF9ECE6A.toInt(), 0xFF7DCFFF.toInt(), 0xFFBB9AF7.toInt(),
            0xFF9D7CD8.toInt(), 0xFF89DDFF.toInt(), 0xFFE0AF68.toInt(), 0xFFFF9E64.toInt(),
            0xFFF7768E.toInt(), 0xFF73DACA.toInt()
        ),
        GuiTheme(
            "Catppuccin", 0xFF1E1E2E.toInt(), 0xFF45475A.toInt(), 0xFF313244.toInt(), 0xFF45475A.toInt(),
            0xFF3B3D52.toInt(), 0xFF414359.toInt(), 0xFF292A3B.toInt(), 0xFF272738.toInt(),
            0xFFCDD6F4.toInt(), 0xFFBAC2DE.toInt(), 0xFFA6ADC8.toInt(), 0xFF7F849C.toInt(),
            0xFF89B4FA.toInt(), 0xFFA6E3A1.toInt(), 0xFF89DCEB.toInt(), 0xFFCBA6F7.toInt(),
            0xFFB4A0E8.toInt(), 0xFF74C7EC.toInt(), 0xFFF9E2AF.toInt(), 0xFFFAB387.toInt(),
            0xFFF38BA8.toInt(), 0xFF94E2D5.toInt()
        ),
        GuiTheme(
            "Nord", 0xFF2E3440.toInt(), 0xFF4C566A.toInt(), 0xFF3B4252.toInt(), 0xFF4C566A.toInt(),
            0xFF434C5E.toInt(), 0xFF474F63.toInt(), 0xFF353C4A.toInt(), 0xFF353B48.toInt(),
            0xFFECEFF4.toInt(), 0xFFD8DEE9.toInt(), 0xFF9BA6BA.toInt(), 0xFF6C7A92.toInt(),
            0xFF81A1C1.toInt(), 0xFFA3BE8C.toInt(), 0xFF88C0D0.toInt(), 0xFFB48EAD.toInt(),
            0xFFA07CA0.toInt(), 0xFF8FBCBB.toInt(), 0xFFEBCB8B.toInt(), 0xFFD08770.toInt(),
            0xFFBF616A.toInt(), 0xFF8FBCBB.toInt()
        ),
        GuiTheme(
            "Gruvbox", 0xFF282828.toInt(), 0xFF504945.toInt(), 0xFF3C3836.toInt(), 0xFF504945.toInt(),
            0xFF464140.toInt(), 0xFF4A443F.toInt(), 0xFF32302F.toInt(), 0xFF32302F.toInt(),
            0xFFEBDBB2.toInt(), 0xFFD5C4A1.toInt(), 0xFFA89984.toInt(), 0xFF7C6F64.toInt(),
            0xFF83A598.toInt(), 0xFFB8BB26.toInt(), 0xFF8EC07C.toInt(), 0xFFD3869B.toInt(),
            0xFFC08497.toInt(), 0xFF83A598.toInt(), 0xFFFABD2F.toInt(), 0xFFFE8019.toInt(),
            0xFFFB4934.toInt(), 0xFF8EC07C.toInt()
        ),
        GuiTheme(
            "Rose Pine", 0xFF191724.toInt(), 0xFF403D52.toInt(), 0xFF1F1D2E.toInt(), 0xFF34314A.toInt(),
            0xFF26233A.toInt(), 0xFF2A2740.toInt(), 0xFF1D1B2A.toInt(), 0xFF21202E.toInt(),
            0xFFE0DEF4.toInt(), 0xFFC8C4DE.toInt(), 0xFF908CAA.toInt(), 0xFF6E6A86.toInt(),
            0xFF9CCFD8.toInt(), 0xFF95B1AC.toInt(), 0xFF9CCFD8.toInt(), 0xFFC4A7E7.toInt(),
            0xFFB39DDB.toInt(), 0xFF31748F.toInt(), 0xFFF6C177.toInt(), 0xFFEA9A97.toInt(),
            0xFFEB6F92.toInt(), 0xFF9CCFD8.toInt()
        )
    )

    val LAYOUTS = listOf(
        GuiLayout("Comfortable", 210f, 844f, 700f, 3),
        GuiLayout("Compact", 190f, 640f, 620f, 2),
        GuiLayout("Tall", 180f, 500f, 740f, 2)
    )

    var themeIndex = 0
        private set
    var layoutIndex = 0
        private set

    val layout: GuiLayout get() = LAYOUTS[layoutIndex.coerceIn(0, LAYOUTS.lastIndex)]

    fun applyTheme(index: Int) {
        themeIndex = index.coerceIn(0, THEMES.lastIndex)
        val t = THEMES[themeIndex]
        PANEL_BG = t.panelBg
        PANEL_BORDER = t.panelBorder
        CARD_BG = t.cardBg
        CARD_BORDER = t.cardBorder
        CARD_HOVER = t.cardHover
        NAV_SELECTED = t.navSelected
        NAV_HOVER = t.navHover
        TRACK = t.track
        TEXT_PRIMARY = t.textPrimary
        TEXT_SECONDARY = t.textSecondary
        TEXT_MUTED = t.textMuted
        TEXT_DIM = t.textDim
        BLUE = t.blue
        GREEN = t.green
        CYAN = t.cyan
        PURPLE = t.purple
        PURPLE2 = t.purple2
        SKY = t.sky
        YELLOW = t.yellow
        ORANGE = t.orange
        RED = t.red
        TEAL = t.teal
    }

    fun setLayout(index: Int) {
        layoutIndex = index.coerceIn(0, LAYOUTS.lastIndex)
    }

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

        // The readout is a text field: type a value (decimals welcome) and it takes effect as
        // you type when it is in range; leaving the field clamps it and tidies the text. The
        // slider's step only applies to dragging, so a typed 1.234 is kept as 1.234.
        var current = initial
        var syncing = false
        // Only the number is in the field; the words the format wraps around it ("up to … for
        // the tab list", "slot …") sit either side as plain labels.
        val parts = valueParts(initial, format)
        val suffix = Text(parts[2], TEXT_SECONDARY, 13f, false)
            .setPositioning(-16f, Pos.ParentPixels, 14f, Pos.ParentPixels)
            .alignRight()
            .childOf(card)
        val suffixW = if (parts[2].isEmpty()) 0f else textWidth(parts[2], 13f) + 6f
        val input = TextInput(initialValue = parts[1], fontSize = 13f)
            .setSizing(64f, Size.Pixels, 26f, Size.Pixels)
            .setPositioning(-16f - suffixW, Pos.ParentPixels, 8f, Pos.ParentPixels)
            .alignRight()
            .backgroundColor(alpha(TRACK))
            .borderColor(edge(CARD_BORDER))
            .borderRadius(7f)
            .borderThickness(EDGE_WIDTH)
            .childOf(card)
        val prefix = Text(parts[0], TEXT_SECONDARY, 13f, false)
            .setPositioning(-16f - suffixW - 64f - 6f, Pos.ParentPixels, 14f, Pos.ParentPixels)
            .alignRight()
            .childOf(card)
        fun display(v: Float): String {
            val p = valueParts(v, format)
            prefix.text = p[0]
            suffix.text = p[2]
            return p[1]
        }

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
            current = v
            if (!input.isFocused && !syncing) {
                syncing = true
                input.value = display(v)
                syncing = false
            }
            onChange(v)
        }
        // A whole-number step means the setting behind the slider is an integer: a typed 1.5
        // would be shown, then silently lost on the next open. Fractional or no step keeps
        // the typed value exactly.
        fun snap(v: Float): Float =
            if (step != null && step >= 1f) min + Math.round((v - min) / step) * step else v
        input.onValueChange { text ->
            if (syncing) return@onValueChange
            val typed = parseNumber(text as String) ?: return@onValueChange
            if (typed < min || typed > max) return@onValueChange
            val v = snap(typed)
            current = v
            syncing = true
            slider.setValue(v, animated = false, silent = true)
            syncing = false
            onChange(v)
        }
        numberFields += NumberField(input) {
            val v = snap((parseNumber(input.value) ?: current).coerceIn(min, max))
            current = v
            syncing = true
            slider.setValue(v, animated = false, silent = true)
            input.value = display(v)
            syncing = false
            onChange(v)
        }
        return y + 87f
    }

    // ---- editable slider readouts ------------------------------------------------------

    private class NumberField(val input: TextInput, val commit: () -> Unit) {
        var wasFocused = false
    }

    private val numberFields = ArrayList<NumberField>()
    private val NUMBER = Regex("-?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)")

    /** Call when the element tree is torn down, so fields from the old tree are forgotten. */
    fun clearNumberFields() {
        numberFields.clear()
    }

    /**
     * Once a tick from the screen: a field that has just lost focus (Enter, Escape, or a click
     * elsewhere — all handled inside Vexel with no callback) commits and tidies its text.
     */
    fun pollNumberFields() {
        for (field in numberFields) {
            val focused = field.input.isFocused
            if (field.wasFocused && !focused) field.commit()
            field.wasFocused = focused
        }
    }

    private fun parseNumber(text: String): Float? {
        val match = NUMBER.find(text) ?: return null
        return match.value.replace(',', '.').toFloatOrNull()
    }

    /**
     * {prefix, number, suffix}: the caller's formatted text split around its number, with the
     * number at full typed precision. A format with no number at all is shown as the suffix.
     */
    private fun valueParts(v: Float, format: (Float) -> String): Array<String> {
        val formatted = format(v)
        val numberIn = NUMBER.find(formatted) ?: return arrayOf("", plainNumber(v), formatted)
        return arrayOf(
            formatted.substring(0, numberIn.range.first).trim(),
            plainNumber(v),
            formatted.substring(numberIn.range.last + 1).trim()
        )
    }

    private fun plainNumber(v: Float): String =
        if (v == v.toLong().toFloat()) v.toLong().toString()
        else String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')

    private fun textWidth(text: String, size: Float): Float =
        xyz.meowing.vexel.Vexel.renderer.textWidth(text, size, xyz.meowing.vexel.Vexel.defaultFont)

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
