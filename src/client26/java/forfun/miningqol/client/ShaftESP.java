package forfun.miningqol.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import forfun.miningqol.client.utils.render.SeeThroughBoxRenderer;
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
 * is unchanged; rendering uses through-wall outlined entity bounding boxes.
 */
public class ShaftESP {
    private static boolean isInMines = false;
    private static int locationCheckCooldown = 0;
    private static boolean littlefootEnabled = true;
    private static boolean littlefootTracer = true; // draw a line from the crosshair to each littlefoot
    private static boolean mobsEnabled = false;
    private static boolean externalEsp = false;

    private static float[] littlefootColor = {0.0f, 1.0f, 0.4f};
    private static float[] mobColor = {1.0f, 0.2f, 0.2f};
    private static float mobAlpha = 0.2f;

    /**
     * What is drawn for one tracked entity. Colours are resolved at render time so config
     * changes apply live.
     *
     * @param mob        the skin-matched mob, or null for a nametag Littlefoot / generic mob
     * @param littlefoot whether it gets the Littlefoot treatment (tracer, auto-party sign-up)
     */
    private record Track(SkinMob mob, boolean littlefoot) {
        float[] color() {
            return littlefoot ? littlefootColor : mobColor;
        }

        float alpha() {
            return littlefoot ? 1.0f : mobAlpha;
        }

        String name(Entity entity) {
            if (mob != null) return mob.displayName();
            return littlefoot ? "Littlefoot" : entity.getName().getString();
        }
    }

    private static final Map<Integer, Track> trackedEntities = new HashMap<>();
    // IDs of nametag stands identified as littlefoot
    private static final Set<Integer> littlefootEntityIds = new HashSet<>();
    /** Entity ID -> skin hash ("" when the profile carries none), so the profile is decoded once. */
    private static final Map<Integer, String> skinHashes = new HashMap<>();

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

        if (!littlefootEnabled && !mobsEnabled && !anySkinMobEnabled()) {
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
        skinHashes.keySet().removeIf(id -> level.getEntity(id) == null);

        // Build tracked entities
        trackedEntities.clear();

        // Skin pass first: Hypixel's humanoid mobs are fake players in a fixed skin, and the
        // profile's texture hash names the mob exactly — so the box lands on the mob itself.
        List<Player> skinned = level.getEntitiesOfClass(
            Player.class,
            client.player.getBoundingBox().inflate(800),
            e -> isTrackableMob(client, e)
        );
        List<Player> skinLittlefeet = new java.util.ArrayList<>();
        for (Player player : skinned) {
            String hash = skinHashes.computeIfAbsent(player.getId(), id -> {
                String h = SkinMob.skinHash(player);
                return h == null ? "" : h;
            });
            SkinMob mob = SkinMob.byHash(hash);
            if (mob == null) continue;
            boolean littlefoot = mob == SkinMob.LITTLEFOOT;
            if (littlefoot ? !littlefootEnabled : !mob.isEnabled()) continue;
            trackedEntities.put(player.getId(), new Track(mob, littlefoot));
            if (littlefoot) skinLittlefeet.add(player);
        }

        // Identify littlefoot entities via nametag armor stands — the fallback for when the skin
        // is not recognised. A stand sitting over a skin-matched Littlefoot is its own label, and
        // drawing both would box the mob twice.
        List<ArmorStand> nametagStands = level.getEntitiesOfClass(
            ArmorStand.class,
            client.player.getBoundingBox().inflate(800),
            stand -> stand.hasCustomName() && stand.isInvisible()
        );

        for (ArmorStand stand : nametagStands) {
            String name = stand.getCustomName().getString()
                .replaceAll("\u00A7.", "").trim().toLowerCase();

            if (!name.contains("littlefoot")) continue;
            boolean labelsSkinMob = false;
            for (Player mob : skinLittlefeet) {
                if (Math.abs(mob.getX() - stand.getX()) < 2.0 && Math.abs(mob.getZ() - stand.getZ()) < 2.0
                        && stand.getY() - mob.getY() > -1.0 && stand.getY() - mob.getY() < 4.0) {
                    labelsSkinMob = true;
                    break;
                }
            }
            if (!labelsSkinMob) littlefootEntityIds.add(stand.getId());
        }

        if (mobsEnabled) {
            // Hypixel humanoid mobs are fake Player entities, so excluding Player wholesale drops
            // most mineshaft mobs. Keep real tab-listed players out while allowing NPC players.
            List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                client.player.getBoundingBox().inflate(800),
                e -> isTrackableMob(client, e)
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
                            e -> !(e instanceof ArmorStand) && isTrackableMob(client, e)
                        ).isEmpty();
                        if (hasMobBelow) continue;
                    } else {
                        // Skip visible armor stands without custom names and no base plate (corpses)
                        if (!stand.hasCustomName() && !stand.showBasePlate()) continue;
                    }
                }

                // A skin match already placed here keeps its own colour.
                trackedEntities.putIfAbsent(entity.getId(),
                    new Track(null, littlefootEntityIds.contains(entity.getId())));
            }
        }

        // Not an else: the mob pass filters invisible nametag stands down to ones carrying a health
        // marker, which the Littlefoot nametag has none of — so with Mob ESP on it would otherwise
        // be dropped entirely, taking its box and tracer with it.
        if (littlefootEnabled) {
            for (int id : littlefootEntityIds) {
                trackedEntities.put(id, new Track(null, true));
            }
        }

    }

    /** Any of the skin-matched mobs other than the Littlefoot switched on. */
    private static boolean anySkinMobEnabled() {
        for (SkinMob mob : SkinMob.values()) {
            if (mob != SkinMob.LITTLEFOOT && mob.isEnabled()) return true;
        }
        return false;
    }

    private static boolean isTrackableMob(Minecraft client, LivingEntity entity) {
        if (entity == client.player) return false;
        if (!(entity instanceof Player player)) return true;

        // Real accounts are version-4 UUIDs with an active tab entry. Hypixel's humanoid mobs are
        // fake players: normally UUID v2, and sometimes v4 but absent from tab after spawning.
        return player.getUUID().version() != 4
            || client.getConnection() == null
            || client.getConnection().getPlayerInfo(player.getUUID()) == null;
    }

    public static void render(CameraRenderState cameraState, Matrix4fc viewMatrix) {
        // External mode hands rendering to the overlay app — drawing here too would
        // double up every box. Gated on a live overlay so closing the overlay (or the
        // feed being off) falls back to in-game drawing instead of showing nothing.
        if (externalEsp && EspHooks.isOverlayConnected()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || trackedEntities.isEmpty()) return;

        Vec3 cam = cameraState.pos;
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        Matrix4f pose = new Matrix4f(viewMatrix);

        // One buffer acquisition, one endBatch at the very end — the same shape
        // OrderedWaypointRenderer uses, whose tracer works. Ending the batch between the
        // boxes and the tracer is what stopped the tracer from drawing.
        VertexConsumer quads = buffers.getBuffer(RenderTypes.textBackgroundSeeThrough());

        {
            for (Map.Entry<Integer, Track> entry : trackedEntities.entrySet()) {
                Entity entity = client.level.getEntity(entry.getKey());
                if (entity == null) continue;

                float[] color = entry.getValue().color();
                float alpha = entry.getValue().alpha();
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

                SeeThroughBoxRenderer.outline(quads, pose,
                    (float) minX, (float) minY, (float) minZ,
                    (float) maxX, (float) maxY, (float) maxZ,
                    color[0], color[1], color[2], alpha);
            }
        }

        if (littlefootTracer) {
            Vector3f forward = new Vector3f(0, 0, -1);
            cameraState.orientation.transform(forward);
            Vec3 lineStart = cam.add(forward.x, forward.y, forward.z);
            for (Map.Entry<Integer, Track> entry : trackedEntities.entrySet()) {
                if (!entry.getValue().littlefoot()) continue;
                Entity entity = client.level.getEntity(entry.getKey());
                if (entity == null) continue;

                Vec3 target;
                if (entity instanceof ArmorStand stand && stand.isMarker()) {
                    target = new Vec3(entity.getX(), entity.getY() - 0.8, entity.getZ());
                } else {
                    target = entity.getBoundingBox().getCenter();
                }
                line(buffers, pose, cam, lineStart, target, littlefootColor, 1.0f);
            }
        }

        buffers.endBatch();
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
        skinHashes.clear();
        isInMines = false;
    }

    /**
     * Whether the ESP is currently tracking a Littlefoot. Backs the auto-party's
     * Littlefoot sign-up, which wants the mob itself rather than the shaft id.
     *
     * <p>Only populated while Littlefoot ESP is on and you are in a mineshaft.
     */
    public static boolean hasLittlefoot() {
        if (!littlefootEnabled) return false;
        if (!littlefootEntityIds.isEmpty()) return true;
        for (Track track : trackedEntities.values()) {
            if (track.littlefoot()) return true;
        }
        return false;
    }

    /** Per-skin-mob toggle and colour — the Littlefoot's colour lives here too. */
    public static boolean isSkinMobEnabled(SkinMob mob) {
        return mob == SkinMob.LITTLEFOOT ? littlefootEnabled : mob.isEnabled();
    }

    public static void setSkinMobEnabled(SkinMob mob, boolean value) {
        if (mob == SkinMob.LITTLEFOOT) littlefootEnabled = value;
        else mob.setEnabled(value);
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

    public static float[] getLittlefootColor() { return littlefootColor; }
    public static void setLittlefootColor(float r, float g, float b) { littlefootColor = new float[]{r, g, b}; }
    public static float[] getMobColor() { return mobColor; }
    public static void setMobColor(float r, float g, float b) { mobColor = new float[]{r, g, b}; }
    public static float getMobAlpha() { return mobAlpha; }
    public static void setMobAlpha(float alpha) { mobAlpha = alpha; }

    public static boolean isMobsEnabled() {
        return mobsEnabled;
    }

    /** Shaft ESP draws boxes only; the glow paths are Corpse ESP's. */
    public static EntityEspMode getRenderMode() {
        return EntityEspMode.BOX;
    }

    public static boolean isGlowTarget(Entity entity) {
        return false;
    }

    public static float[] getEspColor(Entity entity) {
        Track track = trackedEntities.get(entity.getId());
        return track == null ? mobColor : track.color();
    }

    /**
     * External mode: the overlay draws these instead of the in-game renderer, so exactly
     * one of the two is ever responsible for a given mob.
     */
    public static void setExternalEsp(boolean value) {
        externalEsp = value;
        if (value) EspHooks.enableFeed();
    }

    public static boolean isExternalEsp() {
        return externalEsp;
    }

    /** Snapshot for the external overlay feed. Client thread only. */
    public static java.util.List<EspTarget> espTargets() {
        Minecraft client = Minecraft.getInstance();
        java.util.List<EspTarget> out = new java.util.ArrayList<>();
        if (!externalEsp || client.level == null) return out;

        for (Map.Entry<Integer, Track> entry : trackedEntities.entrySet()) {
            Entity entity = client.level.getEntity(entry.getKey());
            if (entity == null) continue;
            Track track = entry.getValue();
            // Marker armour stands sit ~1.8 above the mob they label; drop to the mob itself.
            double y = entity instanceof ArmorStand stand && stand.isMarker()
                ? entity.getY() - 1.8
                : entity.getY();
            out.add(new EspTarget(
                entity.getX(), y, entity.getZ(),
                track.name(entity),
                track.littlefoot() ? "littlefoot" : "mob",
                EspTarget.packColor(track.color())));
        }
        return out;
    }

    // Legacy compat - maps to littlefoot toggle
    public static void setEnabled(boolean value) {
        littlefootEnabled = value;
    }

    public static boolean isEnabled() {
        return littlefootEnabled;
    }
}
