package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class AutoClickerHUD {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("miningqol", "autoclicker_hud");

    private static boolean registered = false;
    private static boolean enabled = true;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        HudElementRegistry.addLast(HUD_ID, (context, tickCounter) -> render(context));
    }

    public static void render(GuiGraphicsExtractor ctx) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || !AutoClickerManager.isEnabled() || client.player == null || client.options.hideGui) {
            return;
        }

        double remainingSeconds = AutoClickerManager.getRemainingSeconds();

        String timerText;
        int color;

        if (remainingSeconds <= 0) {
            timerText = "READY";
            color = 0xFF00FF00;
        } else {
            int totalSeconds = (int) remainingSeconds;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            timerText = String.format("%d:%02d", minutes, seconds);
            color = 0xFFFFFFFF;
        }

        int screenWidth = ctx.guiWidth();
        int screenHeight = ctx.guiHeight();
        int x = screenWidth / 2;
        int y = screenHeight / 2 + 20;

        int textWidth = client.font.width(timerText);

        ctx.text(
            client.font,
            timerText,
            x - textWidth / 2,
            y,
            color,
            true
        );
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
