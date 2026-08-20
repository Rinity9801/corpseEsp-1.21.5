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
    /**
     * The menu names its commission items "Commission #1", "Commission #2"...
     *
     * <p>Matched exactly so the rest of the menu is skipped — "Commission Milestones" sits in there
     * too and would otherwise be read as a commission.
     */
    private static final Pattern COMMISSION_ITEM = Pattern.compile("Commission #\\d+");
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
    /** Last set of commissions read straight out of the Royal Pigeon menu, and when. */
    private static List<CommissionEntry> guiEntries = List.of();
    /** Which menu slots those came from — the exact slots a commission is claimed from. */
    private static List<Integer> guiSlots = List.of();
    private static long guiEntriesAt = 0L;
    /**
     * Which area the menu's Filter was showing, or null if it could not be read.
     *
     * <p>The menu lists whichever area the filter is set to, but this HUD only ever appears in the
     * Glacite areas. Overriding correct Glacite data with a Dwarven Mines listing would be worse
     * than the tab-list lag this override exists to fix.
     */
    private static String guiFilterArea = null;
    /** Raw slot contents of the last Commissions menu seen, parsed or not — see refreshGuiEntries. */
    private static List<String> lastMenuDump = List.of();
    private static long lastMenuDumpAt = 0L;
    /**
     * How long a menu reading outranks the tab list after the menu closes.
     *
     * <p>The tab list lags a claim by a few seconds, so without this the HUD drops straight back to
     * listing a commission that has already been handed in. Kept short on purpose: the snapshot is
     * frozen, so holding it any longer would stall the progress updates the tab list is good for.
     */
    private static final long GUI_OVERRIDE_MS = 10_000L;

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
                    CommStatsHUD.renderNvg();
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
        MqoChat.reply(Component.literal("\u00A76[MQO] \u00A7fCommission HUD debug \u00A77(tab lines: " + tabLines.size() + ")"));
        for (String line : tabLines) {
            CommissionEntry parsed = parseCommission(line);
            if (parsed != null) {
                MqoChat.reply(Component.literal("\u00A78  '" + line + "' \u00A7a-> commission " + parsed.name() + " " + Math.round(parsed.progress()) + "%"));
            }
        }
        lastDataRefreshAt = 0; // force a fresh evaluation
        refreshCachedState(mc);
        MqoChat.reply(Component.literal("\u00A76[MQO] \u00A7fenabled=" + enabled
            + " \u00A7fallowedLocation=" + cachedAllowedLocation
            + " \u00A7fcommissions=" + cachedEntries.size()
            + " \u00A7fpos=" + hudX + "," + hudY));

        // Which source won matters most when the HUD looks stale right after a claim.
        long age = System.currentTimeMillis() - guiEntriesAt;
        if (guiEntriesAt == 0) {
            MqoChat.reply(Component.literal("\u00A78  menu: \u00A7cnothing parsed \u00A77(open the Royal Pigeon once)"));
        } else {
            MqoChat.reply(Component.literal("\u00A78  menu: " + guiEntries.size() + " entries, "
                + (age / 1000) + "s ago, filter=" + (guiFilterArea == null ? "?" : guiFilterArea)
                + " \u00A77"
                + (age < GUI_OVERRIDE_MS ? "\u00A7a(overriding the tab list)" : "\u00A78(expired, using tab)")));
            for (int i = 0; i < guiEntries.size(); i++) {
                CommissionEntry entry = guiEntries.get(i);
                MqoChat.reply(Component.literal("\u00A78    slot "
                    + (i < guiSlots.size() ? guiSlots.get(i) : -1) + ": " + entry.name() + " "
                    + Math.round(entry.progress()) + "%"));
            }
        }
    }

    /**
     * The first non-empty lore line after the one matching {@code anchor}.
     *
     * <p>The lore is positional, not labelled: the commission's real name follows the three-line
     * blurb that ends "rewards.", and its percentage follows a bare "Progress" line. Anchoring on
     * those and skipping blanks avoids depending on fixed indices, which shift as Hypixel adds or
     * removes blank separator lines.
     */
    private static @Nullable String loreValueAfter(List<String> lore,
                                                   java.util.function.Predicate<String> anchor) {
        for (int i = 0; i < lore.size(); i++) {
            if (!anchor.test(lore.get(i))) {
                continue;
            }
            for (int j = i + 1; j < lore.size(); j++) {
                if (!lore.get(j).isEmpty()) {
                    return lore.get(j);
                }
            }
            return null;
        }
        return null;
    }

    /** /commhudslots — the raw menu contents, for when nothing parses out of it. */
    public static void dumpMenuSlots() {
        if (lastMenuDump.isEmpty()) {
            MqoChat.reply(Component.literal("\u00A76[MQO] \u00A77No Commissions menu seen yet \u2014 open the Royal Pigeon, then run this."));
            return;
        }
        MqoChat.reply(Component.literal("\u00A76[MQO] \u00A7fCommissions menu \u00A78("
            + ((System.currentTimeMillis() - lastMenuDumpAt) / 1000) + "s ago, "
            + lastMenuDump.size() + " lines)"));
        for (String line : lastMenuDump) {
            MqoChat.reply(Component.literal("\u00A78  " + line));
        }
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

    /**
     * Client tick — reads the Royal Pigeon menu.
     *
     * <p>Has to be a tick rather than part of the render pass: the HUD deliberately stops drawing
     * while any screen is open, so anything hung off rendering never runs at the one moment the menu
     * is actually there to read.
     */
    public static void tick() {
        refreshGuiEntries(Minecraft.getInstance());
    }

    /**
     * Reads the commissions out of the Royal Pigeon menu while it is open.
     *
     * <p>The menu is correct the instant a commission is claimed; the tab list keeps advertising the
     * old one for several seconds afterwards, which is what made the HUD look stuck.
     */
    private static void refreshGuiEntries(Minecraft mc) {
        if (mc.screen == null || mc.player == null || mc.player.containerMenu == null) {
            return;
        }
        String title = mc.screen.getTitle() == null ? "" : mc.screen.getTitle().getString();
        String normalized = title.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT)
            .replaceFirst("^\\(\\d+/\\d+\\)\\s*", "");
        if (!normalized.startsWith("commission")) {
            return;
        }

        List<CommissionEntry> found = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        List<String> dump = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (net.minecraft.world.inventory.Slot slot : mc.player.containerMenu.slots) {
            // The menu's own slots only. The bottom 36 belong to your inventory, and a commission
            // item sitting in there — or any item whose lore happens to parse — is not a commission
            // you can claim from this screen.
            if (slot.container == mc.player.getInventory()) {
                continue;
            }
            net.minecraft.world.item.ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            // getHoverName(), not the CUSTOM_NAME component: that component is only one of the ways
            // a name can be set (item_name is another), and reading it directly meant every slot was
            // skipped whenever Hypixel used a different one.
            String label = stack.getHoverName() == null ? ""
                : stack.getHoverName().getString().replaceAll("§.", "").trim();

            List<String> loreLines = new ArrayList<>();
            net.minecraft.world.item.component.ItemLore lore =
                stack.get(net.minecraft.core.component.DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) {
                    loreLines.add(line.getString().replaceAll("§.", "").trim());
                }
            }

            // Captured whether or not the parse works — a command can't be run while the menu is
            // open, so without recording this there is no way to see what the slots actually hold.
            dump.add("slot " + slot.index + ": '" + label + "'");
            for (String line : loreLines) {
                if (!line.isEmpty()) {
                    dump.add("    | " + line);
                }
            }

            // The item is called "Commission #1" — the real name is inside the lore, so the item name
            // is only useful for telling a commission apart from "Commission Milestones", "Filter"
            // and the rest of the menu furniture.
            if ("Filter".equalsIgnoreCase(label)) {
                // The selected area is the line marked with the arrow.
                for (String line : loreLines) {
                    if (line.startsWith("\u25B6")) {
                        guiFilterArea = line.substring(1).trim();
                        break;
                    }
                }
                continue;
            }
            if (!COMMISSION_ITEM.matcher(label).matches()) {
                continue;
            }

            String name = loreValueAfter(loreLines, line -> line.endsWith("rewards."));
            String progressText = loreValueAfter(loreLines, line -> line.equalsIgnoreCase("Progress"));
            if (name == null || progressText == null) {
                continue;
            }

            // Back through parseCommission so the menu accepts exactly the same commissions the tab
            // list does — same name hints, same texture check — rather than a second, divergent set.
            CommissionEntry parsed = parseCommission(name + ": " + progressText);
            if (parsed != null && seen.add(parsed.name().toLowerCase(Locale.ROOT))) {
                found.add(parsed);
                slots.add(slot.index);
            }
        }

        if (!dump.isEmpty()) {
            lastMenuDump = dump;
            lastMenuDumpAt = System.currentTimeMillis();
        }
        if (!found.isEmpty()) {
            guiEntries = found;
            guiSlots = slots;
            guiEntriesAt = System.currentTimeMillis();
        }
    }

    /** Shared with CommStatsHUD, which shows in the same areas. */
    static boolean isAllowedHudLocation(Minecraft mc) {
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

        // A recent menu reading replaces the tab list outright rather than merging with it. Merging
        // would defeat the point: the commission you just claimed is absent from the menu but still
        // present in the tab list, so it would simply come back in from there.
        // Unknown filter still overrides: a parse miss on the Filter item should not silently
        // disable this, only a filter that is definitely showing the wrong area.
        boolean filterUsable = guiFilterArea == null
            || guiFilterArea.toLowerCase(Locale.ROOT).contains("glacite");
        if (!guiEntries.isEmpty()
                && filterUsable
                && System.currentTimeMillis() - guiEntriesAt < GUI_OVERRIDE_MS
                && guiEntries.size() >= result.size()) {
            return guiEntries;
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
