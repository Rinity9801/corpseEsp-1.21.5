package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PickaxeCooldownHUD {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("miningqol", "pickaxe_cooldown_hud");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("(.+?):\\s+(\\d+)s");
    private static final Pattern READY_PATTERN = Pattern.compile("(.+?):\\s+(Available|Ready|✔)");

    private static final String[] PICKAXE_ABILITIES = {
        "Pickobulus",
        "Mining Speed Boost",
        "Maniac Miner",
        "Sheer Force",
        "Vein Seeker"
    };

    private static boolean registered = false;
    private static boolean enabled = true;

    private static String currentCooldown = "Ready";
    private static String abilityName = "Pickaxe";
    private static long lastUpdate = 0;

    private static int lastKnownCooldownSeconds = 0;
    private static long lastCooldownUpdateTime = 0;
    private static boolean isOnCooldown = false;

    private static int hudX = 10;
    private static int hudY = 50;
    private static float scale = 1.0f;

    private static boolean titleEnabled = true;
    private static int titleThreshold = 5;
    private static long lastTitleSetTime = 0;
    private static int lastTitleCooldown = -1;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        HudElementRegistry.addLast(HUD_ID, (context, tickCounter) -> render(context));
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.getConnection() == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < 500) return;
        lastUpdate = currentTime;

        ClientPacketListener connection = client.getConnection();
        Collection<PlayerInfo> playerList = connection.getListedOnlinePlayers();

        for (PlayerInfo entry : playerList) {
            Component displayName = entry.getTabListDisplayName();
            if (displayName == null) continue;

            String line = displayName.getString();
            String cleanLine = line.replaceAll("\\([!A-Z]-[a-z]\\)", "").trim();

            for (String ability : PICKAXE_ABILITIES) {
                if (cleanLine.contains(ability)) {
                    Matcher cooldownMatcher = COOLDOWN_PATTERN.matcher(cleanLine);
                    if (cooldownMatcher.find()) {
                        abilityName = cooldownMatcher.group(1).trim();
                        int cooldownSeconds = Integer.parseInt(cooldownMatcher.group(2));

                        if (cooldownSeconds != lastKnownCooldownSeconds) {
                            lastKnownCooldownSeconds = cooldownSeconds;
                            lastCooldownUpdateTime = currentTime;
                        }

                        isOnCooldown = true;
                        currentCooldown = cooldownSeconds + "s";
                        return;
                    }

                    Matcher readyMatcher = READY_PATTERN.matcher(cleanLine);
                    if (readyMatcher.find()) {
                        abilityName = readyMatcher.group(1).trim();
                        currentCooldown = "Ready";
                        isOnCooldown = false;
                        lastKnownCooldownSeconds = 0;
                        return;
                    }
                }
            }
        }
    }

    public static void render(GuiGraphicsExtractor ctx) {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Font font = client.font;

        String displayCooldown = currentCooldown;
        int interpolatedCooldown;

        if (isOnCooldown && lastCooldownUpdateTime > 0) {
            long elapsedMs = System.currentTimeMillis() - lastCooldownUpdateTime;
            int elapsedSeconds = (int) (elapsedMs / 1000);
            interpolatedCooldown = lastKnownCooldownSeconds - elapsedSeconds;

            if (interpolatedCooldown > 0 && interpolatedCooldown <= lastKnownCooldownSeconds) {
                displayCooldown = interpolatedCooldown + "s";

                if (titleEnabled && interpolatedCooldown <= titleThreshold) {
                    long currentTime = System.currentTimeMillis();

                    if (lastTitleCooldown != interpolatedCooldown || currentTime - lastTitleSetTime > 500) {
                        client.gui.setTimes(0, 15, 3);
                        client.gui.setTitle(Component.literal(""));
                        client.gui.setSubtitle(Component.literal("§6" + abilityName + ": §c§l" + interpolatedCooldown + "s"));
                        lastTitleSetTime = currentTime;
                        lastTitleCooldown = interpolatedCooldown;
                    }
                } else {
                    lastTitleCooldown = -1;
                }
            } else if (interpolatedCooldown <= 0) {
                displayCooldown = "Ready";
                lastTitleCooldown = -1;
            }
        }

        String displayText = displayCooldown.equals("Ready")
            ? "§a" + abilityName + ": §2✔ Ready"
            : "§6" + abilityName + ": §c" + displayCooldown;

        ctx.text(font, displayText, hudX, hudY, 0xFFFFFFFF, true);
    }

    public static double getCurrentCooldown() {
        return lastKnownCooldownSeconds;
    }

    public static double getInterpolatedCooldown() {
        if (!isOnCooldown || lastCooldownUpdateTime <= 0) {
            return 0;
        }
        long elapsedMs = System.currentTimeMillis() - lastCooldownUpdateTime;
        double elapsedSeconds = elapsedMs / 1000.0;
        double interpolated = lastKnownCooldownSeconds - elapsedSeconds;
        return Math.max(0, interpolated);
    }

    public static boolean isOnCooldown() {
        return isOnCooldown;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static int getX() {
        return hudX;
    }

    public static int getY() {
        return hudY;
    }

    public static void setPosition(int x, int y) {
        hudX = x;
        hudY = y;
    }

    public static float getScale() {
        return scale;
    }

    public static void setScale(float newScale) {
        scale = Math.max(0.5f, Math.min(3.0f, newScale));
    }

    public static int getWidth() {
        return (int) (100 * scale);
    }

    public static int getHeight() {
        return (int) (20 * scale);
    }

    public static boolean isTitleEnabled() {
        return titleEnabled;
    }

    public static void setTitleEnabled(boolean value) {
        titleEnabled = value;
    }

    public static int getTitleThreshold() {
        return titleThreshold;
    }

    public static void setTitleThreshold(int threshold) {
        titleThreshold = threshold;
    }
}
