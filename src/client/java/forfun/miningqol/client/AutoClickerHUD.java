package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class AutoClickerHUD {
    private static boolean enabled = true;

    public static void render(DrawContext context, MinecraftClient client) {
        if (!enabled || !AutoClickerManager.isEnabled() || client.player == null || client.options.hudHidden) {
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

        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = screenWidth / 2;
        int y = screenHeight / 2 + 20;

        int textWidth = client.textRenderer.getWidth(timerText);

        context.drawTextWithShadow(
            client.textRenderer,
            timerText,
            x - textWidth / 2,
            y,
            color
        );
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
