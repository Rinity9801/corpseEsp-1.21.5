package forfun.miningqol.client.sacks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SackTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("SackTracker");
    private static final Map<String, Long> sackContents = new HashMap<>();
    private static long lastUpdateTime = 0;

    // Pattern to extract count from lore like "Stored: 12,345/20,000"
    private static final Pattern STORED_PATTERN = Pattern.compile("Stored:\\s*([\\d,]+)/");

    // Map display names to Bazaar product IDs - only coal/gabagool chain items
    private static final Map<String, String> ITEM_TO_BAZAAR_ID = new HashMap<>();

    static {
        // Coal chain items (what you mine/craft)
        ITEM_TO_BAZAAR_ID.put("Enchanted Coal", "ENCHANTED_COAL");
        ITEM_TO_BAZAAR_ID.put("Enchanted Sulphur", "ENCHANTED_SULPHUR");
        ITEM_TO_BAZAAR_ID.put("Sulphuric Coal", "SULPHURIC_COAL");

        // Gabagool chain items
        ITEM_TO_BAZAAR_ID.put("Crude Gabagool", "CRUDE_GABAGOOL");
        ITEM_TO_BAZAAR_ID.put("Fuel Gabagool", "FUEL_GABAGOOL");
        ITEM_TO_BAZAAR_ID.put("Heavy Gabagool", "HEAVY_GABAGOOL");
        ITEM_TO_BAZAAR_ID.put("Hypergolic Gabagool", "HYPERGOLIC_GABAGOOL");
    }

    public static void parseEnchantedMiningSack(GenericContainerScreen screen) {
        String title = screen.getTitle().getString();
        if (!title.contains("Enchanted Mining Sack")) {
            return;
        }

        LOGGER.info("[SackTracker] Parsing Enchanted Mining Sack");
        sackContents.clear();

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            String itemName = stack.getName().getString();
            // Remove color codes and trim
            itemName = itemName.replaceAll("\u00A7[0-9a-fk-or]", "").trim();

            // Check if this item is in our mapping
            String bazaarId = ITEM_TO_BAZAAR_ID.get(itemName);
            if (bazaarId == null) continue;

            // Parse the stored count from lore
            var lore = stack.getTooltip(net.minecraft.item.Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, net.minecraft.item.tooltip.TooltipType.BASIC);
            for (Text line : lore) {
                String lineStr = line.getString();
                Matcher matcher = STORED_PATTERN.matcher(lineStr);
                if (matcher.find()) {
                    String countStr = matcher.group(1).replace(",", "");
                    try {
                        long count = Long.parseLong(countStr);
                        if (count > 0) {
                            sackContents.put(bazaarId, count);
                            LOGGER.info("[SackTracker] Found {} x {}", count, bazaarId);
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warn("[SackTracker] Failed to parse count: {}", countStr);
                    }
                    break;
                }
            }
        }

        lastUpdateTime = System.currentTimeMillis();
        LOGGER.info("[SackTracker] Parsed {} item types from sack", sackContents.size());
    }

    public static Map<String, Long> getSackContents() {
        return new HashMap<>(sackContents);
    }

    public static long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public static boolean hasData() {
        return !sackContents.isEmpty();
    }
}
