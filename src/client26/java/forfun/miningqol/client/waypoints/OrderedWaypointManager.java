package forfun.miningqol.client.waypoints;

import forfun.miningqol.client.MqoChat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 26.1.2 port of the ordered waypoints manager (src/client is Yarn-mapped and
 * doesn't compile against unobfuscated 26.x — keep the two in sync by hand).
 */
public class OrderedWaypointManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("OrderedWaypointManager");
    private static final File ROUTES_DIR = new File("config/miningqol_routes");

    private static List<OrderedWaypoint> currentRoute = new ArrayList<>();
    private static int currentIndex = 0;
    private static boolean enabled = true;

    // Config options
    private static float[] currentWaypointColor = {85f/255f, 1f, 85f/255f}; // Green
    private static float[] previousWaypointColor = {85f/255f, 85f/255f, 1f}; // Blue
    private static float[] nextWaypointColor = {1f, 1f, 85f/255f}; // Yellow
    private static float currentWaypointAlpha = 0.6f;
    private static float previousWaypointAlpha = 0.6f;
    private static float nextWaypointAlpha = 0.6f;

    private static int nextCount = 2;
    private static float waypointRange = 4.5f;

    private static boolean traceLine = true;
    private static float[] traceLineColor = {85f/255f, 1f, 85f/255f}; // Green
    private static float traceLineAlpha = 1f;

    private static boolean showDistance = true;
    private static boolean showName = true;

    private static boolean showAll = false;
    private static boolean editMode = false;
    private static float[] showAllWaypointColor = {0f, 1f, 0f}; // Green
    private static float showAllWaypointAlpha = 0.4f;
    private static float[] editModeLineColor = {1f, 0f, 0f}; // Red
    private static float editModeLineAlpha = 0.4f;

    // Lobby check (block scanner)
    private static boolean lobbyCheckEnabled = false;
    private static String lobbyCheckBlock = "minecraft:coal_ore";
    private static int lobbyCheckInterval = 10;
    private static int lobbyCheckRadius = 2;
    private static int waypointsReachedSinceLastCheck = 0;
    private static List<Integer> wrongWaypoints = new ArrayList<>();

    // Block outline around the next waypoint (highlights lobby-check blocks)
    private static boolean blockOutlineAroundWaypoint = false;
    private static int blockOutlineRadius = 3;
    private static float[] blockOutlineColor = {1f, 1f, 1f};
    private static float blockOutlineAlpha = 0.8f;

    // World change / teleport tracking
    private static Object lastWorld = null;
    private static BlockPos lastPlayerPos = null;
    private static final double TELEPORT_THRESHOLD = 100.0;

    public static void init() {
        ROUTES_DIR.mkdirs();
    }

    public static boolean isEnabled() {
        return enabled && !currentRoute.isEmpty();
    }

    public static boolean isEnabledRaw() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static float[] getCurrentWaypointColor() { return currentWaypointColor; }
    public static void setCurrentWaypointColor(float r, float g, float b) { currentWaypointColor = new float[]{r, g, b}; }
    public static float getCurrentWaypointAlpha() { return currentWaypointAlpha; }
    public static void setCurrentWaypointAlpha(float alpha) { currentWaypointAlpha = alpha; }
    public static float[] getPreviousWaypointColor() { return previousWaypointColor; }
    public static void setPreviousWaypointColor(float r, float g, float b) { previousWaypointColor = new float[]{r, g, b}; }
    public static float getPreviousWaypointAlpha() { return previousWaypointAlpha; }
    public static void setPreviousWaypointAlpha(float alpha) { previousWaypointAlpha = alpha; }
    public static float[] getNextWaypointColor() { return nextWaypointColor; }
    public static void setNextWaypointColor(float r, float g, float b) { nextWaypointColor = new float[]{r, g, b}; }
    public static float getNextWaypointAlpha() { return nextWaypointAlpha; }
    public static void setNextWaypointAlpha(float alpha) { nextWaypointAlpha = alpha; }
    public static int getNextCount() { return nextCount; }
    public static void setNextCount(int count) { nextCount = count; }
    public static float getWaypointRange() { return waypointRange; }
    public static void setWaypointRange(float range) { waypointRange = range; }
    public static boolean isTraceLineEnabled() { return traceLine; }
    public static void setTraceLineEnabled(boolean value) { traceLine = value; }
    public static float[] getTraceLineColor() { return traceLineColor; }
    public static void setTraceLineColor(float r, float g, float b) { traceLineColor = new float[]{r, g, b}; }
    public static float getTraceLineAlpha() { return traceLineAlpha; }
    public static void setTraceLineAlpha(float alpha) { traceLineAlpha = alpha; }
    public static boolean isShowDistance() { return showDistance; }
    public static void setShowDistance(boolean show) { showDistance = show; }
    public static boolean isShowName() { return showName; }
    public static void setShowName(boolean show) { showName = show; }
    public static boolean isShowAll() { return showAll; }
    public static void setShowAll(boolean show) { showAll = show; }
    public static float[] getShowAllWaypointColor() { return showAllWaypointColor; }
    public static float getShowAllWaypointAlpha() { return showAllWaypointAlpha; }
    public static float[] getEditModeLineColor() { return editModeLineColor; }
    public static float getEditModeLineAlpha() { return editModeLineAlpha; }

    // Edit mode (session-only): renders the entire route
    public static boolean isEditMode() { return editMode; }
    public static void setEditMode(boolean mode) { editMode = mode; }
    public static void toggleEditMode() {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route loaded.");
            return;
        }
        editMode = !editMode;
        sendMessage(editMode
            ? "\u00A7aEdit mode \u00A72enabled\u00A7a — showing the entire route (" + currentRoute.size() + " waypoints)."
            : "\u00A7aEdit mode \u00A7cdisabled\u00A7a.");
    }

    // Lobby check
    public static boolean isLobbyCheckEnabled() { return lobbyCheckEnabled; }
    public static void setLobbyCheckEnabled(boolean value) { lobbyCheckEnabled = value; }
    public static String getLobbyCheckBlock() { return lobbyCheckBlock; }
    public static void setLobbyCheckBlock(String block) { lobbyCheckBlock = block; }
    public static int getLobbyCheckInterval() { return lobbyCheckInterval; }
    public static void setLobbyCheckInterval(int interval) { lobbyCheckInterval = interval; }
    public static int getLobbyCheckRadius() { return lobbyCheckRadius; }
    public static void setLobbyCheckRadius(int radius) { lobbyCheckRadius = radius; }
    public static List<Integer> getWrongWaypoints() { return wrongWaypoints; }

    // Block outline around waypoint
    public static boolean isBlockOutlineAroundWaypoint() { return blockOutlineAroundWaypoint; }
    public static void setBlockOutlineAroundWaypoint(boolean value) { blockOutlineAroundWaypoint = value; }
    public static int getBlockOutlineRadius() { return blockOutlineRadius; }
    public static void setBlockOutlineRadius(int radius) { blockOutlineRadius = radius; }
    public static float[] getBlockOutlineColor() { return blockOutlineColor; }
    public static void setBlockOutlineColor(float r, float g, float b) { blockOutlineColor = new float[]{r, g, b}; }
    public static float getBlockOutlineAlpha() { return blockOutlineAlpha; }
    public static void setBlockOutlineAlpha(float alpha) { blockOutlineAlpha = alpha; }

    // Route methods
    public static List<OrderedWaypoint> getCurrentRoute() {
        return currentRoute;
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static OrderedWaypoint getCurrentWaypoint() {
        if (currentRoute.isEmpty() || currentIndex >= currentRoute.size()) return null;
        return currentRoute.get(currentIndex);
    }

    public static OrderedWaypoint getNextWaypoint() {
        if (currentRoute.isEmpty()) return null;
        int nextIdx = (currentIndex + 1) % currentRoute.size();
        return currentRoute.get(nextIdx);
    }

    public static OrderedWaypoint getPreviousWaypoint() {
        if (currentRoute.isEmpty()) return null;
        int prevIdx = (currentIndex - 1 + currentRoute.size()) % currentRoute.size();
        return currentRoute.get(prevIdx);
    }

    public static List<OrderedWaypoint> getNextWaypoints(int count) {
        List<OrderedWaypoint> result = new ArrayList<>();
        if (currentRoute.isEmpty()) return result;

        for (int i = 1; i <= count; i++) {
            int idx = (currentIndex + i) % currentRoute.size();
            result.add(currentRoute.get(idx));
        }
        return result;
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        boolean shouldReset = false;

        // Detect world change and reset to waypoint 1
        if (lastWorld != client.level) {
            if (lastWorld != null && !currentRoute.isEmpty()) {
                shouldReset = true;
                LOGGER.info("[OrderedWaypointManager] World changed, reset to waypoint #1");
            }
            lastWorld = client.level;
        }

        // Detect teleports (large position changes) - for servers like Hypixel
        BlockPos currentPos = client.player.blockPosition();
        if (lastPlayerPos != null && !shouldReset && !currentRoute.isEmpty()) {
            double distance = Math.sqrt(lastPlayerPos.distSqr(currentPos));
            if (distance > TELEPORT_THRESHOLD) {
                shouldReset = true;
                LOGGER.info("[OrderedWaypointManager] Teleport detected ({}m), reset to waypoint #1", (int) distance);
            }
        }
        lastPlayerPos = currentPos;

        if (shouldReset) {
            currentIndex = 0;
            wrongWaypoints.clear();
            waypointsReachedSinceLastCheck = 0;

            // Rescan for bad waypoints after a short delay (let chunks load)
            if (lobbyCheckEnabled) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        client.execute(() -> {
                            if (client.level != null && !currentRoute.isEmpty()) {
                                checkRouteBlocks();
                            }
                        });
                    } catch (InterruptedException e) {
                        LOGGER.error("Failed to rescan after reset", e);
                    }
                }).start();
            }
        }

        if (!enabled || currentRoute.isEmpty()) return;

        BlockPos playerPos = client.player.blockPosition();
        // Check if player reached the NEXT waypoint (target), then advance
        OrderedWaypoint target = getNextWaypoint();

        if (target != null && target.distanceTo(playerPos) < waypointRange) {
            advanceToNext();
        }
    }

    public static void advanceToNext() {
        if (currentRoute.isEmpty()) return;

        currentIndex = (currentIndex + 1) % currentRoute.size();

        if (lobbyCheckEnabled) {
            java.util.Set<Integer> upcomingIndices = new java.util.HashSet<>();
            for (int i = 0; i < lobbyCheckInterval && i < currentRoute.size(); i++) {
                int idx = (currentIndex + i) % currentRoute.size();
                OrderedWaypoint wp = currentRoute.get(idx);
                upcomingIndices.add(wp.getIndex());
            }
            wrongWaypoints.removeIf(idx -> !upcomingIndices.contains(idx));

            waypointsReachedSinceLastCheck++;
            if (waypointsReachedSinceLastCheck >= lobbyCheckInterval - 1) {
                waypointsReachedSinceLastCheck = 0;
                checkRouteBlocks();
            }
        }
    }

    public static void checkRouteBlocks() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (currentRoute.isEmpty()) return;

        Block expectedBlock;
        try {
            expectedBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(lobbyCheckBlock));
        } catch (Exception e) {
            LOGGER.warn("Invalid lobby check block: " + lobbyCheckBlock);
            return;
        }
        if (expectedBlock == null || expectedBlock == Blocks.AIR) {
            LOGGER.warn("Invalid lobby check block: " + lobbyCheckBlock);
            return;
        }

        int radius = lobbyCheckRadius;

        for (int i = 0; i < lobbyCheckInterval && i < currentRoute.size(); i++) {
            int idx = (currentIndex + i) % currentRoute.size();
            OrderedWaypoint wp = currentRoute.get(idx);
            BlockPos pos = wp.getPosition();
            Integer waypointIndex = wp.getIndex();

            // Only check waypoints in loaded chunks
            if (!client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }

            boolean foundBlock = false;
            for (int dx = -radius; dx <= radius && !foundBlock; dx++) {
                for (int dy = -radius; dy <= radius && !foundBlock; dy++) {
                    for (int dz = -radius; dz <= radius && !foundBlock; dz++) {
                        BlockPos checkPos = pos.offset(dx, dy, dz);
                        BlockState state = client.level.getBlockState(checkPos);
                        if (state.getBlock() == expectedBlock) {
                            foundBlock = true;
                        }
                    }
                }
            }

            if (!foundBlock) {
                if (!wrongWaypoints.contains(waypointIndex)) {
                    wrongWaypoints.add(waypointIndex);
                }
            } else {
                wrongWaypoints.remove(waypointIndex);
            }
        }
    }

    public static void manualLobbyCheck() {
        if (!lobbyCheckEnabled) {
            sendMessage("\u00A7cLobby check is disabled. Enable it in settings.");
            return;
        }
        checkRouteBlocks();
        if (wrongWaypoints.isEmpty()) {
            sendMessage("\u00A7aAll waypoints have the correct block!");
        }
    }

    public static void resetLobbyCheckCounter() {
        waypointsReachedSinceLastCheck = 0;
        wrongWaypoints.clear();
    }

    public static void add() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        BlockPos pos = client.player.blockPosition().below();
        int newIndex = currentRoute.size() + 1;
        currentRoute.add(new OrderedWaypoint(pos, newIndex));

        sendMessage("\u00A7aAdded waypoint \u00A7e#" + newIndex + " \u00A7aat \u00A7f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    public static void insert(int number) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (number < 1 || number > currentRoute.size() + 1) {
            sendMessage("\u00A7cInvalid position. Must be between 1 and " + (currentRoute.size() + 1));
            return;
        }

        BlockPos pos = client.player.blockPosition().below();

        for (int i = number - 1; i < currentRoute.size(); i++) {
            currentRoute.get(i).setIndex(currentRoute.get(i).getIndex() + 1);
        }

        currentRoute.add(number - 1, new OrderedWaypoint(pos, number));
        sendMessage("\u00A7aInserted waypoint \u00A7e#" + number + " \u00A7aat \u00A7f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    public static void remove(int number) {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo waypoints to remove.");
            return;
        }

        if (number < 1 || number > currentRoute.size()) {
            sendMessage("\u00A7cInvalid waypoint number. Must be between 1 and " + currentRoute.size());
            return;
        }

        currentRoute.remove(number - 1);

        for (int i = number - 1; i < currentRoute.size(); i++) {
            currentRoute.get(i).setIndex(i + 1);
        }

        if (currentIndex >= currentRoute.size()) {
            currentIndex = 0;
        }

        sendMessage("\u00A7aRemoved waypoint \u00A7e#" + number);
    }

    public static void move(int number) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route loaded.");
            return;
        }

        if (number < 1 || number > currentRoute.size()) {
            sendMessage("\u00A7cInvalid waypoint number. Must be between 1 and " + currentRoute.size());
            return;
        }

        BlockPos pos = client.player.blockPosition().below();
        currentRoute.get(number - 1).setPosition(pos);
        sendMessage("\u00A7aMoved waypoint \u00A7e#" + number + " \u00A7ato \u00A7f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    public static void swap(int first, int second) {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route loaded.");
            return;
        }

        if (first < 1 || first > currentRoute.size() || second < 1 || second > currentRoute.size()) {
            sendMessage("\u00A7cInvalid waypoint number. Must be between 1 and " + currentRoute.size());
            return;
        }

        if (first == second) {
            sendMessage("\u00A7cThose are the same waypoint.");
            return;
        }

        OrderedWaypoint a = currentRoute.get(first - 1);
        OrderedWaypoint b = currentRoute.get(second - 1);
        currentRoute.set(first - 1, b);
        currentRoute.set(second - 1, a);
        a.setIndex(second);
        b.setIndex(first);

        // Positions behind the indices changed; let the next lobby check re-flag.
        wrongWaypoints.clear();

        sendMessage("\u00A7aSwapped waypoints \u00A7e#" + first + " \u00A7aand \u00A7e#" + second);
    }

    public static void skip(int amount) {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route loaded.");
            return;
        }

        currentIndex = (currentIndex + amount) % currentRoute.size();
        if (currentIndex < 0) currentIndex += currentRoute.size();

        sendMessage("\u00A7aSkipped " + amount + " waypoint(s). Now at \u00A7e#" + (currentIndex + 1));
    }

    public static void skipTo(int number) {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route loaded.");
            return;
        }

        if (number < 1 || number > currentRoute.size()) {
            sendMessage("\u00A7cInvalid waypoint number. Must be between 1 and " + currentRoute.size());
            return;
        }

        currentIndex = number - 1;
        sendMessage("\u00A7aSkipped to waypoint \u00A7e#" + number);
    }

    public static void save(String name) {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo waypoints to save.");
            return;
        }

        File routeFile = new File(ROUTES_DIR, name + ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(routeFile))) {
            for (OrderedWaypoint wp : currentRoute) {
                BlockPos pos = wp.getPosition();
                writer.println(pos.getX() + "," + pos.getY() + "," + pos.getZ());
            }
            sendMessage("\u00A7aSaved route as \u00A7e" + name + ".txt \u00A7awith " + currentRoute.size() + " waypoints.");
        } catch (IOException e) {
            LOGGER.error("Failed to save route: " + e.getMessage());
            sendMessage("\u00A7cFailed to save route: " + e.getMessage());
        }
    }

    public static void load(String name) {
        File routeFile = new File(ROUTES_DIR, name + ".txt");
        if (!routeFile.exists()) {
            String[] routes = ROUTES_DIR.list((dir, n) -> n.endsWith(".txt"));
            String available = routes != null && routes.length > 0
                ? String.join(", ", routes).replace(".txt", "")
                : "none";
            sendMessage("\u00A7cRoute \u00A7e" + name + " \u00A7cnot found. Available: " + available);
            return;
        }

        currentRoute.clear();
        int index = 1;

        try {
            List<String> lines = Files.readAllLines(routeFile.toPath());
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    String[] coords = line.split(",");
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());
                    int z = Integer.parseInt(coords[2].trim());
                    currentRoute.add(new OrderedWaypoint(new BlockPos(x, y, z), index));
                    index++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse waypoint line: " + line);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load route: " + e.getMessage());
            sendMessage("\u00A7cFailed to load route: " + e.getMessage());
            return;
        }

        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo valid waypoints found in " + name + ".txt");
            return;
        }

        startAtClosestWaypoint();

        enabled = true;
        resetLobbyCheckCounter();
        sendMessage("\u00A7aLoaded route \u00A7e" + name + " \u00A7awith " + currentRoute.size() + " waypoints. Starting at \u00A7e#" + (currentIndex + 1));

        if (lobbyCheckEnabled) {
            checkRouteBlocks();
        }
    }

    public static void loadFromClipboard() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String clipboardContent = client.keyboardHandler.getClipboard();
        if (clipboardContent == null || clipboardContent.trim().isEmpty()) {
            sendMessage("\u00A7cClipboard is empty.");
            return;
        }

        currentRoute.clear();
        String trimmed = clipboardContent.trim();

        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            parseJsonWaypoints(trimmed);
        } else {
            parseTextWaypoints(trimmed);
        }

        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo valid waypoints found in clipboard.");
            return;
        }

        startAtClosestWaypoint();

        enabled = true;
        resetLobbyCheckCounter();
        sendMessage("\u00A7aLoaded " + currentRoute.size() + " waypoints from clipboard. Starting at \u00A7e#" + (currentIndex + 1));

        if (lobbyCheckEnabled) {
            checkRouteBlocks();
        }
    }

    private static void startAtClosestWaypoint() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            BlockPos playerPos = client.player.blockPosition();
            double minDist = Double.MAX_VALUE;
            int closestIdx = 0;

            for (int i = 0; i < currentRoute.size(); i++) {
                double dist = currentRoute.get(i).distanceTo(playerPos);
                if (dist < minDist) {
                    minDist = dist;
                    closestIdx = i;
                }
            }
            currentIndex = closestIdx;
        } else {
            currentIndex = 0;
        }
    }

    private static void parseJsonWaypoints(String content) {
        // Parse JSON format: [{"x": 495, "y": 84, "z": 221}, ...]
        int index = 1;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\"x\"\\s*:\\s*(-?\\d+).*?\"y\"\\s*:\\s*(-?\\d+).*?\"z\"\\s*:\\s*(-?\\d+)",
            java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            try {
                int x = Integer.parseInt(matcher.group(1));
                int y = Integer.parseInt(matcher.group(2));
                int z = Integer.parseInt(matcher.group(3));
                currentRoute.add(new OrderedWaypoint(new BlockPos(x, y, z), index));
                index++;
            } catch (Exception e) {
                LOGGER.warn("Failed to parse JSON waypoint: " + e.getMessage());
            }
        }
    }

    private static void parseTextWaypoints(String content) {
        // Parse text format: "x,y,z" per line or "x y z", optionally "index:x,y,z"
        String[] lines = content.split("\\r?\\n");
        int index = 1;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                int x, y, z;
                String[] coords;

                if (line.contains(":")) {
                    String[] parts = line.split(":");
                    coords = parts[parts.length - 1].split(",");
                } else if (line.contains(",")) {
                    coords = line.split(",");
                } else {
                    coords = line.split("\\s+");
                }

                if (coords.length < 3) {
                    continue;
                }

                x = (int) Double.parseDouble(coords[0].trim());
                y = (int) Double.parseDouble(coords[1].trim());
                z = (int) Double.parseDouble(coords[2].trim());

                currentRoute.add(new OrderedWaypoint(new BlockPos(x, y, z), index));
                index++;
            } catch (Exception e) {
                LOGGER.warn("Failed to parse waypoint line: " + line);
            }
        }
    }

    public static void unload() {
        currentRoute.clear();
        currentIndex = 0;
        editMode = false;
        sendMessage("\u00A7aRoute unloaded.");
    }

    public static void exportToClipboard() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route to export.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (OrderedWaypoint wp : currentRoute) {
            BlockPos pos = wp.getPosition();
            sb.append(pos.getX()).append(",").append(pos.getY()).append(",").append(pos.getZ()).append("\n");
        }

        client.keyboardHandler.setClipboard(sb.toString().trim());
        sendMessage("\u00A7aExported " + currentRoute.size() + " waypoints to clipboard.");
    }

    public static void export() {
        exportToClipboard();
    }

    public static void deleteRoute(String name) {
        File routeFile = new File(ROUTES_DIR, name + ".txt");
        if (!routeFile.exists()) {
            sendMessage("\u00A7cRoute \u00A7e" + name + " \u00A7cnot found.");
            return;
        }

        if (routeFile.delete()) {
            sendMessage("\u00A7aDeleted route \u00A7e" + name + ".txt");
        } else {
            sendMessage("\u00A7cFailed to delete route \u00A7e" + name + ".txt");
        }
    }

    public static void listRoutes() {
        File[] files = ROUTES_DIR.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            sendMessage("\u00A7cNo saved routes.");
            return;
        }

        sendMessage("\u00A76Saved routes:");
        for (File file : files) {
            String name = file.getName().replace(".txt", "");
            try {
                long lineCount = Files.lines(file.toPath()).filter(l -> !l.trim().isEmpty()).count();
                sendMessage("\u00A7e  " + name + " \u00A77(" + lineCount + " waypoints)");
            } catch (IOException e) {
                sendMessage("\u00A7e  " + name + " \u00A77(unknown waypoints)");
            }
        }
    }

    public static void info() {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route loaded.");
            return;
        }

        sendMessage("\u00A76Current route: \u00A7f" + currentRoute.size() + " waypoints");
        sendMessage("\u00A76Current position: \u00A7e#" + (currentIndex + 1));
        sendMessage("\u00A76Enabled: \u00A7f" + enabled);
    }

    public static void toggle() {
        enabled = !enabled;
        sendMessage("\u00A7aOrdered waypoints " + (enabled ? "\u00A72enabled" : "\u00A7cdisabled"));
    }

    private static void sendMessage(String msg) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            MqoChat.reply(Component.literal("\u00A76[MQO] " + msg));
        }
    }
}
