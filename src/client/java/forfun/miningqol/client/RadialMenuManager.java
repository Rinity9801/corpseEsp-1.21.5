package forfun.miningqol.client;

import forfun.miningqol.client.config.MiningConfig;
import forfun.miningqol.client.gui.RadialMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Backing store + actions for the radial menu. Eight fixed slots, each holding a
 * label and a command to run; persisted in {@link MiningConfig#radialEntries}.
 */
public class RadialMenuManager {
    public static final int SLOTS = 8;

    public static List<MiningConfig.RadialEntry> getEntries() {
        return MiningqolClient.getConfig().radialEntries;
    }

    public static void open() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(new RadialMenuScreen()));
    }

    public static void setCommand(int slot, String command) {
        if (!valid(slot)) return;
        MiningConfig cfg = MiningqolClient.getConfig();
        MiningConfig.RadialEntry e = cfg.radialEntries.get(slot - 1);
        e.command = command;
        if (e.label == null || e.label.isEmpty()) e.label = command;
        cfg.save();
        msg("§aSlot " + slot + " runs: §f" + command + " §7(label: " + e.label + ")");
    }

    public static void setLabel(int slot, String label) {
        if (!valid(slot)) return;
        MiningConfig cfg = MiningqolClient.getConfig();
        cfg.radialEntries.get(slot - 1).label = label;
        cfg.save();
        msg("§aSlot " + slot + " label: §f" + label);
    }

    public static void clear(int slot) {
        if (!valid(slot)) return;
        MiningConfig cfg = MiningqolClient.getConfig();
        MiningConfig.RadialEntry e = cfg.radialEntries.get(slot - 1);
        e.command = "";
        e.label = "";
        cfg.save();
        msg("§aSlot " + slot + " cleared");
    }

    public static void list() {
        msg("§6=== Radial Menu ===");
        List<MiningConfig.RadialEntry> es = getEntries();
        for (int i = 0; i < SLOTS; i++) {
            MiningConfig.RadialEntry e = es.get(i);
            if (e.command == null || e.command.isEmpty()) {
                msg("§7" + (i + 1) + ": §8(empty)");
            } else {
                msg("§7" + (i + 1) + ": §f" + e.label + " §8→ §7" + e.command);
            }
        }
        msg("§8Set with §7/radial set <1-8> <command>§8, label with §7/radial label <1-8> <text>");
    }

    /** Run a slot's command as if the player typed it (leading slash optional). */
    public static void run(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || command == null || command.isEmpty()) return;
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        client.player.networkHandler.sendChatCommand(cmd);
    }

    private static boolean valid(int slot) {
        if (slot < 1 || slot > SLOTS) {
            msg("§cSlot must be 1-" + SLOTS);
            return false;
        }
        return true;
    }

    private static void msg(String s) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§6[MQO] " + s), false);
    }
}
