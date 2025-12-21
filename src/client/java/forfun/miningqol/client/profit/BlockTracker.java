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
    private static long lastBlockTime = 0;
    private static final long RESET_DELAY = 60000; // 1 minute without blocks = reset

    private static long totalBlocks = 0;
    private static String currentMaterial = "COAL";

    // Cached coins per hour - only updated on sack messages
    private static double cachedCoinsPerHour = 0;

    // Pattern to match sack messages like "[Sacks] +22,400 items (Last 30s.)"
    private static final Pattern SACK_PATTERN = Pattern.compile("\\[Sacks\\] \\+([\\d,]+) items");

    // Mapping from display names to bazaar item IDs
    private static final Map<String, String> MATERIAL_TO_ENCHANTED = new HashMap<>();
    private static final Map<String, String[]> MATERIAL_DISPLAY_NAMES = new HashMap<>();

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

        // Map material types to possible display names in hover/message text
        MATERIAL_DISPLAY_NAMES.put("COAL", new String[]{"Coal"});
        MATERIAL_DISPLAY_NAMES.put("DIAMOND", new String[]{"Diamond"});
        MATERIAL_DISPLAY_NAMES.put("GOLD", new String[]{"Gold Ingot", "Gold"});
        MATERIAL_DISPLAY_NAMES.put("MYCELIUM", new String[]{"Mycelium"});
        MATERIAL_DISPLAY_NAMES.put("RED_SAND", new String[]{"Red Sand"});
        MATERIAL_DISPLAY_NAMES.put("OBSIDIAN", new String[]{"Obsidian"});
        MATERIAL_DISPLAY_NAMES.put("QUARTZ", new String[]{"Nether Quartz", "Quartz"});
        MATERIAL_DISPLAY_NAMES.put("EMERALD", new String[]{"Emerald"});
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

    public static String[] getAllMaterials() {
        return new String[]{"COAL", "DIAMOND", "GOLD", "MYCELIUM", "RED_SAND", "OBSIDIAN", "QUARTZ", "EMERALD"};
    }

    public static void onChatMessage(Text message) {
        String messageText = message.getString();

        // Check if it's a sack message
        Matcher matcher = SACK_PATTERN.matcher(messageText);
        if (!matcher.find()) {
            return;
        }

        // Extract the amount
        String amountStr = matcher.group(1).replace(",", "");
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
        for (String name : displayNames) {
            if (hoverText.contains(name)) {
                matchesMaterial = true;
                break;
            }
        }

        if (!matchesMaterial) {
            return;
        }

        // Track the blocks
        if (!isTracking) {
            startSession();
        }

        lastBlockTime = System.currentTimeMillis();
        totalBlocks += amount;

        // Update cached values
        updateCachedValues();
    }

    private static void updateCachedValues() {
        long sessionTime = System.currentTimeMillis() - sessionStartTime;

        // Get enchanted price
        String enchantedId = MATERIAL_TO_ENCHANTED.get(currentMaterial);
        double enchantedPrice = BazaarPriceManager.getBlockPrice(enchantedId);

        // Calculate coins per hour: 160 raw blocks = 1 enchanted
        if (sessionTime > 0) {
            double blocksPerHour = totalBlocks / (sessionTime / (1000.0 * 60.0 * 60.0));
            double enchantedPerHour = blocksPerHour / 160.0;
            cachedCoinsPerHour = enchantedPerHour * enchantedPrice;
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
        if (isTracking && System.currentTimeMillis() - lastBlockTime > RESET_DELAY) {
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
        lastBlockTime = 0;
        totalBlocks = 0;
        cachedCoinsPerHour = 0;
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static long getSessionTime() {
        if (!isTracking) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    public static long getTotalBlocks() {
        return totalBlocks;
    }

    public static double getCoinsPerHour() {
        // Return cached value (only updates on sack messages)
        return cachedCoinsPerHour;
    }

    public static double getTotalValue() {
        // Calculate in real-time
        String enchantedId = MATERIAL_TO_ENCHANTED.get(currentMaterial);
        double enchantedPrice = BazaarPriceManager.getBlockPrice(enchantedId);
        double enchantedCount = totalBlocks / 160.0;
        return enchantedCount * enchantedPrice;
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
            return String.format("$%.2fB", coins / 1_000_000_000.0);
        } else if (coins >= 1_000_000) {
            return String.format("$%.2fM", coins / 1_000_000.0);
        } else if (coins >= 1_000) {
            return String.format("$%.2fK", coins / 1_000.0);
        } else {
            return String.format("$%.0f", coins);
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
