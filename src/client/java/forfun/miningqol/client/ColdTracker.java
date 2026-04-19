package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColdTracker {
    private static final Pattern COLD_PATTERN = Pattern.compile("Cold:\\s*(-?\\d+)");
    private static int currentCold = 0;
    private static int tickCounter = 0;

    public static void tick() {
        tickCounter++;
        if (tickCounter >= 10) { // Update every 10 ticks (0.5s)
            tickCounter = 0;
            currentCold = readCold();
        }
    }

    public static int getCold() {
        return currentCold;
    }

    private static int readCold() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return 0;

        // Try tab list first
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getDisplayName() != null) {
                String text = entry.getDisplayName().getString().replaceAll("§.", "");
                Matcher m = COLD_PATTERN.matcher(text);
                if (m.find()) {
                    return Math.abs(Integer.parseInt(m.group(1)));
                }
            }
        }

        // Fallback: try scoreboard sidebar
        if (mc.world != null) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (sidebar != null) {
                for (var scoreEntry : scoreboard.getScoreboardEntries(sidebar)) {
                    String memberName = scoreEntry.name().getString();
                    Team team = scoreboard.getScoreHolderTeam(memberName);
                    String display = team != null
                            ? team.getPrefix().getString() + team.getSuffix().getString()
                            : memberName;
                    String stripped = display.replaceAll("§.", "");
                    Matcher m = COLD_PATTERN.matcher(stripped);
                    if (m.find()) {
                        return Math.abs(Integer.parseInt(m.group(1)));
                    }
                }
            }
        }

        return 0;
    }
}
