package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
    private static final HudAnchor ANCHOR = new HudAnchor(10, 62, RollingMinerCooldown::getWidth, RollingMinerCooldown::getHeight);
    private static final float[] cooldownLabelColor = {1.0f, 170.0f / 255.0f, 0.0f};
    private static final float[] cooldownValueColor = {1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
    private static final float[] readyLabelColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
    private static final float[] readyValueColor = {0.0f, 170.0f / 255.0f, 0.0f};

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
        boolean ready = secondsLeft <= 0;

        context.text(
            client.font,
            formatText(ready ? "✔ Ready" : secondsLeft + "s", ready),
            ANCHOR.x(),
            ANCHOR.y(),
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
        ANCHOR.set(x, y);
    }

    /** Edge anchor for the config — see {@link HudAnchor}. */
    public static HudAnchor anchor() { return ANCHOR; }

    public static int getX() {
        return ANCHOR.x();
    }

    public static int getY() {
        return ANCHOR.y();
    }

    /** Measured from the text actually drawn, so the mover box hugs it. */
    public static int getWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) return 120;
        return Math.max(12, client.font.width(getPreviewText()));
    }

    public static int getHeight() {
        Minecraft client = Minecraft.getInstance();
        return client.font == null ? 10 : client.font.lineHeight;
    }

    public static float[] getCooldownLabelColor() {
        return cooldownLabelColor.clone();
    }

    public static void setCooldownLabelColor(float red, float green, float blue) {
        setColor(cooldownLabelColor, red, green, blue);
    }

    public static float[] getCooldownValueColor() {
        return cooldownValueColor.clone();
    }

    public static void setCooldownValueColor(float red, float green, float blue) {
        setColor(cooldownValueColor, red, green, blue);
    }

    public static float[] getReadyLabelColor() {
        return readyLabelColor.clone();
    }

    public static void setReadyLabelColor(float red, float green, float blue) {
        setColor(readyLabelColor, red, green, blue);
    }

    public static float[] getReadyValueColor() {
        return readyValueColor.clone();
    }

    public static void setReadyValueColor(float red, float green, float blue) {
        setColor(readyValueColor, red, green, blue);
    }

    public static Component getPreviewText() {
        return formatText("✔ Ready", true);
    }

    private static Component formatText(String value, boolean ready) {
        float[] labelColor = ready ? readyLabelColor : cooldownLabelColor;
        float[] valueColor = ready ? readyValueColor : cooldownValueColor;
        MutableComponent text = Component.literal("Rolling Miner: ")
            .setStyle(Style.EMPTY.withColor(toRgb(labelColor)));
        return text.append(Component.literal(value).setStyle(Style.EMPTY.withColor(toRgb(valueColor))));
    }

    private static int toRgb(float[] color) {
        int red = Math.round(color[0] * 255.0f);
        int green = Math.round(color[1] * 255.0f);
        int blue = Math.round(color[2] * 255.0f);
        return (red << 16) | (green << 8) | blue;
    }

    private static void setColor(float[] color, float red, float green, float blue) {
        color[0] = clampColor(red);
        color[1] = clampColor(green);
        color[2] = clampColor(blue);
    }

    private static float clampColor(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int secondsLeft() {
        long remaining = cooldownEndsAt - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }
}
