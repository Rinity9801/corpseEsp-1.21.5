package forfun.miningqol.client.profit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ProfitDebugger {

    public static void showCalculationDetails() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.literal("§d[Profit Tracker] §eCalculation Details:"), false);
        client.player.sendMessage(Text.literal("§8" + "=".repeat(50)), false);

        
        long sessionTime = GemstoneTracker.getSessionTime();
        double hours = sessionTime / (1000.0 * 60.0 * 60.0);
        client.player.sendMessage(Text.literal("§6Session Info:"), false);
        client.player.sendMessage(Text.literal("  §7Time: §f" + GemstoneTracker.formatTime(sessionTime)), false);
        client.player.sendMessage(Text.literal("  §7Hours: §f" + String.format("%.4f", hours)), false);
        client.player.sendMessage(Text.literal("  §7Total Value: §6" + GemstoneTracker.formatCoins(GemstoneTracker.getTotalValue())), false);

        
        double baseCoinsPerHour = hours > 0 ? GemstoneTracker.getTotalValue() / hours : 0;
        client.player.sendMessage(Text.literal(""), false);
        client.player.sendMessage(Text.literal("§aBase Coins/hr Calculation:"), false);
        client.player.sendMessage(Text.literal("  §7Formula: §fTotal Value / Hours"), false);
        client.player.sendMessage(Text.literal("  §7= §f" + GemstoneTracker.formatCoins(GemstoneTracker.getTotalValue()) + " / " + String.format("%.4f", hours)), false);
        client.player.sendMessage(Text.literal("  §7= §a" + GemstoneTracker.formatCoins(baseCoinsPerHour)), false);

        
        if (GemstoneTracker.isIncludingRough()) {
            int pristine = GemstoneTracker.getPristineChance();
            double roughMultiplier = (1 - (pristine / 100.0)) / (pristine / 100.0);
            double roughValue = baseCoinsPerHour / 80.0 * roughMultiplier;
            double totalWithRough = baseCoinsPerHour + roughValue;

            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(Text.literal("§7Rough Gems Calculation:"), false);
            client.player.sendMessage(Text.literal("  §7Pristine: §f" + pristine + "%"), false);
            client.player.sendMessage(Text.literal("  §7Multiplier: §f(1 - " + pristine/100.0 + ") / " + pristine/100.0 + " = " + String.format("%.4f", roughMultiplier)), false);
            client.player.sendMessage(Text.literal("  §7Rough Value: §f" + GemstoneTracker.formatCoins(baseCoinsPerHour) + " / 80 * " + String.format("%.4f", roughMultiplier)), false);
            client.player.sendMessage(Text.literal("  §7= §e" + GemstoneTracker.formatCoins(roughValue)), false);
            client.player.sendMessage(Text.literal("  §7Total: §a" + GemstoneTracker.formatCoins(baseCoinsPerHour) + " + " + GemstoneTracker.formatCoins(roughValue)), false);
            client.player.sendMessage(Text.literal("  §7= §a" + GemstoneTracker.formatCoins(totalWithRough)), false);
        }

        
        double flawlessPrice = BazaarPriceManager.getGemPrice("RUBY", 3);
        double flawlessPerHour = GemstoneTracker.getFlawlessPerHour();
        client.player.sendMessage(Text.literal(""), false);
        client.player.sendMessage(Text.literal("§bFlawless/hr Calculation:"), false);
        client.player.sendMessage(Text.literal("  §7Flawless Ruby Price: §e" + GemstoneTracker.formatCoins(flawlessPrice)), false);
        client.player.sendMessage(Text.literal("  §7Formula: §fCoins/hr / Flawless Price"), false);
        client.player.sendMessage(Text.literal("  §7= §f" + GemstoneTracker.formatCoins(GemstoneTracker.getCoinsPerHour()) + " / " + GemstoneTracker.formatCoins(flawlessPrice)), false);
        client.player.sendMessage(Text.literal("  §7= §b" + String.format("%.1f", flawlessPerHour) + " fl/hr"), false);

        
        client.player.sendMessage(Text.literal(""), false);
        client.player.sendMessage(Text.literal("§ePrice Source: §f" + (BazaarPriceManager.isUsingNPCPrices() ? "NPC Prices" : "Bazaar Prices")), false);
        client.player.sendMessage(Text.literal("§8" + "=".repeat(50)), false);
    }
}
