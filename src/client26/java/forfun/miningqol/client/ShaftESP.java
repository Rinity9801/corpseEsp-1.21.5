package forfun.miningqol.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 26.1.2 port of ShaftESP. Entity scanning (littlefoot nametags + mineshaft mobs)
 * is unchanged; rendering goes through the immediate BufferSource at the tail of
 * LevelRenderer.renderLevel (see LevelRendererMixin) using
 * RenderTypes.textBackgroundSeeThrough() filled boxes — the same low-alpha filled
 * box approach the 1.21.11 branch used.
 */
public class ShaftESP {
    private static final int FULL_BRIGHT = 15728880;

    private static boolean isInMines = false;
    private static int locationCheckCooldown = 0;
    private static boolean littlefootEnabled = true;
    private static boolean littlefootTracer = true; // draw a line from the crosshair to each littlefoot
    private static boolean mobsEnabled = false;

    private static final float[] LITTLEFOOT_COLOR = {0.0f, 1.0f, 0.4f};
    private static final float LITTLEFOOT_ALPHA = 0.2f;
    private static float[] mobColor = {1.0f, 0.2f, 0.2f};
    private static float mobAlpha = 0.2f;

    // Entity ID -> is-littlefoot (colors resolved at render time so config changes apply live)
    private static final Map<Integer, Boolean> trackedEntities = new HashMap<>();
    // IDs of entities identified as littlefoot
    private static final Set<Integer> littlefootEntityIds = new HashSet<>();

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
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;

        if (level == null || client.player == null) {
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
        trackedEntities.keySet().removeIf(id -> level.getEntity(id) == null);
        littlefootEntityIds.removeIf(id -> level.getEntity(id) == null);

        // Identify littlefoot entities via nametag armor stands
        List<ArmorStand> nametagStands = level.getEntitiesOfClass(
            ArmorStand.class,
            client.player.getBoundingBox().inflate(800),
            stand -> stand.hasCustomName() && stand.isInvisible()
        );

        for (ArmorStand stand : nametagStands) {
            String name = stand.getCustomName().getString()
                .replaceAll("\u00A7.", "").trim().toLowerCase();

            if (name.contains("littlefoot")) {
                littlefootEntityIds.add(stand.getId());
            }
        }

        // Build tracked entities
        trackedEntities.clear();

        if (mobsEnabled) {
            // Track all living entities except the player
            List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                client.player.getBoundingBox().inflate(800),
                e -> !(e instanceof Player)
            );

            for (LivingEntity entity : entities) {
                if (entity instanceof ArmorStand stand) {
                    if (stand.isInvisible()) {
                        // Only include invisible armor stands if they're mob nametags (contain \u2764)
                        if (!stand.hasCustomName()) continue;
                        if (!stand.getCustomName().getString().contains("\u2764")) continue;
                        // Skip nametag if there's a real mob entity nearby (avoid double highlight)
                        AABB nearbyBox = new AABB(
                            stand.getX() - 1.5, stand.getY() - 3, stand.getZ() - 1.5,
                            stand.getX() + 1.5, stand.getY() + 1, stand.getZ() + 1.5
                        );
                        boolean hasMobBelow = !level.getEntitiesOfClass(
                            LivingEntity.class, nearbyBox,
                            e -> !(e instanceof ArmorStand) && !(e instanceof Player)
                        ).isEmpty();
                        if (hasMobBelow) continue;
                    } else {
                        // Skip visible armor stands without custom names and no base plate (corpses)
                        if (!stand.hasCustomName() && !stand.showBasePlate()) continue;
                    }
                }

                trackedEntities.put(entity.getId(), littlefootEntityIds.contains(entity.getId()));
            }
        } else if (littlefootEnabled) {
            for (int id : littlefootEntityIds) {
                trackedEntities.put(id, true);
            }
        }
    }

    public static void render(CameraRenderState cameraState, Matrix4fc viewMatrix) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || trackedEntities.isEmpty()) return;

        Vec3 cam = cameraState.pos;
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        Matrix4f pose = new Matrix4f(viewMatrix);

        // Filled see-through boxes with low alpha (same as the 1.21.11 branch).
        VertexConsumer quads = buffers.getBuffer(RenderTypes.textBackgroundSeeThrough());

        // A point just in front of the camera — the start of the littlefoot tracer line.
        Vector3f forward = new Vector3f(0, 0, -1);
        cameraState.orientation.transform(forward);
        Vec3 lineStart = cam.add(forward.x, forward.y, forward.z);

        for (Map.Entry<Integer, Boolean> entry : trackedEntities.entrySet()) {
            Entity entity = client.level.getEntity(entry.getKey());
            if (entity == null) continue;

            boolean littlefoot = entry.getValue();
            float[] color = littlefoot ? LITTLEFOOT_COLOR : mobColor;
            float alpha = littlefoot ? LITTLEFOOT_ALPHA : mobAlpha;
            double minX, minY, minZ, maxX, maxY, maxZ;

            if (entity instanceof ArmorStand stand && stand.isMarker()) {
                minX = entity.getX() - cam.x - 0.4;
                minY = entity.getY() - cam.y - 1.8;
                minZ = entity.getZ() - cam.z - 0.4;
                maxX = entity.getX() - cam.x + 0.4;
                maxY = entity.getY() - cam.y + 0.2;
                maxZ = entity.getZ() - cam.z + 0.4;
            } else {
                AABB box = entity.getBoundingBox();
                minX = box.minX - cam.x;
                minY = box.minY - cam.y;
                minZ = box.minZ - cam.z;
                maxX = box.maxX - cam.x;
                maxY = box.maxY - cam.y;
                maxZ = box.maxZ - cam.z;
            }

            box(quads, pose,
                (float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) maxY, (float) maxZ,
                color[0], color[1], color[2], alpha);

            // Tracer line from the crosshair to the littlefoot box centre (like ordered waypoints).
            if (littlefoot && littlefootTracer) {
                Vec3 target = new Vec3(
                    (minX + maxX) / 2 + cam.x,
                    (minY + maxY) / 2 + cam.y,
                    (minZ + maxZ) / 2 + cam.z);
                line(buffers, pose, cam, lineStart, target, color, 1.0f);
            }
        }

        buffers.endBatch();
    }

    private static void box(VertexConsumer buffer, Matrix4f pose,
                            float minX, float minY, float minZ,
                            float maxX, float maxY, float maxZ,
                            float red, float green, float blue, float alpha) {
        int r = (int) (red * 255);
        int g = (int) (green * 255);
        int b = (int) (blue * 255);
        int a = (int) (alpha * 255);

        // Bottom
        quad(buffer, pose, r, g, b, a,
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        // Top
        quad(buffer, pose, r, g, b, a,
            minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        // North
        quad(buffer, pose, r, g, b, a,
            minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
        // South
        quad(buffer, pose, r, g, b, a,
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        // West
        quad(buffer, pose, r, g, b, a,
            minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
        // East
        quad(buffer, pose, r, g, b, a,
            maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose, int r, int g, int b, int a,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a).setLight(FULL_BRIGHT);
    }

    private static void line(MultiBufferSource.BufferSource buffers, Matrix4f pose, Vec3 camPos,
                             Vec3 from, Vec3 to, float[] color, float alpha) {
        VertexConsumer buffer = buffers.getBuffer(RenderTypes.lines());

        float startX = (float) (from.x - camPos.x);
        float startY = (float) (from.y - camPos.y);
        float startZ = (float) (from.z - camPos.z);
        float endX = (float) (to.x - camPos.x);
        float endY = (float) (to.y - camPos.y);
        float endZ = (float) (to.z - camPos.z);

        Vector3f normal = new Vector3f(endX - startX, endY - startY, endZ - startZ);
        if (normal.lengthSquared() < 1.0e-6f) {
            normal.set(0, 1, 0);
        } else {
            normal.normalize();
        }

        buffer.addVertex(pose, startX, startY, startZ)
            .setColor(color[0], color[1], color[2], alpha)
            .setNormal(normal.x, normal.y, normal.z)
            .setLineWidth(2.0f);
        buffer.addVertex(pose, endX, endY, endZ)
            .setColor(color[0], color[1], color[2], alpha)
            .setNormal(normal.x, normal.y, normal.z)
            .setLineWidth(2.0f);
    }

    public static void onWorldUnload() {
        trackedEntities.clear();
        littlefootEntityIds.clear();
        isInMines = false;
    }

    public static boolean isLittlefootTracer() {
        return littlefootTracer;
    }

    public static void setLittlefootTracer(boolean value) {
        littlefootTracer = value;
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

    public static float[] getMobColor() { return mobColor; }
    public static void setMobColor(float r, float g, float b) { mobColor = new float[]{r, g, b}; }
    public static float getMobAlpha() { return mobAlpha; }
    public static void setMobAlpha(float alpha) { mobAlpha = alpha; }

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
