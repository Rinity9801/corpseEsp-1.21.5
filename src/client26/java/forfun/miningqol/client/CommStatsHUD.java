package forfun.miningqol.client;

import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * Standalone HUD for the commission stats — styled after the 1.21 collection
 * tracker HUD: shadowed text lines (no backdrop), a white title with a red
 * "(paused)" tag when the rate clock is idle, and gray labels with colored
 * values. Rendered from CommissionHUD's Vexel NanoVG hook; visible in the same
 * areas, gated on the Commission Stats toggle (CommTracker.isStatsEnabled()).
 */
public class CommStatsHUD {
    private static final long DATA_REFRESH_INTERVAL_MS = 200L;

    private static final int COLOR_TITLE = 0xFFF3F4F6;
    private static final int COLOR_TAG = 0xFFFF5555;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_TOTAL = 0xFFF3F4F6;
    private static final int COLOR_RATE = 0xFF55C8F0;
    private static final int COLOR_SESSION = 0xFF7FDB8A;
    private static final int COLOR_CALC = 0xFF666B76;
    private static final int COLOR_SHADOW = 0xE0101018;

    private static int hudX = 10;
    private static int hudY = 220;
    private static float scale = 1.0f;
    private static int lastWidth = 110;
    private static int lastHeight = 42;
    private static long lastRefreshAt = 0L;
    private static boolean cachedAllowedLocation = false;

    private CommStatsHUD() {}

    public static void setPosition(int x, int y) {
        hudX = x;
        hudY = y;
    }

    public static int getX() { return hudX; }
    public static int getY() { return hudY; }

    public static void setScale(float newScale) {
        scale = Math.max(0.5f, Math.min(2.0f, newScale));
    }

    public static float getScale() { return scale; }

    /** Last drawn size in gui-scaled units (for the move editor's drag box). */
    public static int getWidth() { return lastWidth; }
    public static int getHeight() { return lastHeight; }

    /** Called every frame from CommissionHUD's Vexel render hook. */
    public static void renderNvg() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        boolean editor = mc.screen instanceof forfun.miningqol.client.gui.CommStatsHudPositionScreen;
        if (!editor) {
            if (!CommTracker.isStatsEnabled()) {
                return;
            }
            if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastRefreshAt >= DATA_REFRESH_INTERVAL_MS) {
                lastRefreshAt = now;
                cachedAllowedLocation = CommissionHUD.isAllowedHudLocation(mc);
            }
            if (!cachedAllowedLocation) {
                return;
            }
        }

        xyz.meowing.vexel.api.RenderAPI r = xyz.meowing.vexel.Vexel.getRenderer();
        xyz.meowing.vexel.api.style.Font font = xyz.meowing.vexel.Vexel.getDefaultFont();

        float f = (float) mc.getWindow().getScreenWidth() / Math.max(1, mc.getWindow().getGuiScaledWidth());
        float u = f * scale;
        float size = 9f * u;
        float titleStep = 12f * u;
        float rowStep = 10f * u;

        double rate = CommTracker.getCommsPerHour();
        boolean hasRate = rate >= 0;
        boolean paused = CommTracker.isPaused();
        String rateStr = hasRate ? String.format(Locale.US, "%.1f", rate) : "Calculating...";

        float x = hudX * f;
        float y = hudY * f;
        float maxW;

        // Title (+ paused tag, like colltrack's "(afk)")
        maxW = shadowText(r, font, "Commissions", x, y, size, COLOR_TITLE, u);
        if (paused) {
            maxW += shadowText(r, font, " (paused)", x + maxW, y, size, COLOR_TAG, u);
        }
        y += titleStep;

        maxW = Math.max(maxW, labelValue(r, font, "Total: ", String.format(Locale.US, "%,d", CommTracker.getTotalCompleted()),
            x, y, size, COLOR_TOTAL, u));
        y += rowStep;
        maxW = Math.max(maxW, labelValue(r, font, "Comms/hr: ", rateStr,
            x, y, size, hasRate ? COLOR_RATE : COLOR_CALC, u));
        y += rowStep;
        maxW = Math.max(maxW, labelValue(r, font, "Session: ", String.format(Locale.US, "%,d", CommTracker.getSessionCompleted()),
            x, y, size, COLOR_SESSION, u));

        lastWidth = Math.round(maxW / f);
        lastHeight = Math.round((titleStep + rowStep * 2f + size) / f);

        if (editor) {
            r.hollowRect(hudX * f - 2f, hudY * f - 2f, maxW + 4f, titleStep + rowStep * 2f + size + 4f,
                Math.max(1f, u), 0xFF88AAFF, 3f * u);
        }
    }

    /** Gray label + colored value on one line; returns the line's width. */
    private static float labelValue(xyz.meowing.vexel.api.RenderAPI r, xyz.meowing.vexel.api.style.Font font,
                                    String label, String value, float x, float y, float size, int valueColor, float u) {
        float w = shadowText(r, font, label, x, y, size, COLOR_LABEL, u);
        w += shadowText(r, font, value, x + w, y, size, valueColor, u);
        return w;
    }

    /** Minecraft-style drop shadow; returns the text width. */
    private static float shadowText(xyz.meowing.vexel.api.RenderAPI r, xyz.meowing.vexel.api.style.Font font,
                                    String text, float x, float y, float size, int color, float u) {
        float o = Math.max(1f, u);
        r.text(text, x + o, y + o, size, COLOR_SHADOW, font);
        r.text(text, x, y, size, color, font);
        return r.textWidth(text, size, font);
    }
}
