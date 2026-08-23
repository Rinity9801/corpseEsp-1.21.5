package forfun.miningqol.client.gui

import xyz.meowing.knit.api.render.KnitResolution
import xyz.meowing.vexel.Vexel.renderer
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

/**
 * Root element that draws a fixed design-sized layout at an integer scale, so the
 * settings GUIs stay legible on high-resolution displays instead of shrinking. Mouse
 * coordinates are mapped back into design space for every child.
 */
class ScaledUiRoot(
    private val scale: Float,
    designWidth: Float,
    designHeight: Float
) : VexelElement<ScaledUiRoot>() {
    init {
        setSizing(designWidth, Size.Pixels, designHeight, Size.Pixels)
        setPositioning(Pos.ScreenCenter, Pos.ScreenCenter)
        xConstraint = designWidth * (1f - scale) / 2f
        yConstraint = designHeight * (1f - scale) / 2f
    }

    private fun logicalX(screenX: Float): Float = x + (screenX - x) / scale
    private fun logicalY(screenY: Float): Float = y + (screenY - y) / scale

    override fun onRender(mouseX: Float, mouseY: Float) {
        val snappedX = round(x)
        val snappedY = round(y)
        if (x != snappedX) x = snappedX
        if (y != snappedY) y = snappedY
    }

    override fun renderChildren(mouseX: Float, mouseY: Float) {
        renderer.push()
        renderer.translate(x, y)
        renderer.scale(scale, scale)
        renderer.translate(-x, -y)
        val logicalMouseX = logicalX(mouseX)
        val logicalMouseY = logicalY(mouseY)
        children.forEach { it.render(logicalMouseX, logicalMouseY) }
        renderer.pop()
    }

    override fun handleMouseMove(mouseX: Float, mouseY: Float): Boolean =
        super.handleMouseMove(logicalX(mouseX), logicalY(mouseY))

    override fun handleMouseClick(mouseX: Float, mouseY: Float, button: Int): Boolean =
        super.handleMouseClick(logicalX(mouseX), logicalY(mouseY), button)

    override fun handleMouseRelease(mouseX: Float, mouseY: Float, button: Int): Boolean =
        super.handleMouseRelease(logicalX(mouseX), logicalY(mouseY), button)

    override fun handleMouseScroll(
        mouseX: Float,
        mouseY: Float,
        horizontal: Double,
        vertical: Double
    ): Boolean = super.handleMouseScroll(logicalX(mouseX), logicalY(mouseY), horizontal, vertical)
}

/** The largest integer scale that still fits a [designWidth] x [designHeight] layout on screen. */
fun uiScaleFor(designWidth: Float, designHeight: Float): Float {
    val resolutionRatio = minOf(
        KnitResolution.windowWidth / 1920f,
        KnitResolution.windowHeight / 1080f
    )
    val desiredScale = ceil(resolutionRatio).toInt().coerceIn(1, 3)
    val maximumFitScale = floor(
        minOf(
            KnitResolution.windowWidth / designWidth,
            KnitResolution.windowHeight / designHeight
        )
    ).toInt().coerceAtLeast(1)
    return minOf(desiredScale, maximumFitScale).toFloat()
}
