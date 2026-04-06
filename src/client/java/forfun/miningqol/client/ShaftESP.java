package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
//? if is1_21_11 {
import forfun.miningqol.client.utils.RenderHelper;
//?} else {
/*import net.minecraft.client.render.debug.DebugRenderer;
*///?}
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class ShaftESP {
    private static boolean isInMines = false;
    private static int locationCheckCooldown = 0;
    private static boolean littlefootEnabled = true;
    private static boolean mobsEnabled = false;

    private static final float[] LITTLEFOOT_COLOR = {0.0f, 1.0f, 0.4f};
    private static final float[] MOB_COLOR = {1.0f, 0.6f, 0.2f};

    // Entity ID -> color for rendering
    private static final Map<Integer, float[]> trackedEntities = new HashMap<>();
    // IDs of entities identified as littlefoot (the actual mob, not the nametag armor stand)
    private static final Set<Integer> littlefootEntityIds = new HashSet<>();

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
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;

        if (world == null || client.player == null) {
            trackedEntities.clear();
            littlefootEntityIds.clear();
            return;
        }

        boolean wasInMines = isInMines;
        locationCheckCooldown--;
        if (locationCheckCooldown <= 0) {
            isInMines = checkIfInMineshaft();
            locationCheckCooldown = 20;
        }

        if (!littlefootEnabled && !mobsEnabled) {
            trackedEntities.clear();
            littlefootEntityIds.clear();
            return;
        }

        // Clear targets when leaving mineshaft
        if (wasInMines && !isInMines) {
            trackedEntities.clear();
            littlefootEntityIds.clear();
        }

        if (!isInMines) return;

        // Remove stale entity IDs
        trackedEntities.keySet().removeIf(id -> world.getEntityById(id) == null);
        littlefootEntityIds.removeIf(id -> world.getEntityById(id) == null);

        // Scan for littlefoot nametag armor stands and find the actual mob below
        List<ArmorStandEntity> stands = world.getEntitiesByClass(
            ArmorStandEntity.class,
            client.player.getBoundingBox().expand(800),
            stand -> stand.hasCustomName() && stand.isInvisible()
        );

        for (ArmorStandEntity stand : stands) {
            String name = stand.getCustomName().getString()
                .replaceAll("\u00A7.", "").trim().toLowerCase();

            if (name.contains("littlefoot")) {
                // Search for the actual mob entity below the nametag armor stand
                Box searchBox = new Box(
                    stand.getX() - 1.5, stand.getY() - 4, stand.getZ() - 1.5,
                    stand.getX() + 1.5, stand.getY() + 0.5, stand.getZ() + 1.5
                );

                List<LivingEntity> nearbyMobs = world.getEntitiesByClass(
                    LivingEntity.class, searchBox,
                    e -> !(e instanceof ArmorStandEntity) && !(e instanceof PlayerEntity)
                );

                LivingEntity closest = null;
                double closestDist = Double.MAX_VALUE;
                for (LivingEntity mob : nearbyMobs) {
                    double dist = mob.squaredDistanceTo(stand);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = mob;
                    }
                }

                if (closest != null) {
                    littlefootEntityIds.add(closest.getId());
                } else {
                    // Fallback: track the armor stand itself if no mob found nearby
                    littlefootEntityIds.add(stand.getId());
                }
            }
        }

        // Build tracked entities map based on toggles
        trackedEntities.clear();

        if (mobsEnabled) {
            // Track all living entities that aren't players or armor stands
            List<LivingEntity> mobs = world.getEntitiesByClass(
                LivingEntity.class,
                client.player.getBoundingBox().expand(800),
                e -> !(e instanceof ArmorStandEntity) && !(e instanceof PlayerEntity)
            );

            for (LivingEntity mob : mobs) {
                float[] color = littlefootEntityIds.contains(mob.getId()) ? LITTLEFOOT_COLOR : MOB_COLOR;
                trackedEntities.put(mob.getId(), color);
            }
        } else if (littlefootEnabled) {
            // Only track littlefoot entities
            for (int id : littlefootEntityIds) {
                trackedEntities.put(id, LITTLEFOOT_COLOR);
            }
        }
    }

    public static void render(MatrixStack matrices, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        //? if is1_21_11 {
        Vec3d cam = camera.getCameraPos();
        //?} else {
        /*Vec3d cam = camera.getPos();
        *///?}
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (Map.Entry<Integer, float[]> entry : trackedEntities.entrySet()) {
            Entity entity = client.world.getEntityById(entry.getKey());
            if (entity == null) continue;

            float[] color = entry.getValue();
            Box box = entity.getBoundingBox();

            double minX = box.minX - cam.x;
            double minY = box.minY - cam.y;
            double minZ = box.minZ - cam.z;
            double maxX = box.maxX - cam.x;
            double maxY = box.maxY - cam.y;
            double maxZ = box.maxZ - cam.z;

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);

            //? if is1_21_11 {
            RenderHelper.drawBox(matrices, immediate,
                minX, minY, minZ, maxX, maxY, maxZ,
                color[0], color[1], color[2], 0.4f);
            //?} else {
            /*DebugRenderer.drawBox(matrices, immediate,
                minX, minY, minZ, maxX, maxY, maxZ,
                color[0], color[1], color[2], 0.4f);
            *///?}

            immediate.draw();

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    public static void onWorldUnload() {
        trackedEntities.clear();
        littlefootEntityIds.clear();
        isInMines = false;
    }

    public static void setLittlefootEnabled(boolean value) {
        littlefootEnabled = value;
    }

    public static boolean isLittlefootEnabled() {
        return littlefootEnabled;
    }

    public static void setMobsEnabled(boolean value) {
        mobsEnabled = value;
    }

    public static boolean isMobsEnabled() {
        return mobsEnabled;
    }

    // Legacy compat - maps to littlefoot toggle
    public static void setEnabled(boolean value) {
        littlefootEnabled = value;
    }

    public static boolean isEnabled() {
        return littlefootEnabled;
    }
}
