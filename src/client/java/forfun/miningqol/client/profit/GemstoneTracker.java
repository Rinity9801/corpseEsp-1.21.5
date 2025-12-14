package forfun.miningqol.client.profit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class GemstoneTracker {
    private static boolean isTracking = false;
    private static long sessionStartTime = 0;
    private static long lastGemTime = 0;
    private static final long RESET_DELAY = 30000; 

    private static final Map<String, Integer> gemCounts = new HashMap<>();
    private static double totalValue = 0;
    private static int pristineChance = 20; 
    private static boolean includeRough = false;
    private static int gemTier = 1; 

    public static void setPristineChance(int chance) {
        pristineChance = Math.max(0, Math.min(100, chance));
    }

    public static int getPristineChance() {
        return pristineChance;
    }

    public static void setIncludeRough(boolean include) {
        includeRough = include;
    }

    public static boolean isIncludingRough() {
        return includeRough;
    }

    public static void setGemTier(int tier) {
        gemTier = Math.max(1, Math.min(3, tier));
    }

    public static int getGemTier() {
        return gemTier;
    }

    public static String getGemTierName() {
        switch (gemTier) {
            case 1: return "Flawed";
            case 2: return "Fine";
            case 3: return "Flawless";
            default: return "Flawed";
        }
    }

    public static void onPristineGem(String gemType, int amount) {
        if (!isTracking) {
            startSession();
        }

        lastGemTime = System.currentTimeMillis();

        gemCounts.put(gemType, gemCounts.getOrDefault(gemType, 0) + amount);

        double gemPrice = BazaarPriceManager.getGemPrice(gemType, gemTier);
        double gemValue = (gemPrice / Math.pow(80, gemTier - 1)) * amount;

        totalValue += gemValue;
    }

    public static void tick() {
        if (isTracking && System.currentTimeMillis() - lastGemTime > RESET_DELAY) {
            reset();
        }
    }

    public static void startSession() {
        BazaarPriceManager.updatePrices();
        sessionStartTime = System.currentTimeMillis();
        isTracking = true;
    }

    public static void reset() {
        isTracking = false;
        sessionStartTime = 0;
        lastGemTime = 0;
        gemCounts.clear();
        totalValue = 0;
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static long getSessionTime() {
        if (!isTracking) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    public static double getTotalValue() {
        return totalValue;
    }

    public static double getCoinsPerHour() {
        if (!isTracking || getSessionTime() == 0) return 0;

        double hours = getSessionTime() / (1000.0 * 60.0 * 60.0);
        double baseCoins = totalValue / hours;

        if (includeRough) {
            double roughMultiplier = (1 - (pristineChance / 100.0)) / (pristineChance / 100.0);
            double roughValue = baseCoins / 80.0 * roughMultiplier;
            return baseCoins + roughValue;
        }

        return baseCoins;
    }

    public static double getFlawlessPerHour() {
        if (!isTracking) return 0;

        double flawlessPrice = BazaarPriceManager.getGemPrice("RUBY", 3);
        if (flawlessPrice == 0) return 0;

        return getCoinsPerHour() / flawlessPrice;
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
}
