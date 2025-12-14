package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class LobbyFinderHUD {

    public static void render(DrawContext context) {
        if (!LobbyFinder.shouldDisplayUnavailable()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;

        // Display just above the hotbar (actionbar area but higher to avoid conflicts)
        String text = "§c§lLOBBY UNAVAILABLE";
        int textWidth = textRenderer.getWidth(text);

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position above hotbar (hotbar is at screenHeight - 40, we go higher)
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 80;

        // Background
        context.fill(x - 4, y - 2, x + textWidth + 4, y + textRenderer.fontHeight + 2, 0xAA000000);

        // Border
        context.drawStrokedRectangle(x - 4, y - 2, textWidth + 8, textRenderer.fontHeight + 4, 0xFFFF4444);

        // Text
        context.drawTextWithShadow(textRenderer, text, x, y, 0xFFFFFFFF);
    }
}
