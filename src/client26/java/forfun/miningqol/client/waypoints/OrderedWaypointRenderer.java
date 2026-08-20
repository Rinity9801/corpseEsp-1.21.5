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
 *  - lines:  RenderTypes.lines() (depth-tested; no stock see-through line layer, which
 *            is why the block outline draws its edges as thin quads — see buildOutlineEdges)
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
        if (target != null && OrderedWaypointManager.isBlockOutlineAroundWaypoint()) {
            refreshOutlineCache(client, target.getPosition());
            float[] outlineColor = OrderedWaypointManager.getBlockOutlineColor();
            float outlineAlpha = OrderedWaypointManager.getBlockOutlineAlpha();
            float dial = OrderedWaypointManager.getBlockOutlineThickness();
            // Only the camera-relative transform happens per frame. Colour, alpha and thickness are
            // applied here rather than baked in, so changing them in the GUI still looks instant.
            // Faces first so the edges land on top of the fill rather than under it.
            if (OrderedWaypointManager.isBlockOutlineFill()) {
                for (double[] f : cachedFaces) {
                    face(quads, pose, camPos, f[0], f[1], f[2], (int) f[3],
                        outlineColor, outlineAlpha * FILL_ALPHA);
                }
            }
            for (double[] e : cachedEdges) {
                edge(quads, pose, camPos, e[0], e[1], e[2], e[3], e[4], e[5],
                    outlineColor, outlineAlpha, dial);
            }
        } else if (!cachedEdges.isEmpty()) {
            cachedEdges = java.util.List.of();
            cachedFaces = java.util.List.of();
            outlineCenter = null;
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

    /**
     * Scales the configured thickness into blocks.
     *
     * <p>The setting is a 1-9 dial rather than a raw block measurement — 1.5 lands on 0.015, which
     * is what the hardcoded value was and roughly what 1.21.11's GL lines looked like.
     */
    /**
     * Blocks of edge half-thickness per dial unit, per block of distance.
     *
     * <p>Scaled by distance so an edge covers a roughly constant number of PIXELS. Sizing edges in
     * world space meant they ballooned up close and thinned to nothing far away, which is what made
     * the outline look chunky and uneven.
     */
    private static final float EDGE_SCREEN_SCALE = 0.001f;
    /** Keeps a very near edge visible and a very far one from swallowing the vein. */
    private static final float EDGE_MIN = 0.004f;
    private static final float EDGE_MAX = 0.08f;

    /**
     * Cached outline geometry, in world coordinates.
     *
     * <p>The scan and the edge topology only change when blocks are mined or the target moves, but
     * they were being redone every frame: at radius 3 that is 343 {@code getBlockState} calls and as
     * many {@code BlockPos} allocations per frame, plus an {@code Identifier.parse} and a registry
     * lookup, plus ~30 more allocations for every matching block. Rebuilding a few times a second
     * instead and replaying the result cuts almost all of it.
     *
     * <p>World coordinates rather than camera-relative, so moving the camera does not invalidate it.
     */
    private static java.util.List<double[]> cachedEdges = java.util.List.of();
    /** Exposed faces of the matching blocks, as {@code {x, y, z, faceCode}} — the optional fill. */
    private static java.util.List<double[]> cachedFaces = java.util.List.of();
    /** The fill sits well under the outline so the edges stay the thing you read. */
    private static final float FILL_ALPHA = 0.25f;
    private static BlockPos outlineCenter;
    private static long outlineBuiltAt;
    private static String outlineBlockId;
    private static int outlineRadius = -1;
    /** Blocks get mined while you look at them, so the cache still has to expire on its own. */
    private static final long OUTLINE_REFRESH_MS = 250;

    private static void refreshOutlineCache(Minecraft client, BlockPos center) {
        String blockId = OrderedWaypointManager.getLobbyCheckBlock();
        int radius = OrderedWaypointManager.getBlockOutlineRadius();
        long now = System.currentTimeMillis();

        boolean stale = outlineCenter == null
            || !outlineCenter.equals(center)
            || !blockId.equals(outlineBlockId)
            || radius != outlineRadius
            || now - outlineBuiltAt > OUTLINE_REFRESH_MS;
        if (!stale) return;

        outlineCenter = center;
        outlineBlockId = blockId;
        outlineRadius = radius;
        outlineBuiltAt = now;

        net.minecraft.world.level.block.Block expected;
        try {
            expected = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                net.minecraft.resources.Identifier.parse(blockId));
        } catch (Exception e) {
            cachedEdges = java.util.List.of();
            cachedFaces = java.util.List.of();
            return;
        }
        if (expected == null || expected == net.minecraft.world.level.block.Blocks.AIR) {
            cachedEdges = java.util.List.of();
            cachedFaces = java.util.List.of();
            return;
        }

        // One mutable cursor for the whole scan instead of a BlockPos per position.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        java.util.Set<BlockPos> matching = new java.util.HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (client.level.getBlockState(cursor).getBlock() == expected) {
                        matching.add(cursor.immutable());
                    }
                }
            }
        }

        cachedFaces = new java.util.ArrayList<>();
        cachedEdges = buildOutlineEdges(matching, cachedFaces);
    }

    /** {@code matching.contains(x, y, z)} without allocating a BlockPos for the lookup. */
    private static boolean has(java.util.Set<BlockPos> matching, BlockPos.MutableBlockPos cursor,
                               int x, int y, int z) {
        return matching.contains(cursor.set(x, y, z));
    }

    /**
     * Wireframe around the matching blocks, drawing only the edges on the outside of the shape.
     *
     * <p>This is 1.21.11's algorithm. The 26.1.2 port had replaced it with a translucent filled cube
     * per block, which reads as a blob rather than an outline once more than a couple of blocks match.
     *
     * <p>Edges are thin quads rather than {@code RenderTypes.lines()}: 1.21.11 got its see-through
     * look from {@code glDisable(GL_DEPTH_TEST)}, and there is no see-through line layer here, so
     * real lines would disappear behind terrain. Quads go through the same no-depth layer the boxes
     * use, which keeps both the wireframe look and the visible-through-walls behaviour.
     */
    private static java.util.List<double[]> buildOutlineEdges(java.util.Set<BlockPos> matching,
                                                             java.util.List<double[]> faces) {
        java.util.List<double[]> edges = new java.util.ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();

        for (BlockPos pos : matching) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            double minX = x, minY = y, minZ = z;
            double maxX = x + 1.0, maxY = y + 1.0, maxZ = z + 1.0;

            // A face is on the outside when no matching block sits against it. Each direction is
            // tested once and reused; the previous version called pos.north() twice per axis and
            // allocated a BlockPos for every one of these ~30 lookups.
            boolean downExposed = !has(matching, c, x, y - 1, z);
            boolean upExposed = !has(matching, c, x, y + 1, z);
            boolean northExposed = !has(matching, c, x, y, z - 1);
            boolean southExposed = !has(matching, c, x, y, z + 1);
            boolean westExposed = !has(matching, c, x - 1, y, z);
            boolean eastExposed = !has(matching, c, x + 1, y, z);

            // Only the outside faces: filling interior ones would stack translucent layers and turn
            // the vein into a solid blob, which is the look the wireframe replaced.
            if (downExposed) faces.add(new double[]{x, y, z, 0});
            if (upExposed) faces.add(new double[]{x, y, z, 1});
            if (northExposed) faces.add(new double[]{x, y, z, 2});
            if (southExposed) faces.add(new double[]{x, y, z, 3});
            if (westExposed) faces.add(new double[]{x, y, z, 4});
            if (eastExposed) faces.add(new double[]{x, y, z, 5});

            boolean north = !northExposed, south = !southExposed;
            boolean west = !westExposed, east = !eastExposed;
            boolean down = !downExposed, up = !upExposed;

            // An edge is interior when the neighbour sharing it also has that face exposed — the
            // two faces are coplanar, so the edge between them is not a silhouette.
            boolean northHasDownExposed = north && !has(matching, c, x, y - 1, z - 1);
            boolean northHasUpExposed = north && !has(matching, c, x, y + 1, z - 1);
            boolean southHasDownExposed = south && !has(matching, c, x, y - 1, z + 1);
            boolean southHasUpExposed = south && !has(matching, c, x, y + 1, z + 1);
            boolean westHasDownExposed = west && !has(matching, c, x - 1, y - 1, z);
            boolean westHasUpExposed = west && !has(matching, c, x - 1, y + 1, z);
            boolean eastHasDownExposed = east && !has(matching, c, x + 1, y - 1, z);
            boolean eastHasUpExposed = east && !has(matching, c, x + 1, y + 1, z);

            boolean northHasWestExposed = north && !has(matching, c, x - 1, y, z - 1);
            boolean northHasEastExposed = north && !has(matching, c, x + 1, y, z - 1);
            boolean southHasWestExposed = south && !has(matching, c, x - 1, y, z + 1);
            boolean southHasEastExposed = south && !has(matching, c, x + 1, y, z + 1);

            boolean downHasNorthExposed = down && !has(matching, c, x, y - 1, z - 1);
            boolean downHasSouthExposed = down && !has(matching, c, x, y - 1, z + 1);
            boolean downHasWestExposed = down && !has(matching, c, x - 1, y - 1, z);
            boolean downHasEastExposed = down && !has(matching, c, x + 1, y - 1, z);
            boolean upHasNorthExposed = up && !has(matching, c, x, y + 1, z - 1);
            boolean upHasSouthExposed = up && !has(matching, c, x, y + 1, z + 1);
            boolean upHasWestExposed = up && !has(matching, c, x - 1, y + 1, z);
            boolean upHasEastExposed = up && !has(matching, c, x + 1, y + 1, z);

            boolean westHasNorthExposed = west && !has(matching, c, x - 1, y, z - 1);
            boolean westHasSouthExposed = west && !has(matching, c, x - 1, y, z + 1);
            boolean eastHasNorthExposed = east && !has(matching, c, x + 1, y, z - 1);
            boolean eastHasSouthExposed = east && !has(matching, c, x + 1, y, z + 1);

            if (downExposed) {
                if (!northHasDownExposed) edges.add(new double[]{minX, minY, minZ, maxX, minY, minZ});
                if (!southHasDownExposed) edges.add(new double[]{minX, minY, maxZ, maxX, minY, maxZ});
                if (!westHasDownExposed) edges.add(new double[]{minX, minY, minZ, minX, minY, maxZ});
                if (!eastHasDownExposed) edges.add(new double[]{maxX, minY, minZ, maxX, minY, maxZ});
            }
            if (upExposed) {
                if (!northHasUpExposed) edges.add(new double[]{minX, maxY, minZ, maxX, maxY, minZ});
                if (!southHasUpExposed) edges.add(new double[]{minX, maxY, maxZ, maxX, maxY, maxZ});
                if (!westHasUpExposed) edges.add(new double[]{minX, maxY, minZ, minX, maxY, maxZ});
                if (!eastHasUpExposed) edges.add(new double[]{maxX, maxY, minZ, maxX, maxY, maxZ});
            }
            if (northExposed) {
                if (!downExposed && !downHasNorthExposed) edges.add(new double[]{minX, minY, minZ, maxX, minY, minZ});
                if (!upExposed && !upHasNorthExposed) edges.add(new double[]{minX, maxY, minZ, maxX, maxY, minZ});
                if (!westHasNorthExposed) edges.add(new double[]{minX, minY, minZ, minX, maxY, minZ});
                if (!eastHasNorthExposed) edges.add(new double[]{maxX, minY, minZ, maxX, maxY, minZ});
            }
            if (southExposed) {
                if (!downExposed && !downHasSouthExposed) edges.add(new double[]{minX, minY, maxZ, maxX, minY, maxZ});
                if (!upExposed && !upHasSouthExposed) edges.add(new double[]{minX, maxY, maxZ, maxX, maxY, maxZ});
                if (!westHasSouthExposed) edges.add(new double[]{minX, minY, maxZ, minX, maxY, maxZ});
                if (!eastHasSouthExposed) edges.add(new double[]{maxX, minY, maxZ, maxX, maxY, maxZ});
            }
            if (westExposed) {
                if (!downExposed && !downHasWestExposed) edges.add(new double[]{minX, minY, minZ, minX, minY, maxZ});
                if (!upExposed && !upHasWestExposed) edges.add(new double[]{minX, maxY, minZ, minX, maxY, maxZ});
                if (!northExposed && !northHasWestExposed) edges.add(new double[]{minX, minY, minZ, minX, maxY, minZ});
                if (!southExposed && !southHasWestExposed) edges.add(new double[]{minX, minY, maxZ, minX, maxY, maxZ});
            }
            if (eastExposed) {
                if (!downExposed && !downHasEastExposed) edges.add(new double[]{maxX, minY, minZ, maxX, minY, maxZ});
                if (!upExposed && !upHasEastExposed) edges.add(new double[]{maxX, maxY, minZ, maxX, maxY, maxZ});
                if (!northExposed && !northHasEastExposed) edges.add(new double[]{maxX, minY, minZ, maxX, maxY, minZ});
                if (!southExposed && !southHasEastExposed) edges.add(new double[]{maxX, minY, maxZ, maxX, maxY, maxZ});
            }
        }
        return edges;
    }

    /**
     * One outline edge, as a thin box so it reads as a line from any angle.
     *
     * <p>Only the two axes the edge does NOT run along are thickened, which keeps corners meeting
     * cleanly instead of overshooting.
     */
    /** One exposed block face, for the fill. {@code code} is down/up/north/south/west/east. */
    private static void face(VertexConsumer quads, Matrix4f pose, Vec3 camPos,
                             double bx, double by, double bz, int code,
                             float[] color, float alpha) {
        float o = 0.002f;   // nudge off the block face so it doesn't z-fight the terrain
        float minX = (float) (bx - camPos.x) + o;
        float minY = (float) (by - camPos.y) + o;
        float minZ = (float) (bz - camPos.z) + o;
        float maxX = minX + 1.0f - o * 2;
        float maxY = minY + 1.0f - o * 2;
        float maxZ = minZ + 1.0f - o * 2;

        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        int a = (int) (alpha * 255);

        switch (code) {
            case 0 -> quad(quads, pose, r, g, b, a, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
            case 1 -> quad(quads, pose, r, g, b, a, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
            case 2 -> quad(quads, pose, r, g, b, a, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
            case 3 -> quad(quads, pose, r, g, b, a, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
            case 4 -> quad(quads, pose, r, g, b, a, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
            default -> quad(quads, pose, r, g, b, a, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
        }
    }

    private static void edge(VertexConsumer quads, Matrix4f pose, Vec3 camPos,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             float[] color, float alpha, float dial) {
        // Half-thickness grows with distance, so apparent width stays put as you move.
        double mx = (x1 + x2) * 0.5 - camPos.x;
        double my = (y1 + y2) * 0.5 - camPos.y;
        double mz = (z1 + z2) * 0.5 - camPos.z;
        double dist = Math.sqrt(mx * mx + my * my + mz * mz);
        float t = (float) Math.max(EDGE_MIN, Math.min(EDGE_MAX, dial * EDGE_SCREEN_SCALE * dist));

        float tx = x1 == x2 ? t : 0f;
        float ty = y1 == y2 ? t : 0f;
        float tz = z1 == z2 ? t : 0f;

        float minX = (float) (Math.min(x1, x2) - camPos.x) - tx;
        float maxX = (float) (Math.max(x1, x2) - camPos.x) + tx;
        float minY = (float) (Math.min(y1, y2) - camPos.y) - ty;
        float maxY = (float) (Math.max(y1, y2) - camPos.y) + ty;
        float minZ = (float) (Math.min(z1, z2) - camPos.z) - tz;
        float maxZ = (float) (Math.max(z1, z2) - camPos.z) + tz;

        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        int a = (int) (alpha * 255);

        quad(quads, pose, r, g, b, a, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        quad(quads, pose, r, g, b, a, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        quad(quads, pose, r, g, b, a, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
        quad(quads, pose, r, g, b, a, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        quad(quads, pose, r, g, b, a, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
        quad(quads, pose, r, g, b, a, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
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
        if (waypoint.isEtherwarp() && textBuilder.length() > 0) {
            textBuilder.append(" (EW)");
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
