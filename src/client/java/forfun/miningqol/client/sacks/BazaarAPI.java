package forfun.miningqol.client.sacks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

public class BazaarAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger("BazaarAPI");
    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";

    private static Map<String, BazaarProduct> cachedProducts = new HashMap<>();
    private static long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 60000; // 1 minute cache

    public static class BazaarProduct {
        public final String productId;
        public final double topBuyPrice;  // Price to insta-sell (top buy order)
        public final double topSellPrice; // Price for sell offer (top sell order)

        public BazaarProduct(String productId, double topBuyPrice, double topSellPrice) {
            this.productId = productId;
            this.topBuyPrice = topBuyPrice;
            this.topSellPrice = topSellPrice;
        }
    }

    public static CompletableFuture<Map<String, BazaarProduct>> fetchPrices() {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache
            if (System.currentTimeMillis() - lastFetchTime < CACHE_DURATION_MS && !cachedProducts.isEmpty()) {
                LOGGER.info("[BazaarAPI] Using cached prices ({} products)", cachedProducts.size());
                return cachedProducts;
            }

            try {
                LOGGER.info("[BazaarAPI] Fetching fresh prices from Hypixel API...");
                URL url = new URL(BAZAAR_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "MiningQOL-Mod");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    LOGGER.error("[BazaarAPI] HTTP error: {}", responseCode);
                    return cachedProducts;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                if (!json.get("success").getAsBoolean()) {
                    LOGGER.error("[BazaarAPI] API returned success=false");
                    return cachedProducts;
                }

                JsonObject products = json.getAsJsonObject("products");
                Map<String, BazaarProduct> newProducts = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                    String productId = entry.getKey();
                    JsonObject product = entry.getValue().getAsJsonObject();

                    double topBuyPrice = 0;
                    double topSellPrice = 0;

                    // Get top buy order price (insta-sell value)
                    JsonArray buySummary = product.getAsJsonArray("buy_summary");
                    if (buySummary != null && buySummary.size() > 0) {
                        topBuyPrice = buySummary.get(0).getAsJsonObject().get("pricePerUnit").getAsDouble();
                    }

                    // Get top sell order price (sell-offer value)
                    JsonArray sellSummary = product.getAsJsonArray("sell_summary");
                    if (sellSummary != null && sellSummary.size() > 0) {
                        topSellPrice = sellSummary.get(0).getAsJsonObject().get("pricePerUnit").getAsDouble();
                    }

                    newProducts.put(productId, new BazaarProduct(productId, topBuyPrice, topSellPrice));
                }

                cachedProducts = newProducts;
                lastFetchTime = System.currentTimeMillis();
                LOGGER.info("[BazaarAPI] Fetched {} products", newProducts.size());
                return newProducts;

            } catch (Exception e) {
                LOGGER.error("[BazaarAPI] Failed to fetch prices", e);
                return cachedProducts;
            }
        });
    }

    public static Map<String, BazaarProduct> getCachedProducts() {
        return cachedProducts;
    }

    public static boolean hasCachedData() {
        return !cachedProducts.isEmpty();
    }
}
