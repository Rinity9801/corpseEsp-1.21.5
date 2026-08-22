package forfun.miningqol.client.collection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forfun.miningqol.client.profit.BazaarPriceManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * API-based collection profit tracker, modelled on SkyblockCollectionTracker.
 * Polls the backend on a schedule, reads the player's lifetime collection count,
 * and derives collection/hr and coins/hr from the delta since tracking started.
 * Prices come from the existing {@link BazaarPriceManager}.
 */
public class CollectionTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("CollectionTracker");
    // Hypixel only refreshes collection data every few minutes, so polling faster
    // than this just re-reads stale values (SCT uses 180s + a 150s cache).
    private static final long POLL_SECONDS = 180;
    // How many consecutive unchanged polls before we treat the player as AFK and stop.
    // SCT stops after the first unchanged poll; we allow one extra to tolerate a single
    // Hypixel API-lag window while actively mining.
    private static final int STALL_LIMIT = 2;
    // EMA weight for new rate samples (0..1). Higher = more responsive, lower = smoother.
    private static final double RATE_SMOOTHING = 0.5;

    /** material alias -> Hypixel collection key + bazaar enchanted id + display + raw:enchanted ratio. */
    public static final class Mat {
        final String collectionKey;
        final String enchantedId;
        final String display;
        final int ratio;

        Mat(String collectionKey, String enchantedId, String display, int ratio) {
            this.collectionKey = collectionKey;
            this.enchantedId = enchantedId;
            this.display = display;
            this.ratio = ratio;
        }
    }

    // collectionKey values are the exact keys Hypixel uses in members.<uuid>.collection.
    // If Hypixel renames one, fix it here (use `/colltrack list` in-game to see live keys).
    private static final Map<String, Mat> MATS = new LinkedHashMap<>();
    static {
        MATS.put("coal", new Mat("COAL", "ENCHANTED_COAL", "Coal", 160));
        MATS.put("iron", new Mat("IRON_INGOT", "ENCHANTED_IRON", "Iron", 160));
        MATS.put("gold", new Mat("GOLD_INGOT", "ENCHANTED_GOLD", "Gold", 160));
        MATS.put("diamond", new Mat("DIAMOND", "ENCHANTED_DIAMOND", "Diamond", 160));
        MATS.put("lapis", new Mat("INK_SACK:4", "ENCHANTED_LAPIS_LAZULI", "Lapis", 160));
        MATS.put("emerald", new Mat("EMERALD", "ENCHANTED_EMERALD", "Emerald", 160));
        MATS.put("redstone", new Mat("REDSTONE", "ENCHANTED_REDSTONE", "Redstone", 160));
        MATS.put("quartz", new Mat("QUARTZ", "ENCHANTED_QUARTZ", "Quartz", 160));
        MATS.put("obsidian", new Mat("OBSIDIAN", "ENCHANTED_OBSIDIAN", "Obsidian", 160));
        MATS.put("glowstone", new Mat("GLOWSTONE_DUST", "ENCHANTED_GLOWSTONE", "Glowstone", 160));
        MATS.put("mycelium", new Mat("MYCELIUM", "ENCHANTED_MYCELIUM", "Mycelium", 160));
        MATS.put("redsand", new Mat("SAND:1", "ENCHANTED_RED_SAND", "Red Sand", 160));
        MATS.put("hardstone", new Mat("HARD_STONE", "ENCHANTED_HARD_STONE", "Hard Stone", 160));
        MATS.put("mithril", new Mat("MITHRIL_ORE", "ENCHANTED_MITHRIL", "Mithril", 160));
        MATS.put("sulphur", new Mat("SULPHUR_ORE", "ENCHANTED_SULPHUR", "Sulphur", 160));
        MATS.put("umber", new Mat("UMBER", "ENCHANTED_UMBER", "Umber", 160));
    }

    private static ScheduledExecutorService scheduler;
    private static volatile boolean tracking = false;
    private static volatile Mat mat = null;

    private static volatile long startTime = 0;              // clean baseline time (first observed change)
    private static volatile long sessionStartCollection = -1; // clean baseline value, for session totals
    private static volatile long previousCollection = -1;   // value at the previous poll, for stall detection
    private static volatile long lastChangeValue = -1;      // value at the previous *change*, for the delta rate
    private static volatile long lastChangeTime = 0;
    private static volatile long currentCollection = 0;
    private static volatile double collectionPerHour = 0;
    private static volatile double coinsPerHour = 0;
    private static volatile double totalCoins = 0;
    private static volatile int stallCount = 0;             // consecutive unchanged polls
    private static volatile boolean stalled = false;        // last poll showed no change (HUD indicator)

    // HUD position/scale (persisted in MiningConfig, editable via /colltrack move).
    private static volatile int hudX = 10;
    private static volatile int hudY = 120;
    private static volatile float scale = 1.0f;

    public static void setPosition(int x, int y) { hudX = x; hudY = y; }
    public static int getX() { return hudX; }
    public static int getY() { return hudY; }
    public static void setScale(float s) { scale = Math.max(0.5f, Math.min(3.0f, s)); }
    public static float getScale() { return scale; }
    public static int getWidth() { return (int) (108 * scale); }
    public static int getHeight() { return (int) (52 * scale); }

    public static boolean isTracking() {
        return tracking;
    }

    public static String materialsList() {
        return String.join(", ", MATS.keySet());
    }

    private static String playerUuid() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;
        return client.player.getUuid().toString().replace("-", "");
    }

    public static void start(String alias) {
        if (!CollectionApi.isConfigured()) {
            msg("§cBackend not configured. Set BASE_URL in CollectionApi.java (see backend/README.md).");
            return;
        }
        alias = alias.toLowerCase();
        Mat m = MATS.get(alias);
        if (m == null) {
            msg("§cUnknown material '" + alias + "'. Options: §f" + materialsList());
            return;
        }
        String uuid = playerUuid();
        if (uuid == null) {
            msg("§cCould not resolve your UUID (are you in-game?).");
            return;
        }

        stop(false);
        mat = m;
        startTime = 0;                 // clean baseline is set on the first observed change
        sessionStartCollection = -1;
        previousCollection = -1;
        lastChangeValue = -1;
        lastChangeTime = 0;
        currentCollection = 0;
        collectionPerHour = 0;
        coinsPerHour = 0;
        totalCoins = 0;
        stallCount = 0;
        stalled = false;
        tracking = true;

        BazaarPriceManager.updateBlockPrices();
        msg("§aTracking §e" + m.display + " §acollection via API (polling every " + POLL_SECONDS + "s).");
        msg("§7Rate starts after the first change; auto-stops if your collection stops rising.");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Sybau-CollectionPoll");
            t.setDaemon(true);
            return t;
        });
        final String fuuid = uuid;
        scheduler.scheduleAtFixedRate(() -> poll(fuuid), 0, POLL_SECONDS, TimeUnit.SECONDS);
    }

    public static void stop(boolean announce) {
        tracking = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (announce) msg("§cStopped collection tracking.");
    }

    private static void poll(String uuid) {
        try {
            if (!tracking || mat == null) return;
            String body = CollectionApi.fetchCollection(uuid, mat.collectionKey);
            if (body == null) return;
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has(mat.collectionKey)) {
                LOGGER.warn("[CollectionTracker] key {} missing from response: {}", mat.collectionKey, body);
                return;
            }
            calc(json.get(mat.collectionKey).getAsLong());
        } catch (Exception e) {
            LOGGER.error("[CollectionTracker] poll error: {}", e.getMessage());
        }
    }

    private static void calc(long value) {
        long now = System.currentTimeMillis();
        currentCollection = value;

        // Very first poll: this lifetime value is STALE (Hypixel batches its saves), so we
        // only record it. Using it as the baseline would make the first jump — which includes
        // mining done before/at start — divide by a tiny window and spike the rate to millions.
        if (previousCollection < 0) {
            previousCollection = value;
            return;
        }

        // Stall / AFK detection: no change since the last poll. Freeze the rate instead of
        // diluting it with idle time, and end the session after STALL_LIMIT stalls in a row.
        if (value == previousCollection) {
            stalled = true;
            if (++stallCount >= STALL_LIMIT) autoStop();
            return;
        }
        stalled = false;
        stallCount = 0;
        previousCollection = value;

        // First observed *change* flushes whatever Hypixel had buffered before we started.
        // Use it as the clean baseline; a rate needs a second change to compute a delta.
        if (sessionStartCollection < 0) {
            sessionStartCollection = value;
            startTime = now;
            lastChangeValue = value;
            lastChangeTime = now;
            return;
        }

        // Rate = collection gained between the last two *changes*, over the time between them.
        // Both Hypixel samples lag by ~the same amount, so the lag cancels in the delta — this
        // reads the true rate quickly instead of the slow ramp-up of a cumulative average
        // (which is what made coal read ~1.6M/hr instead of ~8M/hr). A light EMA absorbs the
        // chunky save cadence.
        double intervalHours = (now - lastChangeTime) / 3_600_000.0;
        if (intervalHours > 0) {
            double instRate = (value - lastChangeValue) / intervalHours;
            collectionPerHour = collectionPerHour > 0
                ? RATE_SMOOTHING * instRate + (1 - RATE_SMOOTHING) * collectionPerHour
                : instRate;
        }
        lastChangeValue = value;
        lastChangeTime = now;

        double enchPrice = BazaarPriceManager.getBlockPrice(mat.enchantedId);
        long sessionGained = value - sessionStartCollection;
        totalCoins = (sessionGained / (double) mat.ratio) * enchPrice;
        coinsPerHour = (collectionPerHour / (double) mat.ratio) * enchPrice;
    }

    /** Collection stopped rising for STALL_LIMIT polls — end the session so idle time can't skew the rate. */
    private static void autoStop() {
        Mat m = mat;
        stop(false);
        MinecraftClient.getInstance().execute(() ->
            msg("§eStopped tracking " + (m != null ? m.display : "") + " §7— collection stopped rising (AFK / API lag)."));
    }

    /** Discovery helper: dump every collection key the player has, to find exact names. */
    public static void dumpCollections() {
        if (!CollectionApi.isConfigured()) {
            msg("§cBackend not configured. Set BASE_URL in CollectionApi.java.");
            return;
        }
        String uuid = playerUuid();
        if (uuid == null) {
            msg("§cCould not resolve your UUID (are you in-game?).");
            return;
        }
        msg("§7Fetching collections...");
        new Thread(() -> {
            String body = CollectionApi.fetchCollection(uuid, "*");
            MinecraftClient.getInstance().execute(() -> {
                if (body == null) {
                    msg("§cFailed to fetch collections (check backend / Collection API setting).");
                    return;
                }
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    msg("§6=== Collection keys (" + json.size() + ") ===");
                    json.entrySet().forEach(e -> msg("§7" + e.getKey() + ": §f" + e.getValue().getAsLong()));
                } catch (Exception e) {
                    msg("§cParse error: " + e.getMessage());
                }
            });
        }, "Sybau-CollectionDump").start();
    }

    private static void msg(String s) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§6[MQO] " + s), false);
    }

    // ===== HUD =====
    public static void render(DrawContext context) {
        if (!tracking || mat == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        boolean npc = BazaarPriceManager.isUsingNPCPrices();
        String tag = stalled ? " §c(afk)" : (npc ? " §7(npc)" : "");
        boolean hasRate = collectionPerHour > 0;
        draw(context,
            "§f" + mat.display + " Collection" + tag,
            "§7Total: §f" + fmt(currentCollection),
            "§7Coll/hr: §b" + (hasRate ? fmt((long) collectionPerHour) : "§8Calculating..."),
            "§7Coins/hr: §6" + (hasRate ? fmt((long) coinsPerHour) : "§8Calculating..."),
            "§7Session: §a" + fmt((long) totalCoins));
    }

    /** Sample HUD used by the position screen so it stays visible while repositioning. */
    public static void renderPreview(DrawContext context) {
        draw(context,
            "§fMithril Collection",
            "§7Total: §f22,710,831",
            "§7Coll/hr: §b48,000",
            "§7Coins/hr: §61,240,000",
            "§7Session: §a415,000");
    }

    private static void draw(DrawContext context, String title, String l2, String l3, String l4, String l5) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        var m = context.getMatrices();
        m.pushMatrix();
        m.translate(hudX, hudY);
        m.scale(scale);
        int y = 0;
        context.drawTextWithShadow(tr, title, 0, y, 0xFFFFFFFF); y += 12;
        context.drawTextWithShadow(tr, l2, 0, y, 0xFFFFFFFF); y += 10;
        context.drawTextWithShadow(tr, l3, 0, y, 0xFFFFFFFF); y += 10;
        context.drawTextWithShadow(tr, l4, 0, y, 0xFFFFFFFF); y += 10;
        context.drawTextWithShadow(tr, l5, 0, y, 0xFFFFFFFF);
        m.popMatrix();
    }

    private static String fmt(long n) {
        return String.format("%,d", n);
    }
}
