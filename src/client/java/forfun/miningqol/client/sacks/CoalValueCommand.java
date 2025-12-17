package forfun.miningqol.client.sacks;

import forfun.miningqol.client.gui.CoalValueScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CoalValueCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("CoalValueCommand");
    private static final DecimalFormat COIN_FORMAT = new DecimalFormat("#,##0.0");
    private static final DecimalFormat COUNT_FORMAT = new DecimalFormat("#,###");

    public enum SellMethod {
        INSTASELL,
        SELLOFFER
    }

    // Recipes:
    // 16 Enchanted Coal + 1 Enchanted Sulphur → 4 Sulphuric Coal
    // 24 Crude Gabagool + 1 Sulphuric Coal → 1 Fuel Gabagool
    // 24 Fuel Gabagool + 1 Sulphuric Coal → 1 Heavy Gabagool
    // 12 Heavy Gabagool + 1 Sulphuric Coal → 1 Hypergolic Gabagool

    public static void execute(SellMethod method) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (!SackTracker.hasData()) {
            client.player.sendMessage(Text.literal("§c[CoalValue] No sack data! Open your Enchanted Mining Sack first."), false);
            return;
        }

        Map<String, Long> sackContents = SackTracker.getSackContents();
        long enchantedCoal = sackContents.getOrDefault("ENCHANTED_COAL", 0L);

        if (enchantedCoal == 0) {
            client.player.sendMessage(Text.literal("§c[CoalValue] No Enchanted Coal in sack!"), false);
            return;
        }

        client.player.sendMessage(Text.literal("§6[CoalValue] Fetching Bazaar prices..."), false);

        BazaarAPI.fetchPrices().thenAccept(products -> {
            client.execute(() -> calculateCraftingOptions(client, method, products, enchantedCoal));
        });
    }

    private static void calculateCraftingOptions(MinecraftClient client, SellMethod method,
                                                  Map<String, BazaarAPI.BazaarProduct> products, long enchantedCoal) {
        if (client.player == null) return;

        if (products.isEmpty()) {
            client.player.sendMessage(Text.literal("§c[CoalValue] Failed to fetch Bazaar prices!"), false);
            return;
        }

        // Get prices based on sell method
        double ecoalSellPrice = getPrice(products, "ENCHANTED_COAL", method);
        double sulphurBuyPrice = getBuyPrice(products, "ENCHANTED_SULPHUR", method);
        double sulphuricSellPrice = getPrice(products, "SULPHURIC_COAL", method);
        double crudeGabaBuyPrice = getBuyPrice(products, "CRUDE_GABAGOOL", method);
        double fuelGabaSellPrice = getPrice(products, "FUEL_GABAGOOL", method);
        double fuelGabaBuyPrice = getBuyPrice(products, "FUEL_GABAGOOL", method);
        double heavyGabaBuyPrice = getBuyPrice(products, "HEAVY_GABAGOOL", method);
        double heavyGabaInstaBuyPrice = getInstaBuyPrice(products, "HEAVY_GABAGOOL");
        double hyperGabaSellPrice = getPrice(products, "HYPERGOLIC_GABAGOOL", method);

        String methodName = (method == SellMethod.INSTASELL) ? "Insta-Sell" : "Sell Offer";

        List<CoalValueScreen.CraftingOption> options = new ArrayList<>();

        // Option 1: Sell Enchanted Coal directly
        double option1Profit = enchantedCoal * ecoalSellPrice;
        options.add(new CoalValueScreen.CraftingOption(
            "Sell Enchanted Coal",
            "",
            List.of(),
            option1Profit
        ));

        // Option 2: Craft to Sulphuric Coal
        long sulphuricCoalCrafts = enchantedCoal / 16;
        long sulphuricCoalOutput = sulphuricCoalCrafts * 4;
        double sulphurCost = sulphuricCoalCrafts * sulphurBuyPrice;
        double option2Revenue = sulphuricCoalOutput * sulphuricSellPrice;
        double option2Profit = option2Revenue - sulphurCost;
        options.add(new CoalValueScreen.CraftingOption(
            "Craft Sulphuric Coal",
            COUNT_FORMAT.format(sulphuricCoalOutput) + " Sulphuric Coal",
            List.of(COUNT_FORMAT.format(sulphuricCoalCrafts) + " Sulphur (-" + COIN_FORMAT.format(sulphurCost) + ")"),
            option2Profit
        ));

        // Option 3: Craft to Fuel Gabagool
        long fuelGabaCrafts = sulphuricCoalOutput;
        long crudeGabaNeeded = fuelGabaCrafts * 24;
        double crudeGabaCost = crudeGabaNeeded * crudeGabaBuyPrice;
        double option3Revenue = fuelGabaCrafts * fuelGabaSellPrice;
        double option3Profit = option3Revenue - sulphurCost - crudeGabaCost;
        options.add(new CoalValueScreen.CraftingOption(
            "Craft Fuel Gabagool",
            COUNT_FORMAT.format(fuelGabaCrafts) + " Fuel Gabagool",
            List.of(
                COUNT_FORMAT.format(sulphuricCoalCrafts) + " Sulphur (-" + COIN_FORMAT.format(sulphurCost) + ")",
                COUNT_FORMAT.format(crudeGabaNeeded) + " Crude Gaba (-" + COIN_FORMAT.format(crudeGabaCost) + ")"
            ),
            option3Profit
        ));

        // Option 4: Craft to Hypergolic Gabagool (buy order Heavy)
        long hyperGabaCrafts = sulphuricCoalOutput;
        long heavyGabaNeeded = hyperGabaCrafts * 12;
        double heavyGabaCost = heavyGabaNeeded * heavyGabaBuyPrice;
        double option4Revenue = hyperGabaCrafts * hyperGabaSellPrice;
        double option4Profit = option4Revenue - sulphurCost - heavyGabaCost;
        options.add(new CoalValueScreen.CraftingOption(
            "Craft Hypergolic (buy order Heavy)",
            COUNT_FORMAT.format(hyperGabaCrafts) + " Hypergolic Gabagool",
            List.of(
                COUNT_FORMAT.format(sulphuricCoalCrafts) + " Sulphur (-" + COIN_FORMAT.format(sulphurCost) + ")",
                COUNT_FORMAT.format(heavyGabaNeeded) + " Heavy Gaba (-" + COIN_FORMAT.format(heavyGabaCost) + ")"
            ),
            option4Profit
        ));

        // Option 5: Craft to Hypergolic Gabagool (insta-buy Heavy)
        double heavyGabaInstaCost = heavyGabaNeeded * heavyGabaInstaBuyPrice;
        double option5Revenue = hyperGabaCrafts * hyperGabaSellPrice;
        double option5Profit = option5Revenue - sulphurCost - heavyGabaInstaCost;
        options.add(new CoalValueScreen.CraftingOption(
            "Craft Hypergolic (insta-buy Heavy)",
            COUNT_FORMAT.format(hyperGabaCrafts) + " Hypergolic Gabagool",
            List.of(
                COUNT_FORMAT.format(sulphuricCoalCrafts) + " Sulphur (-" + COIN_FORMAT.format(sulphurCost) + ")",
                COUNT_FORMAT.format(heavyGabaNeeded) + " Heavy Gaba (-" + COIN_FORMAT.format(heavyGabaInstaCost) + ")"
            ),
            option5Profit
        ));

        // Option 6: Full craft chain - Crude → Fuel → Heavy → Hypergolic
        // Sulphuric Coal allocation: 1 per Fuel, 1 per Heavy, 1 per Hypergolic
        // For H Hypergolic: need 12*H Heavy, need 24*12*H = 288*H Fuel
        // Sulphuric needed: 288*H (for Fuel) + 12*H (for Heavy) + H (for Hypergolic) = 301*H
        // So H = sulphuricCoalOutput / 301
        long option6Hypergolic = sulphuricCoalOutput / 301;
        long option6Heavy = option6Hypergolic * 12;
        long option6Fuel = option6Heavy * 24; // 288 * H
        long option6CrudeNeeded = option6Fuel * 24; // 24 Crude per Fuel
        double option6CrudeCost = option6CrudeNeeded * crudeGabaBuyPrice;
        double option6Revenue = option6Hypergolic * hyperGabaSellPrice;
        double option6Profit = option6Revenue - sulphurCost - option6CrudeCost;
        options.add(new CoalValueScreen.CraftingOption(
            "Craft Hypergolic (full chain)",
            COUNT_FORMAT.format(option6Hypergolic) + " Hypergolic Gabagool",
            List.of(
                COUNT_FORMAT.format(sulphuricCoalCrafts) + " Sulphur (-" + COIN_FORMAT.format(sulphurCost) + ")",
                COUNT_FORMAT.format(option6CrudeNeeded) + " Crude Gaba (-" + COIN_FORMAT.format(option6CrudeCost) + ")"
            ),
            option6Profit
        ));

        // Find best option index
        int bestIndex = 0;
        double bestProfit = option1Profit;
        double[] profits = {option1Profit, option2Profit, option3Profit, option4Profit, option5Profit, option6Profit};
        for (int i = 1; i < profits.length; i++) {
            if (profits[i] > bestProfit) {
                bestProfit = profits[i];
                bestIndex = i;
            }
        }

        // Open the GUI
        client.setScreen(new CoalValueScreen(enchantedCoal, methodName, options, bestIndex));
    }

    private static double getPrice(Map<String, BazaarAPI.BazaarProduct> products, String productId, SellMethod method) {
        // For selling products:
        // INSTASELL = sell to buy orders = topSellPrice (lower, instant)
        // SELLOFFER = create sell order = topBuyPrice (higher, wait)
        BazaarAPI.BazaarProduct product = products.get(productId);
        if (product == null) return 0;
        return (method == SellMethod.INSTASELL) ? product.topSellPrice : product.topBuyPrice;
    }

    private static double getBuyPrice(Map<String, BazaarAPI.BazaarProduct> products, String productId, SellMethod method) {
        // For buying materials: always use buy orders (cheaper)
        // topSellPrice = buy order price (lower cost)
        BazaarAPI.BazaarProduct product = products.get(productId);
        if (product == null) return 0;
        return product.topSellPrice;
    }

    private static double getInstaBuyPrice(Map<String, BazaarAPI.BazaarProduct> products, String productId) {
        // For insta-buying: use topBuyPrice (higher cost, instant)
        BazaarAPI.BazaarProduct product = products.get(productId);
        if (product == null) return 0;
        return product.topBuyPrice;
    }
}
