package forfun.miningqol.client.waypoints;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.List;

/**
 * 26.1.2 port of the ordered waypoint renderer. The RenderLayer/GL-state approach from
 * 1.21.x is gone; this draws through the immediate BufferSource at the tail of
 * LevelRenderer.renderLevel (see LevelRendererMixin):
 *  - boxes:  RenderTypes.textBackgroundSeeThrough() — a no-depth colored-quad layer
 *            (what vanilla nametag backgrounds use), so boxes show through walls
 *  - labels: Font.drawInBatch with DisplayMode.SEE_THROUGH
 *  - lines:  RenderTypes.lines() (depth-tested; no stock see-through line layer)
 */
public class OrderedWaypointRenderer {
    private static final int FULL_BRIGHT = 15728880;

    public static void render(CameraRenderState cameraState, Matrix4fc viewMatrix) {
        if (!OrderedWaypointManager.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        List<OrderedWaypoint> route = OrderedWaypointManager.getCurrentRoute();
        if (route.isEmpty()) return;

        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        Matrix4f pose = new Matrix4f(viewMatrix);
        Vec3 camPos = cameraState.pos;

        OrderedWaypoint current = OrderedWaypointManager.getCurrentWaypoint();
        OrderedWaypoint prev = OrderedWaypointManager.getPreviousWaypoint();
        List<OrderedWaypoint> nextWaypoints = OrderedWaypointManager.getNextWaypoints(
            OrderedWaypointManager.getNextCount()
        );

        // ===== Boxes =====
        VertexConsumer quads = buffers.getBuffer(RenderTypes.textBackgroundSeeThrough());

        if (current != null) {
            box(quads, pose, camPos, current.getPosition(),
                OrderedWaypointManager.getCurrentWaypointColor(), OrderedWaypointManager.getCurrentWaypointAlpha());
        }

        if (OrderedWaypointManager.isShowAll() || OrderedWaypointManager.isEditMode()) {
            float[] allColor = OrderedWaypointManager.getShowAllWaypointColor();
            float allAlpha = OrderedWaypointManager.getShowAllWaypointAlpha();
            for (OrderedWaypoint wp : route) {
                if (wp != current && wp != prev && !nextWaypoints.contains(wp)) {
                    box(quads, pose, camPos, wp.getPosition(), allColor, allAlpha);
                }
            }
        }

        if (prev != null && prev != current) {
            box(quads, pose, camPos, prev.getPosition(),
                OrderedWaypointManager.getPreviousWaypointColor(), OrderedWaypointManager.getPreviousWaypointAlpha());
        }

        float[] nextColor = OrderedWaypointManager.getNextWaypointColor();
        float nextAlpha = OrderedWaypointManager.getNextWaypointAlpha();
        for (int i = 0; i < nextWaypoints.size(); i++) {
            OrderedWaypoint next = nextWaypoints.get(i);
            if (next != current) {
                float alphaMultiplier = Math.max(0.2f, 1.0f - (i * 0.25f));
                box(quads, pose, camPos, next.getPosition(), nextColor, nextAlpha * alphaMultiplier);
            }
        }

        List<Integer> wrongWaypoints = OrderedWaypointManager.getWrongWaypoints();
        if (!wrongWaypoints.isEmpty()) {
            float[] wrongColor = {1.0f, 0.0f, 0.0f};
            for (OrderedWaypoint wp : route) {
                if (wrongWaypoints.contains(wp.getIndex())) {
                    box(quads, pose, camPos, wp.getPosition(), wrongColor, 1.0f);
                }
            }
        }

        // ===== Lines =====
        OrderedWaypoint target = OrderedWaypointManager.getNextWaypoint();
        if (target != null && OrderedWaypointManager.isTraceLineEnabled()) {
            // From just in front of the camera to the target (like 1.21).
            Vector3f forward = new Vector3f(0, 0, -1);
            cameraState.orientation.transform(forward);
            Vec3 start = camPos.add(forward.x, forward.y, forward.z);
            BlockPos t = target.getPosition();
            line(buffers, pose, camPos, start, new Vec3(t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5),
                OrderedWaypointManager.getTraceLineColor(), OrderedWaypointManager.getTraceLineAlpha());
        }

        if (OrderedWaypointManager.isEditMode() && route.size() >= 2) {
            float[] lineColor = OrderedWaypointManager.getEditModeLineColor();
            float lineAlpha = OrderedWaypointManager.getEditModeLineAlpha();
            for (int i = 0; i < route.size(); i++) {
                BlockPos from = route.get(i).getPosition();
                BlockPos to = route.get((i + 1) % route.size()).getPosition();
                line(buffers, pose, camPos,
                    new Vec3(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5),
                    new Vec3(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5),
                    lineColor, lineAlpha);
            }
        }

        // ===== Block outline around the next waypoint (lobby-check blocks) =====
        // 1.21 drew exposed line edges; here matching blocks get thin see-through
        // boxes instead — same information, simpler under the new pipeline.
        if (target != null && OrderedWaypointManager.isBlockOutlineAroundWaypoint()) {
            net.minecraft.world.level.block.Block expected = null;
            try {
                expected = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                    net.minecraft.resources.Identifier.parse(OrderedWaypointManager.getLobbyCheckBlock()));
            } catch (Exception ignored) {}
            if (expected != null && expected != net.minecraft.world.level.block.Blocks.AIR) {
                int radius = OrderedWaypointManager.getBlockOutlineRadius();
                float[] outlineColor = OrderedWaypointManager.getBlockOutlineColor();
                float outlineAlpha = OrderedWaypointManager.getBlockOutlineAlpha() * 0.35f;
                BlockPos center = target.getPosition();
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            BlockPos p = center.offset(dx, dy, dz);
                            if (client.level.getBlockState(p).getBlock() == expected) {
                                box(quads, pose, camPos, p, outlineColor, outlineAlpha);
                            }
                        }
                    }
                }
            }
        }

        // ===== Labels =====
        if (OrderedWaypointManager.isEditMode()) {
            for (OrderedWaypoint wp : route) {
                label(buffers, viewMatrix, cameraState, wp);
            }
        } else {
            if (current != null) {
                label(buffers, viewMatrix, cameraState, current);
            }
            for (OrderedWaypoint next : nextWaypoints) {
                if (next != current) {
                    label(buffers, viewMatrix, cameraState, next);
                }
            }
        }

        buffers.endBatch();
    }

    private static void box(VertexConsumer buffer, Matrix4f pose, Vec3 camPos, BlockPos blockPos,
                            float[] color, float alpha) {
        // Slightly shrink the box to avoid Z-fighting with blocks
        float offset = 0.002f;
        float minX = (float) (blockPos.getX() - camPos.x) + offset;
        float minY = (float) (blockPos.getY() - camPos.y) + offset;
        float minZ = (float) (blockPos.getZ() - camPos.z) + offset;
        float maxX = minX + 1.0f - (offset * 2);
        float maxY = minY + 1.0f - (offset * 2);
        float maxZ = minZ + 1.0f - (offset * 2);

        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
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

    private static void label(MultiBufferSource.BufferSource buffers, Matrix4fc viewMatrix,
                              CameraRenderState cameraState, OrderedWaypoint waypoint) {
        Minecraft client = Minecraft.getInstance();
        BlockPos pos = waypoint.getPosition();
        Vec3 camPos = cameraState.pos;

        double x = pos.getX() + 0.5 - camPos.x;
        double y = pos.getY() + 1.5 - camPos.y;
        double z = pos.getZ() + 0.5 - camPos.z;

        double distance = Math.sqrt(x * x + y * y + z * z);
        if (distance > 64) return;

        StringBuilder textBuilder = new StringBuilder();
        if (OrderedWaypointManager.isShowName()) {
            textBuilder.append("#").append(waypoint.getIndex());
        }
        if (OrderedWaypointManager.isShowDistance()) {
            if (textBuilder.length() > 0) textBuilder.append(" ");
            textBuilder.append(String.format("%.1fm", distance));
        }
        String text = textBuilder.toString();
        if (text.isEmpty()) return;

        PoseStack matrices = new PoseStack();
        matrices.mulPose(viewMatrix);
        matrices.translate(x, y, z);
        matrices.mulPose(cameraState.orientation);
        float scale = (float) (0.025 * Math.max(1, distance / 10));
        matrices.scale(scale, -scale, scale);

        Font font = client.font;
        float width = font.width(text);
        font.drawInBatch(text, -width / 2.0f, 0, 0xFFFFFFFF, false,
            matrices.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, FULL_BRIGHT);
    }
}
