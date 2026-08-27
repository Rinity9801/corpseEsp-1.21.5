package forfun.miningqol.client;

import net.minecraft.client.Minecraft;

import java.util.function.IntSupplier;

/**
 * A HUD position that stays put across relaunches, resolution and GUI-scale changes.
 *
 * <p>Positions are edited and drawn in gui-scaled pixels, but stored as an edge plus a pixel
 * offset, per axis: the nearest screen edge when the HUD sits in the outer third of the screen,
 * the centre otherwise. A HUD ten pixels from the left stays ten pixels from the left on any
 * screen; one against the right edge keeps its right edge there; a centred one stays centred.
 *
 * <p>The offset is always to the HUD's top-left corner, so it never involves the HUD's own size.
 * That matters: a HUD like the commission panel changes size with its contents, layout and scale,
 * and any scheme that folded the size into the anchor saved it against one size and resolved it on
 * the next launch against another, so the panel landed somewhere new every relaunch.
 *
 * <p>Pixels are re-derived lazily, on the first read after the gui-scaled size changes.
 */
public final class HudAnchor {
    public static final int EDGE_START = 0;
    public static final int CENTRE = 1;
    public static final int EDGE_END = 2;
    /** No anchor yet: only the pixel position is known (legacy config); captured on first draw. */
    public static final int NONE = -1;

    private final IntSupplier width;
    private final IntSupplier height;
    private int x;
    private int y;
    private int modeX = NONE;
    private int modeY = NONE;
    private int offX;
    private int offY;
    /** Gui-scaled size the pixel position is valid for; 0 forces a re-derive. */
    private int refW = 0;
    private int refH = 0;

    public HudAnchor(int x, int y, IntSupplier width, IntSupplier height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Pixel position from the editor; the anchor is re-read off the current screen. */
    public void set(int x, int y) {
        this.x = x;
        this.y = y;
        capture();
    }

    /**
     * Saved anchor from config — outranks the pixel position, which was for whatever screen saved it.
     *
     * <p>An invalid mode (config from before anchors existed) resets to pixel-only, so the anchor
     * is read off the screen on the first draw rather than kept from {@link #set}, which during
     * init sees the window before it has reached its real size.
     */
    public void load(int modeX, int offX, int modeY, int offY) {
        if (valid(modeX) && valid(modeY)) {
            this.modeX = modeX;
            this.offX = offX;
            this.modeY = modeY;
            this.offY = offY;
        } else {
            this.modeX = NONE;
            this.modeY = NONE;
        }
        this.refW = 0;
        this.refH = 0;
    }

    public int modeX() { ensureCaptured(); return modeX; }
    public int modeY() { ensureCaptured(); return modeY; }
    public int offX() { ensureCaptured(); return offX; }
    public int offY() { ensureCaptured(); return offY; }

    public int x() {
        resolve();
        return x;
    }

    public int y() {
        resolve();
        return y;
    }

    private static boolean valid(int mode) {
        return mode == EDGE_START || mode == CENTRE || mode == EDGE_END;
    }

    private void ensureCaptured() {
        if (modeX == NONE || modeY == NONE) capture();
    }

    private void capture() {
        int[] screen = screen();
        int[] size = screen == null ? null : size();
        if (size == null) {
            return;
        }
        int[] ax = captureAxis(x, size[0], screen[0]);
        int[] ay = captureAxis(y, size[1], screen[1]);
        modeX = ax[0];
        offX = ax[1];
        modeY = ay[0];
        offY = ay[1];
        refW = screen[0];
        refH = screen[1];
    }

    /**
     * {mode, offset} for a position {@code p} of extent {@code e} on an axis of length {@code n}.
     *
     * <p>The extent only picks the edge; the offset is always measured to the HUD's own top-left
     * corner. Measuring to its far edge or centre would be the ideal for resolution changes, but
     * it makes the round-trip depend on the HUD's size — and the commission panel's size is not
     * known until it has drawn once, so an offset saved against the drawn size and resolved at
     * launch against the default one walked the panel across the screen a step per restart.
     */
    private static int[] captureAxis(int p, int e, int n) {
        int centre = p + e / 2;
        if (centre < n / 3) {
            return new int[] {EDGE_START, p};
        }
        if (centre > n - n / 3) {
            return new int[] {EDGE_END, n - p};
        }
        return new int[] {CENTRE, p - n / 2};
    }

    private static int resolveAxis(int mode, int off, int n) {
        return switch (mode) {
            case EDGE_END -> n - off;
            case CENTRE -> n / 2 + off;
            default -> off;
        };
    }

    private void resolve() {
        int[] screen = screen();
        if (screen == null || (screen[0] == refW && screen[1] == refH)) {
            return;
        }
        if (modeX == NONE || modeY == NONE) {
            // Legacy pixel-only position: adopt where it sits on this screen as the anchor.
            capture();
            return;
        }
        x = resolveAxis(modeX, offX, screen[0]);
        y = resolveAxis(modeY, offY, screen[1]);
        refW = screen[0];
        refH = screen[1];
    }

    private int[] size() {
        // Sizes come from the HUDs' own measurers, some of which need the font — not there yet
        // while the config is applied during init. Leave the anchor unresolved until it is.
        try {
            return new int[] {Math.max(0, width.getAsInt()), Math.max(0, height.getAsInt())};
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int[] screen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return null;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        return new int[] {w, h};
    }
}
