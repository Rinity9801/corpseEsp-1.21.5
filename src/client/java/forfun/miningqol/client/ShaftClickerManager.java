package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.Collection;

public class ShaftClickerManager {
    private static boolean enabled = false;
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
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.options != null) {
                    client.options.attackKey.setPressed(false);
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(
                enabled ? "\u00A76[MQO] \u00A7aShaft Clicker enabled" : "\u00A76[MQO] \u00A7cShaft Clicker disabled"), false);
        }
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective sidebarObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (sidebarObjective != null) {
            String objectiveName = sidebarObjective.getDisplayName().getString();
            if (objectiveName.contains("SKYBLOCK") || objectiveName.contains("SKY BLOCK")) {
                Collection<Team> teams = scoreboard.getTeams();
                for (Team team : teams) {
                    for (String member : team.getPlayerList()) {
                        String line = team.getPrefix().getString() + member + team.getSuffix().getString();
                        String cleanLine = line.replaceAll("§.", "").trim();

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

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

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
                    client.options.attackKey.setPressed(false);
                    wasAutoClicking = false;
                }
            }
        }

        // Pause when ability is ready
        if (!paused && !abilityPaused && !PickaxeCooldownHUD.isOnCooldown()) {
            abilityPaused = true;
            if (wasAutoClicking) {
                client.options.attackKey.setPressed(false);
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
                    client.options.attackKey.setPressed(false);
                    wasAutoClicking = false;
                }
                return;
            }
        }

        // Auto click when on the mining slot, but don't force-release when off it
        if (currentSlot == miningSlot) {
            client.options.attackKey.setPressed(true);
            wasAutoClicking = true;
        } else if (wasAutoClicking) {
            client.options.attackKey.setPressed(false);
            wasAutoClicking = false;
        }
    }

    public static void cleanup() {
        if (enabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.options.attackKey.setPressed(false);
            }
        }
    }
}
