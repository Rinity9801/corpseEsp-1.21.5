package forfun.miningqol.client.profit;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("BlockTracker");

    private static boolean isTracking = false;
    private static long sessionStartTime = 0;
    private static long lastActivityTime = 0;
    private static final long RESET_DELAY = 60000; // 1 minute without activity = reset

    // Track raw items and enchanted items separately
    private static long totalRawItems = 0;
    private static long totalEnchantedItems = 0;
    private static String currentMaterial = "COAL";

    // Source tracking
    public enum Source { SACKS, INVENTORY }
    private static final Map<Source, Long> rawItemsBySource = new HashMap<>();
    private static final Map<Source, Long> enchantedItemsBySource = new HashMap<>();

    // Pattern to match sack messages like "[Sacks] +22,400 items (Last 30s.)" or "[Sacks] +22,400 items."
    private static final Pattern SACK_PATTERN = Pattern.compile("\\[Sacks\\] \\+([\\d,]+) items");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\(Last (\\d+)s\\.?\\)");

    // Mapping from display names to bazaar item IDs
    private static final Map<String, String> MATERIAL_TO_ENCHANTED = new HashMap<>();
    private static final Map<String, String> MATERIAL_TO_RAW = new HashMap<>();
    private static final Map<String, String[]> MATERIAL_DISPLAY_NAMES = new HashMap<>();
    private static final Map<String, String[]> MATERIAL_ITEM_IDS = new HashMap<>(); // For inventory tracking
    private static final Map<String, Integer> ENCHANTED_RATIO = new HashMap<>(); // Raw items per enchanted

    static {
        // Map material types to their enchanted bazaar IDs
        MATERIAL_TO_ENCHANTED.put("COAL", "ENCHANTED_COAL");
        MATERIAL_TO_ENCHANTED.put("DIAMOND", "ENCHANTED_DIAMOND");
        MATERIAL_TO_ENCHANTED.put("GOLD", "ENCHANTED_GOLD");
        MATERIAL_TO_ENCHANTED.put("MYCELIUM", "ENCHANTED_MYCELIUM_CUBE");
        MATERIAL_TO_ENCHANTED.put("RED_SAND", "ENCHANTED_RED_SAND_CUBE");
        MATERIAL_TO_ENCHANTED.put("OBSIDIAN", "ENCHANTED_OBSIDIAN");
        MATERIAL_TO_ENCHANTED.put("QUARTZ", "ENCHANTED_QUARTZ");
        MATERIAL_TO_ENCHANTED.put("EMERALD", "ENCHANTED_EMERALD");

        // Map material types to their raw bazaar IDs
        MATERIAL_TO_RAW.put("COAL", "COAL");
        MATERIAL_TO_RAW.put("DIAMOND", "DIAMOND");
        MATERIAL_TO_RAW.put("GOLD", "GOLD_INGOT");
        MATERIAL_TO_RAW.put("MYCELIUM", "MYCEL");
        MATERIAL_TO_RAW.put("RED_SAND", "RED_SAND");
        MATERIAL_TO_RAW.put("OBSIDIAN", "OBSIDIAN");
        MATERIAL_TO_RAW.put("QUARTZ", "QUARTZ");
        MATERIAL_TO_RAW.put("EMERALD", "EMERALD");

        // Enchanted ratios (raw items per enchanted)
        ENCHANTED_RATIO.put("COAL", 160);
        ENCHANTED_RATIO.put("DIAMOND", 160);
        ENCHANTED_RATIO.put("GOLD", 160);
        ENCHANTED_RATIO.put("MYCELIUM", 25600); // Cube = 160 * 160
        ENCHANTED_RATIO.put("RED_SAND", 25600); // Cube = 160 * 160
        ENCHANTED_RATIO.put("OBSIDIAN", 160);
        ENCHANTED_RATIO.put("QUARTZ", 160);
        ENCHANTED_RATIO.put("EMERALD", 160);

        // Map material types to possible display names in hover/message text
        MATERIAL_DISPLAY_NAMES.put("COAL", new String[]{"Coal"});
        MATERIAL_DISPLAY_NAMES.put("DIAMOND", new String[]{"Diamond"});
        MATERIAL_DISPLAY_NAMES.put("GOLD", new String[]{"Gold Ingot", "Gold"});
        MATERIAL_DISPLAY_NAMES.put("MYCELIUM", new String[]{"Mycelium"});
        MATERIAL_DISPLAY_NAMES.put("RED_SAND", new String[]{"Red Sand"});
        MATERIAL_DISPLAY_NAMES.put("OBSIDIAN", new String[]{"Obsidian"});
        MATERIAL_DISPLAY_NAMES.put("QUARTZ", new String[]{"Nether Quartz", "Quartz"});
        MATERIAL_DISPLAY_NAMES.put("EMERALD", new String[]{"Emerald"});

        // Map material types to Minecraft item IDs (for inventory tracking)
        MATERIAL_ITEM_IDS.put("COAL", new String[]{"minecraft:coal"});
        MATERIAL_ITEM_IDS.put("DIAMOND", new String[]{"minecraft:diamond"});
        MATERIAL_ITEM_IDS.put("GOLD", new String[]{"minecraft:gold_ingot"});
        MATERIAL_ITEM_IDS.put("MYCELIUM", new String[]{"minecraft:mycelium"});
        MATERIAL_ITEM_IDS.put("RED_SAND", new String[]{"minecraft:red_sand"});
        MATERIAL_ITEM_IDS.put("OBSIDIAN", new String[]{"minecraft:obsidian"});
        MATERIAL_ITEM_IDS.put("QUARTZ", new String[]{"minecraft:quartz"});
        MATERIAL_ITEM_IDS.put("EMERALD", new String[]{"minecraft:emerald"});

        // Initialize source maps
        for (Source source : Source.values()) {
            rawItemsBySource.put(source, 0L);
            enchantedItemsBySource.put(source, 0L);
        }
    }

    public static void setMaterial(String material) {
        currentMaterial = material;
    }

    public static String getMaterial() {
        return currentMaterial;
    }

    public static String getMaterialDisplayName() {
        String[] names = MATERIAL_DISPLAY_NAMES.get(currentMaterial);
        if (names != null && names.length > 0) {
            return names[0];
        }
        return currentMaterial;
    }

    public static String getEnchantedDisplayName() {
        return "Ench. " + getMaterialDisplayName();
    }

    public static String[] getAllMaterials() {
        return new String[]{"COAL", "DIAMOND", "GOLD", "MYCELIUM", "RED_SAND", "OBSIDIAN", "QUARTZ", "EMERALD"};
    }

    public static int getEnchantedRatio() {
        return ENCHANTED_RATIO.getOrDefault(currentMaterial, 160);
    }

    /**
     * Called when items are added to inventory (from mixin)
     */
    public static void onInventoryItemAdd(String itemId, String itemName, int amount) {
        // Check if this item matches our tracked material
        String[] trackedIds = MATERIAL_ITEM_IDS.get(currentMaterial);
        if (trackedIds == null) return;

        boolean matches = false;
        for (String id : trackedIds) {
            if (itemId.equals(id)) {
                matches = true;
                break;
            }
        }

        if (!matches) return;

        // Check if it's enchanted (by name containing "Enchanted")
        boolean isEnchanted = itemName.contains("Enchanted");

        if (!isTracking) {
            startSession();
        }

        lastActivityTime = System.currentTimeMillis();

        if (isEnchanted) {
            totalEnchantedItems += amount;
            enchantedItemsBySource.merge(Source.INVENTORY, (long) amount, Long::sum);
            LOGGER.info("[Inventory] Tracked {} enchanted {} (total enchanted: {})", amount, currentMaterial, totalEnchantedItems);
        } else {
            totalRawItems += amount;
            rawItemsBySource.merge(Source.INVENTORY, (long) amount, Long::sum);
            LOGGER.info("[Inventory] Tracked {} raw {} (total raw: {})", amount, currentMaterial, totalRawItems);
        }
    }

    /**
     * Called when sack chat messages are received
     */
    public static void onChatMessage(Text message) {
        String messageText = message.getString();

        // Check if it's a sack message
        Matcher sackMatcher = SACK_PATTERN.matcher(messageText);
        if (!sackMatcher.find()) {
            return;
        }

        // Extract the amount
        String amountStr = sackMatcher.group(1).replace(",", "");
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            return;
        }

        // Check hover text to see what item was added
        String hoverText = extractHoverText(message);
        if (hoverText == null || hoverText.isEmpty()) {
            return;
        }

        // Check if the hover text contains our tracked material
        String[] displayNames = MATERIAL_DISPLAY_NAMES.get(currentMaterial);
        if (displayNames == null) {
            return;
        }

        boolean matchesMaterial = false;
        boolean isEnchanted = hoverText.contains("Enchanted");
        for (String name : displayNames) {
            if (hoverText.contains(name)) {
                matchesMaterial = true;
                break;
            }
        }

        if (!matchesMaterial) {
            return;
        }

        // Track the items
        if (!isTracking) {
            startSession();
        }

        lastActivityTime = System.currentTimeMillis();

        if (isEnchanted) {
            totalEnchantedItems += amount;
            enchantedItemsBySource.merge(Source.SACKS, amount, Long::sum);
            LOGGER.info("[Sacks] Tracked {} enchanted {} (total enchanted: {})", amount, currentMaterial, totalEnchantedItems);
        } else {
            totalRawItems += amount;
            rawItemsBySource.merge(Source.SACKS, amount, Long::sum);
            LOGGER.info("[Sacks] Tracked {} raw {} (total raw: {})", amount, currentMaterial, totalRawItems);
        }
    }

    private static String extractHoverText(Text message) {
        StringBuilder hoverContent = new StringBuilder();

        // Check the main text's hover event
        HoverEvent hover = message.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowText showText) {
            hoverContent.append(showText.value().getString());
        }

        // Also check siblings
        for (Text sibling : message.getSiblings()) {
            HoverEvent siblingHover = sibling.getStyle().getHoverEvent();
            if (siblingHover instanceof HoverEvent.ShowText showText) {
                hoverContent.append(showText.value().getString());
            }
            // Recursively check nested siblings
            hoverContent.append(extractHoverText(sibling));
        }

        return hoverContent.toString();
    }

    public static void tick() {
        if (isTracking && System.currentTimeMillis() - lastActivityTime > RESET_DELAY) {
            reset();
        }
    }

    public static void startSession() {
        BazaarPriceManager.updateBlockPrices();
        sessionStartTime = System.currentTimeMillis();
        isTracking = true;
    }

    public static void reset() {
        isTracking = false;
        sessionStartTime = 0;
        lastActivityTime = 0;
        totalRawItems = 0;
        totalEnchantedItems = 0;
        for (Source source : Source.values()) {
            rawItemsBySource.put(source, 0L);
            enchantedItemsBySource.put(source, 0L);
        }
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static long getSessionTime() {
        if (!isTracking || sessionStartTime == 0) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    /**
     * Get total enchanted items (directly collected + converted from raw)
     */
    public static long getTotalEnchantedItems() {
        int ratio = getEnchantedRatio();
        return totalEnchantedItems + (totalRawItems / ratio);
    }

    /**
     * Get total raw items collected
     */
    public static long getTotalRawItems() {
        return totalRawItems;
    }

    /**
     * Get total raw items equivalent (enchanted * ratio + raw)
     */
    public static long getTotalRawEquivalent() {
        int ratio = getEnchantedRatio();
        return (totalEnchantedItems * ratio) + totalRawItems;
    }

    /**
     * Get total value in coins
     */
    public static double getTotalValue() {
        String enchantedId = MATERIAL_TO_ENCHANTED.get(currentMaterial);
        double enchantedPrice = BazaarPriceManager.getBlockPrice(enchantedId);

        // Calculate value: (enchanted items * price) + (raw items / ratio * price)
        int ratio = getEnchantedRatio();
        double enchantedValue = totalEnchantedItems * enchantedPrice;
        double rawValue = (totalRawItems / (double) ratio) * enchantedPrice;

        return enchantedValue + rawValue;
    }

    /**
     * Get coins per hour based on real elapsed time
     */
    public static double getCoinsPerHour() {
        long sessionMs = getSessionTime();
        if (sessionMs == 0) return 0.0;
        double hours = sessionMs / (1000.0 * 60.0 * 60.0);
        return getTotalValue() / hours;
    }

    /**
     * Get enchanted items per hour
     */
    public static double getEnchantedPerHour() {
        long sessionMs = getSessionTime();
        if (sessionMs == 0) return 0.0;
        double hours = sessionMs / (1000.0 * 60.0 * 60.0);
        return getTotalEnchantedItems() / hours;
    }

    /**
     * Get collection (raw items) per hour
     */
    public static double getCollectionPerHour() {
        long sessionMs = getSessionTime();
        if (sessionMs == 0) return 0.0;
        double hours = sessionMs / (1000.0 * 60.0 * 60.0);
        return getTotalRawEquivalent() / hours;
    }

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

    public static String formatCoins(double coins) {
        if (coins >= 1_000_000_000) {
            return String.format("%.2fB", coins / 1_000_000_000.0);
        } else if (coins >= 1_000_000) {
            return String.format("%.2fM", coins / 1_000_000.0);
        } else if (coins >= 1_000) {
            return String.format("%.1fK", coins / 1_000.0);
        } else {
            return String.format("%.0f", coins);
        }
    }

    public static String formatNumber(double number) {
        if (number >= 1_000_000) {
            return String.format("%.2fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.format("%.0f", number);
        }
    }

    public static String formatBlocks(long blocks) {
        if (blocks >= 1_000_000) {
            return String.format("%.2fM", blocks / 1_000_000.0);
        } else if (blocks >= 1_000) {
            return String.format("%.1fK", blocks / 1_000.0);
        } else {
            return String.format("%d", blocks);
        }
    }
}
