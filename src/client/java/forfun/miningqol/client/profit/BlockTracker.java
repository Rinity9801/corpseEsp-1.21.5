package forfun.miningqol.client.profit;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("BlockTracker");

    // Display modes
    public enum DisplayMode { SEPARATE, COMBINED }
    private static DisplayMode displayMode = DisplayMode.SEPARATE;

    // Debug mode
    private static boolean debugEnabled = false;

    private static final long RESET_DELAY = 90000; // 1.5 minutes without activity = reset
    private static final long HIDE_DELAY = 35000; // Hide HUD after 35 seconds of inactivity

    // Per-material tracking data
    public static class MaterialData {
        public long totalRawEquivalent = 0; // Total items as raw equivalent (enchanted * 160 + raw)
        public double totalCoins = 0; // Total coin value accumulated
        public long sessionStartTime = 0; // When tracking started (wall clock)
        public long lastActivityTime = 0;

        public boolean isActive() {
            return sessionStartTime > 0 && (System.currentTimeMillis() - lastActivityTime) < RESET_DELAY;
        }

        public boolean shouldShow() {
            return sessionStartTime > 0 && (System.currentTimeMillis() - lastActivityTime) < HIDE_DELAY;
        }

        public long getElapsedTimeMs() {
            if (sessionStartTime == 0) return 0;
            return System.currentTimeMillis() - sessionStartTime;
        }

        public void reset() {
            totalRawEquivalent = 0;
            totalCoins = 0;
            sessionStartTime = 0;
            lastActivityTime = 0;
        }
    }

    // Track all materials
    private static final Map<String, MaterialData> materialDataMap = new LinkedHashMap<>();

    // Pattern to match sack messages like "[Sacks] +22,400 items (Last 30s.)" or "[Sacks] +22,400 items."
    private static final Pattern SACK_PATTERN = Pattern.compile("\\[Sacks\\] \\+([\\d,]+) items");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\(Last (\\d+)s\\.?\\)");
    // Pattern to match individual items in hover text: "+61,266 Coal (Mining Sack)"
    private static final Pattern HOVER_ITEM_PATTERN = Pattern.compile("\\+([\\d,]+) (.+?) \\(");

    // Material mappings
    private static final Map<String, String> MATERIAL_TO_ENCHANTED = new HashMap<>();
    private static final Map<String, String[]> MATERIAL_DISPLAY_NAMES = new HashMap<>();
    private static final Map<String, Integer> ENCHANTED_RATIO = new HashMap<>();

    static {
        // Enchanted bazaar IDs
        MATERIAL_TO_ENCHANTED.put("COAL", "ENCHANTED_COAL");
        MATERIAL_TO_ENCHANTED.put("DIAMOND", "ENCHANTED_DIAMOND");
        MATERIAL_TO_ENCHANTED.put("GOLD", "ENCHANTED_GOLD");
        MATERIAL_TO_ENCHANTED.put("IRON", "ENCHANTED_IRON");
        MATERIAL_TO_ENCHANTED.put("REDSTONE", "ENCHANTED_REDSTONE");
        MATERIAL_TO_ENCHANTED.put("LAPIS", "ENCHANTED_LAPIS_LAZULI");
        MATERIAL_TO_ENCHANTED.put("MYCELIUM", "ENCHANTED_MYCELIUM_CUBE");
        MATERIAL_TO_ENCHANTED.put("RED_SAND", "ENCHANTED_RED_SAND_CUBE");
        MATERIAL_TO_ENCHANTED.put("OBSIDIAN", "ENCHANTED_OBSIDIAN");
        MATERIAL_TO_ENCHANTED.put("QUARTZ", "ENCHANTED_QUARTZ");
        MATERIAL_TO_ENCHANTED.put("EMERALD", "ENCHANTED_EMERALD");
        MATERIAL_TO_ENCHANTED.put("GLOWSTONE", "ENCHANTED_GLOWSTONE");
        MATERIAL_TO_ENCHANTED.put("HARDSTONE", "ENCHANTED_HARD_STONE");
        MATERIAL_TO_ENCHANTED.put("MITHRIL", "ENCHANTED_MITHRIL");
        MATERIAL_TO_ENCHANTED.put("TITANIUM", "ENCHANTED_TITANIUM");
        MATERIAL_TO_ENCHANTED.put("SULPHUR", "ENCHANTED_SULPHUR");
        MATERIAL_TO_ENCHANTED.put("UMBER", "ENCHANTED_UMBER");

        // Enchanted ratios (raw items per enchanted)
        ENCHANTED_RATIO.put("COAL", 160);
        ENCHANTED_RATIO.put("DIAMOND", 160);
        ENCHANTED_RATIO.put("GOLD", 160);
        ENCHANTED_RATIO.put("IRON", 160);
        ENCHANTED_RATIO.put("REDSTONE", 160);
        ENCHANTED_RATIO.put("LAPIS", 160);
        ENCHANTED_RATIO.put("MYCELIUM", 25600);
        ENCHANTED_RATIO.put("RED_SAND", 25600);
        ENCHANTED_RATIO.put("OBSIDIAN", 160);
        ENCHANTED_RATIO.put("QUARTZ", 160);
        ENCHANTED_RATIO.put("EMERALD", 160);
        ENCHANTED_RATIO.put("GLOWSTONE", 160);
        ENCHANTED_RATIO.put("HARDSTONE", 160);
        ENCHANTED_RATIO.put("MITHRIL", 160);
        ENCHANTED_RATIO.put("TITANIUM", 160);
        ENCHANTED_RATIO.put("SULPHUR", 160);
        ENCHANTED_RATIO.put("UMBER", 160);

        // Display names for matching sack hover text
        MATERIAL_DISPLAY_NAMES.put("COAL", new String[]{"Coal"});
        MATERIAL_DISPLAY_NAMES.put("DIAMOND", new String[]{"Diamond"});
        MATERIAL_DISPLAY_NAMES.put("GOLD", new String[]{"Gold Ingot", "Gold"});
        MATERIAL_DISPLAY_NAMES.put("IRON", new String[]{"Iron Ingot", "Iron"});
        MATERIAL_DISPLAY_NAMES.put("REDSTONE", new String[]{"Redstone"});
        MATERIAL_DISPLAY_NAMES.put("LAPIS", new String[]{"Lapis Lazuli", "Lapis"});
        MATERIAL_DISPLAY_NAMES.put("MYCELIUM", new String[]{"Mycelium"});
        MATERIAL_DISPLAY_NAMES.put("RED_SAND", new String[]{"Red Sand"});
        MATERIAL_DISPLAY_NAMES.put("OBSIDIAN", new String[]{"Obsidian"});
        MATERIAL_DISPLAY_NAMES.put("QUARTZ", new String[]{"Nether Quartz", "Quartz"});
        MATERIAL_DISPLAY_NAMES.put("EMERALD", new String[]{"Emerald"});
        MATERIAL_DISPLAY_NAMES.put("GLOWSTONE", new String[]{"Glowstone Dust", "Glowstone"});
        MATERIAL_DISPLAY_NAMES.put("HARDSTONE", new String[]{"Hard Stone", "Hardstone"});
        MATERIAL_DISPLAY_NAMES.put("MITHRIL", new String[]{"Mithril"});
        MATERIAL_DISPLAY_NAMES.put("TITANIUM", new String[]{"Titanium"});
        MATERIAL_DISPLAY_NAMES.put("SULPHUR", new String[]{"Sulphur"});
        MATERIAL_DISPLAY_NAMES.put("UMBER", new String[]{"Umber"});
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
    }

    public static DisplayMode getDisplayMode() {
        return displayMode;
    }

    public static String[] getAllMaterials() {
        return MATERIAL_TO_ENCHANTED.keySet().toArray(new String[0]);
    }

    public static String getMaterialDisplayName(String material) {
        String[] names = MATERIAL_DISPLAY_NAMES.get(material);
        if (names != null && names.length > 0) {
            return names[0];
        }
        return material;
    }

    public static int getEnchantedRatio(String material) {
        return ENCHANTED_RATIO.getOrDefault(material, 160);
    }

    private static MaterialData getOrCreateData(String material) {
        return materialDataMap.computeIfAbsent(material, k -> new MaterialData());
    }

    /**
     * Called when items are added to inventory (from mixin)
     * Disabled - only tracking via sacks now
     */
    public static void onInventoryItemAdd(String itemId, String itemName, int amount) {
        // Inventory tracking disabled - only use sack messages
    }

    /**
     * Called when sack chat messages are received
     */
    public static void onChatMessage(Text message) {
        String messageText = message.getString();

        // Check if this is a sack message
        Matcher sackMatcher = SACK_PATTERN.matcher(messageText);
        if (!sackMatcher.find()) return;

        // Extract seconds from "(Last Xs.)" if present, default to 30
        int seconds = 30;
        Matcher timeMatcher = TIME_PATTERN.matcher(messageText);
        if (timeMatcher.find()) {
            try {
                seconds = Integer.parseInt(timeMatcher.group(1));
            } catch (NumberFormatException e) {
                seconds = 30;
            }
        }
        if (seconds <= 0) seconds = 30;

        String hoverText = extractHoverText(message);
        if (hoverText == null || hoverText.isEmpty()) return;

        // First sack message - update prices
        if (materialDataMap.isEmpty()) {
            BazaarPriceManager.updateBlockPrices();
        }

        // First pass: collect all items per material (combine raw + enchanted)
        // Map of material -> total raw equivalent for this message
        Map<String, Long> materialRawEquivalents = new HashMap<>();

        // Parse each line of the hover text for individual items
        // Format: "+61,266 Coal (Mining Sack)" or "+3 Enchanted Coal (Enchanted Mining Sack)"
        String[] lines = hoverText.split("\n");
        for (String line : lines) {
            Matcher itemMatcher = HOVER_ITEM_PATTERN.matcher(line);
            if (!itemMatcher.find()) continue;

            String amountStr = itemMatcher.group(1).replace(",", "");
            String itemName = itemMatcher.group(2).trim();

            long amount;
            try {
                amount = Long.parseLong(amountStr);
            } catch (NumberFormatException e) {
                continue;
            }

            // Check if enchanted and get base item name
            boolean isEnchanted = itemName.startsWith("Enchanted ");
            String baseItemName = isEnchanted ? itemName.substring("Enchanted ".length()) : itemName;

            // Find which material this is
            String material = findMaterialByName(baseItemName);
            if (material == null) continue;

            // Convert to raw equivalent and accumulate
            int ratio = getEnchantedRatio(material);
            long rawEquivalent = isEnchanted ? amount * ratio : amount;
            materialRawEquivalents.merge(material, rawEquivalent, Long::sum);

            if (debugEnabled) {
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(net.minecraft.text.Text.literal(
                        String.format("\u00A7b[Parse] \u00A77%,d %s \u00A7e%s \u00A77-> %,d raw equiv",
                            amount, isEnchanted ? "ench" : "raw", material, rawEquivalent)
                    ), false);
                }
            }
        }

        // Second pass: track each material with combined totals
        for (Map.Entry<String, Long> entry : materialRawEquivalents.entrySet()) {
            String material = entry.getKey();
            long totalRawEquivalent = entry.getValue();
            trackMaterial(material, totalRawEquivalent, seconds);
        }

        // Debug: Log calculation details after processing sack message
        if (debugEnabled) {
            logCalculationDetails();
        }
    }

    /**
     * Track a material with its combined raw equivalent from a sack message
     */
    private static void trackMaterial(String material, long rawEquivalentThisEvent, int seconds) {
        MaterialData data = getOrCreateData(material);
        int ratio = getEnchantedRatio(material);

        // Start session timer on first event
        if (data.sessionStartTime == 0) {
            data.sessionStartTime = System.currentTimeMillis();
        }

        // Add to totals
        data.totalRawEquivalent += rawEquivalentThisEvent;
        data.lastActivityTime = System.currentTimeMillis();

        // Calculate coins for this event using double to preserve fractional enchanted items
        String enchantedId = MATERIAL_TO_ENCHANTED.get(material);
        double enchantedPrice = BazaarPriceManager.getBlockPrice(enchantedId);
        double enchantedCount = rawEquivalentThisEvent / (double) ratio;
        double coinsThisEvent = enchantedCount * enchantedPrice;

        // Accumulate total coins
        data.totalCoins += coinsThisEvent;

        // Calculate current rate using wall-clock time
        double elapsedHours = data.getElapsedTimeMs() / (1000.0 * 60.0 * 60.0);
        double currentCoinsPerHour = elapsedHours > 0 ? data.totalCoins / elapsedHours : 0;

        LOGGER.info("[Sacks] {} raw equiv {} -> {} coins (total: {}, {}/hr)",
            rawEquivalentThisEvent, material,
            String.format("%.0f", coinsThisEvent),
            String.format("%.0f", data.totalCoins),
            String.format("%.0f", currentCoinsPerHour));

        if (debugEnabled) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(net.minecraft.text.Text.literal(
                    String.format("\u00A7a[Track] \u00A7e%s \u00A77%,d raw -> \u00A7a%,.0f \u00A77coins (total: \u00A7a%,.0f\u00A77, \u00A7a%,.0f\u00A77/hr)",
                        material, rawEquivalentThisEvent, coinsThisEvent, data.totalCoins, currentCoinsPerHour)
                ), false);
            }
        }
    }

    /**
     * Find material by exact item name match
     */
    private static String findMaterialByName(String itemName) {
        for (Map.Entry<String, String[]> entry : MATERIAL_DISPLAY_NAMES.entrySet()) {
            for (String name : entry.getValue()) {
                if (itemName.equals(name) || itemName.equalsIgnoreCase(name)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private static String extractHoverText(Text message) {
        StringBuilder hoverContent = new StringBuilder();

        HoverEvent hover = message.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowText showText) {
            hoverContent.append(showText.value().getString());
        }

        for (Text sibling : message.getSiblings()) {
            HoverEvent siblingHover = sibling.getStyle().getHoverEvent();
            if (siblingHover instanceof HoverEvent.ShowText showText) {
                hoverContent.append(showText.value().getString());
            }
            hoverContent.append(extractHoverText(sibling));
        }

        return hoverContent.toString();
    }

    /**
     * Log detailed calculation breakdown for debugging
     */
    private static void logCalculationDetails() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(net.minecraft.text.Text.literal("\u00A76--- Profit Calculation ---"), false);

        for (String material : getActiveMaterials()) {
            MaterialData data = materialDataMap.get(material);
            if (data == null) continue;

            int ratio = getEnchantedRatio(material);
            String enchantedId = MATERIAL_TO_ENCHANTED.get(material);
            double enchantedPrice = BazaarPriceManager.getBlockPrice(enchantedId);
            double coinsPerHour = getCoinsPerHour(material);
            long elapsedSeconds = data.getElapsedTimeMs() / 1000;

            client.player.sendMessage(net.minecraft.text.Text.literal(
                String.format("\u00A7e%s\u00A77: %,d raw equiv, %,.0f coins, %ds elapsed",
                    material, data.totalRawEquivalent, data.totalCoins, elapsedSeconds)
            ), false);
            client.player.sendMessage(net.minecraft.text.Text.literal(
                String.format("  \u00A77Price=\u00A7a%.1f\u00A77, Rate=\u00A7a%,.0f\u00A77/hr", enchantedPrice, coinsPerHour)
            ), false);
        }
        client.player.sendMessage(net.minecraft.text.Text.literal("\u00A76-------------------------"), false);
    }

    public static void tick() {
        // Reset materials that have been inactive
        for (MaterialData data : materialDataMap.values()) {
            if (data.sessionStartTime > 0 && !data.isActive()) {
                data.reset();
            }
        }
    }

    public static void reset() {
        materialDataMap.clear();
    }

    // Required for world change handling
    public static void onWorldChange() {
        // Optional: could reset on world change
    }

    /**
     * Get list of materials currently being tracked (with recent activity)
     */
    public static List<String> getActiveMaterials() {
        List<String> active = new ArrayList<>();
        for (Map.Entry<String, MaterialData> entry : materialDataMap.entrySet()) {
            if (entry.getValue().shouldShow()) {
                active.add(entry.getKey());
            }
        }
        return active;
    }

    /**
     * Check if any materials are being tracked
     */
    public static boolean hasActiveMaterials() {
        return !getActiveMaterials().isEmpty();
    }

    // Per-material getters
    public static long getTotalRawEquivalent(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null) return 0;
        return data.totalRawEquivalent;
    }

    public static long getTotalEnchantedItems(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null) return 0;
        int ratio = getEnchantedRatio(material);
        return data.totalRawEquivalent / ratio;
    }

    public static double getTotalValue(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null) return 0;
        return data.totalCoins;
    }

    public static long getSessionTime(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null) return 0;
        return data.getElapsedTimeMs();
    }

    public static double getCoinsPerHour(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null || data.totalCoins == 0) return 0;

        // Total coins / elapsed wall-clock time
        double elapsedHours = data.getElapsedTimeMs() / (1000.0 * 60.0 * 60.0);
        if (elapsedHours <= 0) return 0;
        return data.totalCoins / elapsedHours;
    }

    public static double getEnchantedPerHour(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null || data.getElapsedTimeMs() == 0) return 0;
        int ratio = getEnchantedRatio(material);
        double enchantedCount = data.totalRawEquivalent / (double) ratio;
        double hours = data.getElapsedTimeMs() / (1000.0 * 60.0 * 60.0);
        return enchantedCount / hours;
    }

    public static double getCollectionPerHour(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null || data.getElapsedTimeMs() == 0) return 0;
        double hours = data.getElapsedTimeMs() / (1000.0 * 60.0 * 60.0);
        return data.totalRawEquivalent / hours;
    }

    // Combined totals (for COMBINED display mode)
    public static double getCombinedTotalValue() {
        double total = 0;
        for (String material : getActiveMaterials()) {
            total += getTotalValue(material);
        }
        return total;
    }

    public static double getCombinedCoinsPerHour() {
        double total = 0;
        for (String material : getActiveMaterials()) {
            total += getCoinsPerHour(material);
        }
        return total;
    }

    public static long getCombinedSessionTime() {
        long maxTime = 0;
        for (String material : getActiveMaterials()) {
            maxTime = Math.max(maxTime, getSessionTime(material));
        }
        return maxTime;
    }

    // Formatting utilities
    public static String formatTime(long millis) {
        if (millis == 0) return "n/a";
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    public static String formatWithCommas(long number) {
        return String.format("%,d", number);
    }

    // Legacy compatibility methods
    @Deprecated
    public static void setMaterial(String material) {
        // No longer needed - auto-detect
    }

    @Deprecated
    public static String getMaterial() {
        List<String> active = getActiveMaterials();
        return active.isEmpty() ? "COAL" : active.get(0);
    }

    @Deprecated
    public static boolean isTracking() {
        return hasActiveMaterials();
    }
}
