package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class RollingMinerCooldown {
    private static final Identifier HUD_ID =
        Identifier.fromNamespaceAndPath("miningqol", "rolling_miner_cooldown_hud");
    private static final String TRIGGER_MESSAGE = "rolling miner granted you double drops!";
    private static final long COOLDOWN_MILLIS = 20_000L;

    private static boolean registered;
    private static boolean enabled;
    private static long cooldownEndsAt;
    private static int hudX = 10;
    private static int hudY = 62;

    private RollingMinerCooldown() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            HUD_ID,
            (context, tickCounter) -> render(context)
        );
    }

    public static void onGameMessage(String message) {
        if (enabled && message.toLowerCase(Locale.ROOT).contains(TRIGGER_MESSAGE)) {
            cooldownEndsAt = System.currentTimeMillis() + COOLDOWN_MILLIS;
        }
    }

    public static void tick(Minecraft client) {
        if (!enabled || cooldownEndsAt == 0) {
            return;
        }
        if (client.player == null) {
            cooldownEndsAt = 0;
            return;
        }
        if (System.currentTimeMillis() >= cooldownEndsAt) {
            cooldownEndsAt = 0;
        }
    }

    public static void render(GuiGraphicsExtractor context) {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int secondsLeft = secondsLeft();
        String displayText = secondsLeft > 0
            ? "§6Rolling Miner: §c" + secondsLeft + "s"
            : "§aRolling Miner: §2✔ Ready";

        context.text(
            client.font,
            displayText,
            hudX,
            hudY,
            0xFFFFFFFF,
            true
        );
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            cooldownEndsAt = 0;
        }
    }

    public static void setPosition(int x, int y) {
        hudX = x;
        hudY = y;
    }

    public static int getX() {
        return hudX;
    }

    public static int getY() {
        return hudY;
    }

    public static int getWidth() {
        return 120;
    }

    public static int getHeight() {
        return 12;
    }

    private static int secondsLeft() {
        long remaining = cooldownEndsAt - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }
}
