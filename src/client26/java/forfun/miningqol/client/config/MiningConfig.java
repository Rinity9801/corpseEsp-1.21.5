package forfun.miningqol.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forfun.miningqol.client.ColdTracker;
import forfun.miningqol.client.CommandKeybindManager;
import forfun.miningqol.client.CommissionHUD;
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.FiletWarning;
import forfun.miningqol.client.LobbyFinder;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.ShaftESP;
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

    public boolean lapisEnabled = true;
    public boolean tungstenEnabled = true;
    public boolean umberEnabled = true;
    public boolean vanguardEnabled = true;

    public boolean shaftESPEnabled = true;
    public boolean shaftESPMobsEnabled = false;
    public float[] shaftESPMobColor = {1.0f, 0.2f, 0.2f};
    public float shaftESPMobAlpha = 0.2f;

    public boolean pickaxeCooldownEnabled = true;
    public int pickaxeCooldownX = 10;
    public int pickaxeCooldownY = 50;
    public float pickaxeCooldownScale = 1.0f;
    public boolean pickaxeCooldownTitleEnabled = true;
    public int pickaxeCooldownTitleThreshold = 5;

    public boolean filetWarningEnabled = false;

    public java.util.List<String> lobbyFinderBlocks = new java.util.ArrayList<>();
    public java.util.Map<String, String> commandKeybinds = new java.util.HashMap<>();

    // Cheat-only fields (plain data; applied via CheatHooks on -cheat builds,
    // harmlessly ignored on legit)
    public int autoClickerMiningSlot = 0;
    public boolean autoClickerRodSwap = true;
    public boolean autoClickerSecondDrill = false;
    public int autoClickerSecondDrillSlot = 3;
    public boolean autoClickerHudEnabled = true;
    public int autoClickerMainDrillDelay = 3;
    public int autoClickerSecondDrillDelay = 3;
    public int coldClickerMiningSlot = 0;
    public boolean coldClickerRodSwap = true;
    public int coldClickerSecondDrillSlot = 3;
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
    public String emptyStashMaterial = "COAL";
    public int emptyStashDelay = 4;

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
        if (commissionHudLayout == null) {
            commissionHudLayout = "GRID";
        }
        if (orderedWaypointCurrentColor == null) orderedWaypointCurrentColor = new float[]{85f/255f, 1f, 85f/255f};
        if (orderedWaypointNextColor == null) orderedWaypointNextColor = new float[]{1f, 1f, 85f/255f};
        if (orderedWaypointPreviousColor == null) orderedWaypointPreviousColor = new float[]{85f/255f, 85f/255f, 1f};
        if (orderedWaypointTraceLineColor == null) orderedWaypointTraceLineColor = new float[]{85f/255f, 1f, 85f/255f};
        if (shaftESPMobColor == null) shaftESPMobColor = new float[]{1.0f, 0.2f, 0.2f};
        if (orderedWaypointBlockOutlineColor == null) orderedWaypointBlockOutlineColor = new float[]{1f, 1f, 1f};
        if (emptyStashMaterial == null) emptyStashMaterial = "COAL";
        if (orderedWaypointLobbyCheckBlock == null) orderedWaypointLobbyCheckBlock = "minecraft:coal_ore";
        if (lobbyFinderBlocks == null) lobbyFinderBlocks = new java.util.ArrayList<>();
        if (commandKeybinds == null) commandKeybinds = new java.util.HashMap<>();
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
        CommissionHUD.setEnabled(commissionHudEnabled);
        CommissionHUD.setPosition(commissionHudX, commissionHudY);
        CommissionHUD.setScale(commissionHudScale);
        CommissionHUD.setBackgroundEnabled(commissionHudBackground);
        try {
            CommissionHUD.setLayoutMode(CommissionHUD.LayoutMode.valueOf(commissionHudLayout));
        } catch (Exception e) {
            CommissionHUD.setLayoutMode(CommissionHUD.LayoutMode.GRID);
        }

        if (CorpseESP.isLapisEnabled() != lapisEnabled) CorpseESP.toggleLapis();
        if (CorpseESP.isTungstenEnabled() != tungstenEnabled) CorpseESP.toggleTungsten();
        if (CorpseESP.isUmberEnabled() != umberEnabled) CorpseESP.toggleUmber();
        if (CorpseESP.isVanguardEnabled() != vanguardEnabled) CorpseESP.toggleVanguard();

        ShaftESP.setLittlefootEnabled(shaftESPEnabled);
        ShaftESP.setMobsEnabled(shaftESPMobsEnabled);
        ShaftESP.setMobColor(shaftESPMobColor[0], shaftESPMobColor[1], shaftESPMobColor[2]);
        ShaftESP.setMobAlpha(shaftESPMobAlpha);

        PickaxeCooldownHUD.setEnabled(pickaxeCooldownEnabled);
        PickaxeCooldownHUD.setPosition(pickaxeCooldownX, pickaxeCooldownY);
        PickaxeCooldownHUD.setScale(pickaxeCooldownScale);
        PickaxeCooldownHUD.setTitleEnabled(pickaxeCooldownTitleEnabled);
        PickaxeCooldownHUD.setTitleThreshold(pickaxeCooldownTitleThreshold);

        FiletWarning.setEnabled(filetWarningEnabled);

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
    }

    public void loadFromGame() {
        commissionHudEnabled = CommissionHUD.isEnabled();
        commissionHudX = CommissionHUD.getX();
        commissionHudY = CommissionHUD.getY();
        commissionHudScale = CommissionHUD.getScale();
        commissionHudBackground = CommissionHUD.isBackgroundEnabled();
        commissionHudLayout = CommissionHUD.getLayoutMode().name();

        lapisEnabled = CorpseESP.isLapisEnabled();
        tungstenEnabled = CorpseESP.isTungstenEnabled();
        umberEnabled = CorpseESP.isUmberEnabled();
        vanguardEnabled = CorpseESP.isVanguardEnabled();

        shaftESPEnabled = ShaftESP.isLittlefootEnabled();
        shaftESPMobsEnabled = ShaftESP.isMobsEnabled();
        shaftESPMobColor = ShaftESP.getMobColor();
        shaftESPMobAlpha = ShaftESP.getMobAlpha();

        pickaxeCooldownEnabled = PickaxeCooldownHUD.isEnabled();
        pickaxeCooldownX = PickaxeCooldownHUD.getX();
        pickaxeCooldownY = PickaxeCooldownHUD.getY();
        pickaxeCooldownScale = PickaxeCooldownHUD.getScale();
        pickaxeCooldownTitleEnabled = PickaxeCooldownHUD.isTitleEnabled();
        pickaxeCooldownTitleThreshold = PickaxeCooldownHUD.getTitleThreshold();

        filetWarningEnabled = FiletWarning.isEnabled();

        commandKeybinds.clear();
        for (java.util.Map.Entry<Integer, String> entry : CommandKeybindManager.getAllKeybinds().entrySet()) {
            commandKeybinds.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        lobbyFinderBlocks.clear();
        for (net.minecraft.core.BlockPos pos : LobbyFinder.getTrackedBlocks()) {
            lobbyFinderBlocks.add(pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }

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
    }
}
