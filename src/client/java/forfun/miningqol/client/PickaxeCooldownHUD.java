package forfun.miningqol.client;

import forfun.miningqol.client.config.MiningConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PickaxeCooldownHUD {
    private static final Identifier HUD_ID = Identifier.of("miningqol", "pickaxe_cooldown_hud");
    private static final Logger LOGGER = LoggerFactory.getLogger("PickaxeCooldownHUD");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("(.+?):\\s+(\\d+)s");
    private static final Pattern READY_PATTERN = Pattern.compile("(.+?):\\s+(Available|Ready|✔)");

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
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            HUD_ID,
            (context, tickCounter) -> render(context)
        );
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < 500) return;
        lastUpdate = currentTime;

        Collection<PlayerListEntry> playerList = client.player.networkHandler.getPlayerList();

        String[] pickaxeAbilities = {
            "Pickobulus",
            "Mining Speed Boost",
            "Maniac Miner",
            "Sheer Force",
            "Vein Seeker"
        };

        for (PlayerListEntry entry : playerList) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            String line = displayName.getString();
            String cleanLine = line.replaceAll("\\([!A-Z]-[a-z]\\)", "").trim();

            for (String ability : pickaxeAbilities) {
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

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        MiningConfig config = MiningqolClient.getConfig();
        if (!config.pickaxeCooldownEnabled) return;

        TextRenderer textRenderer = client.textRenderer;

        String displayCooldown = currentCooldown;
        int interpolatedCooldown = 0;

        if (isOnCooldown && lastCooldownUpdateTime > 0) {
            long elapsedMs = System.currentTimeMillis() - lastCooldownUpdateTime;
            int elapsedSeconds = (int) (elapsedMs / 1000);
            interpolatedCooldown = lastKnownCooldownSeconds - elapsedSeconds;

            if (interpolatedCooldown > 0 && interpolatedCooldown <= lastKnownCooldownSeconds) {
                displayCooldown = interpolatedCooldown + "s";


                if (titleEnabled && interpolatedCooldown <= titleThreshold && interpolatedCooldown > 0) {
                    long currentTime = System.currentTimeMillis();



                    if (lastTitleCooldown != interpolatedCooldown || currentTime - lastTitleSetTime > 500) {
                        client.inGameHud.setTitleTicks(0, 15, 3);
                        client.inGameHud.setTitle(Text.literal(""));
                        client.inGameHud.setSubtitle(Text.literal("§6" + abilityName + ": §c§l" + interpolatedCooldown + "s"));
                        lastTitleSetTime = currentTime;
                        lastTitleCooldown = interpolatedCooldown;
                    }
                } else if (interpolatedCooldown > titleThreshold || interpolatedCooldown <= 0) {

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


        context.drawTextWithShadow(textRenderer, displayText, hudX, hudY, 0xFFFFFFFF);
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
        return MiningqolClient.getConfig().pickaxeCooldownEnabled;
    }

    public static void setEnabled(boolean enabled) {
        MiningqolClient.getConfig().pickaxeCooldownEnabled = enabled;
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
        return (int)(100 * scale);
    }

    public static int getHeight() {
        return (int)(20 * scale);
    }

    public static boolean isTitleEnabled() {
        return titleEnabled;
    }

    public static void setTitleEnabled(boolean enabled) {
        titleEnabled = enabled;
    }

    public static int getTitleThreshold() {
        return titleThreshold;
    }

    public static void setTitleThreshold(int threshold) {
        titleThreshold = threshold;
    }
}
