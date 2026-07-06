package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;

public class ShaftClickerManager {
    private static boolean enabled = false;
    private static boolean showToggleMessage = true;
    private static int miningSlot = 0;
    private static boolean paused = false;
    private static boolean abilityPaused = false; // Paused because ability is ready
    private static int locationCheckCooldown = 0;
    private static boolean inMineshaft = false;
    private static boolean wasInMineshaft = false;
    private static boolean wasAutoClicking = false; // Track if we were holding attack key

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            paused = false;
            if (wasAutoClicking) {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.options != null) {
                    client.options.keyAttack.setDown(false);
                }
                wasAutoClicking = false;
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        setEnabled(!enabled);
        if (!showToggleMessage) return;
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(
                enabled ? "\u00A76[MQO] \u00A7aShaft Clicker enabled" : "\u00A76[MQO] \u00A7cShaft Clicker disabled"));
        }
    }

    public static void setShowToggleMessage(boolean value) {
        showToggleMessage = value;
    }

    public static boolean isShowToggleMessage() {
        return showToggleMessage;
    }

    public static void setMiningSlot(int slot) {
        miningSlot = slot;
    }

    public static int getMiningSlot() {
        return miningSlot;
    }

    public static boolean isPaused() {
        return paused;
    }

    private static boolean checkIfInMineshaft() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebarObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

        if (sidebarObjective != null) {
            String objectiveName = sidebarObjective.getDisplayName().getString();
            if (objectiveName.contains("SKYBLOCK") || objectiveName.contains("SKY BLOCK")) {
                Collection<PlayerTeam> teams = scoreboard.getPlayerTeams();
                for (PlayerTeam team : teams) {
                    for (String member : team.getPlayers()) {
                        String line = team.getPlayerPrefix().getString() + member + team.getPlayerSuffix().getString();
                        String cleanLine = line.replaceAll("\u00A7.", "").trim();

                        if (cleanLine.contains("Mineshaft")) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static void tick() {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        // Check location periodically
        locationCheckCooldown--;
        if (locationCheckCooldown <= 0) {
            wasInMineshaft = inMineshaft;
            inMineshaft = checkIfInMineshaft();
            locationCheckCooldown = 20;

            if (inMineshaft && !wasInMineshaft) {
                // Just entered mineshaft - pause clicking
                paused = true;
                if (wasAutoClicking) {
                    client.options.keyAttack.setDown(false);
                    wasAutoClicking = false;
                }
            }
        }

        // Pause when ability is ready
        if (!paused && !abilityPaused && !PickaxeCooldownHUD.isOnCooldown()) {
            abilityPaused = true;
            if (wasAutoClicking) {
                client.options.keyAttack.setDown(false);
                wasAutoClicking = false;
            }
        }

        int currentSlot = client.player.getInventory().getSelectedSlot();

        if (paused || abilityPaused) {
            // Resume when player switches back to mining slot
            // (they swapped away to use ability or handle mineshaft, then came back)
            if (currentSlot == miningSlot && (abilityPaused ? PickaxeCooldownHUD.isOnCooldown() : !inMineshaft)) {
                paused = false;
                abilityPaused = false;
            } else {
                // Only release the key once to avoid blocking normal clicks
                if (wasAutoClicking) {
                    client.options.keyAttack.setDown(false);
                    wasAutoClicking = false;
                }
                return;
            }
        }

        // Auto click when on the mining slot, but don't force-release when off it
        if (currentSlot == miningSlot) {
            client.options.keyAttack.setDown(true);
            wasAutoClicking = true;
        } else if (wasAutoClicking) {
            client.options.keyAttack.setDown(false);
            wasAutoClicking = false;
        }
    }

    public static void cleanup() {
        if (enabled) {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.options.keyAttack.setDown(false);
            }
        }
    }
}
