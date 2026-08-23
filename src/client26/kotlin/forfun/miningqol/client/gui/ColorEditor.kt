package forfun.miningqol.client.gui

import xyz.meowing.vexel.Vexel.renderer
import xyz.meowing.vexel.api.style.Gradient
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.TextInput
import java.awt.Color
import kotlin.math.roundToInt

/**
 * MoulConfig-shaped colour editor drawn in the Prisma palette: a dimmed modal
 * over the settings GUI with a saturation/brightness square, vertical hue and
 * alpha bars, a live preview and hex/channel inputs.
 *
 * Only one editor is open at a time; it attaches to the scaled UI root so it
 * covers (and swallows input from) everything behind it. Edits apply live.
 */
object ColorEditor {
    private var current: ColorEditorOverlay? = null

    fun isOpen(): Boolean = current != null

    /**
     * Opens the editor above [anchor]'s root element.
     *
     * @param onChange fired on every edit with the live colour (alpha is 255 when
     *   [showAlpha] is false).
     */
    fun open(
        anchor: VexelElement<*>,
        title: String,
        initial: Color,
        showAlpha: Boolean,
        previewRowCount: Int = 0,
        preview: (() -> List<HudRow>)? = null,
        onChange: (Color) -> Unit
    ) {
        close()
        val root = anchor.getRootElement()
        current = ColorEditorOverlay(title, initial, showAlpha, previewRowCount, preview, onChange)
            .setSizing(100f, Size.Percent, 100f, Size.Percent)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(root)
    }

    /** Closes the open editor; returns whether there was one. */
    fun close(): Boolean {
        val overlay = current ?: return false
        current = null
        (overlay.parent as? VexelElement<*>)?.children?.remove(overlay)
        overlay.destroy()
        return true
    }

    /** Drops the reference without touching the tree — for a wholesale GUI rebuild. */
    fun forget() {
        current = null
    }
}

/**
 * Small preview chip for a colour row: a muted base so transparency reads,
 * the colour on top, and a hairline border.
 */
class ColorSwatch(var color: Int) : VexelElement<ColorSwatch>() {
    override fun onRender(mouseX: Float, mouseY: Float) {
        renderer.rect(x, y, width, height, 0xFF303034.toInt(), 6f)
        renderer.rect(x, y, width, height, color, 6f)
        renderer.hollowRect(x, y, width, height, 1f, SettingsUi.edge(SettingsUi.CARD_BORDER), 6f)
    }
}

class ColorEditorOverlay(
    title: String,
    private val initial: Color,
    private val showAlpha: Boolean,
    previewRowCount: Int,
    private val previewRows: (() -> List<HudRow>)?,
    private val onChange: (Color) -> Unit
) : VexelElement<ColorEditorOverlay>() {

    private var hue: Float
    private var saturation: Float
    private var brightness: Float
    private var opacity: Float = if (showAlpha) initial.alpha / 255f else 1f

    private var draggingSv = false
    private var draggingHue = false
    private var draggingAlpha = false
    private var syncing = false

    // The preview strip sits between the title and the picker, so everything below it
    // shifts down by its height and the panel grows to match.
    private val previewHeight = if (previewRows == null) 0f else previewRowCount * 22f + 10f
    private val previewOffset = if (previewRows == null) 0f else previewHeight + 14f

    private val panelWidth = 460f
    private val panelHeight = 306f + previewOffset
    private val areaSize = 200f
    private val barWidth = 20f
    private val svX = 20f
    private val hueX = 232f
    private val alphaX = 262f
    private val columnX = 300f
    private val columnWidth = 140f
    private val topY = 52f + previewOffset

    private val panel: Rectangle
    private val svArea: SvArea
    private val hueBar: HueBar
    private val alphaBar: AlphaBar
    private val preview: Swatch
    private val hexInput: TextInput
    private val channelInputs = mutableListOf<TextInput>()
    private val channels = if (showAlpha) listOf("R", "G", "B", "A") else listOf("R", "G", "B")

    init {
        val hsb = Color.RGBtoHSB(initial.red, initial.green, initial.blue, null)
        hue = hsb[0]
        saturation = hsb[1]
        brightness = hsb[2]

        panel = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.PANEL_BG),
            borderColor = SettingsUi.edge(SettingsUi.PANEL_BORDER),
            borderRadius = 16f,
            borderThickness = SettingsUi.EDGE_WIDTH
        )
            .setSizing(panelWidth, Size.Pixels, panelHeight, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
            .childOf(this)
            .apply {
                dropShadow = true
                shadowBlur = 48f
                shadowSpread = 2f
                shadowColor = 0xC0000000.toInt()
            }

        Text(title, SettingsUi.TEXT_PRIMARY, 16f, true)
            .setPositioning(20f, Pos.ParentPixels, 18f, Pos.ParentPixels)
            .childOf(panel)

        val close = Rectangle(
            backgroundColor = 0x00000000,
            borderColor = 0x00000000,
            borderRadius = 8f,
            hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
        )
            .setSizing(26f, Size.Pixels, 26f, Size.Pixels)
            .setPositioning(-16f, Pos.ParentPixels, 14f, Pos.ParentPixels)
            .alignRight()
            .childOf(panel)
        Text("×", SettingsUi.TEXT_MUTED, 17f, false)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
            .childOf(close)
        close.onClick { _ ->
            ColorEditor.close()
            true
        }

        previewRows?.let { rows ->
            HudPreviewSurface(rows)
                .setSizing(panelWidth - 40f, Size.Pixels, previewHeight, Size.Pixels)
                .setPositioning(20f, Pos.ParentPixels, 46f, Pos.ParentPixels)
                .ignoreMouseEvents()
                .childOf(panel)
        }

        svArea = SvArea()
            .setSizing(areaSize, Size.Pixels, areaSize, Size.Pixels)
            .setPositioning(svX, Pos.ParentPixels, topY, Pos.ParentPixels)
            .childOf(panel)
        hueBar = HueBar()
            .setSizing(barWidth, Size.Pixels, areaSize, Size.Pixels)
            .setPositioning(hueX, Pos.ParentPixels, topY, Pos.ParentPixels)
            .childOf(panel)
        alphaBar = AlphaBar()
            .setSizing(barWidth, Size.Pixels, areaSize, Size.Pixels)
            .setPositioning(alphaX, Pos.ParentPixels, topY, Pos.ParentPixels)
            .childOf(panel)
        alphaBar.visible = showAlpha

        Text("BEFORE", SettingsUi.TEXT_DIM, 9f, false)
            .setPositioning(columnX + 4f, Pos.ParentPixels, topY - 12f, Pos.ParentPixels)
            .childOf(panel)
        Text("AFTER", SettingsUi.TEXT_DIM, 9f, false)
            .setPositioning(columnX + columnWidth / 2f + 7f, Pos.ParentPixels, topY - 12f, Pos.ParentPixels)
            .childOf(panel)
        preview = Swatch()
            .setSizing(columnWidth, Size.Pixels, 44f, Size.Pixels)
            .setPositioning(columnX, Pos.ParentPixels, topY, Pos.ParentPixels)
            .childOf(panel)

        hexInput = TextInput(initialValue = formatHex(), placeholder = hexPlaceholder(), fontSize = 12f)
            .setSizing(columnWidth, Size.Pixels, 28f, Size.Pixels)
            .setPositioning(columnX, Pos.ParentPixels, topY + 52f, Pos.ParentPixels)
            .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
            .borderColor(SettingsUi.edge(SettingsUi.CARD_BORDER))
            .borderRadius(7f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .childOf(panel)
        hexInput.onValueChange { value ->
            if (syncing) return@onValueChange
            val parsed = parseHex(value as String)
            hexInput.background.borderColor =
                SettingsUi.edge(if (parsed == null) SettingsUi.RED else SettingsUi.CARD_BORDER)
            if (parsed != null) {
                applyRgba(parsed.red, parsed.green, parsed.blue, parsed.alpha)
                sync(skipHex = true)
            }
        }

        channels.forEachIndexed { index, label ->
            val rowY = topY + 88f + index * 28f
            Text(label, SettingsUi.TEXT_MUTED, 12f, false)
                .setPositioning(columnX + 2f, Pos.ParentPixels, rowY + 6f, Pos.ParentPixels)
                .childOf(panel)

            val input = TextInput(initialValue = channelValue(index).toString(), fontSize = 12f)
                .setSizing(columnWidth - 18f, Size.Pixels, 24f, Size.Pixels)
                .setPositioning(columnX + 18f, Pos.ParentPixels, rowY, Pos.ParentPixels)
                .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
                .borderColor(SettingsUi.edge(SettingsUi.CARD_BORDER))
                .borderRadius(7f)
                .borderThickness(SettingsUi.EDGE_WIDTH)
                .childOf(panel)
            channelInputs += input

            input.onValueChange { value ->
                if (syncing) return@onValueChange
                val parsed = (value as String).trim().toIntOrNull()
                val valid = parsed != null && parsed in 0..255
                input.background.borderColor =
                    SettingsUi.edge(if (valid) SettingsUi.CARD_BORDER else SettingsUi.RED)
                if (valid) {
                    val values = intArrayOf(red(), green(), blue(), alphaByte())
                    values[index] = parsed!!
                    applyRgba(values[0], values[1], values[2], values[3])
                    sync(skipChannel = index)
                }
            }
        }

        button(alignRight = false, label = "Reset") {
            val hsbReset = Color.RGBtoHSB(initial.red, initial.green, initial.blue, null)
            hue = hsbReset[0]
            saturation = hsbReset[1]
            brightness = hsbReset[2]
            opacity = if (showAlpha) initial.alpha / 255f else 1f
            emit()
            sync()
        }
        button(alignRight = true, label = "Done") { ColorEditor.close() }

        onClick { event ->
            // Clicking the dimmed backdrop (never the panel — it eats its own clicks) closes.
            if (!panel.isPointInside(event.x, event.y)) ColorEditor.close()
            true
        }
        onMouseScroll { true }
    }

    private fun button(alignRight: Boolean, label: String, action: () -> Unit) {
        val accent = if (alignRight) SettingsUi.PURPLE else SettingsUi.CARD_BORDER
        val card = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.CARD_BG),
            borderColor = SettingsUi.edge(accent),
            borderRadius = 9f,
            borderThickness = SettingsUi.EDGE_WIDTH,
            hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
        )
            .setSizing(100f, Size.Pixels, 30f, Size.Pixels)
            .setPositioning(20f, Pos.ParentPixels, panelHeight - 44f, Pos.ParentPixels)
            .childOf(panel)
        // Vexel's alignment path ignores setOffset, so the inset rides on the constraint.
        if (alignRight) card.setPositioning(-20f, Pos.ParentPixels, panelHeight - 44f, Pos.ParentPixels).alignRight()
        Text(label, if (alignRight) SettingsUi.TEXT_PRIMARY else SettingsUi.TEXT_SECONDARY, 13f, true)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
            .childOf(card)
        card.onClick { _ ->
            action()
            true
        }
    }

    // ---- colour state -------------------------------------------------------

    private fun rgb(): Color = Color(Color.HSBtoRGB(hue, saturation, brightness))
    private fun red(): Int = rgb().red
    private fun green(): Int = rgb().green
    private fun blue(): Int = rgb().blue
    private fun alphaByte(): Int = (opacity * 255f).roundToInt().coerceIn(0, 255)

    private fun channelValue(index: Int): Int {
        val base = rgb()
        return when (index) {
            0 -> base.red
            1 -> base.green
            2 -> base.blue
            else -> alphaByte()
        }
    }

    private fun currentColor(): Color = rgb().let { Color(it.red, it.green, it.blue, alphaByte()) }

    private fun applyRgba(r: Int, g: Int, b: Int, a: Int) {
        val hsb = Color.RGBtoHSB(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), null)
        saturation = hsb[1]
        brightness = hsb[2]
        // Greys and blacks carry no hue — keep the bar where it was instead of
        // snapping it back to red the moment a channel bottoms out.
        if (hsb[1] > 0f && hsb[2] > 0f) hue = hsb[0]
        if (showAlpha) opacity = (a / 255f).coerceIn(0f, 1f)
        emit()
    }

    private fun emit() {
        onChange(currentColor())
    }

    private fun formatHex(): String {
        val color = currentColor()
        return if (showAlpha) String.format("#%02X%02X%02X%02X", color.red, color.green, color.blue, color.alpha)
        else String.format("#%02X%02X%02X", color.red, color.green, color.blue)
    }

    private fun hexPlaceholder(): String = if (showAlpha) "#RRGGBBAA" else "#RRGGBB"

    private fun parseHex(raw: String): Color? {
        var hex = raw.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        if (hex.length == 3 || (hex.length == 4 && showAlpha)) {
            hex = hex.map { "$it$it" }.joinToString("")
        }
        if (hex.length != 6 && !(showAlpha && hex.length == 8)) return null
        return try {
            Color(
                hex.substring(0, 2).toInt(16),
                hex.substring(2, 4).toInt(16),
                hex.substring(4, 6).toInt(16),
                if (hex.length == 8) hex.substring(6, 8).toInt(16) else alphaByte()
            )
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun sync(skipHex: Boolean = false, skipChannel: Int = -1) {
        syncing = true
        channelInputs.forEachIndexed { index, input ->
            if (index != skipChannel) input.value = channelValue(index).toString()
        }
        if (!skipHex) hexInput.value = formatHex()
        syncing = false
    }

    // ---- input --------------------------------------------------------------

    override fun handleMouseClick(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button == 0) {
            when {
                svArea.isPointInside(mouseX, mouseY) -> {
                    draggingSv = true
                    dragSv(mouseX, mouseY)
                    return true
                }
                hueBar.isPointInside(mouseX, mouseY) -> {
                    draggingHue = true
                    dragHue(mouseY)
                    return true
                }
                showAlpha && alphaBar.isPointInside(mouseX, mouseY) -> {
                    draggingAlpha = true
                    dragAlpha(mouseY)
                    return true
                }
            }
        }
        return super.handleMouseClick(mouseX, mouseY, button)
    }

    override fun handleMouseMove(mouseX: Float, mouseY: Float): Boolean {
        when {
            draggingSv -> dragSv(mouseX, mouseY)
            draggingHue -> dragHue(mouseY)
            draggingAlpha -> dragAlpha(mouseY)
        }
        return super.handleMouseMove(mouseX, mouseY)
    }

    override fun handleMouseRelease(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button == 0) {
            draggingSv = false
            draggingHue = false
            draggingAlpha = false
        }
        return super.handleMouseRelease(mouseX, mouseY, button)
    }

    private fun dragSv(mouseX: Float, mouseY: Float) {
        saturation = ((mouseX - svArea.x) / svArea.width).coerceIn(0f, 1f)
        brightness = (1f - (mouseY - svArea.y) / svArea.height).coerceIn(0f, 1f)
        emit()
        sync()
    }

    private fun dragHue(mouseY: Float) {
        hue = ((mouseY - hueBar.y) / hueBar.height).coerceIn(0f, 1f)
        emit()
        sync()
    }

    private fun dragAlpha(mouseY: Float) {
        opacity = (1f - (mouseY - alphaBar.y) / alphaBar.height).coerceIn(0f, 1f)
        emit()
        sync()
    }

    override fun onRender(mouseX: Float, mouseY: Float) {
        // Light enough that the HUD preview card behind stays readable while you drag.
        renderer.rect(x, y, width, height, 0x8C000000.toInt(), 0f)
    }

    // ---- picker surfaces ----------------------------------------------------

    private fun checkerboard(x: Float, y: Float, w: Float, h: Float) {
        val cell = 6f
        var row = 0
        while (row * cell < h) {
            var col = 0
            while (col * cell < w) {
                val cellX = x + col * cell
                val cellY = y + row * cell
                val cellW = minOf(cell, x + w - cellX)
                val cellH = minOf(cell, y + h - cellY)
                val shade = if ((row + col) % 2 == 0) 0xFF3A3A3D.toInt() else 0xFF232326.toInt()
                renderer.rect(cellX, cellY, cellW, cellH, shade, 0f)
                col++
            }
            row++
        }
    }

    private fun marker(centerX: Float, centerY: Float) {
        renderer.hollowRect(centerX - 6f, centerY - 6f, 12f, 12f, 1.5f, 0x90000000.toInt(), 6f)
        renderer.hollowRect(centerX - 5f, centerY - 5f, 10f, 10f, 1.5f, 0xFFFFFFFF.toInt(), 5f)
    }

    private fun barMarker(barX: Float, barY: Float, barW: Float) {
        renderer.rect(barX - 3f, barY - 2.5f, barW + 6f, 5f, 0x90000000.toInt(), 2.5f)
        renderer.rect(barX - 2f, barY - 1.5f, barW + 4f, 3f, 0xFFFFFFFF.toInt(), 1.5f)
    }

    inner class SvArea : VexelElement<SvArea>() {
        override fun onRender(mouseX: Float, mouseY: Float) {
            val hueColor = Color.HSBtoRGB(hue, 1f, 1f)
            renderer.gradientRect(x, y, width, height, 0xFFFFFFFF.toInt(), hueColor, Gradient.LeftToRight, 6f)
            renderer.gradientRect(x, y, width, height, 0x00000000, 0xFF000000.toInt(), Gradient.TopToBottom, 6f)
            renderer.hollowRect(x, y, width, height, 1f, SettingsUi.edge(SettingsUi.CARD_BORDER), 6f)
            marker(x + saturation * width, y + (1f - brightness) * height)
        }
    }

    inner class HueBar : VexelElement<HueBar>() {
        override fun onRender(mouseX: Float, mouseY: Float) {
            val steps = height.toInt().coerceAtLeast(1)
            val stepHeight = height / steps
            for (i in 0 until steps) {
                val color = Color.HSBtoRGB(i.toFloat() / steps, 1f, 1f)
                renderer.rect(x, y + i * stepHeight, width, stepHeight + 0.5f, color, 0f)
            }
            renderer.hollowRect(x, y, width, height, 1f, SettingsUi.edge(SettingsUi.CARD_BORDER), 0f)
            barMarker(x, y + hue * height, width)
        }
    }

    inner class AlphaBar : VexelElement<AlphaBar>() {
        override fun onRender(mouseX: Float, mouseY: Float) {
            checkerboard(x, y, width, height)
            val base = rgb()
            renderer.gradientRect(
                x, y, width, height,
                Color(base.red, base.green, base.blue, 255).rgb,
                Color(base.red, base.green, base.blue, 0).rgb,
                Gradient.TopToBottom, 0f
            )
            renderer.hollowRect(x, y, width, height, 1f, SettingsUi.edge(SettingsUi.CARD_BORDER), 0f)
            barMarker(x, y + (1f - opacity) * height, width)
        }
    }

    inner class Swatch : VexelElement<Swatch>() {
        override fun onRender(mouseX: Float, mouseY: Float) {
            if (showAlpha) checkerboard(x, y, width, height)
            // Original on the left, live colour on the right — MoulConfig's before/after read.
            val half = width / 2f - 3f
            renderer.rect(x, y, half, height, initial.rgb, 8f)
            renderer.rect(x + width / 2f + 3f, y, half, height, currentColor().rgb, 8f)
            renderer.hollowRect(x, y, half, height, 1f, SettingsUi.edge(SettingsUi.CARD_BORDER), 8f)
            renderer.hollowRect(x + width / 2f + 3f, y, half, height, 1f, SettingsUi.edge(SettingsUi.CARD_BORDER), 8f)
        }
    }
}
