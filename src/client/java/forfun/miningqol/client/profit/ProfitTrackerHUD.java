package forfun.miningqol.client.profit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class ProfitTrackerHUD {
    private static int hudX = 10;
    private static int hudY = 10;
    private static boolean enabled = false;

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

    public static void render(DrawContext context) {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;

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

        int y = hudY;
        context.drawTextWithShadow(textRenderer, uptime, hudX, y, 0xFFFFFFFF);
        y += 10;
        context.drawTextWithShadow(textRenderer, coinsPerHour, hudX, y, 0xFFFFFFFF);
        y += 10;
        context.drawTextWithShadow(textRenderer, flawlessPerHour, hudX, y, 0xFFFFFFFF);
    }
}
