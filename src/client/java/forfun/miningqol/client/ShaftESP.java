package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ShaftESP {
    private static boolean isInMines = false;
    private static int locationCheckCooldown = 0;
    private static boolean enabled = true;

    private static final float[] LITTLEFOOT_COLOR = {0.0f, 1.0f, 0.4f};
    private static final float[] BAZAAR_COLOR = {1.0f, 0.85f, 0.0f};

    // Entity ID -> color and type, tracks live entities
    private static final Map<Integer, ESPTarget> trackedEntities = new HashMap<>();

    // Test entity
    private static ArmorStandEntity testEntity = null;
    private static Vec3d testOrigin = null;
    private static int testTicks = 0;

    private static class ESPTarget {
        final float[] color;
        final boolean isLittlefoot;

        ESPTarget(float[] color, boolean isLittlefoot) {
            this.color = color;
            this.isLittlefoot = isLittlefoot;
        }
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

        // Clear littlefoot targets when leaving mineshaft
        if (wasInMines && !isInMines) {
            trackedEntities.values().removeIf(t -> t.isLittlefoot);
        }

        // Move test entity
        tickTestEntity();

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

            float[] color = null;
            boolean littlefoot = false;
            if (name.contains("bazaar")) {
                color = BAZAAR_COLOR;
            } else if (isInMines && name.contains("littlefoot")) {
                color = LITTLEFOOT_COLOR;
                littlefoot = true;
            }

            if (color != null) {
                trackedEntities.put(stand.getId(), new ESPTarget(color, littlefoot));
            }
        }
    }

    public static void render(MatrixStack matrices, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Vec3d cam = camera.getPos();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (Map.Entry<Integer, ESPTarget> entry : trackedEntities.entrySet()) {
            Entity entity = client.world.getEntityById(entry.getKey());
            if (entity == null) continue;

            ESPTarget target = entry.getValue();

            // Read live position
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
                target.color[0], target.color[1], target.color[2], 0.4f);

            immediate.draw();

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    public static void spawnTestEntity() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        // Remove old test entity
        if (testEntity != null) {
            testEntity.discard();
            trackedEntities.remove(testEntity.getId());
            testEntity = null;
        }

        testOrigin = new Vec3d(client.player.getX() + 3, client.player.getY(), client.player.getZ());
        testTicks = 0;

        ArmorStandEntity stand = new ArmorStandEntity(EntityType.ARMOR_STAND, client.world);
        stand.setPosition(testOrigin.x, testOrigin.y, testOrigin.z);
        stand.setInvisible(true);
        stand.setCustomName(Text.literal("\u00A7f[Lv1] \u00A7aLittlefoot \u00A7f100/100\u2764"));
        stand.setCustomNameVisible(false);

        client.world.addEntity(stand);
        testEntity = stand;

        // Directly track it so it works outside mineshafts for testing
        trackedEntities.put(stand.getId(), new ESPTarget(LITTLEFOOT_COLOR, true));

        client.player.sendMessage(Text.literal("\u00A7a[ShaftESP] Test Littlefoot spawned! It will walk in a circle."), false);
    }

    private static void tickTestEntity() {
        if (testEntity == null || testOrigin == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || !testEntity.isAlive()) {
            testEntity = null;
            return;
        }

        testTicks++;
        double radius = 5.0;
        double angle = testTicks * 0.05;
        double x = testOrigin.x + Math.cos(angle) * radius;
        double z = testOrigin.z + Math.sin(angle) * radius;
        testEntity.setPosition(x, testOrigin.y, z);
    }

    public static void onWorldUnload() {
        trackedEntities.clear();
        testEntity = null;
        testOrigin = null;
        isInMines = false;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
