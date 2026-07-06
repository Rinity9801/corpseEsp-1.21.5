package forfun.miningqol.client.gui;

import forfun.miningqol.client.RadialMenuManager;
import forfun.miningqol.client.config.MiningConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * A radial (wheel) menu. Point the mouse in a direction to highlight the option
 * in that direction, then left-click to run its command. Esc / right-click closes.
 */
public class RadialMenuScreen extends Screen {
    private static final int OUTER_R = 95;
    private static final int INNER_R = 42;
    private static final int LABEL_R = (OUTER_R + INNER_R) / 2;
    private static final double DEADZONE = INNER_R;

    // Colors (ARGB)
    private static final int SEG_EVEN = 0xD024242C;
    private static final int SEG_ODD = 0xD02E2E3A;
    private static final int SEG_SELECTED = 0xF04A90E2;
    private static final int HUB = 0xE014141A;
    private static final int RING_EDGE = 0xFF3A3A46;

    private int selected = -1;

    public RadialMenuScreen() {
        super(Text.literal("Radial Menu"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Slots that actually have a command, paired with their original slot index. */
    private List<int[]> activeIndices(List<MiningConfig.RadialEntry> entries) {
        List<int[]> active = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            MiningConfig.RadialEntry e = entries.get(i);
            if (e.command != null && !e.command.isEmpty()) {
                active.add(new int[]{i});
            }
        }
        return active;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // Note: no full-screen dim — the wheel draws straight over the game.

        List<MiningConfig.RadialEntry> entries = RadialMenuManager.getEntries();
        List<int[]> active = activeIndices(entries);
        int cx = this.width / 2;
        int cy = this.height / 2;

        if (active.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                "§7No radial options set", cx, cy - 12, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer,
                "§8Use §7/radial set <1-8> <command>", cx, cy + 2, 0xFFFFFFFF);
            return;
        }

        int n = active.size();

        // Pick the option in the direction the mouse points (no precise aiming needed).
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        selected = (Math.sqrt(dx * dx + dy * dy) > DEADZONE) ? segmentAt(dx, dy, n) : -1;

        drawWheel(context, cx, cy, n);

        // Labels, centered in each wedge along the ring.
        for (int i = 0; i < n; i++) {
            double phi = (i / (double) n) * Math.PI * 2 - Math.PI / 2; // first wedge at top
            int lx = cx + (int) (Math.cos(phi) * LABEL_R);
            int ly = cy + (int) (Math.sin(phi) * LABEL_R);
            MiningConfig.RadialEntry e = entries.get(active.get(i)[0]);
            String label = (e.label == null || e.label.isEmpty()) ? e.command : e.label;
            boolean sel = i == selected;
            context.drawCenteredTextWithShadow(this.textRenderer, label, lx, ly - 4,
                sel ? 0xFFFFFFFF : 0xFFC5CDD8);
        }

        // Hub text: the selected option's label, or a prompt.
        String center;
        if (selected >= 0) {
            MiningConfig.RadialEntry e = entries.get(active.get(selected)[0]);
            center = "§f" + ((e.label == null || e.label.isEmpty()) ? e.command : e.label);
        } else {
            center = "§7Aim →";
        }
        context.drawCenteredTextWithShadow(this.textRenderer, center, cx, cy - 4, 0xFFFFFFFF);
    }

    /** Which wedge a direction vector falls in (matches the selection math). */
    private int segmentAt(double dx, double dy, int n) {
        double a = Math.atan2(dy, dx) + Math.PI / 2; // rotate so straight up = wedge 0
        a %= Math.PI * 2;
        if (a < 0) a += Math.PI * 2;
        int seg = (int) Math.round(a / (Math.PI * 2) * n) % n;
        return seg < 0 ? seg + n : seg;
    }

    /**
     * Filled N-gon donut of wedges, drawn scanline-by-scanline so the polygon edges
     * and wedge boundaries are exact. The outer boundary has one flat edge per option
     * (6 options = hexagon, 8 = octagon, ...); falls back to a circle below 3 options.
     */
    private void drawWheel(DrawContext context, int cx, int cy, int n) {
        for (int y = -OUTER_R; y <= OUTER_R; y++) {
            boolean inRun = false;
            int runStart = 0;
            int runColor = 0;
            for (int x = -OUTER_R; x <= OUTER_R; x++) {
                double r = Math.sqrt((double) x * x + (double) y * y);
                int color = 0;
                boolean filled = false;
                if (r <= OUTER_R) {
                    double a = Math.atan2(y, x) + Math.PI / 2;
                    a %= Math.PI * 2;
                    if (a < 0) a += Math.PI * 2;
                    double k = polyFactor(a, n);
                    if (r <= OUTER_R * k) {
                        if (r < INNER_R * k) {
                            color = HUB;
                        } else {
                            int seg = (int) Math.round(a / (Math.PI * 2) * n) % n;
                            if (seg < 0) seg += n;
                            color = (seg == selected) ? SEG_SELECTED : (seg % 2 == 0 ? SEG_EVEN : SEG_ODD);
                        }
                        filled = true;
                    }
                }
                if (filled && inRun && color == runColor) {
                    continue;
                }
                if (inRun) {
                    context.fill(cx + runStart, cy + y, cx + x, cy + y + 1, runColor);
                    inRun = false;
                }
                if (filled) {
                    inRun = true;
                    runStart = x;
                    runColor = color;
                }
            }
            if (inRun) {
                context.fill(cx + runStart, cy + y, cx + OUTER_R + 1, cy + y + 1, runColor);
            }
        }

        drawPolyOutline(context, cx, cy, OUTER_R, n, RING_EDGE);
        drawPolyOutline(context, cx, cy, INNER_R, n, RING_EDGE);
    }

    /** Boundary radius factor for a regular N-gon, with a flat edge centered on each wedge. */
    private double polyFactor(double a, int n) {
        if (n < 3) return 1.0; // circle for 1-2 options
        double seg = Math.PI * 2 / n;
        double t = (a + seg / 2) % seg;
        if (t < 0) t += seg;
        double off = t - seg / 2;
        return Math.cos(seg / 2) / Math.cos(off);
    }

    private void drawPolyOutline(DrawContext context, int cx, int cy, int r, int n, int color) {
        int steps = Math.max(180, r * 5);
        for (int i = 0; i < steps; i++) {
            double a = (i / (double) steps) * Math.PI * 2;
            double rr = r * polyFactor(a, n);
            double theta = a - Math.PI / 2;
            int x = cx + (int) Math.round(Math.cos(theta) * rr);
            int y = cy + (int) Math.round(Math.sin(theta) * rr);
            context.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** Run the highlighted option (if any) and close — used by the hold-to-open keybind. */
    public void selectAndClose() {
        String command = null;
        if (selected >= 0) {
            List<MiningConfig.RadialEntry> entries = RadialMenuManager.getEntries();
            List<int[]> active = activeIndices(entries);
            if (selected < active.size()) {
                command = entries.get(active.get(selected)[0]).command;
            }
        }
        this.close();
        if (command != null) RadialMenuManager.run(command);
    }

    @Override
    public boolean mouseClicked(Click click, boolean firstClick) {
        if (click.button() == 0 && selected >= 0) {
            List<MiningConfig.RadialEntry> entries = RadialMenuManager.getEntries();
            List<int[]> active = activeIndices(entries);
            if (selected < active.size()) {
                String command = entries.get(active.get(selected)[0]).command;
                this.close();
                RadialMenuManager.run(command);
                return true;
            }
        }
        if (click.button() == 1) {
            this.close();
            return true;
        }
        return super.mouseClicked(click, firstClick);
    }
}
