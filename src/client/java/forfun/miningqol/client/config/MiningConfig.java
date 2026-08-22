package forfun.miningqol.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//? if isCheat {
import forfun.miningqol.client.AutoClickerHUD;
import forfun.miningqol.client.AutoClickerManager;
import forfun.miningqol.client.InShaftClickManager;
//?}
import forfun.miningqol.client.BlockOutlineRenderer;
//? if isCheat {
import forfun.miningqol.client.CommissionHUD;
import forfun.miningqol.client.CommClaimManager;
//?}
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.EfficientMinerOverlay;
import forfun.miningqol.client.FiletWarning;
import forfun.miningqol.client.GlassSync;
//? if isCheat {
import forfun.miningqol.client.ShaftClickerManager;
//?}
import forfun.miningqol.client.ShaftESP;
import forfun.miningqol.client.NameHider;
import forfun.miningqol.client.PetFlipTooltip;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.RollingMinerCooldown;
import forfun.miningqol.client.profit.BazaarPriceManager;
import forfun.miningqol.client.profit.BlockTracker;
import forfun.miningqol.client.profit.GemstoneTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import forfun.miningqol.client.collection.CollectionTracker;
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
    public int collectionHudX = 10;
    public int collectionHudY = 120;
    public float collectionHudScale = 1.0f;
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
    public boolean autoClickerSecondDrill = false;
    public int autoClickerSecondDrillSlot = 3;
    public boolean autoClickerHudEnabled = true;
    public int autoClickerMainDrillDelay = 3;
    public int autoClickerSecondDrillDelay = 3;

    public int invClickDelay = 3;

    public Map<String, String> commandKeybinds = new HashMap<>();

    // Shaft ESP
    public boolean shaftESPEnabled = true;
    public boolean shaftESPMobsEnabled = false;

    // Cold Clicker
    public boolean coldClickerEnabled = false;
    public int coldClickerMiningSlot = 0;
    public int coldClickerSecondDrillSlot = 3;
    public boolean coldClickerThirdDrillEnabled = false;
    public int coldClickerThirdDrillSlot = 4;
    public int coldClickerMainDrillDelay = 3;
    public int coldClickerSecondDrillDelay = 3;
    public int coldClickerColdThreshold = 50;
    public boolean coldClickerShowToggleMessage = true;

    // Shaft Clicker
    public boolean shaftClickerEnabled = false;
    public int shaftClickerMiningSlot = 0;
    public boolean shaftClickerShowToggleMessage = true;

    public boolean autoSkipShoLoad = false;

    public boolean rollingMinerCooldownEnabled = false;

    public boolean filetWarningEnabled = false;

    public boolean petFlipTooltipEnabled = true;

    public boolean glassSyncEnabled = false;

    public java.util.List<String> lobbyFinderBlocks = new java.util.ArrayList<>();

    // Radial menu: fixed 8 slots, each runs a command
    public static class RadialEntry {
        public String label = "";
        public String command = "";
    }
    public java.util.List<RadialEntry> radialEntries = new java.util.ArrayList<>();

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
    public boolean commClaimBatchMining = true; // true: wait for all mining done; false: claim each
    public boolean commClaimBlockInput = true;  // swallow player clicks/keys while claiming
    public boolean commClaimHideGui = false;    // hide the container GUI while claiming
    public boolean commissionHudEnabled = true;
    public int commissionHudX = 10;
    public int commissionHudY = 90;
    public float commissionHudScale = 1.0f;
    public boolean commissionHudBackground = true;
    public String commissionHudLayout = "GRID";

    public static MiningConfig load() {
        if (!CONFIG_FILE.exists()) {
            LOGGER.info("[MiningConfig] Config file not found, creating default");
            MiningConfig config = new MiningConfig();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            MiningConfig config = GSON.fromJson(reader, MiningConfig.class);
            if (config == null) {
                LOGGER.warn("[MiningConfig] Config deserialized as null, using defaults");
                config = new MiningConfig();
            }
            config.ensureDefaults();
            LOGGER.info("[MiningConfig] Config loaded successfully");
            return config;
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to load config, attempting recovery: " + e.getMessage());
            // Try to recover by loading into a JsonObject and applying manually
            try {
                return recoverConfig();
            } catch (Exception e2) {
                LOGGER.error("[MiningConfig] Recovery failed, using defaults: " + e2.getMessage());
                return new MiningConfig();
            }
        }
    }

    /**
     * Ensures all collection fields are non-null after Gson deserialization.
     * Gson can set fields to null if the JSON value is null or on type mismatch.
     */
    private void ensureDefaults() {
        if (commandKeybinds == null) commandKeybinds = new HashMap<>();
        if (lobbyFinderBlocks == null) lobbyFinderBlocks = new java.util.ArrayList<>();
        if (radialEntries == null) radialEntries = new java.util.ArrayList<>();
        while (radialEntries.size() < 8) radialEntries.add(new RadialEntry());
        for (RadialEntry e : radialEntries) {
            if (e.label == null) e.label = "";
            if (e.command == null) e.command = "";
        }
        if (orderedWaypointCurrentColor == null) orderedWaypointCurrentColor = new float[]{85f/255f, 1f, 85f/255f};
        if (orderedWaypointNextColor == null) orderedWaypointNextColor = new float[]{1f, 1f, 85f/255f};
        if (orderedWaypointPreviousColor == null) orderedWaypointPreviousColor = new float[]{85f/255f, 85f/255f, 1f};
        if (orderedWaypointTraceLineColor == null) orderedWaypointTraceLineColor = new float[]{85f/255f, 1f, 85f/255f};
        if (orderedWaypointBlockOutlineColor == null) orderedWaypointBlockOutlineColor = new float[]{1f, 1f, 1f};
        if (profitTrackerMode == null) profitTrackerMode = "GEMSTONES";
        if (blockTrackerDisplayMode == null) blockTrackerDisplayMode = "SEPARATE";
        if (blockOutlineMode == null) blockOutlineMode = "BOTH";
        if (replacementName == null) replacementName = "Player";
        if (coalValueSellMethod == null) coalValueSellMethod = "SELLOFFER";
        if (coalValueSulphurBuy == null) coalValueSulphurBuy = "BUY_ORDER";
        if (coalValueCrudeBuy == null) coalValueCrudeBuy = "BUY_ORDER";
        if (coalValueFuelBuy == null) coalValueFuelBuy = "BUY_ORDER";
        if (coalValueHeavyBuy == null) coalValueHeavyBuy = "BUY_ORDER";
        if (commissionHudLayout == null) commissionHudLayout = "GRID";
    }

    /**
     * Attempts to recover config by parsing JSON field-by-field,
     * so one bad field doesn't nuke the entire config.
     */
    private static MiningConfig recoverConfig() throws Exception {
        MiningConfig config = new MiningConfig();
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

            for (java.lang.reflect.Field field : MiningConfig.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                if (!json.has(field.getName())) continue;

                try {
                    Object value = GSON.fromJson(json.get(field.getName()), field.getGenericType());
                    if (value != null) {
                        field.setAccessible(true);
                        field.set(config, value);
                    }
                } catch (Exception e) {
                    LOGGER.warn("[MiningConfig] Could not recover field '{}': {}", field.getName(), e.getMessage());
                }
            }
        }
        config.ensureDefaults();
        LOGGER.info("[MiningConfig] Config recovered successfully");
        return config;
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
        CollectionTracker.setPosition(collectionHudX, collectionHudY);
        CollectionTracker.setScale(collectionHudScale);
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

        //? if isCheat {
        // Never restore enabled state for clickers - always start disabled for safety
        AutoClickerManager.setMiningSlot(autoClickerMiningSlot);
        AutoClickerManager.setEnableSecondDrill(autoClickerSecondDrill);
        AutoClickerManager.setSecondDrillSlot(autoClickerSecondDrillSlot);
        AutoClickerManager.setMainDrillDelay(autoClickerMainDrillDelay);
        AutoClickerManager.setSecondDrillDelay(autoClickerSecondDrillDelay);
        AutoClickerHUD.setEnabled(autoClickerHudEnabled);

        forfun.miningqol.client.InventoryClickManager.setClickDelay(invClickDelay);
        //?}

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

        ShaftESP.setLittlefootEnabled(shaftESPEnabled);
        ShaftESP.setMobsEnabled(shaftESPMobsEnabled);

        //? if isCheat {
        InShaftClickManager.setMiningSlot(coldClickerMiningSlot);
        InShaftClickManager.setSecondDrillSlot(coldClickerSecondDrillSlot);
        InShaftClickManager.setThirdDrillEnabled(coldClickerThirdDrillEnabled);
        InShaftClickManager.setThirdDrillSlot(coldClickerThirdDrillSlot);
        InShaftClickManager.setMainDrillDelay(coldClickerMainDrillDelay);
        InShaftClickManager.setSecondDrillDelay(coldClickerSecondDrillDelay);
        InShaftClickManager.setColdThreshold(coldClickerColdThreshold);
        InShaftClickManager.setShowToggleMessage(coldClickerShowToggleMessage);

        ShaftClickerManager.setMiningSlot(shaftClickerMiningSlot);
        ShaftClickerManager.setShowToggleMessage(shaftClickerShowToggleMessage);
        //?}

        FiletWarning.setEnabled(filetWarningEnabled);

        PetFlipTooltip.setEnabled(petFlipTooltipEnabled);

        GlassSync.setEnabled(glassSyncEnabled);

        RollingMinerCooldown.setEnabled(rollingMinerCooldownEnabled);

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

        //? if isCheat {
        // Comm Claim
        CommClaimManager.setBatPersonSlot(commClaimBatPersonSlot);
        CommClaimManager.setDivanSlot(commClaimDivanSlot);
        CommClaimManager.setRefinedToolSlot(commClaimRefinedToolSlot);
        CommClaimManager.setTickDelay(commClaimTickDelay);
        CommClaimManager.setGuiWaitDelay(commClaimGuiWaitDelay);
        CommClaimManager.setAutoTrigger(commClaimAutoTrigger);
        CommClaimManager.setWardrobeSwap(commClaimWardrobeSwap);
        CommClaimManager.setBatchMining(commClaimBatchMining);
        CommClaimManager.setBlockInput(commClaimBlockInput);
        CommClaimManager.setHideGui(commClaimHideGui);
        CommissionHUD.setEnabled(commissionHudEnabled);
        CommissionHUD.setPosition(commissionHudX, commissionHudY);
        CommissionHUD.setScale(commissionHudScale);
        CommissionHUD.setBackgroundEnabled(commissionHudBackground);
        try {
            CommissionHUD.setLayoutMode(CommissionHUD.LayoutMode.valueOf(commissionHudLayout));
        } catch (Exception e) {
            CommissionHUD.setLayoutMode(CommissionHUD.LayoutMode.GRID);
        }
        //?}
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
        collectionHudX = CollectionTracker.getX();
        collectionHudY = CollectionTracker.getY();
        collectionHudScale = CollectionTracker.getScale();
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

        //? if isCheat {
        autoClickerEnabled = AutoClickerManager.isEnabled();
        autoClickerMiningSlot = AutoClickerManager.getMiningSlot();
        autoClickerSecondDrill = AutoClickerManager.isSecondDrillEnabled();
        autoClickerSecondDrillSlot = AutoClickerManager.getSecondDrillSlot();
        autoClickerMainDrillDelay = AutoClickerManager.getMainDrillDelay();
        autoClickerSecondDrillDelay = AutoClickerManager.getSecondDrillDelay();
        autoClickerHudEnabled = AutoClickerHUD.isEnabled();

        invClickDelay = forfun.miningqol.client.InventoryClickManager.getClickDelay();
        //?}

        commandKeybinds.clear();
        for (Map.Entry<Integer, String> entry : forfun.miningqol.client.CommandKeybindManager.getAllKeybinds().entrySet()) {
            commandKeybinds.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        // Save lobby finder blocks
        lobbyFinderBlocks.clear();
        for (net.minecraft.util.math.BlockPos pos : forfun.miningqol.client.LobbyFinder.getTrackedBlocks()) {
            lobbyFinderBlocks.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }

        shaftESPEnabled = ShaftESP.isLittlefootEnabled();
        shaftESPMobsEnabled = ShaftESP.isMobsEnabled();

        //? if isCheat {
        coldClickerEnabled = InShaftClickManager.isEnabled();
        coldClickerMiningSlot = InShaftClickManager.getMiningSlot();
        coldClickerSecondDrillSlot = InShaftClickManager.getSecondDrillSlot();
        coldClickerThirdDrillEnabled = InShaftClickManager.isThirdDrillEnabled();
        coldClickerThirdDrillSlot = InShaftClickManager.getThirdDrillSlot();
        coldClickerMainDrillDelay = InShaftClickManager.getMainDrillDelay();
        coldClickerSecondDrillDelay = InShaftClickManager.getSecondDrillDelay();
        coldClickerColdThreshold = InShaftClickManager.getColdThreshold();
        coldClickerShowToggleMessage = InShaftClickManager.isShowToggleMessage();

        shaftClickerEnabled = ShaftClickerManager.isEnabled();
        shaftClickerMiningSlot = ShaftClickerManager.getMiningSlot();
        shaftClickerShowToggleMessage = ShaftClickerManager.isShowToggleMessage();
        //?}

        filetWarningEnabled = FiletWarning.isEnabled();

        petFlipTooltipEnabled = PetFlipTooltip.isEnabled();

        glassSyncEnabled = GlassSync.isEnabled();

        rollingMinerCooldownEnabled = RollingMinerCooldown.isEnabled();

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

        //? if isCheat {
        // Comm Claim
        commClaimBatPersonSlot = CommClaimManager.getBatPersonSlot();
        commClaimDivanSlot = CommClaimManager.getDivanSlot();
        commClaimRefinedToolSlot = CommClaimManager.getRefinedToolSlot();
        commClaimTickDelay = CommClaimManager.getTickDelay();
        commClaimGuiWaitDelay = CommClaimManager.getGuiWaitDelay();
        commClaimAutoTrigger = CommClaimManager.isAutoTrigger();
        commClaimWardrobeSwap = CommClaimManager.isWardrobeSwap();
        commClaimBatchMining = CommClaimManager.isBatchMining();
        commClaimBlockInput = CommClaimManager.isBlockInput();
        commClaimHideGui = CommClaimManager.isHideGui();
        commissionHudEnabled = CommissionHUD.isEnabled();
        commissionHudX = CommissionHUD.getX();
        commissionHudY = CommissionHUD.getY();
        commissionHudScale = CommissionHUD.getScale();
        commissionHudBackground = CommissionHUD.isBackgroundEnabled();
        commissionHudLayout = CommissionHUD.getLayoutMode().name();
        //?}
    }
}
