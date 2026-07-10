package forfun.miningqol.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
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

    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("miningqol", "commission_hud");
    private static final RenderPipeline GUI_TEXTURED = RenderPipelines.GUI_TEXTURED;
    private static final long DATA_REFRESH_INTERVAL_MS = 200L;
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

    private static boolean registered = false;
    private static boolean nvgHooked = false;
    private static boolean enabled = true;
    private static int hudX = 10;
    private static int hudY = 90;
    private static float scale = 1.0f;
    private static boolean backgroundEnabled = true;
    private static LayoutMode layoutMode = LayoutMode.GRID;
    private static int lastWidth = 220;
    private static int lastHeight = 118;
    private static long lastDataRefreshAt = 0L;
    private static boolean cachedAllowedLocation = false;
    private static List<CommissionEntry> cachedEntries = List.of();

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        // Draw through Vexel's NanoVG frame (same font/glow/rounded look as 1.21).
        // Vexel's static init loads its font from the resource manager, which is null
        // during mod init — so defer first contact with the Vexel class until the
        // client has resources (first tick), then hook its render event once.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (nvgHooked || client.getResourceManager() == null) {
                return;
            }
            nvgHooked = true;
            xyz.meowing.vexel.Vexel.getEventBus().registerJava(
                xyz.meowing.vexel.events.GuiEvent.Render.class, 0, true, event -> {
                    renderNvg();
                    return kotlin.Unit.INSTANCE;
                });
        });
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setPosition(int x, int y) {
        hudX = x;
        hudY = y;
    }

    public static int getX() {
        return hudX;
    }

    public static int getY() {
        return hudY;
    }

    public static void setScale(float newScale) {
        scale = Math.max(0.5f, Math.min(2.0f, newScale));
    }

    public static float getScale() {
        return scale;
    }

    public static void setBackgroundEnabled(boolean value) {
        backgroundEnabled = value;
    }

    public static boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }

    public static void setLayoutMode(LayoutMode mode) {
        layoutMode = mode == null ? LayoutMode.GRID : mode;
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

    /** /commhuddebug — shows what the location gate sees so "HUD not showing" is diagnosable. */
    public static void debugDump() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        List<String> tabLines = getTabLines(mc);
        mc.player.sendSystemMessage(Component.literal("\u00A76[MQO] \u00A7fCommission HUD debug \u00A77(tab lines: " + tabLines.size() + ")"));
        for (String line : tabLines) {
            CommissionEntry parsed = parseCommission(line);
            if (parsed != null) {
                mc.player.sendSystemMessage(Component.literal("\u00A78  '" + line + "' \u00A7a-> commission " + parsed.name() + " " + Math.round(parsed.progress() * 100) + "%"));
            }
        }
        lastDataRefreshAt = 0; // force a fresh evaluation
        refreshCachedState(mc);
        mc.player.sendSystemMessage(Component.literal("\u00A76[MQO] \u00A7fenabled=" + enabled
            + " \u00A7fallowedLocation=" + cachedAllowedLocation
            + " \u00A7fcommissions=" + cachedEntries.size()
            + " \u00A7fpos=" + hudX + "," + hudY));
    }

    /** Renders the real panel with sample data at the configured position (move editor). */
    // ===== NanoVG rendering — matches the 1.21 look (Vexel font, glow, rounded bars) =====

    private static final Map<String, xyz.meowing.vexel.api.style.Image> ICON_CACHE = new HashMap<>();
    private static final java.util.Set<String> ICON_FAILED = new java.util.HashSet<>();

    /** Called every frame from Vexel's render hook (inside beginFrame/endFrame). */
    public static void renderNvg() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        boolean editor = mc.screen instanceof forfun.miningqol.client.gui.CommissionHudPositionScreen;
        if (editor) {
            List<CommissionEntry> sample = cachedEntries.isEmpty()
                ? List.of(new CommissionEntry("Mithril Miner", 62.0), new CommissionEntry("Goblin Slayer", 31.0),
                          new CommissionEntry("Titanium Miner", 100.0), new CommissionEntry("Glacite Walker Slayer", 8.5))
                : cachedEntries;
            drawPanelNvg(sample, true);
            return;
        }

        if (!enabled) {
            return;
        }
        // Vexel's NanoVG pass draws on top of open screens, so hide the HUD when a GUI is
        // open (config menu, inventory, Hypixel menus…). Chat is allowed — HUDs conventionally
        // stay visible while chatting.
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            return;
        }
        refreshCachedState(mc);
        if (!cachedAllowedLocation || cachedEntries.isEmpty()) {
            return;
        }
        drawPanelNvg(cachedEntries, false);
    }

    private static void drawPanelNvg(List<CommissionEntry> entries, boolean editor) {
        Minecraft mc = Minecraft.getInstance();
        xyz.meowing.vexel.api.RenderAPI r = xyz.meowing.vexel.Vexel.getRenderer();
        xyz.meowing.vexel.api.style.Font font = xyz.meowing.vexel.Vexel.getDefaultFont();

        // The NVG frame is the LOGICAL window; hud position/size are gui-scaled units.
        float f = (float) mc.getWindow().getScreenWidth() / Math.max(1, mc.getWindow().getGuiScaledWidth());
        float u = f * scale;

        float width = 220f * u;
        float pad = 8f * u;
        float headerH = 3f * u;
        float rowH = 32f * u;
        float labelSize = 9f * u;
        float valueSize = 8f * u;
        float iconSize = 13f * u;
        float labelGap = 4f * u;
        float barH = 3f * u;
        boolean columnLayout = layoutMode == LayoutMode.COLUMN;
        int rowCount = columnLayout ? entries.size() : (entries.size() + 1) / 2;
        float height = pad + headerH + rowCount * rowH + pad * 0.5f;

        float x = hudX * f;
        float y = hudY * f;
        lastWidth = Math.round(width / f);
        lastHeight = Math.round(height / f);

        if (backgroundEnabled) {
            r.rect(x, y, width, height, 0xE014141B, 8f * u);
            r.hollowRect(x, y, width, height, Math.max(1f, u * 0.8f), 0x552F2F45, 8f * u);
        }
        if (editor) {
            r.hollowRect(x - 2f, y - 2f, width + 4f, height + 4f, Math.max(1f, u), 0xFF88AAFF, 8f * u);
        }

        float cellW = columnLayout ? width - pad * 2f : (width - pad * 3f) / 2f;
        float cy = y + headerH + pad * 0.6f;
        for (int i = 0; i < entries.size(); ) {
            drawCellNvg(r, font, entries.get(i), x + pad, cy, cellW, labelSize, valueSize, iconSize, labelGap, barH, u);
            if (!columnLayout && i + 1 < entries.size()) {
                drawCellNvg(r, font, entries.get(i + 1), x + pad * 2f + cellW, cy, cellW, labelSize, valueSize, iconSize, labelGap, barH, u);
            }
            i += columnLayout ? 1 : 2;
            cy += rowH;
        }
    }

    private static void drawCellNvg(xyz.meowing.vexel.api.RenderAPI r, xyz.meowing.vexel.api.style.Font font,
                                    CommissionEntry entry, float x, float y, float cellW,
                                    float labelSize, float valueSize, float iconSize, float labelGap,
                                    float barH, float u) {
        float textX = x;
        xyz.meowing.vexel.api.style.Image icon = iconFor(entry.name());
        if (icon != null) {
            r.image(icon, x, y + 1f * u, iconSize, iconSize, 3f * u);
            textX = x + iconSize + labelGap;
        }
        float maxTextW = x + cellW - textX;

        String label = entry.name();
        while (label.length() > 3 && r.textWidth(label, labelSize, font) > maxTextW) {
            label = label.substring(0, label.length() - 2);
        }
        int color = progressColor(entry.progress());
        glowText(r, font, label, textX, y, labelSize, 0xFFF3F4F6, 0.16f);
        glowText(r, font, progressLabel(entry.progress()), textX, y + labelSize + 2f * u, valueSize, color, 0.22f);

        float barY = y + labelSize + valueSize + 5.5f * u;
        float animated = animatedProgress(entry);
        r.rect(textX, barY, maxTextW, barH, 0x1AFFFFFF, barH / 2f);
        if (animated > 0.01f) {
            r.rect(textX, barY, Math.max(barH, maxTextW * animated), barH, color, barH / 2f);
        }
    }

    private static void glowText(xyz.meowing.vexel.api.RenderAPI r, xyz.meowing.vexel.api.style.Font font,
                                 String text, float x, float y, float size, int color, float glowAlpha) {
        int glow = ((int) (glowAlpha * 255f) << 24) | (color & 0xFFFFFF);
        float o = Math.max(0.5f, size / 16f);
        r.text(text, x - o, y, size, glow, font);
        r.text(text, x + o, y, size, glow, font);
        r.text(text, x, y - o, size, glow, font);
        r.text(text, x, y + o, size, glow, font);
        r.text(text, x, y, size, color, font);
    }

    private static xyz.meowing.vexel.api.style.Image iconFor(String commissionName) {
        String path = texturePathFor(commissionName);
        if (path == null || ICON_FAILED.contains(path)) {
            return null;
        }
        xyz.meowing.vexel.api.style.Image cached = ICON_CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        try {
            cached = xyz.meowing.vexel.Vexel.getRenderer().createImage(path, 64, 64, java.awt.Color.WHITE, path);
            ICON_CACHE.put(path, cached);
            return cached;
        } catch (Exception e) {
            ICON_FAILED.add(path);
            return null;
        }
    }

    private static void refreshCachedState(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastDataRefreshAt < DATA_REFRESH_INTERVAL_MS) {
            return;
        }
        lastDataRefreshAt = now;
        cachedAllowedLocation = isAllowedHudLocation(mc);
        cachedEntries = cachedAllowedLocation ? readCommissionEntries(mc) : List.of();
    }

    private static boolean isAllowedHudLocation(Minecraft mc) {
        List<String> lines = getSidebarLines(mc);
        boolean sawAllowedArea = false;

        for (String clean : lines) {
            String areaLine = normalizeAreaLine(clean);
            if (areaLine == null) {
                continue;
            }

            if (equalsAnyIgnoreCase(areaLine, "Dwarven Mines", "Crystal Hollows", "Fairy Grotto")) {
                return false;
            }

            if (equalsAnyIgnoreCase(areaLine, "Dwarven Base Camp", "Glacite Tunnels", "Great Glacite Lake", "Glacite Mineshaft", "Glacite Mineshafts", "Vanguard")) {
                sawAllowedArea = true;
            }
        }

        return sawAllowedArea;
    }

    private static List<CommissionEntry> readCommissionEntries(Minecraft mc) {
        List<CommissionEntry> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        // Hypixel puts commission progress in tab-list display names; the scoreboard
        // teams only carry fragments (no percentages), so the tab list is the source.
        for (String line : getTabLines(mc)) {
            addParsed(result, seen, line);
        }
        for (String line : getSidebarLines(mc)) {
            addParsed(result, seen, line);
        }
        return result;
    }

    private static void addParsed(List<CommissionEntry> result, java.util.Set<String> seen, String line) {
        CommissionEntry parsed = parseCommission(line);
        if (parsed != null && seen.add(parsed.name().toLowerCase(Locale.ROOT))) {
            result.add(parsed);
        }
    }

    private static List<String> getTabLines(Minecraft mc) {
        List<String> result = new ArrayList<>();
        if (mc.getConnection() == null) {
            return result;
        }
        for (net.minecraft.client.multiplayer.PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            Component displayName = info.getTabListDisplayName();
            if (displayName == null) continue;
            String clean = displayName.getString().replaceAll("\u00A7.", "").trim();
            if (!clean.isEmpty()) {
                result.add(clean);
            }
        }
        return result;
    }

    private static List<String> getSidebarLines(Minecraft mc) {
        List<String> result = new ArrayList<>();
        if (mc.level == null) {
            return result;
        }

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return result;
        }

        Collection<PlayerTeam> teams = scoreboard.getPlayerTeams();
        for (PlayerTeam team : teams) {
            for (String member : team.getPlayers()) {
                String line = team.getPlayerPrefix().getString() + member + team.getPlayerSuffix().getString();
                String clean = line.replaceAll("§.", "").trim();
                if (!clean.isEmpty()) {
                    result.add(clean);
                }
            }
        }

        return result;
    }

    private static @Nullable CommissionEntry parseCommission(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String clean = raw.replaceAll("§.", "").trim();
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
        if (name.isEmpty() || texturePathFor(name) == null) {
            return null;
        }

        return new CommissionEntry(name, progress);
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
                return Math.max(0.0, Math.min(100.0, current / total * 100.0));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable String normalizeAreaLine(String line) {
        String stripped = line.replaceAll("^[^A-Za-z0-9]+", "").trim();
        if (stripped.isEmpty()) {
            return null;
        }

        String lower = stripped.toLowerCase(Locale.ROOT);
        if (lower.contains("commission") || lower.contains("done") || stripped.contains("%")) {
            return null;
        }

        return stripped.replaceAll("\\s+", " ").trim();
    }

    private static boolean equalsAnyIgnoreCase(String value, String... options) {
        for (String option : options) {
            if (value.equalsIgnoreCase(option)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable String texturePathFor(String commissionName) {
        String normalized = NON_TEXTURE_KEY_CHARS.matcher(commissionName.toLowerCase(Locale.ROOT)).replaceAll("_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        normalized = normalized.replace("commission", "").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        normalized = TEXTURE_KEY_OVERRIDES.getOrDefault(normalized, normalized);
        if (normalized.isEmpty()) {
            return null;
        }
        return "/assets/miningqol/textures/gui/commissions/" + normalized + ".png";
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
            return 0xFF4ADE80;
        }
        if (progress >= 66.0) {
            return 0xFFFFB800;
        }
        if (progress >= 33.0) {
            return 0xFFFF8C42;
        }
        return 0xFFFF5555;
    }

    private static String shortName(String name) {
        return name
            .replace(" Gemstone Collector", "")
            .replace(" Collector", "")
            .replace(" Commission", "")
            .trim();
    }

    private record CommissionEntry(String name, double progress) {
    }
}
