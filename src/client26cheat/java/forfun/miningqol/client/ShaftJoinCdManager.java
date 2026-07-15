package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shaft Join CD: entering a Glacite Mineshaft ("[RANK] <ign> entered Glacite
 * Mineshafts!" in chat, matched on the player's own ign so partymates don't
 * trigger it) starts a cooldown. While it runs, any GUI whose title contains
 * "Glacite" has all clicks blocked, with a timer panel beside it (same spot
 * as the Auto Forge picker). Esc still closes the GUI.
 */
public class ShaftJoinCdManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShaftJoinCdManager");

    private static final String ENTER_NEEDLE = "entered glacite mineshaft";
    private static final String GUI_TITLE_NEEDLE = "glacite";

    private static boolean enabled = true;
    private static int cooldownSeconds = 30;
    private static int ticksLeft = 0;

    // Panel layout, mirroring the Auto Forge picker's placement.
    private static final int PANEL_W = 150;
    private static final int PANEL_H = 40;
    private static final int PANEL_GAP = 10;

    private ShaftJoinCdManager() {}

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static int getCooldownSeconds() { return cooldownSeconds; }
    public static void setCooldownSeconds(int seconds) { cooldownSeconds = Math.max(1, Math.min(60, seconds)); }

    public static void tick() {
        if (ticksLeft > 0) ticksLeft--;
    }

    /** Chat hook: own-ign shaft entry starts (or restarts) the cooldown. */
    public static void onChatMessage(String message) {
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        String s = clean(message);
        if (!s.contains(ENTER_NEEDLE)) return;
        String ign = client.player.getGameProfile().name().toLowerCase();
        if (!s.contains(ign)) return; // someone else's entry message
        ticksLeft = cooldownSeconds * 20;
        LOGGER.info("[ShaftJoinCd] Own shaft entry — blocking Glacite GUIs for {}s", cooldownSeconds);
    }

    /** True while clicks in this screen should be swallowed. */
    public static boolean isBlocking(Screen screen) {
        return enabled && ticksLeft > 0
            && screen instanceof ContainerScreen cs
            && clean(cs.getTitle().getString()).contains(GUI_TITLE_NEEDLE);
    }

    /** Timer panel beside the blocked GUI (drawn via CheatHooks.containerGuiOverlay). */
    public static void renderOnTop(Screen screen, GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        if (!isBlocking(screen)) return;
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        int px = Math.min(w / 2 + 88 + PANEL_GAP, w - PANEL_W - 4);
        int py = (h - PANEL_H) / 2;
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xF0101018);
        ctx.outline(px, py, PANEL_W, PANEL_H, 0xFF404050);
        int secondsLeft = (ticksLeft + 19) / 20;
        ctx.centeredText(font, "§bShaft Join CD", px + PANEL_W / 2, py + 8, 0xFFFFFFFF);
        ctx.centeredText(font, "§fClicks blocked: §e" + secondsLeft + "s", px + PANEL_W / 2, py + 22, 0xFFFFFFFF);
    }

    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("§.", "").trim().toLowerCase();
    }
}
