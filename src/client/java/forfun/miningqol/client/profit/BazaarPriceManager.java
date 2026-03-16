package forfun.miningqol.client.profit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BazaarPriceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("BazaarPriceManager");
    private static final String BAZAAR_API = "https://api.hypixel.net/skyblock/bazaar";
    private static final long CACHE_DURATION = 5 * 60 * 1000; 

    private static final Map<String, Double> gemPrices = new HashMap<>();
    private static final Map<String, Double> blockPrices = new HashMap<>();
    private static long lastUpdate = 0;
    private static long lastBlockUpdate = 0;
    private static boolean useNPCPrices = false;
    private static boolean blockFetchInProgress = false;

    // Block IDs to track
    private static final String[] BLOCK_IDS = {
        "ENCHANTED_COAL", "ENCHANTED_DIAMOND", "ENCHANTED_GOLD",
        "ENCHANTED_MYCELIUM", "ENCHANTED_RED_SAND",
        "ENCHANTED_OBSIDIAN", "ENCHANTED_QUARTZ", "ENCHANTED_EMERALD",
        "ENCHANTED_LAPIS_LAZULI", "ENCHANTED_REDSTONE", "ENCHANTED_HARD_STONE",
        "ENCHANTED_IRON", "ENCHANTED_GLOWSTONE", "ENCHANTED_MITHRIL",
        "ENCHANTED_TITANIUM", "ENCHANTED_SULPHUR", "ENCHANTED_UMBER"
    };

    // NPC sell prices for enchanted blocks (raw_npc_sell * 160)
    private static final Map<String, Double> BLOCK_NPC_PRICES = new HashMap<>();
    static {
        BLOCK_NPC_PRICES.put("ENCHANTED_COAL", 320.0);       // 2 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_DIAMOND", 1280.0);   // 8 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_GOLD", 640.0);       // 4 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_IRON", 480.0);       // 3 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_REDSTONE", 480.0);   // 3 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_LAPIS_LAZULI", 480.0); // 3 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_OBSIDIAN", 1280.0);  // 8 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_QUARTZ", 640.0);     // 4 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_EMERALD", 800.0);    // 5 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_GLOWSTONE", 640.0);  // 4 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_HARD_STONE", 160.0); // 1 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_MITHRIL", 160.0);    // 1 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_TITANIUM", 2560.0);  // 16 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_SULPHUR", 480.0);    // 3 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_UMBER", 480.0);      // 3 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_MYCELIUM", 800.0);   // 5 * 160
        BLOCK_NPC_PRICES.put("ENCHANTED_RED_SAND", 800.0);   // 5 * 160
    }

    public static void setUseNPCPrices(boolean use) {
        useNPCPrices = use;
    }

    public static boolean isUsingNPCPrices() {
        return useNPCPrices;
    }

    public static double getGemPrice(String gemType, int tier) {
        if (useNPCPrices) {
            return getNPCPrice(tier);
        }

        String itemId = getTierPrefix(tier) + "_" + gemType.toUpperCase() + "_GEM";
        double bazaarPrice = gemPrices.getOrDefault(itemId, 0.0);
        double npcPrice = getNPCPrice(tier);

        return Math.max(bazaarPrice, npcPrice);
    }

    private static double getNPCPrice(int tier) {
        return 3 * Math.pow(80, tier);
    }

    private static String getTierPrefix(int tier) {
        switch (tier) {
            case 0: return "ROUGH";
            case 1: return "FLAWED";
            case 2: return "FINE";
            case 3: return "FLAWLESS";
            case 4: return "PERFECT";
            default: return "FLAWED";
        }
    }

    public static CompletableFuture<Boolean> updatePrices() {
        if (useNPCPrices) {
            return CompletableFuture.completedFuture(true);
        }

        if (System.currentTimeMillis() - lastUpdate < CACHE_DURATION) {
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(BAZAAR_API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                JsonObject products = json.getAsJsonObject("products");

                if (products != null) {
                    products.entrySet().forEach(entry -> {
                        String itemId = entry.getKey();
                        if (isGemItem(itemId)) {
                            JsonObject product = entry.getValue().getAsJsonObject();
                            JsonObject quickStatus = product.getAsJsonObject("quick_status");

                            if (quickStatus != null) {
                                double sellPrice = quickStatus.get("sellPrice").getAsDouble();
                                int tier = getTierFromItemId(itemId);
                                double npcPrice = getNPCPrice(tier);
                                gemPrices.put(itemId, Math.max(sellPrice, npcPrice));
                            }
                        }
                    });

                    lastUpdate = System.currentTimeMillis();
                    LOGGER.info("[BazaarPriceManager] Updated gem prices successfully");
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("[BazaarPriceManager] Failed to fetch bazaar prices: " + e.getMessage());
            }
            return false;
        });
    }

    private static boolean isGemItem(String itemId) {
        return (itemId.startsWith("ROUGH_") || itemId.startsWith("FLAWED_") ||
                itemId.startsWith("FINE_") || itemId.startsWith("FLAWLESS_") ||
                itemId.startsWith("PERFECT_")) && itemId.endsWith("_GEM");
    }

    private static int getTierFromItemId(String itemId) {
        if (itemId.startsWith("ROUGH_")) return 0;
        if (itemId.startsWith("FLAWED_")) return 1;
        if (itemId.startsWith("FINE_")) return 2;
        if (itemId.startsWith("FLAWLESS_")) return 3;
        if (itemId.startsWith("PERFECT_")) return 4;
        return 1;
    }

    public static double getBlockPrice(String itemId) {
        if (useNPCPrices) {
            return BLOCK_NPC_PRICES.getOrDefault(itemId, 0.0);
        }
        return blockPrices.getOrDefault(itemId, 0.0);
    }

    public static CompletableFuture<Boolean> updateBlockPrices() {
        if (System.currentTimeMillis() - lastBlockUpdate < CACHE_DURATION) {
            return CompletableFuture.completedFuture(true);
        }
        if (blockFetchInProgress) {
            return CompletableFuture.completedFuture(false);
        }
        blockFetchInProgress = true;

        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(BAZAAR_API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                JsonObject products = json.getAsJsonObject("products");

                if (products != null) {
                    for (String blockId : BLOCK_IDS) {
                        if (products.has(blockId)) {
                            JsonObject product = products.getAsJsonObject(blockId);
                            JsonObject quickStatus = product.getAsJsonObject("quick_status");

                            if (quickStatus != null) {
                                double buyPrice = quickStatus.get("buyPrice").getAsDouble();
                                blockPrices.put(blockId, buyPrice);
                            }
                        }
                    }

                    lastBlockUpdate = System.currentTimeMillis();
                    blockFetchInProgress = false;
                    LOGGER.info("[BazaarPriceManager] Updated block prices successfully");
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("[BazaarPriceManager] Failed to fetch block prices: " + e.getMessage());
            }
            blockFetchInProgress = false;
            return false;
        });
    }
}
