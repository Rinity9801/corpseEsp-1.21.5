package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Every chat message the mod prints goes through here, split into two channels:
 *
 * <ul>
 *   <li>{@link #log} — automatic status chatter (feature toggles, auto-clicker/claim
 *       progress, trackers). Silenced by the Misc "Mod Chat Messages" toggle.</li>
 *   <li>{@link #reply} — the answer to a command the player just typed. Always shown,
 *       so muting the logs never makes a command look broken.</li>
 * </ul>
 */
public final class MqoChat {
    private static boolean logsEnabled = true;

    private MqoChat() {}

    public static void setLogsEnabled(boolean enabled) {
        logsEnabled = enabled;
    }

    public static boolean isLogsEnabled() {
        return logsEnabled;
    }

    public static void log(Component message) {
        if (logsEnabled) send(message);
    }

    public static void log(String message) {
        if (logsEnabled) send(Component.literal(message));
    }

    public static void reply(Component message) {
        send(message);
    }

    public static void reply(String message) {
        send(Component.literal(message));
    }

    private static void send(Component message) {
        Minecraft client = Minecraft.getInstance();
        // Callers off the client thread (sound engine, clicker threads) have to queue.
        if (client.isSameThread()) {
            if (client.player != null) client.player.sendSystemMessage(message);
        } else {
            client.execute(() -> {
                if (client.player != null) client.player.sendSystemMessage(message);
            });
        }
    }
}
