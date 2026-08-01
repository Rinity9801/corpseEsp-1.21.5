package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            MqoChat.reply(Component.literal("§cNo block in crosshair!"));
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        if (trackedBlocks.add(pos)) {
            MqoChat.reply(Component.literal("§aAdded block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            LOGGER.info("[LobbyFinder] Added block at {}", pos);
        } else {
            MqoChat.reply(Component.literal("§eBlock already tracked!"));
        }
    }

    public static void removeBlock() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            MqoChat.reply(Component.literal("§cNo block in crosshair!"));
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        if (trackedBlocks.remove(pos)) {
            MqoChat.reply(Component.literal("§aRemoved block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            LOGGER.info("[LobbyFinder] Removed block at {}", pos);
        } else {
            MqoChat.reply(Component.literal("§eBlock not tracked!"));
        }
    }

    public static void clearAll() {
        trackedBlocks.clear();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            MqoChat.reply(Component.literal("§aCleared all tracked blocks!"));
        }
        LOGGER.info("[LobbyFinder] Cleared all tracked blocks");
    }

    public static void listBlocks() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (trackedBlocks.isEmpty()) {
            MqoChat.reply(Component.literal("§eNo blocks tracked!"));
            return;
        }

        MqoChat.reply(Component.literal("§6Tracked blocks:"));
        for (BlockPos pos : trackedBlocks) {
            MqoChat.reply(Component.literal("§7- " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
        }
    }

    public static void onWorldChange() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || trackedBlocks.isEmpty()) {
            return;
        }

        // Get world ID to detect world changes
        String currentWorldId = client.level.dimension().identifier().toString();

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
                client.schedule(() -> {
                    if (client.level == null) return;

                    // Check all tracked blocks
                    boolean allAir = true;
                    for (BlockPos pos : trackedBlocks) {
                        if (!client.level.getBlockState(pos).isAir()) {
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
