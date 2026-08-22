package forfun.miningqol.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Appends pet-flip data (coins per pet XP, from pets.adong.dev) to the tooltip of
 * any SkyBlock pet item — the same numbers as the Pet Flip Tracker website. Data
 * is fetched lazily on first pet hover and cached; the tooltip never blocks on
 * the network.
 */
public class PetFlipTooltip {
    private static final Logger LOGGER = LoggerFactory.getLogger("PetFlipTooltip");

    private static final String API_URL = "https://pets.adong.dev/api/flips";
    private static final int TIMEOUT_MS = 5000;
    private static final long REFRESH_MS = 10 * 60 * 1000; // scanner updates every ~15 min
    private static final long RETRY_MS = 60 * 1000;

    private static final Pattern PET_NAME = Pattern.compile("\\[Lvl (\\d+)\\] (.+)");
    private static final String[] TIERS = {"MYTHIC", "LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON"};
    // \b keeps COMMON from matching inside UNCOMMON.
    private static final Pattern[] TIER_PATTERNS = java.util.Arrays.stream(TIERS)
        .map(t -> Pattern.compile("\\b" + t + "\\b")).toArray(Pattern[]::new);

    private static boolean enabled = true;

    private static volatile Map<String, JsonObject> petsByName = null;
    private static volatile long dataUpdatedAt = 0;   // "updated" timestamp from the payload
    private static volatile long fetchedAt = 0;
    private static volatile long lastAttempt = 0;
    private static final AtomicBoolean fetching = new AtomicBoolean(false);

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }

    /** Called from ItemStackMixin at the end of getTooltip. Appends to {@code lines}. */
    public static void appendTooltip(ItemStack stack, List<Text> lines) {
        if (!enabled || stack == null || lines == null || lines.isEmpty()) return;

        Matcher m = PET_NAME.matcher(stack.getName().getString());
        if (!m.find()) return;
        int hoverLevel;
        try {
            hoverLevel = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return;
        }
        String petName = m.group(2).replace("✦", "").trim().toLowerCase(Locale.ROOT);

        ensureFresh();
        Map<String, JsonObject> pets = petsByName;
        if (pets == null) {
            lines.add(Text.literal("Pet flip: loading prices…").formatted(Formatting.DARK_GRAY));
            return;
        }

        JsonObject pet = pets.get(petName);
        if (pet == null) return;

        String tier = tierFromTooltip(lines);
        if (tier == null && pet.has("defaultTier")) tier = pet.get("defaultTier").getAsString();
        JsonObject rarities = pet.has("rarities") ? pet.getAsJsonObject("rarities") : null;
        if (rarities == null || tier == null) return;
        JsonObject r = rarities.has(tier) ? rarities.getAsJsonObject(tier) : null;
        if (r == null && pet.has("defaultTier")) r = rarities.getAsJsonObject(pet.get("defaultTier").getAsString());
        if (r == null || !r.has("best") || !r.has("sell") || !r.has("perXp") || r.get("perXp").isJsonNull()) return;

        JsonObject best = r.getAsJsonObject("best");
        JsonObject sell = r.getAsJsonObject("sell");
        long ageMin = Math.max(0, (System.currentTimeMillis() - dataUpdatedAt) / 60000);

        lines.add(Text.empty());
        lines.add(Text.literal("Pet Flip ").formatted(Formatting.GOLD)
            .append(Text.literal("(data " + ageMin + "m old)").formatted(Formatting.DARK_GRAY)));
        lines.add(Text.literal("Best: buy Lvl " + best.get("level").getAsInt()
                + " for " + compact(best.get("price").getAsDouble())
                + " → sell Lvl " + sell.get("level").getAsInt()).formatted(Formatting.GRAY));
        double bestProfit = best.get("profit").getAsDouble();
        lines.add(Text.literal("Profit " + compact(bestProfit) + " · "
                + String.format(Locale.ROOT, "%.2f", r.get("perXp").getAsDouble()) + " coins/XP")
            .formatted(bestProfit >= 0 ? Formatting.GREEN : Formatting.RED));

        // If the market has a listing at this pet's own level, show the flip from here.
        JsonObject atLevel = levelEntry(r, hoverLevel);
        if (atLevel != null && hoverLevel != best.get("level").getAsInt()
                && atLevel.has("perXp") && !atLevel.get("perXp").isJsonNull()) {
            double profit = atLevel.get("profit").getAsDouble();
            lines.add(Text.literal("From Lvl " + hoverLevel + ": profit " + compact(profit) + " · "
                    + String.format(Locale.ROOT, "%.2f", atLevel.get("perXp").getAsDouble()) + " coins/XP")
                .formatted(Formatting.GRAY));
        }
    }

    /** Finds the rarity by scanning the existing tooltip from the bottom (Hypixel puts it in the lore). */
    private static String tierFromTooltip(List<Text> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i).getString().toUpperCase(Locale.ROOT);
            for (int t = 0; t < TIERS.length; t++) {
                if (TIER_PATTERNS[t].matcher(line).find()) return TIERS[t];
            }
        }
        return null;
    }

    private static JsonObject levelEntry(JsonObject rarity, int level) {
        if (!rarity.has("levels")) return null;
        JsonArray levels = rarity.getAsJsonArray("levels");
        for (int i = 0; i < levels.size(); i++) {
            JsonObject l = levels.get(i).getAsJsonObject();
            if (l.get("level").getAsInt() == level) return l;
        }
        return null;
    }

    private static String compact(double n) {
        double abs = Math.abs(n);
        if (abs >= 1e9) return String.format(Locale.ROOT, "%.2fB", n / 1e9);
        if (abs >= 1e6) return String.format(Locale.ROOT, "%.1fM", n / 1e6);
        if (abs >= 1e3) return String.format(Locale.ROOT, "%.0fk", n / 1e3);
        return String.valueOf(Math.round(n));
    }

    /** Kicks off a background refresh if the cache is missing or stale. Never blocks. */
    private static void ensureFresh() {
        long now = System.currentTimeMillis();
        if (petsByName != null && now - fetchedAt < REFRESH_MS) return;
        if (now - lastAttempt < RETRY_MS) return;
        if (!fetching.compareAndSet(false, true)) return;
        lastAttempt = now;

        Thread t = new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Sybau-Mod");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                if (conn.getResponseCode() != 200) {
                    LOGGER.warn("[PetFlipTooltip] fetch failed: {}", conn.getResponseCode());
                    return;
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                }
                JsonObject payload = JsonParser.parseString(sb.toString()).getAsJsonObject();
                Map<String, JsonObject> byName = new HashMap<>();
                JsonArray pets = payload.getAsJsonArray("pets");
                for (int i = 0; i < pets.size(); i++) {
                    JsonObject p = pets.get(i).getAsJsonObject();
                    byName.put(p.get("name").getAsString().toLowerCase(Locale.ROOT), p);
                }
                long updated = 0;
                if (payload.has("updated") && !payload.get("updated").isJsonNull()) {
                    try {
                        updated = java.time.Instant.parse(payload.get("updated").getAsString()).toEpochMilli();
                    } catch (Exception ignored) {}
                }
                dataUpdatedAt = updated;
                petsByName = byName;
                fetchedAt = System.currentTimeMillis();
                LOGGER.info("[PetFlipTooltip] loaded {} pets", byName.size());
            } catch (Exception e) {
                LOGGER.warn("[PetFlipTooltip] fetch error: {}", e.getMessage());
            } finally {
                fetching.set(false);
            }
        }, "Sybau-PetFlip");
        t.setDaemon(true);
        t.start();
    }
}
