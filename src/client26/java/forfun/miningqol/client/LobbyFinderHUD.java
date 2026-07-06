package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class LobbyFinderHUD {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("miningqol", "lobby_finder_hud");
    private static boolean registered = false;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        HudElementRegistry.addLast(HUD_ID, (context, tickCounter) -> render(context));
    }

    public static void render(GuiGraphicsExtractor ctx) {
        if (!LobbyFinder.shouldDisplayUnavailable()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Font font = client.font;

        // Display just above the hotbar (actionbar area but higher to avoid conflicts)
        String text = "§c§lLOBBY UNAVAILABLE";
        int textWidth = font.width(text);

        int screenWidth = ctx.guiWidth();
        int screenHeight = ctx.guiHeight();

        // Position above hotbar (hotbar is at screenHeight - 40, we go higher)
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 80;

        // Background
        ctx.fill(x - 4, y - 2, x + textWidth + 4, y + font.lineHeight + 2, 0xAA000000);

        // Border
        ctx.outline(x - 4, y - 2, textWidth + 8, font.lineHeight + 4, 0xFFFF4444);

        // Text
        ctx.text(font, text, x, y, 0xFFFFFFFF, true);
    }
}
