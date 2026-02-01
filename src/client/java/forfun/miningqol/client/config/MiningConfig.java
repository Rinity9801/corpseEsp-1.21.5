package forfun.miningqol.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forfun.miningqol.client.AutoClickerHUD;
import forfun.miningqol.client.AutoClickerManager;
import forfun.miningqol.client.BlockOutlineRenderer;
import forfun.miningqol.client.CommClaimManager;
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.EfficientMinerOverlay;
import forfun.miningqol.client.GlassSync;
import forfun.miningqol.client.NameHider;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.profit.BazaarPriceManager;
import forfun.miningqol.client.profit.BlockTracker;
import forfun.miningqol.client.profit.GemstoneTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import forfun.miningqol.client.waypoints.OrderedWaypointManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class MiningConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/miningqol.json");

    public boolean lapisEnabled = true;
    public boolean tungstenEnabled = true;
    public boolean umberEnabled = true;
    public boolean vanguardEnabled = true;

    public boolean profitTrackerEnabled = false;
    public int profitTrackerX = 10;
    public int profitTrackerY = 10;
    public float profitTrackerScale = 1.0f;
    public int pristineChance = 20;
    public boolean includeRough = false;
    public boolean useNPCPrices = false;
    public int gemTier = 1;

    // Block profit tracking (Gemstones vs Blocks mode)
    public String profitTrackerMode = "GEMSTONES"; // GEMSTONES or BLOCKS
    public String blockTrackerDisplayMode = "SEPARATE"; // SEPARATE or COMBINED

    public boolean efficientMinerEnabled = false;
    public boolean useOldHeatmap = false;

    public boolean blockOutlineEnabled = false;
    public String blockOutlineMode = "BOTH";
    public float blockOutlineRed = 0.0f;
    public float blockOutlineGreen = 0.0f;
    public float blockOutlineBlue = 0.0f;
    public float blockOutlineAlpha = 0.4f;

    public boolean pickaxeCooldownEnabled = true;
    public int pickaxeCooldownX = 10;
    public int pickaxeCooldownY = 50;
    public float pickaxeCooldownScale = 1.0f;
    public boolean pickaxeCooldownTitleEnabled = true;
    public int pickaxeCooldownTitleThreshold = 5;

    public boolean nameHiderEnabled = false;
    public String replacementName = "Player";
    public boolean useGradient = false;
    public float nameColorRed1 = 1.0f;
    public float nameColorGreen1 = 1.0f;
    public float nameColorBlue1 = 1.0f;
    public float nameColorRed2 = 1.0f;
    public float nameColorGreen2 = 1.0f;
    public float nameColorBlue2 = 1.0f;

    public boolean autoClickerEnabled = false;
    public int autoClickerMiningSlot = 0;
    public boolean autoClickerRodSwap = true;
    public boolean autoClickerSecondDrill = false;
    public int autoClickerSecondDrillSlot = 3;
    public boolean autoClickerHudEnabled = true;

    public Map<String, String> commandKeybinds = new HashMap<>();

    public boolean autoSkipShoLoad = false;

    public boolean glassSyncEnabled = false;

    public java.util.List<String> lobbyFinderBlocks = new java.util.ArrayList<>();

    // Ordered Waypoints
    public boolean orderedWaypointsEnabled = true;
    public float orderedWaypointRange = 4.5f;
    public float[] orderedWaypointCurrentColor = {85f/255f, 1f, 85f/255f};
    public float[] orderedWaypointNextColor = {1f, 1f, 85f/255f};
    public float[] orderedWaypointPreviousColor = {85f/255f, 85f/255f, 1f};
    public float orderedWaypointCurrentAlpha = 0.6f;
    public float orderedWaypointNextAlpha = 0.6f;
    public float orderedWaypointPreviousAlpha = 0.6f;
    public int orderedWaypointNextCount = 2;
    public boolean orderedWaypointTraceLine = true;
    public float[] orderedWaypointTraceLineColor = {85f/255f, 1f, 85f/255f};
    public float orderedWaypointTraceLineAlpha = 1f;
    public boolean orderedWaypointShowDistance = true;
    public boolean orderedWaypointShowName = true;
    public boolean orderedWaypointLobbyCheckEnabled = false;
    public String orderedWaypointLobbyCheckBlock = "minecraft:coal_ore";
    public int orderedWaypointLobbyCheckInterval = 10;
    public int orderedWaypointLobbyCheckRadius = 2;
    public boolean orderedWaypointBlockOutline = false;
    public int orderedWaypointBlockOutlineRadius = 3;
    public float[] orderedWaypointBlockOutlineColor = {1f, 1f, 1f};
    public float orderedWaypointBlockOutlineAlpha = 0.8f;

    // Update checker - remember dismissed version
    public String dismissedUpdateVersion = "";

    // Coal value calculator settings
    public String coalValueSellMethod = "SELLOFFER"; // INSTASELL or SELLOFFER
    public String coalValueSulphurBuy = "BUY_ORDER"; // BUY_ORDER or INSTA_BUY
    public String coalValueCrudeBuy = "BUY_ORDER";
    public String coalValueFuelBuy = "BUY_ORDER";
    public String coalValueHeavyBuy = "BUY_ORDER";
    public boolean coalValueShowSettings = true; // Show settings first time, then go to results

    // Comm Claim settings
    public int commClaimBatPersonSlot = 1; // 1-9, wardrobe slot
    public int commClaimDivanSlot = 2; // 1-9, wardrobe slot
    public int commClaimRefinedToolSlot = 0; // 0-8, hotbar slot
    public int commClaimTickDelay = 2; // 1-10 ticks
    public int commClaimGuiWaitDelay = 3; // 1-10 ticks
    public boolean commClaimAutoTrigger = false; // Auto-trigger on commission complete message
    public boolean commClaimWardrobeSwap = true; // Enable wardrobe armor swapping

    public static MiningConfig load() {
        if (!CONFIG_FILE.exists()) {
            LOGGER.info("[MiningConfig] Config file not found, creating default");
            MiningConfig config = new MiningConfig();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            MiningConfig config = GSON.fromJson(reader, MiningConfig.class);
            LOGGER.info("[MiningConfig] Config loaded successfully");
            return config;
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to load config: " + e.getMessage());
            return new MiningConfig();
        }
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
                LOGGER.info("[MiningConfig] Config saved successfully");
            }
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to save config: " + e.getMessage());
        }
    }

    public void applyToGame() {
        if (lapisEnabled != CorpseESP.isLapisEnabled()) {
            CorpseESP.toggleLapis();
        }
        if (tungstenEnabled != CorpseESP.isTungstenEnabled()) {
            CorpseESP.toggleTungsten();
        }
        if (umberEnabled != CorpseESP.isUmberEnabled()) {
            CorpseESP.toggleUmber();
        }
        if (vanguardEnabled != CorpseESP.isVanguardEnabled()) {
            CorpseESP.toggleVanguard();
        }

        ProfitTrackerHUD.setEnabled(profitTrackerEnabled);
        ProfitTrackerHUD.setPosition(profitTrackerX, profitTrackerY);
        ProfitTrackerHUD.setScale(profitTrackerScale);
        ProfitTrackerHUD.setMode(profitTrackerMode);
        GemstoneTracker.setPristineChance(pristineChance);
        GemstoneTracker.setIncludeRough(includeRough);
        GemstoneTracker.setGemTier(gemTier);
        BazaarPriceManager.setUseNPCPrices(useNPCPrices);
        try {
            BlockTracker.setDisplayMode(BlockTracker.DisplayMode.valueOf(blockTrackerDisplayMode));
        } catch (Exception e) {
            BlockTracker.setDisplayMode(BlockTracker.DisplayMode.SEPARATE);
        }

        EfficientMinerOverlay.setEnabled(efficientMinerEnabled);
        EfficientMinerOverlay.setUseOldHeatmap(useOldHeatmap);

        PickaxeCooldownHUD.setEnabled(pickaxeCooldownEnabled);
        PickaxeCooldownHUD.setPosition(pickaxeCooldownX, pickaxeCooldownY);
        PickaxeCooldownHUD.setScale(pickaxeCooldownScale);
        PickaxeCooldownHUD.setTitleEnabled(pickaxeCooldownTitleEnabled);
        PickaxeCooldownHUD.setTitleThreshold(pickaxeCooldownTitleThreshold);

        BlockOutlineRenderer.setEnabled(blockOutlineEnabled);
        try {
            BlockOutlineRenderer.setMode(BlockOutlineRenderer.OutlineMode.valueOf(blockOutlineMode));
        } catch (Exception e) {
            BlockOutlineRenderer.setMode(BlockOutlineRenderer.OutlineMode.BOTH);
        }
        BlockOutlineRenderer.setColor(blockOutlineRed, blockOutlineGreen, blockOutlineBlue, blockOutlineAlpha);

        NameHider.setEnabled(nameHiderEnabled);
        NameHider.setReplacementName(replacementName);
        NameHider.setUseGradient(useGradient);
        NameHider.setColor1(nameColorRed1, nameColorGreen1, nameColorBlue1);
        NameHider.setColor2(nameColorRed2, nameColorGreen2, nameColorBlue2);

        AutoClickerManager.setEnabled(autoClickerEnabled);
        AutoClickerManager.setMiningSlot(autoClickerMiningSlot);
        AutoClickerManager.setEnableRodSwap(autoClickerRodSwap);
        AutoClickerManager.setEnableSecondDrill(autoClickerSecondDrill);
        AutoClickerManager.setSecondDrillSlot(autoClickerSecondDrillSlot);
        AutoClickerHUD.setEnabled(autoClickerHudEnabled);

        forfun.miningqol.client.CommandKeybindManager.clearAll();
        for (Map.Entry<String, String> entry : commandKeybinds.entrySet()) {
            try {
                int keyCode = Integer.parseInt(entry.getKey());
                forfun.miningqol.client.CommandKeybindManager.registerKeybind(keyCode, entry.getValue());
            } catch (NumberFormatException ignored) {}
        }

        // Load lobby finder blocks
        java.util.Set<net.minecraft.util.math.BlockPos> blocks = new java.util.HashSet<>();
        for (String posStr : lobbyFinderBlocks) {
            try {
                String[] parts = posStr.split(",");
                if (parts.length == 3) {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    blocks.add(new net.minecraft.util.math.BlockPos(x, y, z));
                }
            } catch (NumberFormatException ignored) {}
        }
        forfun.miningqol.client.LobbyFinder.setTrackedBlocks(blocks);

        GlassSync.setEnabled(glassSyncEnabled);

        // Ordered Waypoints
        OrderedWaypointManager.setEnabled(orderedWaypointsEnabled);
        OrderedWaypointManager.setWaypointRange(orderedWaypointRange);
        OrderedWaypointManager.setCurrentWaypointColor(orderedWaypointCurrentColor[0], orderedWaypointCurrentColor[1], orderedWaypointCurrentColor[2]);
        OrderedWaypointManager.setNextWaypointColor(orderedWaypointNextColor[0], orderedWaypointNextColor[1], orderedWaypointNextColor[2]);
        OrderedWaypointManager.setPreviousWaypointColor(orderedWaypointPreviousColor[0], orderedWaypointPreviousColor[1], orderedWaypointPreviousColor[2]);
        OrderedWaypointManager.setCurrentWaypointAlpha(orderedWaypointCurrentAlpha);
        OrderedWaypointManager.setNextWaypointAlpha(orderedWaypointNextAlpha);
        OrderedWaypointManager.setPreviousWaypointAlpha(orderedWaypointPreviousAlpha);
        OrderedWaypointManager.setNextCount(orderedWaypointNextCount);
        OrderedWaypointManager.setTraceLineEnabled(orderedWaypointTraceLine);
        OrderedWaypointManager.setTraceLineColor(orderedWaypointTraceLineColor[0], orderedWaypointTraceLineColor[1], orderedWaypointTraceLineColor[2]);
        OrderedWaypointManager.setTraceLineAlpha(orderedWaypointTraceLineAlpha);
        OrderedWaypointManager.setShowDistance(orderedWaypointShowDistance);
        OrderedWaypointManager.setShowName(orderedWaypointShowName);
        OrderedWaypointManager.setLobbyCheckEnabled(orderedWaypointLobbyCheckEnabled);
        OrderedWaypointManager.setLobbyCheckBlock(orderedWaypointLobbyCheckBlock);
        OrderedWaypointManager.setLobbyCheckInterval(orderedWaypointLobbyCheckInterval);
        OrderedWaypointManager.setLobbyCheckRadius(orderedWaypointLobbyCheckRadius);
        OrderedWaypointManager.setBlockOutlineAroundWaypoint(orderedWaypointBlockOutline);
        OrderedWaypointManager.setBlockOutlineRadius(orderedWaypointBlockOutlineRadius);
        OrderedWaypointManager.setBlockOutlineColor(orderedWaypointBlockOutlineColor[0], orderedWaypointBlockOutlineColor[1], orderedWaypointBlockOutlineColor[2]);
        OrderedWaypointManager.setBlockOutlineAlpha(orderedWaypointBlockOutlineAlpha);

        // Comm Claim
        CommClaimManager.setBatPersonSlot(commClaimBatPersonSlot);
        CommClaimManager.setDivanSlot(commClaimDivanSlot);
        CommClaimManager.setRefinedToolSlot(commClaimRefinedToolSlot);
        CommClaimManager.setTickDelay(commClaimTickDelay);
        CommClaimManager.setGuiWaitDelay(commClaimGuiWaitDelay);
        CommClaimManager.setAutoTrigger(commClaimAutoTrigger);
        CommClaimManager.setWardrobeSwap(commClaimWardrobeSwap);
    }

    public void loadFromGame() {
        lapisEnabled = CorpseESP.isLapisEnabled();
        tungstenEnabled = CorpseESP.isTungstenEnabled();
        umberEnabled = CorpseESP.isUmberEnabled();
        vanguardEnabled = CorpseESP.isVanguardEnabled();

        profitTrackerEnabled = ProfitTrackerHUD.isEnabled();
        profitTrackerX = ProfitTrackerHUD.getX();
        profitTrackerY = ProfitTrackerHUD.getY();
        profitTrackerScale = ProfitTrackerHUD.getScale();
        profitTrackerMode = ProfitTrackerHUD.getMode();
        pristineChance = GemstoneTracker.getPristineChance();
        includeRough = GemstoneTracker.isIncludingRough();
        gemTier = GemstoneTracker.getGemTier();
        useNPCPrices = BazaarPriceManager.isUsingNPCPrices();
        blockTrackerDisplayMode = BlockTracker.getDisplayMode().name();

        efficientMinerEnabled = EfficientMinerOverlay.isEnabled();
        useOldHeatmap = EfficientMinerOverlay.isUsingOldHeatmap();

        pickaxeCooldownEnabled = PickaxeCooldownHUD.isEnabled();
        pickaxeCooldownX = PickaxeCooldownHUD.getX();
        pickaxeCooldownY = PickaxeCooldownHUD.getY();
        pickaxeCooldownScale = PickaxeCooldownHUD.getScale();
        pickaxeCooldownTitleEnabled = PickaxeCooldownHUD.isTitleEnabled();
        pickaxeCooldownTitleThreshold = PickaxeCooldownHUD.getTitleThreshold();

        blockOutlineEnabled = BlockOutlineRenderer.isEnabled();
        blockOutlineMode = BlockOutlineRenderer.getMode().name();
        blockOutlineRed = BlockOutlineRenderer.getRed();
        blockOutlineGreen = BlockOutlineRenderer.getGreen();
        blockOutlineBlue = BlockOutlineRenderer.getBlue();
        blockOutlineAlpha = BlockOutlineRenderer.getAlpha();

        nameHiderEnabled = NameHider.isEnabled();
        replacementName = NameHider.getReplacementName();
        useGradient = NameHider.isUsingGradient();
        nameColorRed1 = NameHider.getRed1();
        nameColorGreen1 = NameHider.getGreen1();
        nameColorBlue1 = NameHider.getBlue1();
        nameColorRed2 = NameHider.getRed2();
        nameColorGreen2 = NameHider.getGreen2();
        nameColorBlue2 = NameHider.getBlue2();

        autoClickerEnabled = AutoClickerManager.isEnabled();
        autoClickerMiningSlot = AutoClickerManager.getMiningSlot();
        autoClickerRodSwap = AutoClickerManager.isRodSwapEnabled();
        autoClickerSecondDrill = AutoClickerManager.isSecondDrillEnabled();
        autoClickerSecondDrillSlot = AutoClickerManager.getSecondDrillSlot();
        autoClickerHudEnabled = AutoClickerHUD.isEnabled();

        commandKeybinds.clear();
        for (Map.Entry<Integer, String> entry : forfun.miningqol.client.CommandKeybindManager.getAllKeybinds().entrySet()) {
            commandKeybinds.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        // Save lobby finder blocks
        lobbyFinderBlocks.clear();
        for (net.minecraft.util.math.BlockPos pos : forfun.miningqol.client.LobbyFinder.getTrackedBlocks()) {
            lobbyFinderBlocks.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }

        glassSyncEnabled = GlassSync.isEnabled();

        // Ordered Waypoints
        orderedWaypointsEnabled = OrderedWaypointManager.isEnabledRaw();
        orderedWaypointRange = OrderedWaypointManager.getWaypointRange();
        orderedWaypointCurrentColor = OrderedWaypointManager.getCurrentWaypointColor();
        orderedWaypointNextColor = OrderedWaypointManager.getNextWaypointColor();
        orderedWaypointPreviousColor = OrderedWaypointManager.getPreviousWaypointColor();
        orderedWaypointCurrentAlpha = OrderedWaypointManager.getCurrentWaypointAlpha();
        orderedWaypointNextAlpha = OrderedWaypointManager.getNextWaypointAlpha();
        orderedWaypointPreviousAlpha = OrderedWaypointManager.getPreviousWaypointAlpha();
        orderedWaypointNextCount = OrderedWaypointManager.getNextCount();
        orderedWaypointTraceLine = OrderedWaypointManager.isTraceLineEnabled();
        orderedWaypointTraceLineColor = OrderedWaypointManager.getTraceLineColor();
        orderedWaypointTraceLineAlpha = OrderedWaypointManager.getTraceLineAlpha();
        orderedWaypointShowDistance = OrderedWaypointManager.isShowDistance();
        orderedWaypointShowName = OrderedWaypointManager.isShowName();
        orderedWaypointLobbyCheckEnabled = OrderedWaypointManager.isLobbyCheckEnabled();
        orderedWaypointLobbyCheckBlock = OrderedWaypointManager.getLobbyCheckBlock();
        orderedWaypointLobbyCheckInterval = OrderedWaypointManager.getLobbyCheckInterval();
        orderedWaypointLobbyCheckRadius = OrderedWaypointManager.getLobbyCheckRadius();
        orderedWaypointBlockOutline = OrderedWaypointManager.isBlockOutlineAroundWaypoint();
        orderedWaypointBlockOutlineRadius = OrderedWaypointManager.getBlockOutlineRadius();
        orderedWaypointBlockOutlineColor = OrderedWaypointManager.getBlockOutlineColor();
        orderedWaypointBlockOutlineAlpha = OrderedWaypointManager.getBlockOutlineAlpha();

        // Comm Claim
        commClaimBatPersonSlot = CommClaimManager.getBatPersonSlot();
        commClaimDivanSlot = CommClaimManager.getDivanSlot();
        commClaimRefinedToolSlot = CommClaimManager.getRefinedToolSlot();
        commClaimTickDelay = CommClaimManager.getTickDelay();
        commClaimGuiWaitDelay = CommClaimManager.getGuiWaitDelay();
        commClaimAutoTrigger = CommClaimManager.isAutoTrigger();
        commClaimWardrobeSwap = CommClaimManager.isWardrobeSwap();
    }
}
