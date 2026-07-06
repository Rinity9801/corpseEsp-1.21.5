package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return 0;

        // Try tab list first
        for (PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
            if (entry.getTabListDisplayName() != null) {
                String text = entry.getTabListDisplayName().getString().replaceAll("§.", "");
                Matcher m = COLD_PATTERN.matcher(text);
                if (m.find()) {
                    return Math.abs(Integer.parseInt(m.group(1)));
                }
            }
        }

        // Fallback: try scoreboard sidebar
        if (mc.level != null) {
            Scoreboard scoreboard = mc.level.getScoreboard();
            Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebar != null) {
                for (PlayerScoreEntry scoreEntry : scoreboard.listPlayerScores(sidebar)) {
                    String memberName = scoreEntry.owner();
                    PlayerTeam team = scoreboard.getPlayersTeam(memberName);
                    String display = team != null
                            ? team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()
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
