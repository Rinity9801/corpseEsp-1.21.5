package forfun.miningqol.client.profit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

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
        scale = Math.max(0.5f, Math.min(3.0f, newScale)); // Clamp between 0.5 and 3.0
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

    private static void renderGemstoneMode(DrawContext context, TextRenderer textRenderer) {
        String uptime = "§6Uptime: §f" + GemstoneTracker.formatTime(GemstoneTracker.getSessionTime());
        String coinsPerHour = "§e$/hr: §a" + GemstoneTracker.formatCoins(GemstoneTracker.getCoinsPerHour());
        String flawlessPerHour = "§d fl/hr: §b" + String.format("%.1f", GemstoneTracker.getFlawlessPerHour());

        String tierIndicator = " §7(" + GemstoneTracker.getGemTierName() + ")";
        coinsPerHour += tierIndicator;
        flawlessPerHour += tierIndicator;

        if (GemstoneTracker.isIncludingRough()) {
            coinsPerHour += " §7+r";
            flawlessPerHour += " §7+r";
        }

        if (BazaarPriceManager.isUsingNPCPrices()) {
            coinsPerHour += " §7npc";
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
        String materialName = BlockTracker.getMaterialDisplayName();
        int lineHeight = (int)(10 * scale);
        int y = hudY;

        // Title line with material name (like "Coal Profit" with colored icon)
        String titleColor = getMaterialColor(BlockTracker.getMaterial());
        String title = titleColor + "◆ §f" + materialName + " Profit";
        drawScaledText(context, textRenderer, title, hudX, y);
        y += lineHeight;

        // Enchanted item count
        String enchantedLine = "§7" + BlockTracker.getEnchantedDisplayName() + ": §f" +
                              formatWithCommas(BlockTracker.getTotalEnchantedItems());
        drawScaledText(context, textRenderer, enchantedLine, hudX, y);
        y += lineHeight;

        // Total value
        String totalLine = "§7Total: §6" + formatWithCommas((long) BlockTracker.getTotalValue());
        drawScaledText(context, textRenderer, totalLine, hudX, y);
        y += lineHeight;

        // Per hour
        String perHourLine = "§7Per Hour: §6" + formatWithCommas((long) BlockTracker.getCoinsPerHour());
        drawScaledText(context, textRenderer, perHourLine, hudX, y);
        y += lineHeight;

        // Enchanted per hour
        String enchPerHourLine = "§7Ench/hr: §a" + formatWithCommas((long) BlockTracker.getEnchantedPerHour());
        drawScaledText(context, textRenderer, enchPerHourLine, hudX, y);
        y += lineHeight;

        // Collection per hour
        String collPerHourLine = "§7Coll/hr: §b" + formatWithCommas((long) BlockTracker.getCollectionPerHour());
        drawScaledText(context, textRenderer, collPerHourLine, hudX, y);
    }

    private static String getMaterialColor(String material) {
        switch (material) {
            case "COAL": return "§8"; // Dark gray
            case "DIAMOND": return "§b"; // Aqua
            case "GOLD": return "§6"; // Gold
            case "EMERALD": return "§a"; // Green
            case "QUARTZ": return "§f"; // White
            case "OBSIDIAN": return "§5"; // Purple
            case "MYCELIUM": return "§d"; // Light purple
            case "RED_SAND": return "§c"; // Red
            default: return "§f";
        }
    }

    private static String formatWithCommas(long number) {
        return String.format("%,d", number);
    }

    private static void drawScaledText(DrawContext context, TextRenderer textRenderer, String text, int x, int y) {
        context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFFFF);
    }
}
