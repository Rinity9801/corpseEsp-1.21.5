package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class LobbyFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger("LobbyFinder");
    private static final Set<BlockPos> trackedBlocks = new HashSet<>();
    private static boolean lobbyAvailable = true;
    private static long displayUntil = 0;
    private static final long DISPLAY_DURATION = 3000; // 3 seconds
    private static String lastWorldId = "";

    public static void addBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§cNo block in crosshair!"), false);
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        if (trackedBlocks.add(pos)) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§aAdded block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
            LOGGER.info("[LobbyFinder] Added block at {}", pos);
        } else {
            client.player.sendMessage(net.minecraft.text.Text.literal("§eBlock already tracked!"), false);
        }
    }

    public static void removeBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§cNo block in crosshair!"), false);
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        if (trackedBlocks.remove(pos)) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§aRemoved block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
            LOGGER.info("[LobbyFinder] Removed block at {}", pos);
        } else {
            client.player.sendMessage(net.minecraft.text.Text.literal("§eBlock not tracked!"), false);
        }
    }

    public static void clearAll() {
        trackedBlocks.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§aCleared all tracked blocks!"), false);
        }
        LOGGER.info("[LobbyFinder] Cleared all tracked blocks");
    }

    public static void listBlocks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (trackedBlocks.isEmpty()) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§eNo blocks tracked!"), false);
            return;
        }

        client.player.sendMessage(net.minecraft.text.Text.literal("§6Tracked blocks:"), false);
        for (BlockPos pos : trackedBlocks) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§7- " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
        }
    }

    public static void onWorldChange() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || trackedBlocks.isEmpty()) {
            return;
        }

        // Get world ID to detect world changes
        String currentWorldId = client.world.getRegistryKey().getValue().toString();

        // Only check if we actually changed worlds
        if (currentWorldId.equals(lastWorldId)) {
            return;
        }

        lastWorldId = currentWorldId;

        // Check if we're in the right location
        if (!isInGlaciteTunnels()) {
            return;
        }

        LOGGER.info("[LobbyFinder] World changed, scheduling block check in 1 second...");

        // Schedule block check after 1 second delay to let world load
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Wait 1 second
                client.execute(() -> {
                    if (client.world == null) return;

                    // Check all tracked blocks
                    boolean allAir = true;
                    for (BlockPos pos : trackedBlocks) {
                        if (!client.world.getBlockState(pos).isAir()) {
                            allAir = false;
                            break;
                        }
                    }

                    lobbyAvailable = !allAir;

                    if (!lobbyAvailable) {
                        displayUntil = System.currentTimeMillis() + DISPLAY_DURATION;
                        LOGGER.info("[LobbyFinder] Lobby unavailable - tracked blocks are air");
                    } else {
                        LOGGER.info("[LobbyFinder] Lobby available - blocks found");
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.error("[LobbyFinder] Failed to check lobby", e);
            }
        }).start();
    }

    public static void tick() {
        // Reset display after duration
        if (System.currentTimeMillis() > displayUntil) {
            lobbyAvailable = true;
        }
    }

    private static boolean isInGlaciteTunnels() {
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

                        if (cleanLine.contains("Glacite Tunnels") || cleanLine.contains("Dwarven Base Camp")) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static boolean shouldDisplayUnavailable() {
        return !lobbyAvailable && System.currentTimeMillis() < displayUntil;
    }

    public static Set<BlockPos> getTrackedBlocks() {
        return new HashSet<>(trackedBlocks);
    }

    public static void setTrackedBlocks(Set<BlockPos> blocks) {
        trackedBlocks.clear();
        trackedBlocks.addAll(blocks);
    }
}
