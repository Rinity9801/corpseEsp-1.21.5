package forfun.miningqol.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 26.1.2 port of CorpseESP. Corpse detection (armor-stand helmet Skyblock IDs)
 * is unchanged; rendering goes through the immediate BufferSource at the tail of
 * LevelRenderer.renderLevel (see LevelRendererMixin) using
 * RenderTypes.textBackgroundSeeThrough() — a through-wall highlight box plus a
 * beacon-style pillar, matching the 1.21 Waypoint look.
 */
public class CorpseESP {
    private static final int FULL_BRIGHT = 15728880;
    private static final float HIGHLIGHT_ALPHA = 0.3f;
    private static final double BEACON_WIDTH = 0.3;
    private static final double BEACON_HEIGHT = 20.0;

    private static final List<CorpseWaypoint> activeWaypoints = new ArrayList<>();
    private static final List<Vec3> claimedPositions = new ArrayList<>();
    private static boolean isInMines = false;
    private static int locationCheckCooldown = 0;

    private static boolean lapisEnabled = true;
    private static boolean tungstenEnabled = true;
    private static boolean umberEnabled = true;
    private static boolean vanguardEnabled = true;
    private static boolean externalEsp = false;

    private static class CorpseWaypoint {
        final BlockPos pos;
        final float[] color;
        final String name;

        CorpseWaypoint(BlockPos pos, float[] color, String name) {
            this.pos = pos;
            this.color = color;
            this.name = name;
        }
    }

    public enum CorpseType {
        LAPIS(new String[]{"LAPIS_ARMOR_HELMET"}, "Lapis", new float[]{0.0f, 0.0f, 1.0f}),
        TUNGSTEN(new String[]{"MINERAL_HELMET"}, "Tungsten", new float[]{1.0f, 1.0f, 1.0f}),
        UMBER(new String[]{"ARMOR_OF_YOG_HELMET", "YOG_HELMET"}, "Umber", new float[]{181f/255f, 98f/255f, 34f/255f}),
        VANGUARD(new String[]{"VANGUARD_HELMET"}, "Vanguard", new float[]{242f/255f, 36f/255f, 184f/255f});

        private final String[] skyblockIds;
        private final String displayName;
        private final float[] color;

        CorpseType(String[] skyblockIds, String displayName, float[] color) {
            this.skyblockIds = skyblockIds;
            this.displayName = displayName;
            this.color = color;
        }

        public static CorpseType fromSkyblockId(String skyblockId) {
            if (skyblockId == null) return null;
            for (CorpseType type : values()) {
                for (String id : type.skyblockIds) {
                    if (id.equals(skyblockId)) {
                        return type;
                    }
                }
            }
            return null;
        }

        public String getDisplayName() {
            return displayName;
        }

        public float[] getColor() {
            return color;
        }
    }

    private static String getSkyblockId(ItemStack stack) {
        if (stack.isEmpty()) return null;

        try {
            CustomData customDataComponent = stack.getComponents().get(DataComponents.CUSTOM_DATA);
            if (customDataComponent == null) return null;

            CompoundTag customData = customDataComponent.copyTag();
            if (!customData.contains("id")) return null;

            Optional<String> idOpt = customData.getString("id");
            if (idOpt.isEmpty()) return null;

            return idOpt.get();
        } catch (Exception e) {
            return null;
        }
    }

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

        if (level == null || client.player == null) return;

        boolean wasInMines = isInMines;
        locationCheckCooldown--;
        if (locationCheckCooldown <= 0) {
            isInMines = checkIfInMineshaft();
            locationCheckCooldown = 20;
        }

        if (wasInMines && !isInMines) {
            claimedPositions.clear();
        }

        activeWaypoints.clear();

        if (!isInMines) {
            return;
        }

        List<ArmorStand> armorStands = level.getEntitiesOfClass(
            ArmorStand.class,
            client.player.getBoundingBox().inflate(800),
            armorStand -> {
                if (armorStand.hasCustomName()) return false;
                if (armorStand.isInvisible()) return false;
                if (armorStand.showBasePlate()) return false;
                return true;
            }
        );
        for (ArmorStand armorStand : armorStands) {
            Vec3 pos = new Vec3(armorStand.getX(), armorStand.getY(), armorStand.getZ());
            BlockPos blockPos = BlockPos.containing(pos);

            ItemStack helmet = armorStand.getItemBySlot(EquipmentSlot.HEAD);

            if (helmet.isEmpty()) continue;

            boolean isClaimed = false;
            for (Vec3 claimedPos : claimedPositions) {
                if (pos.distanceTo(claimedPos) < 5.0) {
                    isClaimed = true;
                    break;
                }
            }
            if (isClaimed) continue;

            String skyblockId = getSkyblockId(helmet);
            CorpseType corpseType = CorpseType.fromSkyblockId(skyblockId);

            if (corpseType != null && isCorpseTypeEnabled(corpseType)) {
                BlockPos waypointPos = blockPos.above(2);
                activeWaypoints.add(new CorpseWaypoint(
                    waypointPos,
                    corpseType.getColor(),
                    corpseType.getDisplayName()
                ));
            }
        }
    }

    public static void render(CameraRenderState cameraState, Matrix4fc viewMatrix) {
        // External mode hands rendering to the overlay app — drawing here too would
        // double up every box. Gated on a live overlay so closing the overlay (or the
        // feed being off) falls back to in-game drawing instead of showing nothing.
        if (externalEsp && EspHooks.isOverlayConnected()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || activeWaypoints.isEmpty()) return;

        Vec3 cam = cameraState.pos;
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        Matrix4f pose = new Matrix4f(viewMatrix);

        VertexConsumer quads = buffers.getBuffer(RenderTypes.textBackgroundSeeThrough());

        for (CorpseWaypoint waypoint : activeWaypoints) {
            double minX = waypoint.pos.getX() - cam.x;
            double minY = waypoint.pos.getY() - cam.y;
            double minZ = waypoint.pos.getZ() - cam.z;
            double maxX = minX + 1.0;
            double maxY = minY + 1.0;
            double maxZ = minZ + 1.0;

            // Highlight box
            box(quads, pose,
                (float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) maxY, (float) maxZ,
                waypoint.color[0], waypoint.color[1], waypoint.color[2], HIGHLIGHT_ALPHA);

            // Beacon-style pillar above the waypoint
            double centerX = minX + 0.5;
            double centerZ = minZ + 0.5;
            box(quads, pose,
                (float) (centerX - BEACON_WIDTH), (float) minY, (float) (centerZ - BEACON_WIDTH),
                (float) (centerX + BEACON_WIDTH), (float) (minY + BEACON_HEIGHT), (float) (centerZ + BEACON_WIDTH),
                waypoint.color[0], waypoint.color[1], waypoint.color[2], HIGHLIGHT_ALPHA * 0.6f);
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

    public static void onCorpseClaimed() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            claimedPositions.add(new Vec3(player.getX(), player.getY(), player.getZ()));
        }
    }

    public static void onWorldUnload() {
        activeWaypoints.clear();
        claimedPositions.clear();
    }

    private static boolean isCorpseTypeEnabled(CorpseType type) {
        switch (type) {
            case LAPIS: return lapisEnabled;
            case TUNGSTEN: return tungstenEnabled;
            case UMBER: return umberEnabled;
            case VANGUARD: return vanguardEnabled;
            default: return true;
        }
    }

    public static void toggleLapis() {
        lapisEnabled = !lapisEnabled;
    }

    public static void toggleTungsten() {
        tungstenEnabled = !tungstenEnabled;
    }

    public static void toggleUmber() {
        umberEnabled = !umberEnabled;
    }

    public static void toggleVanguard() {
        vanguardEnabled = !vanguardEnabled;
    }

    public static boolean isLapisEnabled() {
        return lapisEnabled;
    }

    public static boolean isTungstenEnabled() {
        return tungstenEnabled;
    }

    public static boolean isUmberEnabled() {
        return umberEnabled;
    }

    public static boolean isVanguardEnabled() {
        return vanguardEnabled;
    }

    /**
     * External mode: the overlay draws these instead of the in-game renderer, so exactly
     * one of the two is ever responsible for a given corpse.
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
        if (!externalEsp) return java.util.List.of();
        java.util.List<EspTarget> out = new java.util.ArrayList<>(activeWaypoints.size());
        for (CorpseWaypoint waypoint : activeWaypoints) {
            // Centre of the highlighted block, so the overlay's box lines up with the in-game one.
            out.add(new EspTarget(
                waypoint.pos.getX() + 0.5,
                waypoint.pos.getY(),
                waypoint.pos.getZ() + 0.5,
                waypoint.name,
                "corpse",
                EspTarget.packColor(waypoint.color)));
        }
        return out;
    }

    public static void getCorpseInfo() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        HitResult hitResult = client.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            MqoChat.reply(Component.literal("\u00A7c[Corpse ESP] You must be looking at an entity!"));
            return;
        }

        EntityHitResult entityHit = (EntityHitResult) hitResult;
        Entity entity = entityHit.getEntity();

        if (!(entity instanceof ArmorStand)) {
            MqoChat.reply(Component.literal("\u00A7c[Corpse ESP] You must be looking at an armor stand!"));
            return;
        }

        ArmorStand armorStand = (ArmorStand) entity;
        Vec3 pos = new Vec3(armorStand.getX(), armorStand.getY(), armorStand.getZ());
        BlockPos blockPos = BlockPos.containing(pos);

        ItemStack helmet = armorStand.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = armorStand.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = armorStand.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = armorStand.getItemBySlot(EquipmentSlot.FEET);

        String helmetName = helmet.isEmpty() ? "NONE" : helmet.getHoverName().getString();
        String helmetId = getSkyblockId(helmet);
        String chestplateName = chestplate.isEmpty() ? "NONE" : chestplate.getHoverName().getString();
        String chestplateId = getSkyblockId(chestplate);
        String leggingsName = leggings.isEmpty() ? "NONE" : leggings.getHoverName().getString();
        String leggingsId = getSkyblockId(leggings);
        String bootsName = boots.isEmpty() ? "NONE" : boots.getHoverName().getString();
        String bootsId = getSkyblockId(boots);

        boolean hasCustomName = armorStand.hasCustomName();
        String customName = hasCustomName ? armorStand.getCustomName().getString() : "NONE";
        boolean isInvisible = armorStand.isInvisible();
        boolean hasBasePlate = armorStand.showBasePlate();

        MqoChat.reply(Component.literal("\u00A7e========== ARMOR STAND INFO =========="));
        MqoChat.reply(Component.literal("\u00A76Position: \u00A7f" + blockPos));
        MqoChat.reply(Component.literal("\u00A76Has Custom Name: \u00A7f" + hasCustomName + " \u00A77(" + customName + ")"));
        MqoChat.reply(Component.literal("\u00A76Invisible: \u00A7f" + isInvisible));
        MqoChat.reply(Component.literal("\u00A76Has Base Plate: \u00A7f" + hasBasePlate));
        MqoChat.reply(Component.literal("\u00A7e------- EQUIPMENT -------"));
        MqoChat.reply(Component.literal("\u00A76Helmet: \u00A7f" + helmetName));
        MqoChat.reply(Component.literal("\u00A76  Skyblock ID: \u00A7f" + (helmetId != null ? helmetId : "NONE")));
        MqoChat.reply(Component.literal("\u00A76Chestplate: \u00A7f" + chestplateName));
        MqoChat.reply(Component.literal("\u00A76  Skyblock ID: \u00A7f" + (chestplateId != null ? chestplateId : "NONE")));
        MqoChat.reply(Component.literal("\u00A76Leggings: \u00A7f" + leggingsName));
        MqoChat.reply(Component.literal("\u00A76  Skyblock ID: \u00A7f" + (leggingsId != null ? leggingsId : "NONE")));
        MqoChat.reply(Component.literal("\u00A76Boots: \u00A7f" + bootsName));
        MqoChat.reply(Component.literal("\u00A76  Skyblock ID: \u00A7f" + (bootsId != null ? bootsId : "NONE")));
        MqoChat.reply(Component.literal("\u00A7e===================================="));

        if (!helmet.isEmpty()) {
            try {
                CustomData customDataComponent = helmet.getComponents().get(DataComponents.CUSTOM_DATA);
                if (customDataComponent != null) {
                    CompoundTag customData = customDataComponent.copyTag();
                    MqoChat.reply(Component.literal("\u00A76Helmet NBT: \u00A7f" + customData.toString()));
                }
            } catch (Exception e) {
                MqoChat.reply(Component.literal("\u00A7cError reading helmet NBT: " + e.getMessage()));
            }
        }
    }
}
