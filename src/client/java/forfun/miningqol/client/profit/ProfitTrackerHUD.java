package forfun.miningqol.client.profit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class ProfitTrackerHUD {
    private static int hudX = 10;
    private static int hudY = 10;
    private static float scale = 1.0f;
    private static boolean enabled = false;
    private static String mode = "GEMSTONES"; // GEMSTONES or BLOCKS

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setPosition(int x, int y) {
        hudX = x;
        hudY = y;
    }

    public static int getX() {
        return hudX;
    }

    public static int getY() {
        return hudY;
    }

    public static void setScale(float newScale) {
        scale = Math.max(0.5f, Math.min(3.0f, newScale));
    }

    public static float getScale() {
        return scale;
    }

    public static void setMode(String newMode) {
        mode = newMode;
    }

    public static String getMode() {
        return mode;
    }

    public static boolean isBlockMode() {
        return "BLOCKS".equals(mode);
    }

    public static int getWidth() {
        return (int)(130 * scale);
    }

    public static int getHeight() {
        return isBlockMode() ? (int)(70 * scale) : (int)(30 * scale);
    }

    public static void render(DrawContext context) {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;

        if (isBlockMode()) {
            renderBlockMode(context, textRenderer);
        } else {
            renderGemstoneMode(context, textRenderer);
        }
    }

    public static void renderPreview(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        TextRenderer textRenderer = client.textRenderer;

        if (isBlockMode()) {
            renderBlockModePreview(context, textRenderer);
        } else {
            renderGemstoneModePreview(context, textRenderer);
        }
    }

    private static void renderGemstoneModePreview(DrawContext context, TextRenderer textRenderer) {
        String uptime = "\u00A76Uptime: \u00A7f1h 23m 45s";
        String coinsPerHour = "\u00A7e$/hr: \u00A7a1,234,567 \u00A77(Flawless)";
        String flawlessPerHour = "\u00A7d fl/hr: \u00A7b42.5 \u00A77(Flawless)";

        int lineHeight = (int)(10 * scale);
        int y = hudY;
        drawScaledText(context, textRenderer, uptime, hudX, y);
        y += lineHeight;
        drawScaledText(context, textRenderer, coinsPerHour, hudX, y);
        y += lineHeight;
        drawScaledText(context, textRenderer, flawlessPerHour, hudX, y);
    }

    private static void renderBlockModePreview(DrawContext context, TextRenderer textRenderer) {
        int lineHeight = (int)(10 * scale);
        int y = hudY;

        // Show a sample combined view
        ItemStack diamondIcon = new ItemStack(Items.DIAMOND);
        context.drawItem(diamondIcon, hudX, y - 4);

        String title = "\u00A7fDiamond Profit";
        drawScaledText(context, textRenderer, title, hudX + 18, y);
        y += lineHeight + 4;

        drawScaledText(context, textRenderer, "\u00A77Ench. Diamond: \u00A7f1,234", hudX, y);
        y += lineHeight;
        drawScaledText(context, textRenderer, "\u00A77Total: \u00A761,234,567", hudX, y);
        y += lineHeight;
        drawScaledText(context, textRenderer, "\u00A77Per Hour: \u00A76987,654", hudX, y);
        y += lineHeight;
        drawScaledText(context, textRenderer, "\u00A77Ench/hr: \u00A7a456", hudX, y);
        y += lineHeight;
        drawScaledText(context, textRenderer, "\u00A77Coll/hr: \u00A7b12,345", hudX, y);
    }

    private static void renderGemstoneMode(DrawContext context, TextRenderer textRenderer) {
        String uptime = "\u00A76Uptime: \u00A7f" + GemstoneTracker.formatTime(GemstoneTracker.getSessionTime());
        String coinsPerHour = "\u00A7e$/hr: \u00A7a" + GemstoneTracker.formatCoins(GemstoneTracker.getCoinsPerHour());
        String flawlessPerHour = "\u00A7d fl/hr: \u00A7b" + String.format("%.1f", GemstoneTracker.getFlawlessPerHour());

        String tierIndicator = " \u00A77(" + GemstoneTracker.getGemTierName() + ")";
        coinsPerHour += tierIndicator;
        flawlessPerHour += tierIndicator;

        if (GemstoneTracker.isIncludingRough()) {
            coinsPerHour += " \u00A77+r";
            flawlessPerHour += " \u00A77+r";
        }

        if (BazaarPriceManager.isUsingNPCPrices()) {
            coinsPerHour += " \u00A77npc";
        }

        int lineHeight = (int)(10 * scale);
        int y = hudY;
        drawScaledText(context, textRenderer, uptime, hudX, y);
        y += lineHeight;
        drawScaledText(context, textRenderer, coinsPerHour, hudX, y);
        y += lineHeight;
        drawScaledText(context, textRenderer, flawlessPerHour, hudX, y);
    }

    private static void renderBlockMode(DrawContext context, TextRenderer textRenderer) {
        // Only show if there are active materials being tracked
        if (!BlockTracker.hasActiveMaterials()) {
            return;
        }

        List<String> activeMaterials = BlockTracker.getActiveMaterials();

        if (BlockTracker.getDisplayMode() == BlockTracker.DisplayMode.COMBINED) {
            renderCombinedMode(context, textRenderer, activeMaterials);
        } else {
            renderSeparateMode(context, textRenderer, activeMaterials);
        }
    }

    private static void renderSeparateMode(DrawContext context, TextRenderer textRenderer, List<String> materials) {
        int lineHeight = (int)(10 * scale);
        int trackerWidth = 140; // Width of each tracker column
        int xOffset = 0;

        for (String material : materials) {
            int x = hudX + xOffset;
            int y = hudY;

            // Draw item icon and title
            ItemStack itemIcon = getMaterialItemStack(material);
            context.drawItem(itemIcon, x, y - 4);

            String materialName = BlockTracker.getMaterialDisplayName(material);
            String npcTag = BazaarPriceManager.isUsingNPCPrices() ? " \u00A77(npc)" : "";
            String title = "\u00A7f" + materialName + " Profit" + npcTag;
            drawScaledText(context, textRenderer, title, x + 18, y);
            y += lineHeight + 4;

            // Enchanted item count
            String enchName = "Ench. " + materialName;
            String enchantedLine = "\u00A77" + enchName + ": \u00A7f" +
                    BlockTracker.formatWithCommas(BlockTracker.getTotalEnchantedItems(material));
            drawScaledText(context, textRenderer, enchantedLine, x, y);
            y += lineHeight;

            // Total value
            String totalLine = "\u00A77Total: \u00A76" + BlockTracker.formatWithCommas((long) BlockTracker.getTotalValue(material));
            drawScaledText(context, textRenderer, totalLine, x, y);
            y += lineHeight;

            // Per hour
            String perHourLine = "\u00A77Per Hour: \u00A76" + BlockTracker.formatWithCommas((long) BlockTracker.getCoinsPerHour(material));
            drawScaledText(context, textRenderer, perHourLine, x, y);
            y += lineHeight;

            // Enchanted per hour
            String enchPerHourLine = "\u00A77Ench/hr: \u00A7a" + BlockTracker.formatWithCommas((long) BlockTracker.getEnchantedPerHour(material));
            drawScaledText(context, textRenderer, enchPerHourLine, x, y);
            y += lineHeight;

            // Collection per hour
            String collPerHourLine = "\u00A77Coll/hr: \u00A7b" + BlockTracker.formatWithCommas((long) BlockTracker.getCollectionPerHour(material));
            drawScaledText(context, textRenderer, collPerHourLine, x, y);

            xOffset += trackerWidth;
        }
    }

    private static void renderCombinedMode(DrawContext context, TextRenderer textRenderer, List<String> materials) {
        int lineHeight = (int)(10 * scale);
        int y = hudY;

        // Title with icons of all materials being tracked
        int iconX = hudX;
        for (String material : materials) {
            ItemStack itemIcon = getMaterialItemStack(material);
            context.drawItem(itemIcon, iconX, y - 4);
            iconX += 18;
        }

        String npcTag = BazaarPriceManager.isUsingNPCPrices() ? " \u00A77(npc)" : "";
        String title = "\u00A7fCombined Profit" + npcTag;
        drawScaledText(context, textRenderer, title, iconX, y);
        y += lineHeight + 4;

        // Total value
        String totalLine = "\u00A77Total: \u00A76" + BlockTracker.formatWithCommas((long) BlockTracker.getCombinedTotalValue());
        drawScaledText(context, textRenderer, totalLine, hudX, y);
        y += lineHeight;

        // Per hour
        String perHourLine = "\u00A77Per Hour: \u00A76" + BlockTracker.formatWithCommas((long) BlockTracker.getCombinedCoinsPerHour());
        drawScaledText(context, textRenderer, perHourLine, hudX, y);
        y += lineHeight;

        // Session time
        String timeLine = "\u00A77Uptime: \u00A7f" + BlockTracker.formatTime(BlockTracker.getCombinedSessionTime());
        drawScaledText(context, textRenderer, timeLine, hudX, y);
        y += lineHeight;

        // List each material's contribution
        y += 4; // Small gap
        for (String material : materials) {
            ItemStack itemIcon = getMaterialItemStack(material);
            context.drawItem(itemIcon, hudX, y - 4);

            String materialName = BlockTracker.getMaterialDisplayName(material);
            String materialLine = "\u00A77" + materialName + ": \u00A76" +
                    BlockTracker.formatWithCommas((long) BlockTracker.getTotalValue(material));
            drawScaledText(context, textRenderer, materialLine, hudX + 18, y);
            y += lineHeight;
        }
    }

    private static ItemStack getMaterialItemStack(String material) {
        Item item;
        switch (material) {
            case "COAL":
                item = Items.COAL;
                break;
            case "DIAMOND":
                item = Items.DIAMOND;
                break;
            case "GOLD":
                item = Items.GOLD_INGOT;
                break;
            case "IRON":
                item = Items.IRON_INGOT;
                break;
            case "EMERALD":
                item = Items.EMERALD;
                break;
            case "QUARTZ":
                item = Items.QUARTZ;
                break;
            case "OBSIDIAN":
                item = Items.OBSIDIAN;
                break;
            case "MYCELIUM":
                item = Items.MYCELIUM;
                break;
            case "RED_SAND":
                item = Items.RED_SAND;
                break;
            case "LAPIS":
                item = Items.LAPIS_LAZULI;
                break;
            case "REDSTONE":
                item = Items.REDSTONE;
                break;
            case "GLOWSTONE":
                item = Items.GLOWSTONE_DUST;
                break;
            case "HARDSTONE":
                item = Items.STONE;
                break;
            case "MITHRIL":
                item = Items.PRISMARINE_CRYSTALS;
                break;
            case "TITANIUM":
                item = Items.IRON_INGOT;
                break;
            case "SULPHUR":
                item = Items.GLOWSTONE_DUST;
                break;
            case "UMBER":
                item = Items.BROWN_TERRACOTTA;
                break;
            default:
                item = Items.COAL;
        }
        return new ItemStack(item);
    }

    private static void drawScaledText(DrawContext context, TextRenderer textRenderer, String text, int x, int y) {
        context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFFFF);
    }
}
