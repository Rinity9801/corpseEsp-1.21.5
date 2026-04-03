package forfun.miningqol.client.sacks;

import forfun.miningqol.client.MiningqolClient;
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

    public enum BuyMethod {
        BUY_ORDER,
        INSTA_BUY
    }

    public static void execute() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (!SackTracker.hasData()) {
            client.player.sendMessage(Text.literal("\u00A7c[CoalValue] No sack data! Open your Enchanted Mining Sack first."), false);
            return;
        }

        Map<String, Long> sackContents = SackTracker.getSackContents();
        long enchantedCoal = sackContents.getOrDefault("ENCHANTED_COAL", 0L);

        if (enchantedCoal == 0) {
            client.player.sendMessage(Text.literal("\u00A7c[CoalValue] No Enchanted Coal in sack!"), false);
            return;
        }

        var config = MiningqolClient.getConfig();

        // If settings should be shown, open with settings panel
        // Otherwise fetch prices and go straight to results
        if (config.coalValueShowSettings) {
            client.send(() -> client.setScreen(new CoalValueScreen(enchantedCoal)));
        } else {
            client.player.sendMessage(Text.literal("\u00A76[CoalValue] Fetching Bazaar prices..."), false);
            fetchAndShowResults(client, enchantedCoal);
        }
    }

    public static void fetchAndShowResults(MinecraftClient client, long enchantedCoal) {
        BazaarAPI.fetchPrices().thenAccept(products -> {
            client.execute(() -> {
                if (products.isEmpty()) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("\u00A7c[CoalValue] Failed to fetch Bazaar prices!"), false);
                    }
                    return;
                }
                List<CraftingOption> options = calculateOptions(products, enchantedCoal);
                int bestIndex = findBestOption(options);
                client.setScreen(new CoalValueScreen(enchantedCoal, options, bestIndex));
            });
        });
    }

    public static List<CraftingOption> calculateOptions(Map<String, BazaarAPI.BazaarProduct> products, long enchantedCoal) {
        var config = MiningqolClient.getConfig();
        SellMethod sellMethod = SellMethod.valueOf(config.coalValueSellMethod);
        BuyMethod sulphurBuy = BuyMethod.valueOf(config.coalValueSulphurBuy);
        BuyMethod crudeBuy = BuyMethod.valueOf(config.coalValueCrudeBuy);
        BuyMethod heavyBuy = BuyMethod.valueOf(config.coalValueHeavyBuy);

        // Get prices based on configured methods
        double ecoalSellPrice = getSellPrice(products, "ENCHANTED_COAL", sellMethod);
        double sulphurBuyPrice = getBuyPrice(products, "ENCHANTED_SULPHUR", sulphurBuy);
        double sulphuricSellPrice = getSellPrice(products, "SULPHURIC_COAL", sellMethod);
        double crudeGabaBuyPrice = getBuyPrice(products, "CRUDE_GABAGOOL", crudeBuy);
        double fuelGabaSellPrice = getSellPrice(products, "FUEL_GABAGOOL", sellMethod);
        double heavyGabaBuyPrice = getBuyPrice(products, "HEAVY_GABAGOOL", heavyBuy);
        double hyperGabaSellPrice = getSellPrice(products, "HYPERGOLIC_GABAGOOL", sellMethod);

        List<CraftingOption> options = new ArrayList<>();

        // Option 1: Sell Enchanted Coal directly
        double option1Profit = enchantedCoal * ecoalSellPrice;
        options.add(new CraftingOption(
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
        options.add(new CraftingOption(
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
        options.add(new CraftingOption(
            "Craft Fuel Gabagool",
            COUNT_FORMAT.format(fuelGabaCrafts) + " Fuel Gabagool",
            List.of(
                COUNT_FORMAT.format(sulphuricCoalCrafts) + " Sulphur (-" + COIN_FORMAT.format(sulphurCost) + ")",
                COUNT_FORMAT.format(crudeGabaNeeded) + " Crude Gaba (-" + COIN_FORMAT.format(crudeGabaCost) + ")"
            ),
            option3Profit
        ));

        // Option 4: Craft to Hypergolic Gabagool (buying Heavy)
        long hyperGabaCrafts = sulphuricCoalOutput;
        long heavyGabaNeeded = hyperGabaCrafts * 12;
        double heavyGabaCost = heavyGabaNeeded * heavyGabaBuyPrice;
        double option4Revenue = hyperGabaCrafts * hyperGabaSellPrice;
        double option4Profit = option4Revenue - sulphurCost - heavyGabaCost;
        options.add(new CraftingOption(
            "Craft Hypergolic (buy Heavy)",
            COUNT_FORMAT.format(hyperGabaCrafts) + " Hypergolic Gabagool",
            List.of(
                COUNT_FORMAT.format(sulphuricCoalCrafts) + " Sulphur (-" + COIN_FORMAT.format(sulphurCost) + ")",
                COUNT_FORMAT.format(heavyGabaNeeded) + " Heavy Gaba (-" + COIN_FORMAT.format(heavyGabaCost) + ")"
            ),
            option4Profit
        ));

        // Option 5: Full craft chain - Crude -> Fuel -> Heavy -> Hypergolic
        long option5Hypergolic = sulphuricCoalOutput / 301;
        long option5Heavy = option5Hypergolic * 12;
        long option5Fuel = option5Heavy * 24;
        long option5CrudeNeeded = option5Fuel * 24;
        double option5CrudeCost = option5CrudeNeeded * crudeGabaBuyPrice;
        double option5Revenue = option5Hypergolic * hyperGabaSellPrice;
        double option5Profit = option5Revenue - sulphurCost - option5CrudeCost;
        options.add(new CraftingOption(
            "Craft Hypergolic (full chain)",
            COUNT_FORMAT.format(option5Hypergolic) + " Hypergolic Gabagool",
            List.of(
                COUNT_FORMAT.format(sulphuricCoalCrafts) + " Sulphur (-" + COIN_FORMAT.format(sulphurCost) + ")",
                COUNT_FORMAT.format(option5CrudeNeeded) + " Crude Gaba (-" + COIN_FORMAT.format(option5CrudeCost) + ")"
            ),
            option5Profit
        ));

        return options;
    }

    public static int findBestOption(List<CraftingOption> options) {
        int bestIndex = 0;
        double bestProfit = options.get(0).profit;
        for (int i = 1; i < options.size(); i++) {
            if (options.get(i).profit > bestProfit) {
                bestProfit = options.get(i).profit;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static double getSellPrice(Map<String, BazaarAPI.BazaarProduct> products, String productId, SellMethod method) {
        BazaarAPI.BazaarProduct product = products.get(productId);
        if (product == null) return 0;
        return (method == SellMethod.INSTASELL) ? product.topSellPrice : product.topBuyPrice;
    }

    private static double getBuyPrice(Map<String, BazaarAPI.BazaarProduct> products, String productId, BuyMethod method) {
        BazaarAPI.BazaarProduct product = products.get(productId);
        if (product == null) return 0;
        return (method == BuyMethod.BUY_ORDER) ? product.topSellPrice : product.topBuyPrice;
    }

    public static class CraftingOption {
        public final String name;
        public final String output;
        public final List<String> costs;
        public final double profit;

        public CraftingOption(String name, String output, List<String> costs, double profit) {
            this.name = name;
            this.output = output;
            this.costs = costs;
            this.profit = profit;
        }
    }
}
