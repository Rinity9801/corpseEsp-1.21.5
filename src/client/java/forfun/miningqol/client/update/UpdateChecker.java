package forfun.miningqol.client.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("UpdateChecker");
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Rinity9801/MiningQOL/releases/latest";
    private static final String CURRENT_VERSION = "1.1.0";

    private static String latestVersion = null;
    private static String downloadUrl = null;
    private static String releaseNotes = null;
    private static boolean updateAvailable = false;
    private static boolean checkComplete = false;

    public static CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("[UpdateChecker] Checking for updates...");

                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "MiningQOL-Mod");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    LOGGER.warn("[UpdateChecker] GitHub API returned: {}", responseCode);
                    checkComplete = true;
                    return false;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                // Get version tag (remove 'v' prefix if present)
                String tagName = json.get("tag_name").getAsString();
                latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

                // Get release notes
                releaseNotes = json.has("body") && !json.get("body").isJsonNull()
                        ? json.get("body").getAsString()
                        : "No release notes available.";

                // Get download URL for the jar file
                JsonArray assets = json.getAsJsonArray("assets");
                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    String name = asset.get("name").getAsString();
                    if (name.endsWith(".jar") && !name.contains("sources")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        break;
                    }
                }

                // Compare versions
                updateAvailable = isNewerVersion(latestVersion, CURRENT_VERSION);
                checkComplete = true;

                if (updateAvailable) {
                    LOGGER.info("[UpdateChecker] Update available! Current: {}, Latest: {}", CURRENT_VERSION, latestVersion);
                } else {
                    LOGGER.info("[UpdateChecker] Up to date! Version: {}", CURRENT_VERSION);
                }

                return updateAvailable;

            } catch (Exception e) {
                LOGGER.error("[UpdateChecker] Failed to check for updates", e);
                checkComplete = true;
                return false;
            }
        });
    }

    private static boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");

            int maxLength = Math.max(latestParts.length, currentParts.length);

            for (int i = 0; i < maxLength; i++) {
                int latestNum = i < latestParts.length ? Integer.parseInt(latestParts[i].replaceAll("[^0-9]", "")) : 0;
                int currentNum = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;

                if (latestNum > currentNum) return true;
                if (latestNum < currentNum) return false;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("[UpdateChecker] Error comparing versions", e);
            return false;
        }
    }

    public static CompletableFuture<Boolean> downloadUpdate(Path modsFolder) {
        return CompletableFuture.supplyAsync(() -> {
            if (downloadUrl == null) {
                LOGGER.error("[UpdateChecker] No download URL available");
                return false;
            }

            try {
                LOGGER.info("[UpdateChecker] Downloading update from: {}", downloadUrl);

                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setRequestProperty("User-Agent", "MiningQOL-Mod");

                // Follow redirects
                connection.setInstanceFollowRedirects(true);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    LOGGER.error("[UpdateChecker] Download failed with code: {}", responseCode);
                    return false;
                }

                // Get filename from URL
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
                Path targetPath = modsFolder.resolve(fileName);

                // Download to temp file first
                Path tempPath = modsFolder.resolve(fileName + ".tmp");
                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // Rename temp to final
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                LOGGER.info("[UpdateChecker] Update downloaded to: {}", targetPath);
                return true;

            } catch (Exception e) {
                LOGGER.error("[UpdateChecker] Failed to download update", e);
                return false;
            }
        });
    }

    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static String getDownloadUrl() {
        return downloadUrl;
    }

    public static String getReleaseNotes() {
        return releaseNotes;
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static boolean isCheckComplete() {
        return checkComplete;
    }
}
