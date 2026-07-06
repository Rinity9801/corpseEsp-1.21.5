package forfun.miningqol.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import xyz.meowing.vexel.utils.render.NVGRenderer;
import xyz.meowing.vexel.utils.style.Font;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommissionHUD {
    public enum LayoutMode {
        GRID,
        COLUMN
    }

    private static final long DATA_REFRESH_INTERVAL_MS = 200L;
    private static final long BLUR_REFRESH_INTERVAL_MS = 80L;
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern FRACTION_PROGRESS_PATTERN = Pattern.compile("(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)");
    private static final Pattern STRIP_PROGRESS_SUFFIX = Pattern.compile(
        "[:\\-\\s]*((\\d+(?:\\.\\d+)?)%|(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)|DONE|COMPLETED|CLAIMABLE|CLICK TO CLAIM)$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NON_TEXTURE_KEY_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Map<String, String> TEXTURE_KEY_OVERRIDES = Map.of(
        "corpse_looter", "corpse_looter_v2",
        "umber_collector", "umber_collector_v2"
    );
    private static final String[] COMMISSION_NAME_HINTS = {
        "collector",
        "slayer",
        "explorer",
        "looter",
        "puncher",
        "everywhere"
    };
    private static final Map<String, Float> ANIMATED_PROGRESS = new HashMap<>();
    private static final RenderPipeline GUI_TEXTURED = RenderPipelines.GUI_TEXTURED;

    private static boolean enabled = true;
    private static int hudX = 10;
    private static int hudY = 90;
    private static float scale = 1.0f;
    private static boolean backgroundEnabled = true;
    private static LayoutMode layoutMode = LayoutMode.GRID;
    private static int lastWidth = 200;
    private static int lastHeight = 100;
    private static long lastDataRefreshAt = 0L;
    private static boolean cachedAllowedLocation = false;
    private static List<CommissionEntry> cachedEntries = List.of();

    private static int blurFbo1 = 0;
    private static int blurTex1 = 0;
    private static int blurFbo2 = 0;
    private static int blurTex2 = 0;
    private static int blurW = 0;
    private static int blurH = 0;
    private static int nvgBlurImage = -1;
    private static ByteBuffer blurPixels = null;
    private static long lastBlurCaptureAt = 0L;
    private static int lastBlurPanelX = Integer.MIN_VALUE;
    private static int lastBlurPanelY = Integer.MIN_VALUE;
    private static int lastBlurPanelW = -1;
    private static int lastBlurPanelH = -1;

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setPosition(int x, int y) {
        hudX = x;
        hudY = y;
        invalidateBlurCache();
    }

    public static int getX() {
        return hudX;
    }

    public static int getY() {
        return hudY;
    }

    public static void setScale(float newScale) {
        scale = Math.max(0.5f, Math.min(2.0f, newScale));
        invalidateBlurCache();
    }

    public static float getScale() {
        return scale;
    }

    public static void setBackgroundEnabled(boolean value) {
        backgroundEnabled = value;
        invalidateBlurCache();
    }

    public static boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }

    public static void setLayoutMode(LayoutMode mode) {
        layoutMode = mode == null ? LayoutMode.GRID : mode;
        invalidateBlurCache();
    }

    public static LayoutMode getLayoutMode() {
        return layoutMode;
    }

    public static int getWidth() {
        return lastWidth;
    }

    public static int getHeight() {
        return lastHeight;
    }

    public static void render(DrawContext context) {
        if (!enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        refreshCachedState(client);
        if (!cachedAllowedLocation) {
            return;
        }

        List<CommissionEntry> entries = cachedEntries;
        if (entries.isEmpty()) {
            return;
        }

        renderPanel(context, client, entries, false);
    }

    public static void renderPreview(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        List<CommissionEntry> preview = List.of(
            new CommissionEntry("Aquamarine Gemstone Collector", 42.0),
            new CommissionEntry("Glacite Collector", 100.0),
            new CommissionEntry("Mineshaft Explorer", 20.0),
            new CommissionEntry("Corpse Looter", 80.0)
        );

        renderPanel(context, client, preview, true);
    }

    private static void renderPanel(DrawContext ctx, MinecraftClient mc, List<CommissionEntry> entries, boolean preview) {
        float scaleFactor = mc.getWindow().getWidth() / (float) mc.getWindow().getScaledWidth();
        float s = scale * scaleFactor;
        float x = hudX * scaleFactor;
        float y = hudY * scaleFactor;
        float w = 200f * s;
        float pad = 12f * s;
        float rowH = 30f * s;
        boolean columnLayout = layoutMode == LayoutMode.COLUMN;
        int rowCount = columnLayout ? entries.size() : (entries.size() + 1) / 2;
        float h = 12f * s + rowCount * rowH + 12f * s;
        float radius = 10f * s;
        float labelSize = 8f * s;
        float valueSize = 11f * s;

        lastWidth = Math.max(1, Math.round(w / scaleFactor));
        lastHeight = Math.max(1, Math.round(h / scaleFactor));

        int pixelX = (int) x;
        int pixelY = (int) y;
        int pixelW = (int) w;
        int pixelH = (int) h;
        if (backgroundEnabled && pixelW > 0 && pixelH > 0) {
            ensureBlurFbos(pixelW, pixelH);
            maybeCaptureAndBlur(pixelX, pixelY, pixelW, pixelH, mc.getWindow().getHeight());
        }

        NVGRenderer.INSTANCE.beginFrame(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        long vg = getVg();
        if (vg == 0L) {
            NVGRenderer.INSTANCE.endFrame();
            return;
        }

        Font font = NVGRenderer.INSTANCE.getDefaultFont();
        if (backgroundEnabled && pixelW > 0 && pixelH > 0 && blurPixels != null) {
            if (nvgBlurImage == -1) {
                nvgBlurImage = NanoVG.nvgCreateImageRGBA(vg, blurW, blurH, NanoVG.NVG_IMAGE_FLIPY, blurPixels);
            } else {
                NanoVG.nvgUpdateImage(vg, nvgBlurImage, blurPixels);
            }
        }

        NVGColor c1 = NVGColor.calloc();
        NVGColor c2 = NVGColor.calloc();
        NVGPaint paint = NVGPaint.calloc();

        NanoVG.nvgSave(vg);

        if (backgroundEnabled) {
            c1.r(0f).g(0f).b(0f).a(0.20f);
            c2.r(0f).g(0f).b(0f).a(0f);
            NanoVG.nvgBoxGradient(vg, x, y + 6f * s, w, h, radius, 30f * s, c1, c2, paint);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRect(vg, x - 40f * s, y - 40f * s, w + 80f * s, h + 80f * s);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);

            if (nvgBlurImage != -1) {
                NanoVG.nvgImagePattern(vg, x, y, w, h, 0f, nvgBlurImage, 0.55f, paint);
                NanoVG.nvgBeginPath(vg);
                NanoVG.nvgRoundedRect(vg, x, y, w, h, radius);
                NanoVG.nvgFillPaint(vg, paint);
                NanoVG.nvgFill(vg);
            }

            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, radius);
            c1.r(0.04f).g(0.05f).b(0.12f).a(0.10f);
            NanoVG.nvgFillColor(vg, c1);
            NanoVG.nvgFill(vg);

            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, radius);
            c1.r(1f).g(1f).b(1f).a(0.12f);
            NanoVG.nvgStrokeColor(vg, c1);
            NanoVG.nvgStrokeWidth(vg, 1f);
            NanoVG.nvgStroke(vg);
        }

        float cy = y + 10f * s;
        float cellW = columnLayout ? (w - pad * 2f) : ((w - pad * 2f) / 2f - 6f * s);
        float rightX = x + pad + ((w - pad * 2f) / 2f);
        List<RowIcon> rowIcons = new ArrayList<>();

        if (columnLayout) {
            for (CommissionEntry entry : entries) {
                drawCell(font, entry, x + pad, cy, cellW, labelSize, valueSize, s);
                rowIcons.add(iconForEntry(entry, x + pad, cy + labelSize + 2f * s, scaleFactor, s));
                cy += rowH;
            }
        } else {
            for (int i = 0; i < entries.size(); i += 2) {
                CommissionEntry left = entries.get(i);
                CommissionEntry right = i + 1 < entries.size() ? entries.get(i + 1) : null;

                drawCell(font, left, x + pad, cy, cellW, labelSize, valueSize, s);
                rowIcons.add(iconForEntry(left, x + pad, cy + labelSize + 2f * s, scaleFactor, s));

                if (right != null) {
                    drawCell(font, right, rightX, cy, cellW, labelSize, valueSize, s);
                    rowIcons.add(iconForEntry(right, rightX, cy + labelSize + 2f * s, scaleFactor, s));
                }

                cy += rowH;
            }
        }

        NanoVG.nvgRestore(vg);
        c1.free();
        c2.free();
        paint.free();
        NVGRenderer.INSTANCE.endFrame();

        drawRowIcons(ctx, rowIcons);
    }

    private static void drawCell(Font font, CommissionEntry entry, float x, float y, float maxWidth, float labelSize, float valueSize, float s) {
        String label = trimToWidth(shortName(entry.name()), maxWidth, labelSize, font);
        String value = progressLabel(entry.progress());
        int color = progressColor(entry.progress());
        float iconOffset = 16f * (labelSize / 8f);
        float valueX = x + iconOffset;
        float barY = y + labelSize + valueSize + 5f * s / Math.max(s, 1f);
        float barH = 4f * s;
        float barW = Math.max(24f * s, maxWidth - iconOffset);
        float animatedProgress = animatedProgress(entry);

        drawGlowText(label, x, y, labelSize, rgb(0xF3F4F6), font, 0.16f);
        drawGlowText(value, valueX, y + labelSize + 2f * (labelSize / 8f), valueSize, colorWithAlpha(color, 0.98f), font, 0.22f);
        NVGRenderer.INSTANCE.rect(valueX, barY, barW, barH, colorWithAlpha(0xFFFFFF, 0.10f), barH / 2f);
        if (animatedProgress > 0f) {
            NVGRenderer.INSTANCE.rect(valueX, barY, barW * animatedProgress, barH, color, barH / 2f);
        }
    }

    private static void drawGlowText(String text, float x, float y, float size, int color, Font font, float glowAlpha) {
        int glowColor = colorWithAlpha(0xFFFFFF, Math.min(0.18f, glowAlpha * 0.7f));
        float offset = Math.max(0.45f, size * 0.05f);
        NVGRenderer.INSTANCE.text(text, x - offset, y, size, glowColor, font);
        NVGRenderer.INSTANCE.text(text, x + offset, y, size, glowColor, font);
        NVGRenderer.INSTANCE.text(text, x, y - offset, size, glowColor, font);
        NVGRenderer.INSTANCE.text(text, x, y + offset, size, glowColor, font);
        NVGRenderer.INSTANCE.text(text, x, y, size, color, font);
    }

    private static void drawCommissionTexture(DrawContext ctx, Identifier texture, int x, int y, int size) {
        // Match Divan's overlay draw more literally: draw the top-left 64x64 region
        // from a 64x64 texture into a 16px-style HUD slot.
        ctx.drawTexture(GUI_TEXTURED, texture, x, y, 0f, 0f, size, size, 64, 64, 64, 64);
    }

    private static void drawRowIcons(DrawContext ctx, List<RowIcon> icons) {
        for (RowIcon icon : icons) {
            if (icon.texture == null) {
                continue;
            }
            drawCommissionTexture(ctx, icon.texture, icon.x, icon.y, icon.size);
        }
    }

    private static RowIcon iconForEntry(CommissionEntry entry, float pixelX, float pixelY, float scaleFactor, float s) {
        Identifier texture = textureFor(entry.name());
        int x = Math.round(pixelX / scaleFactor);
        int y = Math.round(pixelY / scaleFactor);
        int size = Math.max(1, Math.round(13f * s / scaleFactor));
        return new RowIcon(texture, x, y, size);
    }

    private static List<CommissionEntry> readCommissionEntries(MinecraftClient client) {
        List<CommissionEntry> result = new ArrayList<>();
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            Text displayName = entry.getDisplayName();
            String raw = displayName != null ? displayName.getString() : entry.getProfile().name();
            CommissionEntry parsed = parseCommission(raw);
            if (parsed != null) {
                result.add(parsed);
            }
        }
        return result;
    }

    private static void refreshCachedState(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (now - lastDataRefreshAt < DATA_REFRESH_INTERVAL_MS) {
            return;
        }
        lastDataRefreshAt = now;
        cachedAllowedLocation = isAllowedHudLocation(client);
        cachedEntries = cachedAllowedLocation ? readCommissionEntries(client) : List.of();
    }

    private static boolean isAllowedHudLocation(MinecraftClient client) {
        if (client.world == null) {
            return false;
        }

        List<String> lines = getSidebarLines(client);
        boolean sawAllowedArea = false;

        for (String clean : lines) {
            if (clean == null || clean.isBlank()) {
                continue;
            }

            String areaLine = normalizeAreaLine(clean);
            if (areaLine == null) {
                continue;
            }

            if (equalsAnyIgnoreCase(areaLine, "Dwarven Mines", "Crystal Hollows")) {
                return false;
            }

            if (equalsAnyIgnoreCase(areaLine, "Dwarven Base Camp", "Glacite Tunnels", "Great Glacite Lake", "Glacite Mineshaft", "Glacite Mineshafts", "Vanguard")) {
                sawAllowedArea = true;
            }
        }

        return sawAllowedArea;
    }

    private static List<String> getSidebarLines(MinecraftClient client) {
        List<String> result = new ArrayList<>();
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return result;
        }

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(sidebar));
        entries.sort((a, b) -> Integer.compare(b.value(), a.value()));

        for (ScoreboardEntry entry : entries) {
            String owner = entry.owner();
            Team team = scoreboard.getScoreHolderTeam(owner);
            String line = team != null
                ? team.getPrefix().getString() + owner + team.getSuffix().getString()
                : owner;
            String clean = line.replaceAll("§.", "").trim();
            if (!clean.isEmpty()) {
                result.add(clean);
            }
        }

        return result;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static boolean equalsAnyIgnoreCase(String value, String... options) {
        for (String option : options) {
            if (value.equalsIgnoreCase(option)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable String normalizeAreaLine(String line) {
        String stripped = line.replaceAll("^[^A-Za-z0-9]+", "").trim();
        if (stripped.isEmpty()) {
            return null;
        }

        if (stripped.contains("%") || containsIgnoreCase(stripped, "done") || containsIgnoreCase(stripped, "commission")) {
            return null;
        }

        stripped = stripped.replaceAll("\\s+", " ").trim();
        return stripped;
    }

    private static @Nullable CommissionEntry parseCommission(String raw) {
        if (raw == null) {
            return null;
        }

        String clean = raw.replaceAll("§.", "").trim();
        if (clean.isEmpty()) {
            return null;
        }

        String lower = clean.toLowerCase(Locale.ROOT);
        if (!looksLikeCommissionLine(lower)) {
            return null;
        }

        Double progress = parseProgress(clean);
        if (progress == null) {
            return null;
        }

        String name = clean;
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(0, colon);
        }
        name = STRIP_PROGRESS_SUFFIX.matcher(name.trim()).replaceAll("").trim();
        if (name.isEmpty()) {
            return null;
        }

        if (textureFor(name) == null) {
            return null;
        }

        return new CommissionEntry(name, progress);
    }

    private static @Nullable Double parseProgress(String clean) {
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.contains("done") || lower.contains("completed") || lower.contains("claimable") || lower.contains("click to claim")) {
            return 100.0;
        }

        Matcher matcher = PROGRESS_PATTERN.matcher(clean);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        Matcher fractionMatcher = FRACTION_PROGRESS_PATTERN.matcher(clean);
        if (fractionMatcher.find()) {
            try {
                double current = Double.parseDouble(fractionMatcher.group(1).replace(",", ""));
                double total = Double.parseDouble(fractionMatcher.group(2).replace(",", ""));
                if (total <= 0.0) {
                    return null;
                }
                return Math.max(0.0, Math.min(100.0, (current / total) * 100.0));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private static boolean looksLikeCommissionLine(String lower) {
        boolean hasProgressToken = lower.contains("%")
            || lower.contains("done")
            || lower.contains("completed")
            || lower.contains("claimable")
            || lower.contains("click to claim")
            || FRACTION_PROGRESS_PATTERN.matcher(lower).find();
        if (!hasProgressToken) {
            return false;
        }

        for (String hint : COMMISSION_NAME_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Identifier textureFor(String commissionName) {
        String normalized = NON_TEXTURE_KEY_CHARS.matcher(commissionName.toLowerCase(Locale.ROOT)).replaceAll("_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        normalized = normalized.replace("commission", "").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        normalized = TEXTURE_KEY_OVERRIDES.getOrDefault(normalized, normalized);
        if (normalized.isEmpty()) {
            return null;
        }
        return Identifier.of("miningqol", "textures/gui/commissions/" + normalized + ".png");
    }

    private static String shortName(String name) {
        return name
            .replace(" Gemstone Collector", "")
            .replace(" Collector", "")
            .replace(" Commission", "")
            .trim();
    }

    private static String trimToWidth(String text, float maxWidth, float fontSize, Font font) {
        if (NVGRenderer.INSTANCE.textWidth(text, fontSize, font) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        float ellipsisWidth = NVGRenderer.INSTANCE.textWidth(ellipsis, fontSize, font);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = builder.toString() + text.charAt(i);
            if (NVGRenderer.INSTANCE.textWidth(candidate, fontSize, font) + ellipsisWidth > maxWidth) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder.isEmpty() ? ellipsis : builder + ellipsis;
    }

    private static float animatedProgress(CommissionEntry entry) {
        String key = entry.name().toLowerCase(Locale.ROOT);
        float target = (float) Math.max(0.0, Math.min(100.0, entry.progress())) / 100f;
        float current = ANIMATED_PROGRESS.getOrDefault(key, target);
        float next = current + (target - current) * 0.18f;
        if (Math.abs(target - next) < 0.0025f) {
            next = target;
        }
        ANIMATED_PROGRESS.put(key, next);
        return next;
    }

    private static String progressLabel(double progress) {
        if (progress >= 100.0) {
            return "DONE";
        }
        return String.format(Locale.US, "%.1f%%", Math.floor(progress * 10.0) / 10.0);
    }

    private static int progressColor(double progress) {
        if (progress >= 100.0) {
            return rgb(0x4ADE80);
        }
        if (progress >= 66.0) {
            return rgb(0xFFB800);
        }
        if (progress >= 33.0) {
            return rgb(0xFF8C42);
        }
        return rgb(0xFF5555);
    }

    private static int rgb(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static int colorWithAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static void invalidateBlurCache() {
        lastBlurCaptureAt = 0L;
        lastBlurPanelX = Integer.MIN_VALUE;
        lastBlurPanelY = Integer.MIN_VALUE;
        lastBlurPanelW = -1;
        lastBlurPanelH = -1;
    }

    private static long getVg() {
        try {
            var field = NVGRenderer.class.getDeclaredField("vg");
            field.setAccessible(true);
            return field.getLong(null);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static void ensureBlurFbos(int panelW, int panelH) {
        int bw = Math.max(panelW / 2, 2);
        int bh = Math.max(panelH / 2, 2);
        if (bw == blurW && bh == blurH && blurFbo1 != 0) {
            return;
        }

        if (blurFbo1 != 0) {
            GL30.glDeleteFramebuffers(blurFbo1);
            GL11.glDeleteTextures(blurTex1);
            GL30.glDeleteFramebuffers(blurFbo2);
            GL11.glDeleteTextures(blurTex2);
        }

        long vg = getVg();
        if (vg != 0L && nvgBlurImage != -1) {
            NanoVG.nvgDeleteImage(vg, nvgBlurImage);
            nvgBlurImage = -1;
        }

        blurW = bw;
        blurH = bh;
        blurPixels = BufferUtils.createByteBuffer(bw * bh * 4);

        blurTex1 = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTex1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, bw, bh, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        blurFbo1 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, blurFbo1);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, blurTex1, 0);

        blurTex2 = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTex2);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, bw, bh, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        blurFbo2 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, blurFbo2);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, blurTex2, 0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static void maybeCaptureAndBlur(int px, int py, int pw, int ph, int windowH) {
        long now = System.currentTimeMillis();
        boolean geometryChanged = px != lastBlurPanelX || py != lastBlurPanelY || pw != lastBlurPanelW || ph != lastBlurPanelH;
        if (!geometryChanged && now - lastBlurCaptureAt < BLUR_REFRESH_INTERVAL_MS) {
            return;
        }

        captureAndBlur(px, py, pw, ph, windowH);
        lastBlurCaptureAt = now;
        lastBlurPanelX = px;
        lastBlurPanelY = py;
        lastBlurPanelW = pw;
        lastBlurPanelH = ph;
    }

    private static void captureAndBlur(int px, int py, int pw, int ph, int windowH) {
        int prevFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int glY = windowH - py - ph;

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, blurFbo1);
        GL30.glBlitFramebuffer(px, glY, px + pw, glY + ph, 0, 0, blurW, blurH, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);

        int halfW = Math.max(blurW / 2, 1);
        int halfH = Math.max(blurH / 2, 1);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, blurFbo1);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, blurFbo2);
        GL30.glBlitFramebuffer(0, 0, blurW, blurH, 0, 0, halfW, halfH, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, blurFbo2);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, blurFbo1);
        GL30.glBlitFramebuffer(0, 0, halfW, halfH, 0, 0, blurW, blurH, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, blurFbo1);
        blurPixels.clear();
        GL11.glReadPixels(0, 0, blurW, blurH, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, blurPixels);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
    }

    private record CommissionEntry(String name, double progress) {}

    private record RowIcon(@Nullable Identifier texture, int x, int y, int size) {}

}
