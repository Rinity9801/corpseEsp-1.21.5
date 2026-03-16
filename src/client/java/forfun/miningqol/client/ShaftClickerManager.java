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
    private static int locationCheckCooldown = 0;
    private static boolean inMineshaft = false;
    private static boolean wasInMineshaft = false;

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            paused = false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options != null) {
                client.options.attackKey.setPressed(false);
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        setEnabled(!enabled);
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
                client.options.attackKey.setPressed(false);
            }
        }

        if (paused) {
            // Resume when player switches back to mining slot
            int currentSlot = client.player.getInventory().getSelectedSlot();
            if (!inMineshaft && currentSlot == miningSlot) {
                paused = false;
            } else {
                client.options.attackKey.setPressed(false);
                return;
            }
        }

        // Auto click when on the mining slot
        int currentSlot = client.player.getInventory().getSelectedSlot();
        client.options.attackKey.setPressed(currentSlot == miningSlot);
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
