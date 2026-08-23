package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PickaxeCooldownHUD {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("miningqol", "pickaxe_cooldown_hud");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("(.+?):\\s+(\\d+)s");
    private static final Pattern READY_PATTERN = Pattern.compile("(.+?):\\s+(Available|Ready|✔)");
    private static final Pattern ABILITY_USED_PATTERN =
        Pattern.compile("You used your (.+?) Pickaxe Ability!");

    private static final String[] PICKAXE_ABILITIES = {
        "Pickobulus",
        "Mining Speed Boost",
        "Maniac Miner",
        "Sheer Force",
        "Vein Seeker",
        "Tunnel Vision",
        "Gemstone Fusion"
    };

    /** How long each ability stays active after it is used, in seconds. */
    private static final Map<String, Integer> ABILITY_DURATIONS = new LinkedHashMap<>();

    static {
        ABILITY_DURATIONS.put("Mining Speed Boost", 20);
        ABILITY_DURATIONS.put("Maniac Miner", 35);
        ABILITY_DURATIONS.put("Tunnel Vision", 30);
        ABILITY_DURATIONS.put("Gemstone Fusion", 30);
        ABILITY_DURATIONS.put("Sheer Force", 30);
    }

    private static boolean registered = false;
    private static boolean enabled = true;

    private static String currentCooldown = "Ready";
    private static String abilityName = "Pickaxe";
    private static long lastUpdate = 0;

    private static int lastKnownCooldownSeconds = 0;
    private static long lastCooldownUpdateTime = 0;
    private static boolean isOnCooldown = false;
    private static boolean customCooldownEnabled = false;
    private static int customCooldownSeconds = 120;

    private static int hudX = 10;
    private static int hudY = 50;
    private static float scale = 1.0f;
    private static final float[] cooldownLabelColor = {1.0f, 170.0f / 255.0f, 0.0f};
    private static final float[] cooldownValueColor = {1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
    private static final float[] readyLabelColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
    private static final float[] readyValueColor = {0.0f, 170.0f / 255.0f, 0.0f};
    private static final float[] activeLabelColor = {85.0f / 255.0f, 1.0f, 1.0f};
    private static final float[] activeValueColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};

    private static boolean cooldownOnly = false;
    private static boolean activeTimerEnabled = true;
    private static String activeAbility = "";
    private static long activeUntil = 0;

    private static boolean titleEnabled = true;
    private static int titleThreshold = 5;
    private static long lastTitleSetTime = 0;
    private static int lastTitleCooldown = -1;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            HUD_ID,
            (context, tickCounter) -> render(context)
        );
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.getConnection() == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (customCooldownEnabled) {
            if (isOnCooldown
                && currentTime - lastCooldownUpdateTime >= lastKnownCooldownSeconds * 1000L) {
                markReady();
            }
            return;
        }

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
                        markReady();
                        return;
                    }
                }
            }
        }
    }

    public static void onGameMessage(String message) {
        if (message == null) return;

        Matcher matcher = ABILITY_USED_PATTERN.matcher(message.trim());
        if (!matcher.find()) return;

        String used = matcher.group(1).trim();

        // Tracked whether or not the HUD shows it: other features ask whether an ability
        // is running, and that answer should not change with a display toggle.
        Integer duration = ABILITY_DURATIONS.get(used);
        if (duration != null) {
            activeAbility = used;
            activeUntil = System.currentTimeMillis() + duration * 1000L;
        }

        if (!customCooldownEnabled) return;

        abilityName = used;
        lastKnownCooldownSeconds = customCooldownSeconds;
        lastCooldownUpdateTime = System.currentTimeMillis();
        currentCooldown = customCooldownSeconds + "s";
        isOnCooldown = true;
        lastTitleCooldown = -1;
    }

    private static void markReady() {
        currentCooldown = "Ready";
        isOnCooldown = false;
        lastKnownCooldownSeconds = 0;
        lastTitleCooldown = -1;
    }

    public static void render(GuiGraphicsExtractor ctx) {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Font font = client.font;

        if (activeTimerEnabled) {
            long remainingActiveMs = activeUntil - System.currentTimeMillis();
            if (remainingActiveMs > 0) {
                int remaining = (int) Math.ceil(remainingActiveMs / 1000.0);
                ctx.text(
                    font,
                    formatText(activeAbility, remaining + "s", activeLabelColor, activeValueColor, false),
                    hudX,
                    hudY,
                    0xFFFFFFFF,
                    true
                );
                return;
            }
        }

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
                        client.gui.setSubtitle(formatText(
                            abilityName,
                            interpolatedCooldown + "s",
                            cooldownLabelColor,
                            cooldownValueColor,
                            true
                        ));
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

        boolean ready = displayCooldown.equals("Ready");
        ctx.text(
            font,
            formatText(
                abilityName,
                ready ? "✔ Ready" : displayCooldown,
                ready ? readyLabelColor : cooldownLabelColor,
                ready ? readyValueColor : cooldownValueColor,
                false
            ),
            hudX,
            hudY,
            0xFFFFFFFF,
            true
        );
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

    public static boolean isCustomCooldownEnabled() {
        return customCooldownEnabled;
    }

    public static void setCustomCooldownEnabled(boolean value) {
        if (customCooldownEnabled == value) return;
        customCooldownEnabled = value;
        lastCooldownUpdateTime = 0;
        markReady();
    }

    public static int getCustomCooldownSeconds() {
        return customCooldownSeconds;
    }

    public static void setCustomCooldownSeconds(int seconds) {
        customCooldownSeconds = Math.max(1, Math.min(600, seconds));
    }

    public static boolean isCooldownOnly() {
        return cooldownOnly;
    }

    public static void setCooldownOnly(boolean value) {
        cooldownOnly = value;
    }

    public static boolean isActiveTimerEnabled() {
        return activeTimerEnabled;
    }

    public static void setActiveTimerEnabled(boolean value) {
        activeTimerEnabled = value;
    }

    /** Seconds left on the current ability duration, or 0 when nothing is active. */
    public static int getActiveSecondsRemaining() {
        long remaining = activeUntil - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    public static boolean isAbilityActive() {
        return getActiveSecondsRemaining() > 0;
    }

    public static float[] getActiveLabelColor() {
        return activeLabelColor.clone();
    }

    public static void setActiveLabelColor(float red, float green, float blue) {
        setColor(activeLabelColor, red, green, blue);
    }

    public static float[] getActiveValueColor() {
        return activeValueColor.clone();
    }

    public static void setActiveValueColor(float red, float green, float blue) {
        setColor(activeValueColor, red, green, blue);
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
        return formatText("Pickobulus", "30s", cooldownLabelColor, cooldownValueColor, false);
    }

    private static Component formatText(String label, String value, float[] labelColor, float[] valueColor,
                                        boolean boldValue) {
        MutableComponent valueText = Component.literal(value).setStyle(
            Style.EMPTY.withColor(toRgb(valueColor)).withBold(boldValue)
        );
        if (cooldownOnly) {
            return valueText;
        }
        return Component.literal(label + ": ")
            .setStyle(Style.EMPTY.withColor(toRgb(labelColor)))
            .append(valueText);
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
}
