package forfun.corpse.client;

import forfun.corpse.client.utils.waypoint.NamedWaypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CorpseESP {
    private static final List<NamedWaypoint> activeWaypoints = new ArrayList<>();
    private static final List<Vec3d> claimedPositions = new ArrayList<>();
    private static final List<BlockPos> loggedArmorStands = new ArrayList<>();
    private static final List<BlockPos> loggedWaypoints = new ArrayList<>();
    private static boolean isInMines = false;
    private static int locationCheckCooldown = 0;
    private static int lastWaypointCount = -1;

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
            var customDataComponent = stack.getComponents().get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
            if (customDataComponent == null) return null;

            net.minecraft.nbt.NbtCompound customData = customDataComponent.copyNbt();
            if (!customData.contains("id")) return null;

            var idOpt = customData.getString("id");
            if (idOpt.isEmpty()) return null;

            return idOpt.get();
        } catch (Exception e) {
            return null;
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
                        String cleanLine = line.replaceAll("§.", "").trim();

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

        if (world == null || client.player == null) return;

        locationCheckCooldown--;
        if (locationCheckCooldown <= 0) {
            isInMines = checkIfInMineshaft();
            locationCheckCooldown = 20;
        }

        activeWaypoints.clear();

        if (!isInMines) {
            return;
        }
        List<ArmorStandEntity> allArmorStands = world.getEntitiesByClass(
            ArmorStandEntity.class,
            client.player.getBoundingBox().expand(100),
            armorStand -> !armorStand.hasCustomName()
        );

        MinecraftClient client2 = MinecraftClient.getInstance();
        for (ArmorStandEntity armorStand : allArmorStands) {
            Vec3d pos = armorStand.getPos();
            BlockPos blockPos = BlockPos.ofFloored(pos);

            if (client2.player != null && !loggedArmorStands.contains(blockPos)) {
                ItemStack helmet = armorStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
                String displayName = helmet.isEmpty() ? "NO HELMET" : helmet.getName().getString();
                String skyblockId = getSkyblockId(helmet);
                String invisible = armorStand.isInvisible() ? "INVISIBLE" : "VISIBLE";
                String basePlate = armorStand.shouldShowBasePlate() ? "HAS_BASEPLATE" : "NO_BASEPLATE";
                client2.player.sendMessage(Text.literal("§e[DEBUG] ArmorStand: " + blockPos + " | " + invisible + " | " + basePlate + " | Display: " + displayName + " | SkyblockID: " + skyblockId), false);
                loggedArmorStands.add(blockPos);
            }
        }

        List<ArmorStandEntity> armorStands = world.getEntitiesByClass(
            ArmorStandEntity.class,
            client.player.getBoundingBox().expand(800),
            armorStand -> {
                if (armorStand.hasCustomName()) return false;
                if (armorStand.isInvisible()) return false;
                if (armorStand.shouldShowBasePlate()) return false;
                return true;
            }
        );
        for (ArmorStandEntity armorStand : armorStands) {
            Vec3d pos = armorStand.getPos();
            BlockPos blockPos = BlockPos.ofFloored(pos);

            ItemStack helmet = armorStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);

            if (helmet.isEmpty()) continue;

            boolean isClaimed = false;
            for (Vec3d claimedPos : claimedPositions) {
                if (pos.distanceTo(claimedPos) < 5.0) {
                    isClaimed = true;
                    break;
                }
            }
            if (isClaimed) continue;

            String skyblockId = getSkyblockId(helmet);
            CorpseType corpseType = CorpseType.fromSkyblockId(skyblockId);

            if (corpseType != null) {
                BlockPos waypointPos = blockPos.up(2);
                NamedWaypoint waypoint = new NamedWaypoint(
                    waypointPos,
                    corpseType.getColor(),
                    corpseType.getDisplayName(),
                    true
                );
                activeWaypoints.add(waypoint);

                if (client.player != null && !loggedWaypoints.contains(blockPos)) {
                    client.player.sendMessage(Text.literal("§a[DEBUG] Created waypoint for " + corpseType.getDisplayName() + " at " + waypointPos), false);
                    loggedWaypoints.add(blockPos);
                }
            } else if (skyblockId != null) {
                if (client.player != null && !loggedWaypoints.contains(blockPos)) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Unknown Skyblock ID: " + skyblockId), false);
                    loggedWaypoints.add(blockPos);
                }
            }
        }

        if (client.player != null && activeWaypoints.size() > 0 && lastWaypointCount != activeWaypoints.size()) {
            client.player.sendMessage(Text.literal("§d[DEBUG] Total active waypoints: " + activeWaypoints.size()), false);
            lastWaypointCount = activeWaypoints.size();
        }
    }

    public static void render(MatrixStack matrices, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        for (NamedWaypoint waypoint : activeWaypoints) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);

            waypoint.render(matrices, camera);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    public static void onCorpseClaimed() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            claimedPositions.add(player.getPos());
        }
    }

    public static void onWorldUnload() {
        activeWaypoints.clear();
        claimedPositions.clear();
        loggedArmorStands.clear();
        loggedWaypoints.clear();
        lastWaypointCount = -1;
    }

    public static void getCorpseInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            client.player.sendMessage(Text.literal("§c[Corpse ESP] You must be looking at an entity!"), false);
            return;
        }

        EntityHitResult entityHit = (EntityHitResult) hitResult;
        Entity entity = entityHit.getEntity();

        if (!(entity instanceof ArmorStandEntity)) {
            client.player.sendMessage(Text.literal("§c[Corpse ESP] You must be looking at an armor stand!"), false);
            return;
        }

        ArmorStandEntity armorStand = (ArmorStandEntity) entity;
        Vec3d pos = armorStand.getPos();
        BlockPos blockPos = BlockPos.ofFloored(pos);

        ItemStack helmet = armorStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        ItemStack chestplate = armorStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        ItemStack leggings = armorStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS);
        ItemStack boots = armorStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET);

        String helmetName = helmet.isEmpty() ? "NONE" : helmet.getName().getString();
        String helmetId = getSkyblockId(helmet);
        String chestplateName = chestplate.isEmpty() ? "NONE" : chestplate.getName().getString();
        String chestplateId = getSkyblockId(chestplate);
        String leggingsName = leggings.isEmpty() ? "NONE" : leggings.getName().getString();
        String leggingsId = getSkyblockId(leggings);
        String bootsName = boots.isEmpty() ? "NONE" : boots.getName().getString();
        String bootsId = getSkyblockId(boots);

        boolean hasCustomName = armorStand.hasCustomName();
        String customName = hasCustomName ? armorStand.getCustomName().getString() : "NONE";
        boolean isInvisible = armorStand.isInvisible();
        boolean hasBasePlate = armorStand.shouldShowBasePlate();

        client.player.sendMessage(Text.literal("§e========== ARMOR STAND INFO =========="), false);
        client.player.sendMessage(Text.literal("§6Position: §f" + blockPos), false);
        client.player.sendMessage(Text.literal("§6Has Custom Name: §f" + hasCustomName + " §7(" + customName + ")"), false);
        client.player.sendMessage(Text.literal("§6Invisible: §f" + isInvisible), false);
        client.player.sendMessage(Text.literal("§6Has Base Plate: §f" + hasBasePlate), false);
        client.player.sendMessage(Text.literal("§e------- EQUIPMENT -------"), false);
        client.player.sendMessage(Text.literal("§6Helmet: §f" + helmetName), false);
        client.player.sendMessage(Text.literal("§6  Skyblock ID: §f" + (helmetId != null ? helmetId : "NONE")), false);
        client.player.sendMessage(Text.literal("§6Chestplate: §f" + chestplateName), false);
        client.player.sendMessage(Text.literal("§6  Skyblock ID: §f" + (chestplateId != null ? chestplateId : "NONE")), false);
        client.player.sendMessage(Text.literal("§6Leggings: §f" + leggingsName), false);
        client.player.sendMessage(Text.literal("§6  Skyblock ID: §f" + (leggingsId != null ? leggingsId : "NONE")), false);
        client.player.sendMessage(Text.literal("§6Boots: §f" + bootsName), false);
        client.player.sendMessage(Text.literal("§6  Skyblock ID: §f" + (bootsId != null ? bootsId : "NONE")), false);
        client.player.sendMessage(Text.literal("§e===================================="), false);

        if (!helmet.isEmpty()) {
            try {
                var customDataComponent = helmet.getComponents().get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
                if (customDataComponent != null) {
                    net.minecraft.nbt.NbtCompound customData = customDataComponent.copyNbt();
                    client.player.sendMessage(Text.literal("§6Helmet NBT: §f" + customData.toString()), false);
                }
            } catch (Exception e) {
                client.player.sendMessage(Text.literal("§cError reading helmet NBT: " + e.getMessage()), false);
            }
        }
    }
}
