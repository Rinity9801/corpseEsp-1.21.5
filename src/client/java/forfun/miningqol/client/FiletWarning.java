package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Collection;

public class FiletWarning {
    private static boolean enabled = false;
    private static long lastCheck = 0;
    private static long lastWarning = 0;
    private static boolean filetActive = false;

    public static void tick() {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;

        long now = System.currentTimeMillis();
        if (now - lastCheck < 2000) return; // Check every 2 seconds
        lastCheck = now;

        boolean found = false;
        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
        for (PlayerListEntry entry : entries) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            String line = displayName.getString().replaceAll("\u00A7.", "").trim();
            if (line.contains("Filet O' Fortune")) {
                found = true;
                break;
            }
        }

        boolean wasActive = filetActive;
        filetActive = found;

        // Warn when filet is missing, but not too frequently
        if (!filetActive && now - lastWarning > 30000) {
            lastWarning = now;
            client.inGameHud.setTitleTicks(5, 40, 10);
            client.inGameHud.setTitle(Text.literal(""));
            client.inGameHud.setSubtitle(Text.literal("\u00A7c\u00A7lFilet O' Fortune expired!"));
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
