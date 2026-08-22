package forfun.miningqol.client.collection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Talks to the Sybau collection backend (a Cloudflare Worker that holds the
 * Hypixel API key). Mirrors SkyblockCollectionTracker's TokenManager +
 * HypixelApiFetcher: fetch a signed token, then fetch collection data with it,
 * re-fetching the token automatically on a 401.
 */
public class CollectionApi {
    private static final Logger LOGGER = LoggerFactory.getLogger("CollectionApi");

    // ===== EDIT THIS: your deployed Cloudflare Worker base URL (no trailing slash). =====
    // See backend/README.md for how to deploy and get this URL.
    public static final String BASE_URL = "https://miningqol-collections.alex-dong9801.workers.dev";
    // ====================================================================================

    private static final String USER_AGENT = "Sybau-Mod";
    private static final int TIMEOUT_MS = 5000;

    private static volatile String token = null;

    public static boolean isConfigured() {
        return !BASE_URL.contains("YOUR-SUBDOMAIN");
    }

    /** Fetch and store a fresh token for this player's UUID. Returns true on success. */
    public static boolean fetchAndStoreToken(String uuid) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(BASE_URL + "/token").toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-UUID", uuid);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int code = conn.getResponseCode();
            if (code != 200) {
                LOGGER.error("[CollectionApi] token request failed: {}", code);
                return false;
            }
            JsonObject json = JsonParser.parseString(readBody(conn)).getAsJsonObject();
            token = json.has("token") ? json.get("token").getAsString() : null;
            return token != null;
        } catch (Exception e) {
            LOGGER.error("[CollectionApi] token request error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fetch the collection JSON for a player. {@code collectionKey} is the exact
     * Hypixel collection key (e.g. "MITHRIL_ORE"), or "*" to get the whole map.
     * Returns the raw JSON body, or null on failure.
     */
    public static String fetchCollection(String uuid, String collectionKey) {
        if (token == null && !fetchAndStoreToken(uuid)) return null;
        try {
            HttpURLConnection conn = open(uuid, collectionKey);
            int code = conn.getResponseCode();
            if (code == 200) {
                return readBody(conn);
            } else if (code == 401) {
                LOGGER.warn("[CollectionApi] token expired, refreshing and retrying...");
                if (!fetchAndStoreToken(uuid)) return null;
                conn = open(uuid, collectionKey);
                if (conn.getResponseCode() == 200) return readBody(conn);
                LOGGER.error("[CollectionApi] retry failed: {}", conn.getResponseCode());
            } else {
                LOGGER.error("[CollectionApi] collection request failed: {}", code);
            }
        } catch (Exception e) {
            LOGGER.error("[CollectionApi] collection request error: {}", e.getMessage());
        }
        return null;
    }

    private static HttpURLConnection open(String uuid, String collectionKey) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(BASE_URL + "/collection").toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-UUID", uuid);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("X-COLLECTION", collectionKey);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        return conn;
    }

    private static String readBody(HttpURLConnection conn) throws Exception {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
