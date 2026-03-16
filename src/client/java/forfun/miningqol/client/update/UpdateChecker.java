package forfun.miningqol.client.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger("UpdateChecker");
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Rinity9801/MiningQOL/releases";
    private static final String MOD_ID = "miningqol";
    private static final String MINECRAFT_VERSION = getMinecraftVersion();

    private static String getMinecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String latestVersion = null;
    private static String downloadUrl = null;
    private static String releaseNotes = null;
    private static boolean updateAvailable = false;
    private static boolean checkComplete = false;

    public static CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("[UpdateChecker] Checking for updates for Minecraft {}...", MINECRAFT_VERSION);

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

                JsonArray releases = JsonParser.parseString(response.toString()).getAsJsonArray();

                // Find the latest release for this Minecraft version
                JsonObject matchingRelease = null;
                for (int i = 0; i < releases.size(); i++) {
                    JsonObject release = releases.get(i).getAsJsonObject();
                    String tagName = release.get("tag_name").getAsString();

                    // Check if this release is for our Minecraft version (cheat edition)
                    // Tags should contain the MC version but NOT "normal"
                    if (tagName.contains(MINECRAFT_VERSION) && !tagName.contains("normal")) {
                        matchingRelease = release;
                        break; // First match is the latest for this version
                    }
                }

                if (matchingRelease == null) {
                    LOGGER.info("[UpdateChecker] No releases found for Minecraft {}", MINECRAFT_VERSION);
                    checkComplete = true;
                    return false;
                }

                // Get version tag (remove 'v' prefix if present)
                String tagName = matchingRelease.get("tag_name").getAsString();
                latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

                // Get release notes
                releaseNotes = matchingRelease.has("body") && !matchingRelease.get("body").isJsonNull()
                        ? matchingRelease.get("body").getAsString()
                        : "No release notes available.";

                // Get download URL for the jar file
                JsonArray assets = matchingRelease.getAsJsonArray("assets");
                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    String name = asset.get("name").getAsString();
                    if (name.endsWith(".jar") && !name.contains("sources")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        break;
                    }
                }

                // Compare versions
                String currentVersion = getCurrentVersion();
                updateAvailable = isNewerVersion(latestVersion, currentVersion);
                checkComplete = true;

                if (updateAvailable) {
                    LOGGER.info("[UpdateChecker] Update available! Current: {}, Latest: {}", currentVersion, latestVersion);
                } else {
                    LOGGER.info("[UpdateChecker] Up to date! Version: {}", currentVersion);
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
            if (downloadUrl == null || latestVersion == null) {
                LOGGER.error("[UpdateChecker] No download URL or version available");
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

                // Save with version number in filename
                String fileName = "miningqol-" + latestVersion + ".jar";
                Path targetPath = modsFolder.resolve(fileName);

                // Download to temp file first
                Path tempPath = modsFolder.resolve(fileName + ".tmp");
                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // Rename temp to final
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                LOGGER.info("[UpdateChecker] Update downloaded to: {}", targetPath);

                // Delete old miningqol jars (except the one we just downloaded)
                deleteOldJars(modsFolder, fileName);

                return true;

            } catch (Exception e) {
                LOGGER.error("[UpdateChecker] Failed to download update", e);
                return false;
            }
        });
    }

    private static void deleteOldJars(Path modsFolder, String newFileName) {
        try {
            File[] files = modsFolder.toFile().listFiles((dir, name) ->
                name.startsWith("miningqol") && name.endsWith(".jar") && !name.equals(newFileName)
            );

            if (files != null) {
                for (File file : files) {
                    try {
                        if (file.delete()) {
                            LOGGER.info("[UpdateChecker] Deleted old jar: {}", file.getName());
                        } else {
                            // If we can't delete, try to mark for deletion on exit
                            file.deleteOnExit();
                            LOGGER.info("[UpdateChecker] Marked for deletion on exit: {}", file.getName());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("[UpdateChecker] Could not delete old jar: {}", file.getName());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[UpdateChecker] Error cleaning up old jars", e);
        }
    }

    public static String getCurrentVersion() {
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (modContainer.isPresent()) {
            return modContainer.get().getMetadata().getVersion().getFriendlyString();
        }
        return "unknown";
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
