package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Commission HUD.
 *
 * <p>Data model ported from SkyblockCollectionTracker (ChindeaOne/SkyblockCollectionTracker-fabric,
 * {@code CommissionWidget} + {@code CommissionUtils}); rendering is ours (Vexel NanoVG).
 *
 * <p>The model is one index-aligned list. The tab list's "Commissions:" widget populates it; while
 * the Royal Pigeon menu is open, container slot updates replace entries in place by index — so a
 * claim shows the new commission the instant Hypixel swaps the slot's item, rather than whenever
 * the tab list catches up. A snapshot of the pre-claim state is kept so the tab list's stale
 * re-broadcast a few seconds later cannot resurrect the claimed commission.
 */
public class CommissionHUD {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("MiningQOL");
    public enum LayoutMode {
        GRID,
        COLUMN
    }

    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("miningqol", "commission_hud");
    private static final long LOCATION_REFRESH_INTERVAL_MS = 200L;
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern FRACTION_PROGRESS_PATTERN = Pattern.compile("(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)");
    private static final Pattern STRIP_PROGRESS_SUFFIX = Pattern.compile(
        "[:\\-\\s]*((\\d+(?:\\.\\d+)?)%|(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)|DONE|COMPLETED|CLAIMABLE|CLICK TO CLAIM)$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NON_TEXTURE_KEY_CHARS = Pattern.compile("[^a-z0-9]+");
    /** Tab widget header, colour codes and all (SCT's TabWidget.COMMISSIONS). */
    private static final Pattern COMMISSIONS_HEADER = Pattern.compile("^\\s*(?:§.)*Commissions:\\s*$");
    private static final Pattern FORMATTING = Pattern.compile("§.");
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
    /** Every commission Hypixel hands out (SCT's CommissionParser.COMMISSIONS). */
    private static final List<String> KNOWN_COMMISSIONS = List.of(
        // Dwarven Mines
        "Mithril Miner", "Titanium Miner",
        "Lava Springs Mithril", "Royal Mines Mithril", "Cliffside Veins Mithril", "Rampart's Quarry Mithril", "Upper Mines Mithril",
        "Lava Springs Titanium", "Royal Mines Titanium", "Cliffside Veins Titanium", "Rampart's Quarry Titanium", "Upper Mines Titanium",
        "Goblin Raid Slayer", "Goblin Raid", "Raffle", "Lucky Raffle", "Glacite Walker Slayer", "Goblin Slayer",
        "Elusive Goblin Slayer", "Treasure Hoarder Puncher", "Star Sentry Puncher", "2x Mithril Powder Collector",
        // Crystal Hollows
        "Hard Stone Miner", "Jade Gemstone Collector", "Amber Gemstone Collector", "Topaz Gemstone Collector",
        "Sapphire Gemstone Collector", "Amethyst Gemstone Collector", "Ruby Gemstone Collector", "Chest Looter",
        "Team Treasurite Member Slayer", "Sludge Slayer", "Yog Slayer", "Automaton Slayer", "Thyst Slayer",
        "Boss Corleone Slayer", "Jade Crystal Hunter", "Amber Crystal Hunter", "Topaz Crystal Hunter",
        "Sapphire Crystal Hunter", "Amethyst Crystal Hunter",
        // Glacite Tunnels
        "Mineshaft Explorer", "Corpse Looter", "Maniac Slayer", "Scrap Collector", "Onyx Gemstone Collector",
        "Aquamarine Gemstone Collector", "Peridot Gemstone Collector", "Citrine Gemstone Collector",
        "Glacite Collector", "Umber Collector", "Tungsten Collector"
    );
    private static final Map<String, Float> ANIMATED_PROGRESS = new HashMap<>();

    /** Vanilla tab-list ordering, so the widget's lines come out in the order the tab shows them. */
    private static final Comparator<PlayerInfo> TAB_ORDER = Comparator
        .comparingInt((PlayerInfo info) -> info.getGameMode() == GameType.SPECTATOR ? 1 : 0)
        .thenComparing(info -> info.getTeam() == null ? "" : info.getTeam().getName())
        .thenComparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER);

    private static boolean registered = false;
    private static boolean nvgHooked = false;
    private static boolean enabled = true;
    private static final HudAnchor ANCHOR = new HudAnchor(10, 90, CommissionHUD::getWidth, CommissionHUD::getHeight);
    private static float scale = 1.0f;
    private static boolean backgroundEnabled = true;
    private static LayoutMode layoutMode = LayoutMode.GRID;
    private static int lastWidth = 220;
    private static int lastHeight = 118;
    private static long lastLocationRefreshAt = 0L;
    private static boolean cachedAllowedLocation = false;

    // ===== SCT-style commission model =====

    /** The commissions, index-aligned with the tab widget's lines and the menu's "Commission #N" items. */
    private static List<CommissionEntry> commissions = new ArrayList<>();
    /**
     * Pre-claim snapshots (SCT's {@code ignoredStates}).
     *
     * <p>When a slot update swaps a completed commission for a fresh one of a different type, the
     * list as it stood before the swap is remembered. The tab list keeps advertising exactly that
     * state for a few seconds afterwards; a tab update matching a snapshot is treated as stale
     * rather than applied, otherwise the claimed commission would pop straight back.
     *
     * <p>SCT matches the whole state once and drops it; matching on names only, with an expiry,
     * survives progress-precision differences between menu and tab and a tab that re-emits the
     * stale listing more than once.
     */
    private static final List<IgnoredState> ignoredStates = new ArrayList<>();
    private static final long IGNORED_STATE_MS = 20_000L;
    private static List<String> tabCache = List.of();
    private static int tickCounter = 0;
    private static ClientLevel lastLevel = null;
    /**
     * Which area the menu's Filter was showing, or null if it could not be read.
     *
     * <p>The menu lists whichever area the filter is set to, but this HUD only ever appears in the
     * Glacite areas. Replacing Glacite entries with a Dwarven Mines listing by index would be worse
     * than the tab-list lag the slot hook exists to fix.
     */
    private static String menuFilterArea = null;
    /** Raw slot contents of the last Commissions menu seen, parsed or not — for /commhudslots. */
    private static List<String> lastMenuDump = List.of();
    private static long lastMenuDumpAt = 0L;
    /** Keep the panel up over Hypixel menus, so a claim is visible as it happens. */
    private static boolean showOverMenus = true;
    /** Which reading last touched the list — surfaced by /commhuddebug. */
    private static String lastSource = "none";
    /**
     * The container menu identified as the Commissions menu.
     *
     * <p>Identified once by the screen title, then matched by object (SCT's attachedCommissionMenu)
     * so a slot packet still counts even if the screen has just been swapped or closed client-side.
     */
    private static AbstractContainerMenu trackedMenu = null;

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
        ANCHOR.set(x, y);
    }

    /** Edge anchor for the config — see {@link HudAnchor}. */
    public static HudAnchor anchor() { return ANCHOR; }

    public static int getX() {
        return ANCHOR.x();
    }

    public static int getY() {
        return ANCHOR.y();
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

    public static boolean isShowOverMenus() {
        return showOverMenus;
    }

    public static void setShowOverMenus(boolean value) {
        showOverMenus = value;
    }

    /** /commhuddebug — shows what the model and the location gate see so "HUD not showing" is diagnosable. */
    public static void debugDump() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        List<String> tab = readTabRaw(mc);
        List<String> widget = commissionWidgetLines(tab);
        MqoChat.reply(Component.literal("§6[MQO] §fCommission HUD debug §7(tab lines: " + tab.size()
            + ", widget: " + (widget == null ? "§cnot found" : widget.size() + " lines") + "§7)"));
        if (widget != null) {
            for (String line : widget) {
                CommissionEntry parsed = parseCommission(line);
                MqoChat.reply(Component.literal("§8  '" + line + "' "
                    + (parsed == null ? "§c-> no parse" : "§a-> " + parsed.name() + " " + Math.round(parsed.progress()) + "%")));
            }
        }
        lastLocationRefreshAt = 0; // force a fresh evaluation
        refreshLocation(mc);
        MqoChat.reply(Component.literal("§6[MQO] §fenabled=" + enabled
            + " §fallowedLocation=" + cachedAllowedLocation
            + " §fcommissions=" + commissions.size()
            + " §fsource=" + lastSource
            + " §fmenuOpen=" + isCommissionMenuOpen(mc)
            + " §fpos=" + ANCHOR.x() + "," + ANCHOR.y()));
        for (int i = 0; i < commissions.size(); i++) {
            CommissionEntry entry = commissions.get(i);
            MqoChat.reply(Component.literal("§8  #" + (i + 1) + ": " + entry.name() + " " + Math.round(entry.progress()) + "%"));
        }
        expireIgnoredStates();
        MqoChat.reply(Component.literal("§8  ignored tab states: " + ignoredStates.size()
            + ", filter=" + (menuFilterArea == null ? "?" : menuFilterArea)));
    }

    /** /commhudslots — the raw menu contents, for when nothing parses out of it. */
    public static void dumpMenuSlots() {
        if (lastMenuDump.isEmpty()) {
            MqoChat.reply(Component.literal("§6[MQO] §7No Commissions menu seen yet — open the Royal Pigeon, then run this."));
            return;
        }
        MqoChat.reply(Component.literal("§6[MQO] §fCommissions menu §8("
            + ((System.currentTimeMillis() - lastMenuDumpAt) / 1000) + "s ago, "
            + lastMenuDump.size() + " lines)"));
        for (String line : lastMenuDump) {
            MqoChat.reply(Component.literal("§8  " + line));
        }
    }

    // ===== NanoVG rendering — matches the 1.21 look (Vexel font, glow, rounded bars) =====

    private static final Map<String, xyz.meowing.vexel.api.style.Image> ICON_CACHE = new HashMap<>();
    private static final java.util.Set<String> ICON_FAILED = new java.util.HashSet<>();

    /** Called every frame from Vexel's render hook (inside beginFrame/endFrame). */
    public static void renderNvg() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        boolean editor = mc.screen instanceof forfun.miningqol.client.gui.CommissionHudPositionScreen
            || (mc.screen instanceof forfun.miningqol.client.gui.HudPositionScreen && enabled);
        if (editor) {
            List<CommissionEntry> sample = commissions.isEmpty()
                ? List.of(new CommissionEntry("Mithril Miner", 62.0), new CommissionEntry("Goblin Slayer", 31.0),
                          new CommissionEntry("Titanium Miner", 100.0), new CommissionEntry("Glacite Walker Slayer", 8.5))
                : commissions;
            drawPanelNvg(sample, true);
            return;
        }

        if (!enabled) {
            return;
        }
        // Vexel's NanoVG pass draws on top of open screens, so anything left un-gated paints
        // over the screen. Chat is allowed by convention, and the Commissions menu itself when
        // showOverMenus is on — watching the panel update as you claim is the point of that
        // toggle. Every other screen hides it, the same as the vanilla-drawn HUDs.
        if (mc.screen != null
                && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)
                && !(showOverMenus && isCommissionMenuOpen(mc))) {
            return;
        }
        refreshLocation(mc);
        if (!cachedAllowedLocation || commissions.isEmpty()) {
            return;
        }
        drawPanelNvg(commissions, false);
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

        float x = ANCHOR.x() * f;
        float y = ANCHOR.y() * f;
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

    // ===== Data: tab widget =====

    /**
     * Client tick — SCT's TabData: every 4 ticks, re-read the tab list and, only if it changed,
     * re-parse the Commissions widget. Skipped while the menu is open: the slot hook owns the
     * list then, and the tab list is the laggier source.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            commissions = new ArrayList<>();
            ignoredStates.clear();
            tabCache = List.of();
            menuFilterArea = null;
            trackedMenu = null;
        }
        if (++tickCounter % 4 != 0) {
            return;
        }
        if (isCommissionMenuOpen(mc)) {
            return;
        }
        List<String> tab = readTabRaw(mc);
        if (tab.isEmpty() || tab.equals(tabCache)) {
            return;
        }
        tabCache = tab;
        onTabUpdate(tab);
    }

    /** SCT's CommissionWidget.onTabWidgetsUpdate. */
    private static void onTabUpdate(List<String> tab) {
        List<String> widget = commissionWidgetLines(tab);
        List<String> lines;
        if (widget != null) {
            lines = widget;
        } else {
            // No "Commissions:" header (widget hidden, or Hypixel changed the format): fall back
            // to any tab line that reads like a commission, still in tab order.
            lines = new ArrayList<>();
            for (String raw : tab) {
                lines.add(FORMATTING.matcher(raw).replaceAll("").trim());
            }
        }

        List<CommissionEntry> parsed = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String line : lines) {
            CommissionEntry entry = parseCommission(line);
            if (entry != null && seen.add(entry.name().toLowerCase(Locale.ROOT))) {
                parsed.add(entry);
            }
        }
        if (parsed.isEmpty()) {
            // Widget present but nothing parsed (or absent): keep what we have. Leaving the area
            // is handled by the location gate and the world-change reset, not by the tab list.
            return;
        }
        if (parsed.equals(commissions)) {
            return;
        }

        expireIgnoredStates();
        for (IgnoredState ignored : ignoredStates) {
            if (!ignored.sameNames(parsed)) {
                continue;
            }
            // Stale: the tab still lists the commission that was just claimed. Keep the
            // menu-driven list, but progress of the entries it agrees on is fine to take —
            // never lower, since progress only climbs and the tab lags.
            for (int i = 0; i < parsed.size() && i < commissions.size(); i++) {
                CommissionEntry mine = commissions.get(i);
                CommissionEntry theirs = parsed.get(i);
                if (mine.name().equalsIgnoreCase(theirs.name()) && theirs.progress() > mine.progress()) {
                    commissions.set(i, new CommissionEntry(mine.name(), theirs.progress()));
                }
            }
            lastSource = "tab(stale, ignored)";
            LOGGER.info("[CommHUD] tab update ignored as stale: {}", parsed);
            return;
        }

        commissions = parsed;
        lastSource = "tab";
        LOGGER.info("[CommHUD] tab -> {}", parsed);
    }

    /** SCT's TabWidget.update, for the one widget this HUD cares about: header line, then the indented body. */
    private static @Nullable List<String> commissionWidgetLines(List<String> tab) {
        for (int i = 0; i < tab.size(); i++) {
            if (!COMMISSIONS_HEADER.matcher(tab.get(i)).matches()) {
                continue;
            }
            List<String> body = new ArrayList<>();
            for (int j = i + 1; j < tab.size() && body.size() < 10; j++) {
                String line = tab.get(j);
                String noReset = line.startsWith("§r") ? line.substring(2) : line;
                if (!noReset.startsWith(" ")) {
                    break;
                }
                String clean = FORMATTING.matcher(line).replaceAll("").trim();
                if (!clean.isEmpty()) {
                    body.add(clean);
                }
            }
            return body;
        }
        return null;
    }

    /** The tab list as displayed — colour codes intact, in tab order — the way SCT's TabData reads it. */
    private static List<String> readTabRaw(Minecraft mc) {
        if (mc.getConnection() == null || mc.gui == null) {
            return List.of();
        }
        List<PlayerInfo> infos = new ArrayList<>(mc.getConnection().getListedOnlinePlayers());
        infos.sort(TAB_ORDER);
        PlayerTabOverlay overlay = mc.gui.getTabList();
        List<String> out = new ArrayList<>(infos.size());
        for (PlayerInfo info : infos) {
            Component name = overlay.getNameForDisplay(info);
            out.add(name == null ? "" : name.getString());
        }
        return out;
    }

    // ===== Data: Royal Pigeon menu =====

    /** Whether the commissions menu is the open screen right now. */
    static boolean isCommissionMenuOpen(Minecraft mc) {
        if (mc.screen == null) {
            return false;
        }
        String title = mc.screen.getTitle() == null ? "" : mc.screen.getTitle().getString();
        return FORMATTING.matcher(title).replaceAll("").toLowerCase(Locale.ROOT).contains("commission");
    }

    private static boolean isOurCommissionMenu(Minecraft mc, AbstractContainerMenu menu) {
        if (mc.player == null) {
            return false;
        }
        if (menu == trackedMenu) {
            return true;
        }
        if (mc.player.containerMenu == menu && isCommissionMenuOpen(mc)) {
            trackedMenu = menu;
            LOGGER.info("[CommHUD] tracking commissions menu id={} slots={}", menu.containerId, menu.slots.size());
            return true;
        }
        return false;
    }

    /**
     * Mixin entry — a single slot of a container changed (SCT's CommissionUtils.onSlotUpdated).
     *
     * <p>This is the packet Hypixel sends when a claim swaps the finished commission for the new
     * one, so the list updates the moment it lands.
     */
    public static void onMenuSlotSet(AbstractContainerMenu menu, int slotIndex, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (!isOurCommissionMenu(mc, menu) || slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(slotIndex);
        if (slot.container == mc.player.getInventory() || stack.isEmpty()) {
            return;
        }
        List<String> dump = new ArrayList<>();
        dumpSlot(slot, stack, dump);
        String label = cleanName(stack);
        List<String> lore = loreOf(stack);
        if ("Filter".equalsIgnoreCase(label)) {
            readFilter(lore);
        } else if (isCommissionItem(label, lore)) {
            // Index = this slot's rank among the menu's commission items (SCT's slot layouts
            // 11/13/15 and 11/12/14/15, without hardcoding them). The menu already holds the new
            // stack at this point, and a claim only touches the claimed slot, so the rank is stable.
            int index = 0;
            for (Slot other : menu.slots) {
                if (other.index >= slotIndex) {
                    break;
                }
                if (other.container != mc.player.getInventory() && isCommissionStack(other.getItem())) {
                    index++;
                }
            }
            applyMenuSlot(slot, label, lore, index);
        }
        // Fold the single-slot update into the last dump so /commhudslots stays current.
        List<String> merged = new ArrayList<>(lastMenuDump);
        merged.addAll(dump);
        lastMenuDump = merged;
        lastMenuDumpAt = System.currentTimeMillis();
    }

    /** Mixin entry — the whole container was (re)sent, as on menu open or a Hypixel refresh. */
    public static void onMenuContents(AbstractContainerMenu menu) {
        Minecraft mc = Minecraft.getInstance();
        if (!isOurCommissionMenu(mc, menu)) {
            return;
        }
        readWholeMenu(mc, menu);
    }

    /**
     * Full-menu read: the Filter item first (it gates the rest), then the commission items in
     * slot order so a list that is still empty fills in the right sequence.
     */
    private static void readWholeMenu(Minecraft mc, AbstractContainerMenu menu) {
        List<String> dump = new ArrayList<>();
        List<Slot> commissionSlots = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Slot slot : menu.slots) {
            // The menu's own slots only. The bottom 36 belong to your inventory, and a commission
            // item sitting in there is not a commission you can claim from this screen.
            if (slot.container == mc.player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            // Captured whether or not the parse works — a command can't be run while the menu is
            // open, so without recording this there is no way to see what the slots actually hold.
            dumpSlot(slot, stack, dump);
            String label = cleanName(stack);
            labels.add(slot.index + ":" + label);
            if ("Filter".equalsIgnoreCase(label)) {
                readFilter(loreOf(stack));
            } else if (isCommissionItem(label, loreOf(stack))) {
                commissionSlots.add(slot);
            }
        }
        LOGGER.info("[CommHUD] menu contents: {} commission items, filter={}, items={}",
            commissionSlots.size(), menuFilterArea, labels);
        for (int i = 0; i < commissionSlots.size(); i++) {
            Slot slot = commissionSlots.get(i);
            applyMenuSlot(slot, cleanName(slot.getItem()), loreOf(slot.getItem()), i);
        }
        if (!dump.isEmpty()) {
            lastMenuDump = dump;
            lastMenuDumpAt = System.currentTimeMillis();
        }
    }

    private static void dumpSlot(Slot slot, ItemStack stack, List<String> dump) {
        dump.add("slot " + slot.index + ": '" + cleanName(stack) + "'");
        for (String line : loreOf(stack)) {
            if (!line.isEmpty()) {
                dump.add("    | " + line);
            }
        }
    }

    private static boolean isCommissionStack(ItemStack stack) {
        return !stack.isEmpty() && isCommissionItem(cleanName(stack), loreOf(stack));
    }

    /**
     * Whether a menu item is one of the commissions, judged by its lore as SCT does — the item's
     * own name is not relied on (it is not "Commission #N", whatever it is). A lore line that is a
     * known commission name is the primary signal; the rewards blurb plus a progress/COMPLETED
     * line is the fallback for a commission the list does not know. "Commission Milestones" and
     * the rest of the menu furniture have neither.
     */
    private static boolean isCommissionItem(String label, List<String> lore) {
        if (label.toLowerCase(Locale.ROOT).contains("milestone")) {
            return false;
        }
        for (String line : lore) {
            for (String known : KNOWN_COMMISSIONS) {
                if (line.equalsIgnoreCase(known)) {
                    return true;
                }
            }
        }
        boolean rewards = false;
        boolean progress = false;
        for (String line : lore) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.endsWith("rewards.")) rewards = true;
            if (lower.contains("progress") || lower.contains("completed")) progress = true;
        }
        return rewards && progress;
    }

    private static String cleanName(ItemStack stack) {
        // getHoverName(), not the CUSTOM_NAME component: that component is only one of the ways
        // a name can be set (item_name is another), and reading it directly meant every slot was
        // skipped whenever Hypixel used a different one.
        return stack.getHoverName() == null ? ""
            : FORMATTING.matcher(stack.getHoverName().getString()).replaceAll("").trim();
    }

    private static List<String> loreOf(ItemStack stack) {
        List<String> loreLines = new ArrayList<>();
        net.minecraft.world.item.component.ItemLore lore =
            stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                loreLines.add(FORMATTING.matcher(line.getString()).replaceAll("").trim());
            }
        }
        return loreLines;
    }

    private static void readFilter(List<String> lore) {
        // The selected area is the line marked with the arrow.
        for (String line : lore) {
            if (line.startsWith("▶")) {
                menuFilterArea = line.substring(1).trim();
                return;
            }
        }
    }

    /**
     * One menu item into the list (SCT's CommissionUtils.onSlotUpdated + CommissionWidget.updateCommission).
     *
     * <p>Replaces the entry at the item's index. A completed entry giving way to a fresh one of a
     * different type is a claim: the pre-claim list is snapshotted so the tab list cannot undo it.
     */
    private static void applyMenuSlot(Slot slot, String label, List<String> loreLines, int index) {
        // Only a filter that is definitely showing another area blocks the update. Anything
        // else — unknown, unparsed, or a label Hypixel renames — must not silently disable this
        // and leave the HUD on the laggy tab list.
        if (menuFilterArea != null && equalsAnyIgnoreCase(menuFilterArea, "Dwarven Mines", "Crystal Hollows")) {
            LOGGER.info("[CommHUD] menu slot {} ignored: filter shows {}", slot.index, menuFilterArea);
            return;
        }

        String name = findCommissionName(loreLines);
        if (name == null) {
            LOGGER.info("[CommHUD] menu slot {} ({}) ignored: no commission name in lore {}", slot.index, label, loreLines);
            return;
        }
        String progressText = findProgressText(loreLines);
        // Back through parseCommission so the menu accepts exactly the same commissions the tab
        // list does — same name check — rather than a second, divergent set. A fresh commission
        // with no progress line yet is 0%, as in SCT.
        CommissionEntry updated = parseCommission(name + ": " + (progressText == null ? "0%" : progressText));
        if (updated == null) {
            LOGGER.info("[CommHUD] menu slot {} ({}) ignored: '{}' / '{}' did not parse", slot.index, label, name, progressText);
            return;
        }

        if (index < commissions.size()) {
            CommissionEntry current = commissions.get(index);
            if (current.equals(updated)) {
                return;
            }
            if (current.progress() >= 100.0 && updated.progress() <= 0.0
                    && !current.name().equalsIgnoreCase(updated.name())) {
                ignoredStates.add(IgnoredState.of(commissions, System.currentTimeMillis()));
                LOGGER.info("[CommHUD] claim detected at #{}: {} -> {}", index + 1, current, updated);
            }
            commissions.set(index, updated);
        } else if (index == commissions.size()) {
            commissions.add(updated);
        } else {
            // Slot arrived ahead of its predecessors with nothing to align to; the full-contents
            // read that follows a menu open orders them, so nothing is lost by waiting.
            LOGGER.info("[CommHUD] menu slot {} ({}) deferred: index {} but list has {}", slot.index, label, index, commissions.size());
            return;
        }
        lastSource = "menu";
        LOGGER.info("[CommHUD] menu -> #{} {}", index + 1, updated);
    }

    /**
     * The commission's name out of its lore.
     *
     * <p>SCT's approach first: any lore line that is itself a known commission name. Positional
     * fallback — the name follows the rewards blurb — for a commission the list does not know.
     */
    private static @Nullable String findCommissionName(List<String> lore) {
        for (String line : lore) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            for (String known : KNOWN_COMMISSIONS) {
                if (trimmed.equalsIgnoreCase(known)) {
                    return known;
                }
            }
        }
        return loreValueAfter(lore, line -> line.endsWith("rewards."));
    }

    /**
     * Progress out of a commission item's lore.
     *
     * <p>COMPLETED is checked first and on its own: a finished commission drops the percentage
     * entirely. The percentage is then looked for on the "Progress" line itself and the two
     * after it, rather than assuming it sits on the next non-empty line.
     */
    private static @Nullable String findProgressText(List<String> lore) {
        for (String line : lore) {
            if (line.toLowerCase(Locale.ROOT).contains("completed")) {
                return "100%";
            }
        }
        for (int i = 0; i < lore.size(); i++) {
            if (!lore.get(i).toLowerCase(Locale.ROOT).contains("progress")) {
                continue;
            }
            for (int j = i; j <= Math.min(i + 2, lore.size() - 1); j++) {
                Matcher matcher = PROGRESS_PATTERN.matcher(lore.get(j));
                if (matcher.find()) {
                    return matcher.group();
                }
            }
        }
        return null;
    }

    /** The first non-empty lore line after the one matching {@code anchor}. */
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

    // ===== Data: chat =====

    /** Marks a commission complete the moment chat says so (SCT's completeCollectorCommission). */
    public static void onCommissionComplete(String message) {
        if (message == null) return;
        String clean = FORMATTING.matcher(message).replaceAll("").trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("commission complete");
        if (idx < 0) return;

        // Hypixel writes this both ways round — "Mithril Miner Commission Complete!" and
        // "COMMISSION COMPLETE! Mithril Miner" — so take whichever side carries the name
        // rather than assuming it comes first.
        String before = clean.substring(0, idx).trim();
        String after = clean.substring(idx + "commission complete".length()).trim();
        String name = !before.isEmpty() ? before : after;
        name = name.replaceAll("^[^A-Za-z0-9]+", "").replaceAll("[^A-Za-z0-9)\\s]+$", "").trim();
        if (name.isEmpty()) return;

        String key = name.toLowerCase(Locale.ROOT);
        for (int i = 0; i < commissions.size(); i++) {
            CommissionEntry entry = commissions.get(i);
            String entryKey = entry.name().toLowerCase(Locale.ROOT);
            if (entryKey.equals(key) || key.contains(entryKey) || entryKey.contains(key)) {
                if (entry.progress() < 100.0) {
                    commissions.set(i, new CommissionEntry(entry.name(), 100.0));
                    lastSource = "chat";
                    LOGGER.info("[CommHUD] chat -> #{} {} DONE", i + 1, entry.name());
                }
                return;
            }
        }
    }

    // ===== Data: shared helpers =====

    private static void refreshLocation(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastLocationRefreshAt < LOCATION_REFRESH_INTERVAL_MS) {
            return;
        }
        lastLocationRefreshAt = now;
        cachedAllowedLocation = isAllowedHudLocation(mc);
    }

    private static void expireIgnoredStates() {
        long now = System.currentTimeMillis();
        ignoredStates.removeIf(s -> now - s.at() > IGNORED_STATE_MS);
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
                String clean = FORMATTING.matcher(line).replaceAll("").trim();
                if (!clean.isEmpty()) {
                    result.add(clean);
                }
            }
        }

        return result;
    }

    /** "Name: 12.5%" / "Name: DONE" -> entry, or null if the line is not a commission. */
    private static @Nullable CommissionEntry parseCommission(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String clean = FORMATTING.matcher(raw).replaceAll("").trim();
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

        for (String known : KNOWN_COMMISSIONS) {
            if (lower.startsWith(known.toLowerCase(Locale.ROOT))) {
                return true;
            }
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

    private record CommissionEntry(String name, double progress) {
    }

    /** Names of the list as it stood just before a claim, plus when — see {@link #ignoredStates}. */
    private record IgnoredState(List<String> names, long at) {
        static IgnoredState of(List<CommissionEntry> entries, long at) {
            return new IgnoredState(entries.stream().map(e -> e.name().toLowerCase(Locale.ROOT)).toList(), at);
        }

        boolean sameNames(List<CommissionEntry> entries) {
            if (entries.size() != names.size()) {
                return false;
            }
            for (int i = 0; i < names.size(); i++) {
                if (!names.get(i).equals(entries.get(i).name().toLowerCase(Locale.ROOT))) {
                    return false;
                }
            }
            return true;
        }
    }
}
