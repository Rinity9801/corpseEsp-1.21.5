package forfun.miningqol.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forfun.miningqol.client.BlockOverlay;
import forfun.miningqol.client.ColdTracker;
import forfun.miningqol.client.CommStatsHUD;
import forfun.miningqol.client.CommTracker;
import forfun.miningqol.client.CommandKeybindManager;
import forfun.miningqol.client.CommissionHUD;
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.CritParticleDrop;
import forfun.miningqol.client.EfficientMinerOverlay;
import forfun.miningqol.client.EntityEspMode;
import forfun.miningqol.client.FiletWarning;
import forfun.miningqol.client.LobbyFinder;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.MqoChat;
import forfun.miningqol.client.ShaftESP;
import forfun.miningqol.client.SoundBlocker;
import forfun.miningqol.client.waypoints.OrderedWaypointManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MiningConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/miningqol.json");

    public boolean commissionHudEnabled = true;
    public int commissionHudX = 10;
    public int commissionHudY = 90;
    public float commissionHudScale = 1.0f;
    public boolean commissionHudBackground = true;
    public String commissionHudLayout = "GRID";
    public boolean commissionStatsEnabled = true;
    public long commTrackTotal = 0;
    public int commStatsHudX = 10;
    public int commStatsHudY = 220;
    public float commStatsHudScale = 1.0f;

    public boolean lapisEnabled = true;
    public boolean tungstenEnabled = true;
    public boolean umberEnabled = true;
    public boolean vanguardEnabled = true;

    public boolean shaftESPEnabled = true;
    public boolean shaftESPLittlefootTracer = true;
    public boolean shaftESPMobsEnabled = false;
    public float[] shaftESPMobColor = {1.0f, 0.2f, 0.2f};
    public float shaftESPMobAlpha = 0.2f;
    public String shaftESPRenderMode = "BOX";
    public String corpseESPRenderMode = "BOX";

    public boolean blockOverlayEnabled = false;
    public String blockOverlayMode = "FILLED_OUTLINE";
    public float[] blockOverlayFillColor = {0.0f, 134.0f / 255.0f, 1.0f};
    public float blockOverlayFillAlpha = 50.0f / 255.0f;
    public float[] blockOverlayOutlineColor = {0.0f, 134.0f / 255.0f, 1.0f};
    public float blockOverlayOutlineAlpha = 1.0f;
    public float blockOverlayLineWidth = 2.5f;
    public boolean blockOverlayPhase = false;
    public boolean blockOverlayHideDuringEtherwarp = false;

    public boolean pickaxeCooldownEnabled = true;
    public int pickaxeCooldownX = 10;
    public int pickaxeCooldownY = 50;
    public float pickaxeCooldownScale = 1.0f;
    public boolean pickaxeCooldownTitleEnabled = true;
    public int pickaxeCooldownTitleThreshold = 5;
    public boolean pickaxeCooldownCustomEnabled = false;
    public int pickaxeCooldownCustomSeconds = 120;

    public boolean filetWarningEnabled = false;
    public boolean autoSkipShoLoad = false;

    public boolean efficientMinerEnabled = false;
    public boolean useOldHeatmap = false;

    public java.util.List<String> lobbyFinderBlocks = new java.util.ArrayList<>();
    public java.util.Map<String, String> commandKeybinds = new java.util.HashMap<>();

    public boolean chatLogsEnabled = true;
    public boolean critParticleDrop = false;

    public boolean soundBlockingEnabled = true;
    public java.util.List<String> soundBlockRules = new java.util.ArrayList<>();

    // Cheat-only fields (plain data; applied via CheatHooks on -cheat builds,
    // harmlessly ignored on legit)
    public int autoClickerMiningSlot = 0;
    public boolean autoClickerSecondDrill = false;
    public int autoClickerSecondDrillSlot = 3;
    public boolean autoClickerHudEnabled = true;
    public int autoClickerMainDrillDelay = 3;
    public int autoClickerSecondDrillDelay = 3;
    public int coldClickerMiningSlot = 0;
    public int coldClickerSecondDrillSlot = 3;
    public boolean coldClickerThirdDrillEnabled = false;
    public int coldClickerThirdDrillSlot = 4;
    public int coldClickerMainDrillDelay = 3;
    public int coldClickerSecondDrillDelay = 3;
    public int coldClickerColdThreshold = 50;
    public boolean coldClickerShowToggleMessage = true;
    public int shaftClickerMiningSlot = 0;
    public boolean shaftClickerShowToggleMessage = true;
    public int commClaimBatPersonSlot = 1;
    public int commClaimDivanSlot = 2;
    public int commClaimRefinedToolSlot = 0;
    public int commClaimTickDelay = 2;
    public int commClaimGuiWaitDelay = 3;
    public boolean commClaimAutoTrigger = false;
    public boolean commClaimWardrobeSwap = true;
    public boolean commClaimBatchMining = true;
    public boolean commClaimBlockInput = true;
    public boolean commClaimHideGui = false;
    public String emptyStashMaterial = "COAL";
    public int emptyStashDelay = 4;
    public boolean autoForgeEnabled = true;
    public int autoForgeTickDelay = 3;
    public int autoForgeRunCount = 1;
    public boolean shaftJoinCdEnabled = true;
    public int shaftJoinCdSeconds = 30;

    /** Whole settings-GUI opacity (0.3..1.0). */
    public float guiOpacity = 0.8f;

    public boolean orderedWaypointsEnabled = true;
    public float orderedWaypointRange = 4.5f;
    public int orderedWaypointNextCount = 2;
    public boolean orderedWaypointTraceLine = true;
    public boolean orderedWaypointShowDistance = true;
    public boolean orderedWaypointShowName = true;
    public float[] orderedWaypointCurrentColor = {85f/255f, 1f, 85f/255f};
    public float[] orderedWaypointNextColor = {1f, 1f, 85f/255f};
    public float[] orderedWaypointPreviousColor = {85f/255f, 85f/255f, 1f};
    public float[] orderedWaypointTraceLineColor = {85f/255f, 1f, 85f/255f};
    public float orderedWaypointCurrentAlpha = 0.6f;
    public float orderedWaypointNextAlpha = 0.6f;
    public float orderedWaypointPreviousAlpha = 0.6f;
    public float orderedWaypointTraceLineAlpha = 1f;
    public boolean orderedWaypointLobbyCheckEnabled = false;
    public String orderedWaypointLobbyCheckBlock = "minecraft:coal_ore";
    public int orderedWaypointLobbyCheckInterval = 10;
    public int orderedWaypointLobbyCheckRadius = 2;
    public boolean orderedWaypointBlockOutline = false;
    public int orderedWaypointBlockOutlineRadius = 3;
    public float[] orderedWaypointBlockOutlineColor = {1f, 1f, 1f};
    public float orderedWaypointBlockOutlineAlpha = 0.8f;
    /** Outline edge half-thickness in blocks. 1.5 matches how 1.21.11's GL lines looked. */
    public float orderedWaypointBlockOutlineThickness = 1.5f;
    /** Tint the outlined blocks as well as edging them. */
    public boolean orderedWaypointBlockOutlineFill = true;
    /** Cheat builds: auto right-click when sneaking + aiming at an etherwarp-marked waypoint. */
    public boolean orderedWaypointEtherwarpClick = true;
    /** /mqo skip walks past waypoints whose lobby-check block is gone. */
    public boolean orderedWaypointSkipObstructed = false;
    /** At or below this many blocks near a waypoint, /mqo skip treats it as mined out. */
    public int orderedWaypointObstructedThreshold = 5;

    public static MiningConfig load() {
        if (!CONFIG_FILE.exists()) {
            MiningConfig config = new MiningConfig();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            MiningConfig config = GSON.fromJson(reader, MiningConfig.class);
            if (config == null) {
                config = new MiningConfig();
            }
            config.ensureDefaults();
            return config;
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to load config", e);
            return new MiningConfig();
        }
    }

    private void ensureDefaults() {
        if (commissionHudScale < 0.5f || commissionHudScale > 2.0f) {
            commissionHudScale = 1.0f;
        }
        if (commStatsHudScale < 0.5f || commStatsHudScale > 2.0f) {
            commStatsHudScale = 1.0f;
        }
        if (commissionHudLayout == null) {
            commissionHudLayout = "GRID";
        }
        if (orderedWaypointCurrentColor == null) orderedWaypointCurrentColor = new float[]{85f/255f, 1f, 85f/255f};
        if (orderedWaypointNextColor == null) orderedWaypointNextColor = new float[]{1f, 1f, 85f/255f};
        if (orderedWaypointPreviousColor == null) orderedWaypointPreviousColor = new float[]{85f/255f, 85f/255f, 1f};
        if (orderedWaypointTraceLineColor == null) orderedWaypointTraceLineColor = new float[]{85f/255f, 1f, 85f/255f};
        if (shaftESPMobColor == null) shaftESPMobColor = new float[]{1.0f, 0.2f, 0.2f};
        if (shaftESPRenderMode == null) shaftESPRenderMode = "BOX";
        if (corpseESPRenderMode == null) corpseESPRenderMode = "BOX";
        if (blockOverlayMode == null) blockOverlayMode = "FILLED_OUTLINE";
        if (blockOverlayFillColor == null) blockOverlayFillColor = new float[]{0.0f, 134.0f / 255.0f, 1.0f};
        if (blockOverlayOutlineColor == null) blockOverlayOutlineColor = new float[]{0.0f, 134.0f / 255.0f, 1.0f};
        if (orderedWaypointBlockOutlineColor == null) orderedWaypointBlockOutlineColor = new float[]{1f, 1f, 1f};
        if (emptyStashMaterial == null) emptyStashMaterial = "COAL";
        if (orderedWaypointLobbyCheckBlock == null) orderedWaypointLobbyCheckBlock = "minecraft:coal_ore";
        if (lobbyFinderBlocks == null) lobbyFinderBlocks = new java.util.ArrayList<>();
        if (commandKeybinds == null) commandKeybinds = new java.util.HashMap<>();
        if (soundBlockRules == null) soundBlockRules = new java.util.ArrayList<>();
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[MiningConfig] Failed to save config", e);
        }
    }

    public void applyToGame() {
        forfun.miningqol.client.gui.SettingsUi.setGuiOpacity(guiOpacity);
        CommissionHUD.setEnabled(commissionHudEnabled);
        CommissionHUD.setPosition(commissionHudX, commissionHudY);
        CommissionHUD.setScale(commissionHudScale);
        CommissionHUD.setBackgroundEnabled(commissionHudBackground);
        try {
            CommissionHUD.setLayoutMode(CommissionHUD.LayoutMode.valueOf(commissionHudLayout));
        } catch (Exception e) {
            CommissionHUD.setLayoutMode(CommissionHUD.LayoutMode.GRID);
        }
        CommTracker.setStatsEnabled(commissionStatsEnabled);
        CommTracker.setTotalCompleted(commTrackTotal);
        CommStatsHUD.setPosition(commStatsHudX, commStatsHudY);
        CommStatsHUD.setScale(commStatsHudScale);

        if (CorpseESP.isLapisEnabled() != lapisEnabled) CorpseESP.toggleLapis();
        if (CorpseESP.isTungstenEnabled() != tungstenEnabled) CorpseESP.toggleTungsten();
        if (CorpseESP.isUmberEnabled() != umberEnabled) CorpseESP.toggleUmber();
        if (CorpseESP.isVanguardEnabled() != vanguardEnabled) CorpseESP.toggleVanguard();

        ShaftESP.setLittlefootEnabled(shaftESPEnabled);
        ShaftESP.setLittlefootTracer(shaftESPLittlefootTracer);
        ShaftESP.setMobsEnabled(shaftESPMobsEnabled);
        ShaftESP.setMobColor(shaftESPMobColor[0], shaftESPMobColor[1], shaftESPMobColor[2]);
        ShaftESP.setMobAlpha(shaftESPMobAlpha);
        try {
            ShaftESP.setRenderMode(EntityEspMode.valueOf(shaftESPRenderMode));
        } catch (IllegalArgumentException e) {
            ShaftESP.setRenderMode(EntityEspMode.BOX);
        }
        try {
            CorpseESP.setRenderMode(EntityEspMode.valueOf(corpseESPRenderMode));
        } catch (IllegalArgumentException e) {
            CorpseESP.setRenderMode(EntityEspMode.BOX);
        }

        BlockOverlay.setEnabled(blockOverlayEnabled);
        try {
            BlockOverlay.setMode(BlockOverlay.Mode.valueOf(blockOverlayMode));
        } catch (IllegalArgumentException e) {
            BlockOverlay.setMode(BlockOverlay.Mode.FILLED_OUTLINE);
        }
        BlockOverlay.setFillColor(blockOverlayFillColor[0], blockOverlayFillColor[1], blockOverlayFillColor[2]);
        BlockOverlay.setFillAlpha(blockOverlayFillAlpha);
        BlockOverlay.setOutlineColor(blockOverlayOutlineColor[0], blockOverlayOutlineColor[1], blockOverlayOutlineColor[2]);
        BlockOverlay.setOutlineAlpha(blockOverlayOutlineAlpha);
        BlockOverlay.setLineWidth(blockOverlayLineWidth);
        BlockOverlay.setPhase(blockOverlayPhase);
        BlockOverlay.setHideDuringEtherwarp(blockOverlayHideDuringEtherwarp);

        PickaxeCooldownHUD.setEnabled(pickaxeCooldownEnabled);
        PickaxeCooldownHUD.setPosition(pickaxeCooldownX, pickaxeCooldownY);
        PickaxeCooldownHUD.setScale(pickaxeCooldownScale);
        PickaxeCooldownHUD.setTitleEnabled(pickaxeCooldownTitleEnabled);
        PickaxeCooldownHUD.setTitleThreshold(pickaxeCooldownTitleThreshold);
        PickaxeCooldownHUD.setCustomCooldownSeconds(pickaxeCooldownCustomSeconds);
        PickaxeCooldownHUD.setCustomCooldownEnabled(pickaxeCooldownCustomEnabled);

        FiletWarning.setEnabled(filetWarningEnabled);

        EfficientMinerOverlay.setEnabled(efficientMinerEnabled);
        EfficientMinerOverlay.setUseOldHeatmap(useOldHeatmap);

        CommandKeybindManager.clearAll();
        for (java.util.Map.Entry<String, String> entry : commandKeybinds.entrySet()) {
            try {
                CommandKeybindManager.registerKeybind(Integer.parseInt(entry.getKey()), entry.getValue());
            } catch (NumberFormatException ignored) {}
        }

        java.util.Set<net.minecraft.core.BlockPos> blocks = new java.util.HashSet<>();
        for (String posStr : lobbyFinderBlocks) {
            try {
                String[] parts = posStr.split(",");
                if (parts.length == 3) {
                    blocks.add(new net.minecraft.core.BlockPos(
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                }
            } catch (NumberFormatException ignored) {}
        }
        LobbyFinder.setTrackedBlocks(blocks);

        MqoChat.setLogsEnabled(chatLogsEnabled);
        CritParticleDrop.setEnabled(critParticleDrop);

        SoundBlocker.setBlockingEnabled(soundBlockingEnabled);
        SoundBlocker.setRules(soundBlockRules);

        if (forfun.miningqol.client.CheatHooks.applyConfig != null) {
            forfun.miningqol.client.CheatHooks.applyConfig.run();
        }

        OrderedWaypointManager.setEnabled(orderedWaypointsEnabled);
        OrderedWaypointManager.setWaypointRange(orderedWaypointRange);
        OrderedWaypointManager.setNextCount(orderedWaypointNextCount);
        OrderedWaypointManager.setTraceLineEnabled(orderedWaypointTraceLine);
        OrderedWaypointManager.setShowDistance(orderedWaypointShowDistance);
        OrderedWaypointManager.setShowName(orderedWaypointShowName);
        OrderedWaypointManager.setCurrentWaypointColor(orderedWaypointCurrentColor[0], orderedWaypointCurrentColor[1], orderedWaypointCurrentColor[2]);
        OrderedWaypointManager.setNextWaypointColor(orderedWaypointNextColor[0], orderedWaypointNextColor[1], orderedWaypointNextColor[2]);
        OrderedWaypointManager.setPreviousWaypointColor(orderedWaypointPreviousColor[0], orderedWaypointPreviousColor[1], orderedWaypointPreviousColor[2]);
        OrderedWaypointManager.setTraceLineColor(orderedWaypointTraceLineColor[0], orderedWaypointTraceLineColor[1], orderedWaypointTraceLineColor[2]);
        OrderedWaypointManager.setCurrentWaypointAlpha(orderedWaypointCurrentAlpha);
        OrderedWaypointManager.setNextWaypointAlpha(orderedWaypointNextAlpha);
        OrderedWaypointManager.setPreviousWaypointAlpha(orderedWaypointPreviousAlpha);
        OrderedWaypointManager.setTraceLineAlpha(orderedWaypointTraceLineAlpha);
        OrderedWaypointManager.setLobbyCheckEnabled(orderedWaypointLobbyCheckEnabled);
        OrderedWaypointManager.setLobbyCheckBlock(orderedWaypointLobbyCheckBlock);
        OrderedWaypointManager.setLobbyCheckInterval(orderedWaypointLobbyCheckInterval);
        OrderedWaypointManager.setLobbyCheckRadius(orderedWaypointLobbyCheckRadius);
        OrderedWaypointManager.setBlockOutlineAroundWaypoint(orderedWaypointBlockOutline);
        OrderedWaypointManager.setBlockOutlineRadius(orderedWaypointBlockOutlineRadius);
        OrderedWaypointManager.setBlockOutlineColor(orderedWaypointBlockOutlineColor[0], orderedWaypointBlockOutlineColor[1], orderedWaypointBlockOutlineColor[2]);
        OrderedWaypointManager.setBlockOutlineAlpha(orderedWaypointBlockOutlineAlpha);
        OrderedWaypointManager.setBlockOutlineThickness(orderedWaypointBlockOutlineThickness);
        OrderedWaypointManager.setBlockOutlineFill(orderedWaypointBlockOutlineFill);
        OrderedWaypointManager.setSkipObstructed(orderedWaypointSkipObstructed);
        OrderedWaypointManager.setObstructedThreshold(orderedWaypointObstructedThreshold);
    }

    public void loadFromGame() {
        guiOpacity = forfun.miningqol.client.gui.SettingsUi.getGuiOpacity();
        commissionHudEnabled = CommissionHUD.isEnabled();
        commissionHudX = CommissionHUD.getX();
        commissionHudY = CommissionHUD.getY();
        commissionHudScale = CommissionHUD.getScale();
        commissionHudBackground = CommissionHUD.isBackgroundEnabled();
        commissionHudLayout = CommissionHUD.getLayoutMode().name();
        commissionStatsEnabled = CommTracker.isStatsEnabled();
        commTrackTotal = CommTracker.getTotalCompleted();
        commStatsHudX = CommStatsHUD.getX();
        commStatsHudY = CommStatsHUD.getY();
        commStatsHudScale = CommStatsHUD.getScale();

        lapisEnabled = CorpseESP.isLapisEnabled();
        tungstenEnabled = CorpseESP.isTungstenEnabled();
        umberEnabled = CorpseESP.isUmberEnabled();
        vanguardEnabled = CorpseESP.isVanguardEnabled();

        shaftESPEnabled = ShaftESP.isLittlefootEnabled();
        shaftESPLittlefootTracer = ShaftESP.isLittlefootTracer();
        shaftESPMobsEnabled = ShaftESP.isMobsEnabled();
        shaftESPMobColor = ShaftESP.getMobColor();
        shaftESPMobAlpha = ShaftESP.getMobAlpha();
        shaftESPRenderMode = ShaftESP.getRenderMode().name();
        corpseESPRenderMode = CorpseESP.getRenderMode().name();

        blockOverlayEnabled = BlockOverlay.isEnabled();
        blockOverlayMode = BlockOverlay.getMode().name();
        blockOverlayFillColor = BlockOverlay.getFillColor();
        blockOverlayFillAlpha = BlockOverlay.getFillAlpha();
        blockOverlayOutlineColor = BlockOverlay.getOutlineColor();
        blockOverlayOutlineAlpha = BlockOverlay.getOutlineAlpha();
        blockOverlayLineWidth = BlockOverlay.getLineWidth();
        blockOverlayPhase = BlockOverlay.isPhase();
        blockOverlayHideDuringEtherwarp = BlockOverlay.isHideDuringEtherwarp();

        pickaxeCooldownEnabled = PickaxeCooldownHUD.isEnabled();
        pickaxeCooldownX = PickaxeCooldownHUD.getX();
        pickaxeCooldownY = PickaxeCooldownHUD.getY();
        pickaxeCooldownScale = PickaxeCooldownHUD.getScale();
        pickaxeCooldownTitleEnabled = PickaxeCooldownHUD.isTitleEnabled();
        pickaxeCooldownTitleThreshold = PickaxeCooldownHUD.getTitleThreshold();
        pickaxeCooldownCustomEnabled = PickaxeCooldownHUD.isCustomCooldownEnabled();
        pickaxeCooldownCustomSeconds = PickaxeCooldownHUD.getCustomCooldownSeconds();

        filetWarningEnabled = FiletWarning.isEnabled();
        efficientMinerEnabled = EfficientMinerOverlay.isEnabled();
        useOldHeatmap = EfficientMinerOverlay.isUsingOldHeatmap();

        commandKeybinds.clear();
        for (java.util.Map.Entry<Integer, String> entry : CommandKeybindManager.getAllKeybinds().entrySet()) {
            commandKeybinds.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        lobbyFinderBlocks.clear();
        for (net.minecraft.core.BlockPos pos : LobbyFinder.getTrackedBlocks()) {
            lobbyFinderBlocks.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }

        chatLogsEnabled = MqoChat.isLogsEnabled();
        critParticleDrop = CritParticleDrop.isEnabled();

        soundBlockingEnabled = SoundBlocker.isBlockingEnabled();
        soundBlockRules = SoundBlocker.getRules();

        if (forfun.miningqol.client.CheatHooks.storeConfig != null) {
            forfun.miningqol.client.CheatHooks.storeConfig.run();
        }

        orderedWaypointsEnabled = OrderedWaypointManager.isEnabledRaw();
        orderedWaypointRange = OrderedWaypointManager.getWaypointRange();
        orderedWaypointNextCount = OrderedWaypointManager.getNextCount();
        orderedWaypointTraceLine = OrderedWaypointManager.isTraceLineEnabled();
        orderedWaypointShowDistance = OrderedWaypointManager.isShowDistance();
        orderedWaypointShowName = OrderedWaypointManager.isShowName();
        orderedWaypointCurrentColor = OrderedWaypointManager.getCurrentWaypointColor();
        orderedWaypointNextColor = OrderedWaypointManager.getNextWaypointColor();
        orderedWaypointPreviousColor = OrderedWaypointManager.getPreviousWaypointColor();
        orderedWaypointTraceLineColor = OrderedWaypointManager.getTraceLineColor();
        orderedWaypointCurrentAlpha = OrderedWaypointManager.getCurrentWaypointAlpha();
        orderedWaypointNextAlpha = OrderedWaypointManager.getNextWaypointAlpha();
        orderedWaypointPreviousAlpha = OrderedWaypointManager.getPreviousWaypointAlpha();
        orderedWaypointTraceLineAlpha = OrderedWaypointManager.getTraceLineAlpha();
        orderedWaypointLobbyCheckEnabled = OrderedWaypointManager.isLobbyCheckEnabled();
        orderedWaypointLobbyCheckBlock = OrderedWaypointManager.getLobbyCheckBlock();
        orderedWaypointLobbyCheckInterval = OrderedWaypointManager.getLobbyCheckInterval();
        orderedWaypointLobbyCheckRadius = OrderedWaypointManager.getLobbyCheckRadius();
        orderedWaypointBlockOutline = OrderedWaypointManager.isBlockOutlineAroundWaypoint();
        orderedWaypointBlockOutlineRadius = OrderedWaypointManager.getBlockOutlineRadius();
        orderedWaypointBlockOutlineColor = OrderedWaypointManager.getBlockOutlineColor();
        orderedWaypointBlockOutlineAlpha = OrderedWaypointManager.getBlockOutlineAlpha();
        orderedWaypointBlockOutlineThickness = OrderedWaypointManager.getBlockOutlineThickness();
        orderedWaypointBlockOutlineFill = OrderedWaypointManager.isBlockOutlineFill();
        orderedWaypointSkipObstructed = OrderedWaypointManager.isSkipObstructed();
        orderedWaypointObstructedThreshold = OrderedWaypointManager.getObstructedThreshold();
    }
}
