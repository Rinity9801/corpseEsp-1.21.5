package forfun.miningqol.client.hotm;

import net.minecraft.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class HotmTree {
    private static final Logger LOGGER = LoggerFactory.getLogger("HotmTree");
    private static final int TOTAL_TOKENS = 25;
    private static final File PRESETS_DIR = new File("config/miningqol_hotm");

    private final EnumMap<HotmNode, HotmNode.State> states = new EnumMap<>(HotmNode.class);

    public HotmTree() {
        for (HotmNode node : HotmNode.values()) {
            if (node.isAlwaysEnabled()) {
                states.put(node, HotmNode.State.MAXED);
            } else {
                states.put(node, HotmNode.State.NOT_CLICKED);
            }
        }
    }

    public HotmNode.State getState(HotmNode node) {
        return states.get(node);
    }

    public boolean setState(HotmNode node, HotmNode.State state) {
        if (node.isAlwaysEnabled()) return false;
        if (!state.isValidFor(node.getType())) return false;

        HotmNode.State oldState = states.get(node);
        int delta = state.getTokenCost() - oldState.getTokenCost();
        if (getUsedTokens() + delta > TOTAL_TOKENS) return false;

        states.put(node, state);
        return true;
    }

    public void forceState(HotmNode node, HotmNode.State state) {
        states.put(node, state);
    }

    public int getUsedTokens() {
        int total = 0;
        for (var entry : states.entrySet()) {
            if (entry.getKey().isAlwaysEnabled()) continue;
            total += entry.getValue().getTokenCost();
        }
        return total;
    }

    public int getRemainingTokens() { return TOTAL_TOKENS - getUsedTokens(); }
    public int getTotalTokens() { return TOTAL_TOKENS; }

    public Item getDisplayItem(HotmNode node) {
        return node.getItemForState(states.get(node));
    }

    public HotmNode getNodeAt(int row, int col) {
        for (HotmNode node : HotmNode.values()) {
            if (node.getRow() == row && node.getCol() == col) return node;
        }
        return null;
    }

    public List<HotmNode> getActiveNodes() {
        List<HotmNode> active = new ArrayList<>();
        for (HotmNode node : HotmNode.values()) {
            if (states.get(node) != HotmNode.State.NOT_CLICKED) active.add(node);
        }
        return active;
    }

    /**
     * Returns active nodes in BFS order starting from Mining Speed.
     * Only traverses through active nodes (state != NOT_CLICKED).
     * Adjacency = up/down/left/right (no diagonal).
     */
    public List<HotmNode> getActivationOrder() {
        Set<HotmNode> toActivate = new LinkedHashSet<>();
        for (HotmNode node : HotmNode.values()) {
            if (states.get(node) != HotmNode.State.NOT_CLICKED) toActivate.add(node);
        }

        List<HotmNode> order = new ArrayList<>();
        if (toActivate.isEmpty()) return order;

        Queue<HotmNode> queue = new LinkedList<>();
        Set<HotmNode> visited = new HashSet<>();

        HotmNode start = HotmNode.MINING_SPEED;
        if (toActivate.contains(start)) {
            queue.add(start);
            visited.add(start);
        }

        while (!queue.isEmpty()) {
            HotmNode current = queue.poll();
            order.add(current);

            for (HotmNode neighbor : toActivate) {
                if (visited.contains(neighbor)) continue;
                if (isAdjacent(current, neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return order;
    }

    private boolean isAdjacent(HotmNode a, HotmNode b) {
        int dr = Math.abs(a.getRow() - b.getRow());
        int dc = Math.abs(a.getCol() - b.getCol());
        return (dr + dc) == 1;
    }

    // --- Preset Save/Load ---

    public static void initPresetDir() {
        PRESETS_DIR.mkdirs();
    }

    public void savePreset(String name) {
        try {
            PRESETS_DIR.mkdirs();
            File file = new File(PRESETS_DIR, name + ".txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                for (HotmNode node : HotmNode.values()) {
                    HotmNode.State state = states.get(node);
                    if (state != HotmNode.State.NOT_CLICKED) {
                        writer.println(node.name() + "=" + state.name());
                    }
                }
            }
            LOGGER.info("Saved HOTM preset '{}' ({} tokens)", name, getUsedTokens());
        } catch (IOException e) {
            LOGGER.error("Failed to save HOTM preset '{}'", name, e);
        }
    }

    public boolean loadPreset(String name) {
        File file = new File(PRESETS_DIR, name + ".txt");
        if (!file.exists()) return false;

        for (HotmNode node : HotmNode.values()) {
            if (node.isAlwaysEnabled()) {
                states.put(node, HotmNode.State.MAXED);
            } else {
                states.put(node, HotmNode.State.NOT_CLICKED);
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                try {
                    HotmNode node = HotmNode.valueOf(parts[0]);
                    HotmNode.State state = HotmNode.State.valueOf(parts[1]);
                    if (state.isValidFor(node.getType())) {
                        states.put(node, state);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
            LOGGER.info("Loaded HOTM preset '{}' ({} tokens)", name, getUsedTokens());
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to load HOTM preset '{}'", name, e);
            return false;
        }
    }

    public static List<String> listPresets() {
        List<String> presets = new ArrayList<>();
        File[] files = PRESETS_DIR.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files != null) {
            for (File f : files) {
                presets.add(f.getName().replace(".txt", ""));
            }
            Collections.sort(presets);
        }
        return presets;
    }

    public static void deletePreset(String name) {
        new File(PRESETS_DIR, name + ".txt").delete();
    }

    // Legacy save/load for backwards compat
    public void save() { savePreset("default"); }
    public void load() { loadPreset("default"); }
}
