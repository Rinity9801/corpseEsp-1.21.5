package forfun.miningqol.client.waypoints;

import forfun.miningqol.client.MqoChat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
    /** Whether /mqo skip walks past waypoints that have no looked-for block left near them. */
    private static boolean skipObstructed = false;
    /** At or below this many blocks near a waypoint, it counts as mined out. */
    private static int obstructedThreshold = 5;
    /**
     * Last obstruction state actually observed per waypoint position.
     *
     * <p>Hypixel's render distance is short enough that most of the route ahead sits in unloaded
     * chunks, where the block count cannot be taken at all. Without a memory, a skip stopped at the
     * first waypoint it could not evaluate — so what was seen while passing nearby is recorded here
     * and reused once the chunk is gone again.
     *
     * <p>Keyed by position rather than route index so reordering or re-saving a route cannot
     * misattribute an entry to a different spot.
     */
    private static final java.util.Map<BlockPos, Boolean> obstructionMemory = new java.util.HashMap<>();
    /** Round-robin cursor for the background sweep. */
    private static int sweepCursor = 0;
    /** Waypoints evaluated per tick — a full pass over a 100-point route lands around every 3s. */
    private static final int SWEEP_PER_TICK = 2;
    /**
     * Nothing is remembered, and nothing remembered is trusted, until this time.
     *
     * <p>Set on every reset. Arriving in a lobby streams chunks in over a second or two, and a block
     * count taken mid-stream reads as mined out — which then sticks, so the first few waypoints came
     * up as already done. The lobby check already waits a second for the same reason.
     */
    private static long memoryReadyAt = 0;
    private static final long MEMORY_SETTLE_MS = 3000;
    private static int waypointsReachedSinceLastCheck = 0;
    private static List<Integer> wrongWaypoints = new ArrayList<>();

    // Block outline around the next waypoint (highlights lobby-check blocks)
    private static boolean blockOutlineAroundWaypoint = false;
    private static int blockOutlineRadius = 3;
    private static float[] blockOutlineColor = {1f, 1f, 1f};
    private static float blockOutlineAlpha = 0.8f;
    private static float blockOutlineThickness = 1.5f;
    private static boolean blockOutlineFill = true;

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
    public static boolean isSkipObstructed() { return skipObstructed; }
    public static void setSkipObstructed(boolean value) { skipObstructed = value; }
    public static int getObstructedThreshold() { return obstructedThreshold; }
    public static void setObstructedThreshold(int value) {
        obstructedThreshold = Math.max(0, Math.min(64, value));
    }
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
    public static boolean isBlockOutlineFill() { return blockOutlineFill; }
    public static void setBlockOutlineFill(boolean value) { blockOutlineFill = value; }
    public static float getBlockOutlineThickness() { return blockOutlineThickness; }
    /** Clamped so an edge can never be thick enough to swallow the block it outlines. */
    public static void setBlockOutlineThickness(float value) {
        blockOutlineThickness = Math.max(0.5f, Math.min(9.0f, value));
    }

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
            obstructionMemory.clear();
            memoryReadyAt = System.currentTimeMillis() + MEMORY_SETTLE_MS;
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

        // After the reset handling, so a lobby change clears the memory before anything is written
        // back into it on the same tick.
        sweepObstruction(client);

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

    /** The configured lobby-check block, or null when it isn't a real block id. */
    private static Block expectedLobbyBlock() {
        try {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(lobbyCheckBlock));
            if (block == null || block == Blocks.AIR) {
                LOGGER.warn("Invalid lobby check block: " + lobbyCheckBlock);
                return null;
            }
            return block;
        } catch (Exception e) {
            LOGGER.warn("Invalid lobby check block: " + lobbyCheckBlock);
            return null;
        }
    }

    /**
     * How many {@code expected} blocks sit within {@code radius} of {@code pos}, counting no further
     * than {@code limit}.
     *
     * <p>The cap earns its keep: the lobby check only needs to know whether there is at least one,
     * and the obstruction check only needs to tell "more than the threshold" from "not more" —
     * neither has to walk the rest of the cube once the answer is settled.
     */
    private static int countBlocksNear(Minecraft client, BlockPos pos, Block expected,
                                       int radius, int limit) {
        int found = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (client.level.getBlockState(cursor).getBlock() == expected && ++found >= limit) {
                        return found;
                    }
                }
            }
        }
        return found;
    }

    /** Whether any {@code expected} block sits within {@code radius} of {@code pos}. */
    private static boolean hasBlockNear(Minecraft client, BlockPos pos, Block expected, int radius) {
        return countBlocksNear(client, pos, expected, radius, 1) > 0;
    }

    /**
     * Whether a waypoint has too little of the looked-for block left near it to be worth visiting.
     *
     * <p>Not simply "none left": a couple of stray blocks is a mined-out spot in practice, so the
     * test is a count against {@link #obstructedThreshold} rather than mere presence.
     *
     * <p>Deliberately a different question from the lobby check, which still asks only whether the
     * block is present AT ALL. A waypoint with three coal is the right lobby but not worth mining,
     * so sharing one threshold between the two would break one of them.
     *
     * <p>A waypoint in an unloaded chunk counts as NOT obstructed. That is "cannot tell" rather than
     * "nothing there", and treating it as obstructed would skip straight past waypoints purely
     * because they are out of render distance.
     */
    /**
     * Records the obstruction state of a couple of currently-loaded waypoints each tick.
     *
     * <p>Bounded on purpose: each evaluation is a block scan, so sweeping the whole route every tick
     * would be far more expensive than the outline ever was. A few per tick keeps the memory fresh
     * enough while staying flat regardless of route length.
     */
    private static void sweepObstruction(Minecraft client) {
        if (!skipObstructed || currentRoute.isEmpty() || client.level == null) return;
        if (System.currentTimeMillis() < memoryReadyAt) return;
        Block expected = expectedLobbyBlock();
        if (expected == null) return;

        int size = currentRoute.size();
        for (int i = 0; i < SWEEP_PER_TICK; i++) {
            sweepCursor = (sweepCursor + 1) % size;
            BlockPos pos = currentRoute.get(sweepCursor).getPosition();
            if (!client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            int count = countBlocksNear(client, pos, expected, lobbyCheckRadius,
                obstructedThreshold + 1);
            obstructionMemory.put(pos.immutable(), count <= obstructedThreshold);
        }
    }

    public static boolean isObstructed(OrderedWaypoint wp) {
        Minecraft client = Minecraft.getInstance();
        if (wp == null || client.level == null) return false;
        Block expected = expectedLobbyBlock();
        if (expected == null) return false;

        BlockPos pos = wp.getPosition();
        if (!client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            // Out of range: fall back to whatever was seen last time it was loaded. Still false when
            // nothing has ever been observed — an unseen waypoint is "cannot tell", not "empty".
            // Nothing is trusted while the world is still settling, for the same reason nothing is
            // recorded then.
            if (System.currentTimeMillis() < memoryReadyAt) return false;
            return obstructionMemory.getOrDefault(pos, false);
        }
        boolean obstructed = countBlocksNear(client, pos, expected, lobbyCheckRadius,
            obstructedThreshold + 1) <= obstructedThreshold;
        obstructionMemory.put(pos.immutable(), obstructed);
        return obstructed;
    }

    public static void checkRouteBlocks() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (currentRoute.isEmpty()) return;

        Block expectedBlock = expectedLobbyBlock();
        if (expectedBlock == null) return;

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

            boolean foundBlock = hasBlockNear(client, pos, expectedBlock, radius);

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

        int passed = skipObstructedFrom(amount < 0 ? -1 : 1);

        sendMessage("\u00A7aSkipped " + amount + " waypoint(s). Now at \u00A7e#" + (currentIndex + 1)
            + (passed > 0 ? " \u00A77(passed " + passed + " with no "
                + lobbyCheckBlock.replaceFirst("^minecraft:", "") + " left)" : ""));
    }

    /**
     * Keeps stepping while the waypoint we landed on is obstructed, so one /mqo skip clears a whole
     * mined-out run instead of needing one skip per waypoint.
     *
     * <p>Continues in the direction the skip was going, so a backwards skip keeps going backwards.
     *
     * @return how many obstructed waypoints were stepped over
     */
    private static int skipObstructedFrom(int step) {
        if (!skipObstructed || currentRoute.isEmpty()) return 0;

        int size = currentRoute.size();
        int start = currentIndex;
        int passed = 0;

        // Bounded by the route length: with every waypoint obstructed this would otherwise circle
        // forever, and stepping `size` times just arrives back where it began.
        for (int i = 0; i < size && isObstructed(currentRoute.get(currentIndex)); i++) {
            currentIndex = ((currentIndex + step) % size + size) % size;
            passed++;
        }

        if (isObstructed(currentRoute.get(currentIndex))) {
            // Nothing in the route has the block near it. Staying put beats silently dumping you at
            // an arbitrary index after a full lap.
            currentIndex = start;
            sendMessage("\u00A7eEvery waypoint looks mined out \u2014 staying at \u00A7e#" + (start + 1));
            return 0;
        }
        return passed;
    }

    /** Marks/unmarks waypoint #number as reached by etherwarp (flag travels with the waypoint). */
    public static void toggleEtherwarp(int number) {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route loaded.");
            return;
        }

        if (number < 1 || number > currentRoute.size()) {
            sendMessage("\u00A7cInvalid waypoint number. Must be between 1 and " + currentRoute.size());
            return;
        }

        OrderedWaypoint wp = currentRoute.get(number - 1);
        wp.setEtherwarp(!wp.isEtherwarp());
        sendMessage(wp.isEtherwarp()
            ? "\u00A7aWaypoint \u00A7e#" + number + " \u00A7ais now an \u00A7detherwarp\u00A7a waypoint. Re-save the route to keep it."
            : "\u00A7aWaypoint \u00A7e#" + number + " \u00A7ais no longer an etherwarp waypoint.");
    }

    public static void listEtherwarps() {
        if (currentRoute.isEmpty()) {
            sendMessage("\u00A7cNo route loaded.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (OrderedWaypoint wp : currentRoute) {
            if (wp.isEtherwarp()) {
                if (sb.length() > 0) sb.append("\u00A7a, ");
                sb.append("\u00A7e#").append(wp.getIndex());
            }
        }
        sendMessage(sb.length() > 0
            ? "\u00A7aEtherwarp waypoints: " + sb
            : "\u00A7aNo etherwarp waypoints. Mark one with \u00A7e/mqo ether <number>");
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
                writer.println(pos.getX() + "," + pos.getY() + "," + pos.getZ()
                    + (wp.isEtherwarp() ? ",ether" : ""));
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
        obstructionMemory.clear();
        sweepCursor = 0;
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
                    OrderedWaypoint wp = new OrderedWaypoint(new BlockPos(x, y, z), index);
                    wp.setEtherwarp(isEtherwarpToken(coords));
                    currentRoute.add(wp);
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
        obstructionMemory.clear();
        sweepCursor = 0;
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

    /** Whether an optional trailing token after x,y,z marks the waypoint as an etherwarp. */
    private static boolean isEtherwarpToken(String[] coords) {
        if (coords.length < 4) return false;
        String token = coords[3].trim().toLowerCase(java.util.Locale.ROOT);
        return token.equals("ether") || token.equals("etherwarp") || token.equals("ew") || token.equals("e");
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

                OrderedWaypoint wp = new OrderedWaypoint(new BlockPos(x, y, z), index);
                wp.setEtherwarp(isEtherwarpToken(coords));
                currentRoute.add(wp);
                index++;
            } catch (Exception e) {
                LOGGER.warn("Failed to parse waypoint line: " + line);
            }
        }
    }

    public static void unload() {
        currentRoute.clear();
        obstructionMemory.clear();
        sweepCursor = 0;
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
            sb.append(pos.getX()).append(",").append(pos.getY()).append(",").append(pos.getZ());
            if (wp.isEtherwarp()) sb.append(",ether");
            sb.append("\n");
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
        long etherCount = currentRoute.stream().filter(OrderedWaypoint::isEtherwarp).count();
        if (etherCount > 0) {
            sendMessage("\u00A76Etherwarp waypoints: \u00A7f" + etherCount + " \u00A77(/mqo ether)");
        }
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
