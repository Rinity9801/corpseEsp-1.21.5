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
    private static long lastUpdate = 0;
    private static boolean useNPCPrices = false;

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
}
