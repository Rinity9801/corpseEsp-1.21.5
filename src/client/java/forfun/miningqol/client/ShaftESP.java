package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaftESP {
    private static boolean isInMines = false;
    private static int locationCheckCooldown = 0;
    private static boolean enabled = true;

    private static final float[] LITTLEFOOT_COLOR = {0.0f, 1.0f, 0.4f};

    // Entity ID -> color, tracks live entities
    private static final Map<Integer, float[]> trackedEntities = new HashMap<>();

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
            return;
        }

        boolean wasInMines = isInMines;
        locationCheckCooldown--;
        if (locationCheckCooldown <= 0) {
            isInMines = checkIfInMineshaft();
            locationCheckCooldown = 20;
        }

        if (!enabled) {
            trackedEntities.clear();
            return;
        }

        // Clear targets when leaving mineshaft
        if (wasInMines && !isInMines) {
            trackedEntities.clear();
        }

        if (!isInMines) return;

        // Remove entities no longer in the world
        trackedEntities.keySet().removeIf(id -> world.getEntityById(id) == null);

        // Scan for new matching armor stands
        List<ArmorStandEntity> stands = world.getEntitiesByClass(
            ArmorStandEntity.class,
            client.player.getBoundingBox().expand(800),
            stand -> stand.hasCustomName() && stand.isInvisible()
        );

        for (ArmorStandEntity stand : stands) {
            if (trackedEntities.containsKey(stand.getId())) continue;

            String name = stand.getCustomName().getString()
                .replaceAll("\u00A7.", "").trim().toLowerCase();

            if (name.contains("littlefoot")) {
                trackedEntities.put(stand.getId(), LITTLEFOOT_COLOR);
            }
        }
    }

    public static void render(MatrixStack matrices, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Vec3d cam = camera.getPos();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (Map.Entry<Integer, float[]> entry : trackedEntities.entrySet()) {
            Entity entity = client.world.getEntityById(entry.getKey());
            if (entity == null) continue;

            float[] color = entry.getValue();

            double x = entity.getX() - cam.x;
            double y = entity.getY() - cam.y;
            double z = entity.getZ() - cam.z;

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);

            // Player-sized box: 0.6 wide, 1.8 tall
            DebugRenderer.drawBox(matrices, immediate,
                x - 0.3, y, z - 0.3,
                x + 0.3, y + 1.8, z + 0.3,
                color[0], color[1], color[2], 0.4f);

            immediate.draw();

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    public static void onWorldUnload() {
        trackedEntities.clear();
        isInMines = false;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
