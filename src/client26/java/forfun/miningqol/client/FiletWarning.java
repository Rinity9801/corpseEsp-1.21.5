package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public class FiletWarning {
    private static boolean enabled = false;
    private static long lastCheck = 0;
    private static long lastWarning = 0;
    private static boolean filetActive = false;

    public static void tick() {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) return;

        long now = System.currentTimeMillis();
        if (now - lastCheck < 2000) return; // Check every 2 seconds
        lastCheck = now;

        boolean found = false;
        Collection<PlayerInfo> entries = client.getConnection().getListedOnlinePlayers();
        for (PlayerInfo entry : entries) {
            Component displayName = entry.getTabListDisplayName();
            if (displayName == null) continue;

            String line = displayName.getString().replaceAll("§.", "").trim();
            if (line.contains("Filet O' Fortune")) {
                found = true;
                break;
            }
        }

        filetActive = found;

        // Warn when filet is missing, but not too frequently
        if (!filetActive && now - lastWarning > 30000) {
            lastWarning = now;
            client.gui.setTimes(5, 40, 10);
            client.gui.setTitle(Component.literal(""));
            client.gui.setSubtitle(Component.literal("§c§lFilet O' Fortune expired!"));
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
