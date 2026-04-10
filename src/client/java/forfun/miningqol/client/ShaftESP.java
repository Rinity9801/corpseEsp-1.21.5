package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
//? if is1_21_11 {
import forfun.miningqol.client.utils.RenderHelper;
import net.minecraft.client.render.RenderLayers;
//?} else {
/*import net.minecraft.client.render.RenderLayer;
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
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class ShaftESP {
    private static boolean isInMines = false;
    private static int locationCheckCooldown = 0;
    private static boolean littlefootEnabled = true;
    private static boolean mobsEnabled = false;

    private static final float[] LITTLEFOOT_COLOR = {0.0f, 1.0f, 0.4f};
    private static final float[] MOB_COLOR = {1.0f, 0.2f, 0.2f};

    // Entity ID -> color for rendering
    private static final Map<Integer, float[]> trackedEntities = new HashMap<>();
    // IDs of entities identified as littlefoot
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

        // Identify littlefoot entities via nametag armor stands
        List<ArmorStandEntity> nametagStands = world.getEntitiesByClass(
            ArmorStandEntity.class,
            client.player.getBoundingBox().expand(800),
            stand -> stand.hasCustomName() && stand.isInvisible()
        );

        for (ArmorStandEntity stand : nametagStands) {
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
            List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                client.player.getBoundingBox().expand(800),
                e -> !(e instanceof PlayerEntity)
            );

            for (LivingEntity entity : entities) {
                if (entity instanceof ArmorStandEntity stand) {
                    if (stand.isInvisible()) {
                        // Only include invisible armor stands if they're mob nametags (contain ❤)
                        if (!stand.hasCustomName()) continue;
                        if (!stand.getCustomName().getString().contains("\u2764")) continue;
                        // Skip nametag if there's a real mob entity nearby (avoid double highlight)
                        Box nearbyBox = new Box(
                            stand.getX() - 1.5, stand.getY() - 3, stand.getZ() - 1.5,
                            stand.getX() + 1.5, stand.getY() + 1, stand.getZ() + 1.5
                        );
                        boolean hasMobBelow = !world.getEntitiesByClass(
                            LivingEntity.class, nearbyBox,
                            e -> !(e instanceof ArmorStandEntity) && !(e instanceof PlayerEntity)
                        ).isEmpty();
                        if (hasMobBelow) continue;
                    } else {
                        // Skip visible armor stands without custom names and no base plate (corpses)
                        if (!stand.hasCustomName() && !stand.shouldShowBasePlate()) continue;
                    }
                }

                float[] color = littlefootEntityIds.contains(entity.getId()) ? LITTLEFOOT_COLOR : MOB_COLOR;
                trackedEntities.put(entity.getId(), color);
            }
        } else if (littlefootEnabled) {
            for (int id : littlefootEntityIds) {
                trackedEntities.put(id, LITTLEFOOT_COLOR);
            }
        }
    }

    public static void render(MatrixStack matrices, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || trackedEntities.isEmpty()) return;

        //? if is1_21_11 {
        Vec3d cam = camera.getCameraPos();
        //?} else {
        /*Vec3d cam = camera.getPos();
        *///?}
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);

        //? if is1_21_11 {
        // 1.21.11: RenderLayers.lines() requires LineWidth vertex element,
        // use filled box with low alpha instead
        for (Map.Entry<Integer, float[]> entry : trackedEntities.entrySet()) {
            Entity entity = client.world.getEntityById(entry.getKey());
            if (entity == null) continue;

            float[] color = entry.getValue();
            double minX, minY, minZ, maxX, maxY, maxZ;

            if (entity instanceof ArmorStandEntity stand && stand.isMarker()) {
                minX = entity.getX() - cam.x - 0.4;
                minY = entity.getY() - cam.y - 1.8;
                minZ = entity.getZ() - cam.z - 0.4;
                maxX = entity.getX() - cam.x + 0.4;
                maxY = entity.getY() - cam.y + 0.2;
                maxZ = entity.getZ() - cam.z + 0.4;
            } else {
                Box box = entity.getBoundingBox();
                minX = box.minX - cam.x;
                minY = box.minY - cam.y;
                minZ = box.minZ - cam.z;
                maxX = box.maxX - cam.x;
                maxY = box.maxY - cam.y;
                maxZ = box.maxZ - cam.z;
            }

            RenderHelper.drawBox(matrices, immediate,
                minX, minY, minZ, maxX, maxY, maxZ,
                color[0], color[1], color[2], 0.2f);
            immediate.draw();
        }
        //?} else {
        /*
        VertexConsumer lineBuffer = immediate.getBuffer(RenderLayer.getLines());
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        boolean drewAny = false;
        for (Map.Entry<Integer, float[]> entry : trackedEntities.entrySet()) {
            Entity entity = client.world.getEntityById(entry.getKey());
            if (entity == null) continue;

            float[] color = entry.getValue();
            float x1, y1, z1, x2, y2, z2;

            if (entity instanceof ArmorStandEntity stand && stand.isMarker()) {
                double cx = entity.getX() - cam.x;
                double cy = entity.getY() - cam.y;
                double cz = entity.getZ() - cam.z;
                x1 = (float) (cx - 0.4);
                y1 = (float) (cy - 1.8);
                z1 = (float) (cz - 0.4);
                x2 = (float) (cx + 0.4);
                y2 = (float) (cy + 0.2);
                z2 = (float) (cz + 0.4);
            } else {
                Box box = entity.getBoundingBox();
                x1 = (float) (box.minX - cam.x);
                y1 = (float) (box.minY - cam.y);
                z1 = (float) (box.minZ - cam.z);
                x2 = (float) (box.maxX - cam.x);
                y2 = (float) (box.maxY - cam.y);
                z2 = (float) (box.maxZ - cam.z);
            }

            float r = color[0], g = color[1], b = color[2], a = 1.0f;
            drewAny = true;

            drawEdge(lineBuffer, posMatrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x1, y1, z2, x1, y1, z1, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x1, y2, z2, x1, y2, z1, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
            drawEdge(lineBuffer, posMatrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
        }

        if (drewAny) {
            immediate.draw(RenderLayer.getLines());
        }
        *///?}

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    //? if !is1_21_11 {
    /*private static void drawEdge(VertexConsumer buffer, Matrix4f posMatrix,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float r, float g, float b, float a) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) {
            nx /= len;
            ny /= len;
            nz /= len;
        } else {
            nx = 0;
            ny = 1;
            nz = 0;
        }

        buffer.vertex(posMatrix, x1, y1, z1).color(r, g, b, a).normal(nx, ny, nz);
        buffer.vertex(posMatrix, x2, y2, z2).color(r, g, b, a).normal(nx, ny, nz);
    }
    *///?}

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
