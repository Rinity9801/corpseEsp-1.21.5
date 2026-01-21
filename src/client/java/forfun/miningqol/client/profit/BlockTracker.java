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

    private static final long RESET_DELAY = 60000; // 1 minute without activity = reset
    private static final long HIDE_DELAY = 10000; // Hide HUD after 10 seconds of inactivity

    // Per-material tracking data
    public static class MaterialData {
        public long rawItems = 0;
        public long enchantedItems = 0;
        public long sessionStartTime = 0;
        public long lastActivityTime = 0;

        public boolean isActive() {
            return sessionStartTime > 0 && (System.currentTimeMillis() - lastActivityTime) < RESET_DELAY;
        }

        public boolean shouldShow() {
            return sessionStartTime > 0 && (System.currentTimeMillis() - lastActivityTime) < HIDE_DELAY;
        }

        public void reset() {
            rawItems = 0;
            enchantedItems = 0;
            sessionStartTime = 0;
            lastActivityTime = 0;
        }

        public long getSessionTime() {
            if (sessionStartTime == 0) return 0;
            return System.currentTimeMillis() - sessionStartTime;
        }
    }

    // Track all materials
    private static final Map<String, MaterialData> materialDataMap = new LinkedHashMap<>();

    // Pattern to match sack messages
    private static final Pattern SACK_PATTERN = Pattern.compile("\\[Sacks\\] \\+([\\d,]+) items");

    // Material mappings
    private static final Map<String, String> MATERIAL_TO_ENCHANTED = new HashMap<>();
    private static final Map<String, String[]> MATERIAL_DISPLAY_NAMES = new HashMap<>();
    private static final Map<String, String[]> MATERIAL_ITEM_IDS = new HashMap<>();
    private static final Map<String, Integer> ENCHANTED_RATIO = new HashMap<>();

    static {
        // Enchanted bazaar IDs
        MATERIAL_TO_ENCHANTED.put("COAL", "ENCHANTED_COAL");
        MATERIAL_TO_ENCHANTED.put("DIAMOND", "ENCHANTED_DIAMOND");
        MATERIAL_TO_ENCHANTED.put("GOLD", "ENCHANTED_GOLD");
        MATERIAL_TO_ENCHANTED.put("MYCELIUM", "ENCHANTED_MYCELIUM_CUBE");
        MATERIAL_TO_ENCHANTED.put("RED_SAND", "ENCHANTED_RED_SAND_CUBE");
        MATERIAL_TO_ENCHANTED.put("OBSIDIAN", "ENCHANTED_OBSIDIAN");
        MATERIAL_TO_ENCHANTED.put("QUARTZ", "ENCHANTED_QUARTZ");
        MATERIAL_TO_ENCHANTED.put("EMERALD", "ENCHANTED_EMERALD");

        // Enchanted ratios
        ENCHANTED_RATIO.put("COAL", 160);
        ENCHANTED_RATIO.put("DIAMOND", 160);
        ENCHANTED_RATIO.put("GOLD", 160);
        ENCHANTED_RATIO.put("MYCELIUM", 25600);
        ENCHANTED_RATIO.put("RED_SAND", 25600);
        ENCHANTED_RATIO.put("OBSIDIAN", 160);
        ENCHANTED_RATIO.put("QUARTZ", 160);
        ENCHANTED_RATIO.put("EMERALD", 160);

        // Display names for matching sack hover text
        MATERIAL_DISPLAY_NAMES.put("COAL", new String[]{"Coal"});
        MATERIAL_DISPLAY_NAMES.put("DIAMOND", new String[]{"Diamond"});
        MATERIAL_DISPLAY_NAMES.put("GOLD", new String[]{"Gold Ingot", "Gold"});
        MATERIAL_DISPLAY_NAMES.put("MYCELIUM", new String[]{"Mycelium"});
        MATERIAL_DISPLAY_NAMES.put("RED_SAND", new String[]{"Red Sand"});
        MATERIAL_DISPLAY_NAMES.put("OBSIDIAN", new String[]{"Obsidian"});
        MATERIAL_DISPLAY_NAMES.put("QUARTZ", new String[]{"Nether Quartz", "Quartz"});
        MATERIAL_DISPLAY_NAMES.put("EMERALD", new String[]{"Emerald"});

        // Minecraft item IDs for inventory tracking
        MATERIAL_ITEM_IDS.put("COAL", new String[]{"minecraft:coal"});
        MATERIAL_ITEM_IDS.put("DIAMOND", new String[]{"minecraft:diamond"});
        MATERIAL_ITEM_IDS.put("GOLD", new String[]{"minecraft:gold_ingot"});
        MATERIAL_ITEM_IDS.put("MYCELIUM", new String[]{"minecraft:mycelium"});
        MATERIAL_ITEM_IDS.put("RED_SAND", new String[]{"minecraft:red_sand"});
        MATERIAL_ITEM_IDS.put("OBSIDIAN", new String[]{"minecraft:obsidian"});
        MATERIAL_ITEM_IDS.put("QUARTZ", new String[]{"minecraft:quartz"});
        MATERIAL_ITEM_IDS.put("EMERALD", new String[]{"minecraft:emerald"});
    }

    public static void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
    }

    public static DisplayMode getDisplayMode() {
        return displayMode;
    }

    public static String[] getAllMaterials() {
        return new String[]{"COAL", "DIAMOND", "GOLD", "MYCELIUM", "RED_SAND", "OBSIDIAN", "QUARTZ", "EMERALD"};
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
     */
    public static void onInventoryItemAdd(String itemId, String itemName, int amount) {
        // Find which material this item belongs to
        String material = findMaterialByItemId(itemId);
        if (material == null) return;

        boolean isEnchanted = itemName.contains("Enchanted");
        trackItem(material, amount, isEnchanted, "Inventory");
    }

    /**
     * Called when sack chat messages are received
     */
    public static void onChatMessage(Text message) {
        String messageText = message.getString();

        Matcher sackMatcher = SACK_PATTERN.matcher(messageText);
        if (!sackMatcher.find()) return;

        String amountStr = sackMatcher.group(1).replace(",", "");
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            return;
        }

        String hoverText = extractHoverText(message);
        if (hoverText == null || hoverText.isEmpty()) return;

        // Find which material this is
        String material = findMaterialByHoverText(hoverText);
        if (material == null) return;

        boolean isEnchanted = hoverText.contains("Enchanted");
        trackItem(material, amount, isEnchanted, "Sacks");
    }

    private static void trackItem(String material, long amount, boolean isEnchanted, String source) {
        MaterialData data = getOrCreateData(material);

        // Start session if needed
        if (data.sessionStartTime == 0) {
            BazaarPriceManager.updateBlockPrices();
            data.sessionStartTime = System.currentTimeMillis();
        }

        data.lastActivityTime = System.currentTimeMillis();

        if (isEnchanted) {
            data.enchantedItems += amount;
            LOGGER.info("[{}] Tracked {} enchanted {} (total: {})", source, amount, material, data.enchantedItems);
        } else {
            data.rawItems += amount;
            LOGGER.info("[{}] Tracked {} raw {} (total: {})", source, amount, material, data.rawItems);
        }
    }

    private static String findMaterialByItemId(String itemId) {
        for (Map.Entry<String, String[]> entry : MATERIAL_ITEM_IDS.entrySet()) {
            for (String id : entry.getValue()) {
                if (itemId.equals(id)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private static String findMaterialByHoverText(String hoverText) {
        for (Map.Entry<String, String[]> entry : MATERIAL_DISPLAY_NAMES.entrySet()) {
            for (String name : entry.getValue()) {
                if (hoverText.contains(name)) {
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
    public static long getTotalEnchantedItems(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null) return 0;
        int ratio = getEnchantedRatio(material);
        return data.enchantedItems + (data.rawItems / ratio);
    }

    public static long getTotalRawEquivalent(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null) return 0;
        int ratio = getEnchantedRatio(material);
        return (data.enchantedItems * ratio) + data.rawItems;
    }

    public static double getTotalValue(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null) return 0;

        String enchantedId = MATERIAL_TO_ENCHANTED.get(material);
        double enchantedPrice = BazaarPriceManager.getBlockPrice(enchantedId);
        int ratio = getEnchantedRatio(material);

        double enchantedValue = data.enchantedItems * enchantedPrice;
        double rawValue = (data.rawItems / (double) ratio) * enchantedPrice;

        return enchantedValue + rawValue;
    }

    public static long getSessionTime(String material) {
        MaterialData data = materialDataMap.get(material);
        if (data == null) return 0;
        return data.getSessionTime();
    }

    public static double getCoinsPerHour(String material) {
        long sessionMs = getSessionTime(material);
        if (sessionMs == 0) return 0;
        double hours = sessionMs / (1000.0 * 60.0 * 60.0);
        return getTotalValue(material) / hours;
    }

    public static double getEnchantedPerHour(String material) {
        long sessionMs = getSessionTime(material);
        if (sessionMs == 0) return 0;
        double hours = sessionMs / (1000.0 * 60.0 * 60.0);
        return getTotalEnchantedItems(material) / hours;
    }

    public static double getCollectionPerHour(String material) {
        long sessionMs = getSessionTime(material);
        if (sessionMs == 0) return 0;
        double hours = sessionMs / (1000.0 * 60.0 * 60.0);
        return getTotalRawEquivalent(material) / hours;
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

    // Legacy compatibility methods (for old code that might reference these)
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
