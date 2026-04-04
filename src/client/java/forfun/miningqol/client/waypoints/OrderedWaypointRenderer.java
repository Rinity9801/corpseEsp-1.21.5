package forfun.miningqol.client.waypoints;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrderedWaypointRenderer {

    public static void render(MatrixStack matrices, Camera camera) {
        if (!OrderedWaypointManager.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        List<OrderedWaypoint> route = OrderedWaypointManager.getCurrentRoute();
        if (route.isEmpty()) return;

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        OrderedWaypoint current = OrderedWaypointManager.getCurrentWaypoint();
        OrderedWaypoint prev = OrderedWaypointManager.getPreviousWaypoint();
        List<OrderedWaypoint> nextWaypoints = OrderedWaypointManager.getNextWaypoints(
            OrderedWaypointManager.getNextCount()
        );

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);

        // ===== PHASE 1: Render ALL waypoint boxes first (with depth test disabled) =====

        // Render current waypoint box
        if (current != null) {
            float[] currColor = OrderedWaypointManager.getCurrentWaypointColor();
            float currAlpha = OrderedWaypointManager.getCurrentWaypointAlpha();
            renderWaypointBox(matrices, immediate, camera, current.getPosition(), currColor, currAlpha);
        }

        // Show all waypoints mode
        if (OrderedWaypointManager.isShowAll()) {
            float[] allColor = OrderedWaypointManager.getShowAllWaypointColor();
            float allAlpha = OrderedWaypointManager.getShowAllWaypointAlpha();
            for (OrderedWaypoint wp : route) {
                if (wp != current && wp != prev && !nextWaypoints.contains(wp)) {
                    renderWaypointBox(matrices, immediate, camera, wp.getPosition(), allColor, allAlpha);
                }
            }
        }

        // Render previous waypoint box
        if (prev != null && prev != current) {
            float[] prevColor = OrderedWaypointManager.getPreviousWaypointColor();
            float prevAlpha = OrderedWaypointManager.getPreviousWaypointAlpha();
            renderWaypointBox(matrices, immediate, camera, prev.getPosition(), prevColor, prevAlpha);
        }

        // Render next waypoints boxes with decreasing alpha
        float[] nextColor = OrderedWaypointManager.getNextWaypointColor();
        float nextAlpha = OrderedWaypointManager.getNextWaypointAlpha();
        for (int i = 0; i < nextWaypoints.size(); i++) {
            OrderedWaypoint next = nextWaypoints.get(i);
            if (next != current) {
                // Calculate decreasing alpha: first waypoint has full alpha, subsequent ones fade
                float alphaMultiplier = 1.0f - (i * 0.25f);
                if (alphaMultiplier < 0.2f) alphaMultiplier = 0.2f; // Minimum 20% alpha
                float waypointAlpha = nextAlpha * alphaMultiplier;

                renderWaypointBox(matrices, immediate, camera, next.getPosition(), nextColor, waypointAlpha);
            }
        }

        // Render wrong waypoints (from lobby check) in bright red with full opacity
        List<Integer> wrongWaypoints = OrderedWaypointManager.getWrongWaypoints();
        if (!wrongWaypoints.isEmpty()) {
            float[] wrongColor = {1.0f, 0.0f, 0.0f}; // Bright red
            for (OrderedWaypoint wp : route) {
                if (wrongWaypoints.contains(wp.getIndex())) {
                    renderWaypointBox(matrices, immediate, camera, wp.getPosition(), wrongColor, 1.0f);
                }
            }
        }

        // ===== PHASE 2: Render ALL labels (after all boxes are drawn) =====

        // Render current waypoint label
        if (current != null) {
            renderWaypointLabel(matrices, camera, current);
        }

        // Render next waypoints labels
        for (int i = 0; i < nextWaypoints.size(); i++) {
            OrderedWaypoint next = nextWaypoints.get(i);
            if (next != current) {
                renderWaypointLabel(matrices, camera, next);
            }
        }

        // ===== PHASE 3: Render trace line =====
        OrderedWaypoint target = OrderedWaypointManager.getNextWaypoint();
        if (target != null && OrderedWaypointManager.isTraceLineEnabled()) {
            float[] lineColor = OrderedWaypointManager.getTraceLineColor();
            float lineAlpha = OrderedWaypointManager.getTraceLineAlpha();
            drawLineFromCursor(matrices, camera, target.getPosition(), lineColor, lineAlpha);
        }

        // ===== PHASE 4: Render block outlines around next waypoint =====
        if (target != null && OrderedWaypointManager.isBlockOutlineAroundWaypoint()) {
            renderBlockOutlines(matrices, camera, target.getPosition());
        }

        // Setup mode rendering
        if (OrderedWaypointManager.isSetupMode()) {
            renderSetupMode(matrices, immediate, camera, route);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    private static void renderWaypointBox(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                          Camera camera, BlockPos pos, float[] color, float alpha) {
        //? if is1_21_11 {
        Vec3d cameraPos = camera.getCameraPos();
        //?} else {
        /*Vec3d cameraPos = camera.getPos();
        *///?}

        // Slightly shrink the box to avoid Z-fighting with blocks
        float offset = 0.002f;
        float minX = (float) (pos.getX() - cameraPos.x) + offset;
        float minY = (float) (pos.getY() - cameraPos.y) + offset;
        float minZ = (float) (pos.getZ() - cameraPos.z) + offset;
        float maxX = minX + 1.0f - (offset * 2);
        float maxY = minY + 1.0f - (offset * 2);
        float maxZ = minZ + 1.0f - (offset * 2);

        //? if is1_21_11 {
        VertexConsumer buffer = immediate.getBuffer(RenderLayers.debugQuads());
        //?} else {
        /*VertexConsumer buffer = immediate.getBuffer(RenderLayer.getDebugQuads());
        *///?}
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        float r = color[0];
        float g = color[1];
        float b = color[2];

        // Bottom face
        buffer.vertex(posMatrix, minX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, minX, minY, maxZ).color(r, g, b, alpha);

        // Top face
        buffer.vertex(posMatrix, minX, maxY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(r, g, b, alpha);

        // North face
        buffer.vertex(posMatrix, minX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, minX, maxY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, minY, minZ).color(r, g, b, alpha);

        // South face
        buffer.vertex(posMatrix, minX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(r, g, b, alpha);

        // West face
        buffer.vertex(posMatrix, minX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, minX, minY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, minX, maxY, minZ).color(r, g, b, alpha);

        // East face
        buffer.vertex(posMatrix, maxX, minY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(r, g, b, alpha);
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(r, g, b, alpha);

        // Disable depth test right before drawing to ensure it renders through walls
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        //? if is1_21_11 {
        immediate.draw(RenderLayers.debugQuads());
        //?} else {
        /*immediate.draw(RenderLayer.getDebugQuads());
        *///?}
    }

    private static void renderWaypointLabel(MatrixStack matrices, Camera camera, OrderedWaypoint waypoint) {
        MinecraftClient client = MinecraftClient.getInstance();
        //? if is1_21_11 {
        Vec3d cameraPos = camera.getCameraPos();
        //?} else {
        /*Vec3d cameraPos = camera.getPos();
        *///?}
        BlockPos pos = waypoint.getPosition();

        double x = pos.getX() + 0.5 - cameraPos.x;
        double y = pos.getY() + 1.5 - cameraPos.y;
        double z = pos.getZ() + 0.5 - cameraPos.z;

        double distance = Math.sqrt(x * x + y * y + z * z);
        if (distance > 64) return;

        StringBuilder textBuilder = new StringBuilder();

        // Add waypoint number if show name is enabled
        if (OrderedWaypointManager.isShowName()) {
            textBuilder.append("#").append(waypoint.getIndex());
        }

        // Add distance if enabled
        if (OrderedWaypointManager.isShowDistance()) {
            if (textBuilder.length() > 0) textBuilder.append(" ");
            textBuilder.append(String.format("%.1fm", distance));
        }

        String text = textBuilder.toString();
        if (text.isEmpty()) {
            return;
        }

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());
        float scale = (float) (0.025 * Math.max(1, distance / 10));
        matrices.scale(scale, -scale, scale);

        int textWidth = client.textRenderer.getWidth(text);

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        // Disable depth test so text shows through walls
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        client.textRenderer.draw(
            text,
            -textWidth / 2.0f,
            0,
            0xFFFFFFFF,
            false,
            matrices.peek().getPositionMatrix(),
            immediate,
            TextRenderer.TextLayerType.SEE_THROUGH,
            0x40000000,
            15728880
        );

        immediate.draw();

        matrices.pop();
    }

    private static void drawLineFromCursor(MatrixStack matrices, Camera camera, BlockPos target, float[] color, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        //? if is1_21_11 {
        VertexConsumer buffer = immediate.getBuffer(RenderLayers.lines());
        //?} else {
        /*VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());
        *///?}

        //? if is1_21_11 {
        Vec3d cameraPos = camera.getCameraPos();
        //?} else {
        /*Vec3d cameraPos = camera.getPos();
        *///?}
        Vec3d targetPos = new Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        // Get a point slightly in front of the camera (like Skyblocker)
        Vector3f forward = new Vector3f(0, 0, -1);
        camera.getRotation().transform(forward);
        Vec3d cursorPoint = cameraPos.add(forward.x, forward.y, forward.z);

        // Calculate positions relative to camera (same as waypoint boxes)
        float startX = (float) (cursorPoint.x - cameraPos.x);
        float startY = (float) (cursorPoint.y - cameraPos.y);
        float startZ = (float) (cursorPoint.z - cameraPos.z);
        float endX = (float) (targetPos.x - cameraPos.x);
        float endY = (float) (targetPos.y - cameraPos.y);
        float endZ = (float) (targetPos.z - cameraPos.z);

        // Calculate normal from cursor to target
        Vector3f normal = new Vector3f(endX - startX, endY - startY, endZ - startZ).normalize();

        // Use the matrices from the render context (same as waypoint boxes)
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // Draw line from cursor point to target
        buffer.vertex(posMatrix, startX, startY, startZ)
              .color(color[0], color[1], color[2], alpha)
              .normal(normal.x(), normal.y(), normal.z());

        buffer.vertex(posMatrix, endX, endY, endZ)
              .color(color[0], color[1], color[2], alpha)
              .normal(normal.x(), normal.y(), normal.z());

        //? if is1_21_11 {
        immediate.draw(RenderLayers.lines());
        //?} else {
        /*immediate.draw(RenderLayer.getLines());
        *///?}
    }

    private static void drawLine(MatrixStack matrices, Vec3d cameraPos, Vec3d from, Vec3d to, float[] color, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        //? if is1_21_11 {
        VertexConsumer buffer = immediate.getBuffer(RenderLayers.lines());
        //?} else {
        /*VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());
        *///?}

        // Calculate positions relative to camera (same as waypoint boxes)
        float startX = (float) (from.x - cameraPos.x);
        float startY = (float) (from.y - cameraPos.y);
        float startZ = (float) (from.z - cameraPos.z);
        float endX = (float) (to.x - cameraPos.x);
        float endY = (float) (to.y - cameraPos.y);
        float endZ = (float) (to.z - cameraPos.z);

        // Calculate normal from start to end
        Vector3f normal = new Vector3f(endX - startX, endY - startY, endZ - startZ).normalize();

        // Use the matrices from the render context (same as waypoint boxes)
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        buffer.vertex(posMatrix, startX, startY, startZ)
              .color(color[0], color[1], color[2], alpha)
              .normal(normal.x(), normal.y(), normal.z());

        buffer.vertex(posMatrix, endX, endY, endZ)
              .color(color[0], color[1], color[2], alpha)
              .normal(normal.x(), normal.y(), normal.z());

        //? if is1_21_11 {
        immediate.draw(RenderLayers.lines());
        //?} else {
        /*immediate.draw(RenderLayer.getLines());
        *///?}
    }

    private static void renderSetupMode(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                        Camera camera, List<OrderedWaypoint> route) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Vec3d playerPos = client.player.getEyePos();
        float setupRange = OrderedWaypointManager.getSetupModeRange();
        float[] setupColor = OrderedWaypointManager.getSetupModeColor();
        float setupAlpha = OrderedWaypointManager.getSetupModeAlpha();
        float[] setupLineColor = OrderedWaypointManager.getSetupModeLineColor();
        float setupLineAlpha = OrderedWaypointManager.getSetupModeLineAlpha();

        // Show nearby waypoints in setup mode
        for (OrderedWaypoint wp : route) {
            double dist = wp.distanceTo(BlockPos.ofFloored(playerPos));
            if (dist < setupRange) {
                renderWaypointBox(matrices, immediate, camera, wp.getPosition(), setupColor, setupAlpha);
            }
        }

        // Draw lines between consecutive waypoints in setup range
        //? if is1_21_11 {
        Vec3d cameraPos = camera.getCameraPos();
        //?} else {
        /*Vec3d cameraPos = camera.getPos();
        *///?}
        for (int i = 0; i < route.size(); i++) {
            OrderedWaypoint current = route.get(i);
            OrderedWaypoint next = route.get((i + 1) % route.size());

            double distCurrent = current.distanceTo(BlockPos.ofFloored(playerPos));
            double distNext = next.distanceTo(BlockPos.ofFloored(playerPos));

            if (distCurrent < setupRange || distNext < setupRange) {
                Vec3d fromPos = new Vec3d(
                    current.getPosition().getX() + 0.5,
                    current.getPosition().getY() + 0.5,
                    current.getPosition().getZ() + 0.5
                );
                Vec3d toPos = new Vec3d(
                    next.getPosition().getX() + 0.5,
                    next.getPosition().getY() + 0.5,
                    next.getPosition().getZ() + 0.5
                );

                drawLine(matrices, cameraPos, fromPos, toPos, setupLineColor, setupLineAlpha);
            }
        }
    }

    private static void renderBlockOutlines(MatrixStack matrices, Camera camera, BlockPos waypointPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        String blockId = OrderedWaypointManager.getLobbyCheckBlock();
        Block expectedBlock = Registries.BLOCK.get(Identifier.of(blockId));
        if (expectedBlock == null) return;

        int radius = OrderedWaypointManager.getBlockOutlineRadius();
        float[] color = OrderedWaypointManager.getBlockOutlineColor();
        float alpha = OrderedWaypointManager.getBlockOutlineAlpha();

        // Find all matching blocks in radius
        Set<BlockPos> matchingBlocks = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos checkPos = waypointPos.add(dx, dy, dz);
                    BlockState state = client.world.getBlockState(checkPos);
                    if (state.getBlock() == expectedBlock) {
                        matchingBlocks.add(checkPos);
                    }
                }
            }
        }

        if (matchingBlocks.isEmpty()) return;

        //? if is1_21_11 {
        Vec3d cameraPos = camera.getCameraPos();
        //?} else {
        /*Vec3d cameraPos = camera.getPos();
        *///?}
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        //? if is1_21_11 {
        VertexConsumer buffer = immediate.getBuffer(RenderLayers.lines());
        //?} else {
        /*VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());
        *///?}
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        float r = color[0];
        float g = color[1];
        float b = color[2];

        // Draw connected outlines - only draw external edges
        for (BlockPos pos : matchingBlocks) {
            float minX = (float) (pos.getX() - cameraPos.x);
            float minY = (float) (pos.getY() - cameraPos.y);
            float minZ = (float) (pos.getZ() - cameraPos.z);
            float maxX = minX + 1.0f;
            float maxY = minY + 1.0f;
            float maxZ = minZ + 1.0f;

            // Check which faces are exposed (not adjacent to another matching block)
            boolean downExposed = !matchingBlocks.contains(pos.down());
            boolean upExposed = !matchingBlocks.contains(pos.up());
            boolean northExposed = !matchingBlocks.contains(pos.north());
            boolean southExposed = !matchingBlocks.contains(pos.south());
            boolean westExposed = !matchingBlocks.contains(pos.west());
            boolean eastExposed = !matchingBlocks.contains(pos.east());

            // Helper positions
            BlockPos northPos = pos.north();
            BlockPos southPos = pos.south();
            BlockPos westPos = pos.west();
            BlockPos eastPos = pos.east();
            BlockPos downPos = pos.down();
            BlockPos upPos = pos.up();

            boolean northHasDownExposed = matchingBlocks.contains(northPos) && !matchingBlocks.contains(northPos.down());
            boolean northHasUpExposed = matchingBlocks.contains(northPos) && !matchingBlocks.contains(northPos.up());
            boolean southHasDownExposed = matchingBlocks.contains(southPos) && !matchingBlocks.contains(southPos.down());
            boolean southHasUpExposed = matchingBlocks.contains(southPos) && !matchingBlocks.contains(southPos.up());
            boolean westHasDownExposed = matchingBlocks.contains(westPos) && !matchingBlocks.contains(westPos.down());
            boolean westHasUpExposed = matchingBlocks.contains(westPos) && !matchingBlocks.contains(westPos.up());
            boolean eastHasDownExposed = matchingBlocks.contains(eastPos) && !matchingBlocks.contains(eastPos.down());
            boolean eastHasUpExposed = matchingBlocks.contains(eastPos) && !matchingBlocks.contains(eastPos.up());

            boolean northHasWestExposed = matchingBlocks.contains(northPos) && !matchingBlocks.contains(northPos.west());
            boolean northHasEastExposed = matchingBlocks.contains(northPos) && !matchingBlocks.contains(northPos.east());
            boolean southHasWestExposed = matchingBlocks.contains(southPos) && !matchingBlocks.contains(southPos.west());
            boolean southHasEastExposed = matchingBlocks.contains(southPos) && !matchingBlocks.contains(southPos.east());

            boolean downHasNorthExposed = matchingBlocks.contains(downPos) && !matchingBlocks.contains(downPos.north());
            boolean downHasSouthExposed = matchingBlocks.contains(downPos) && !matchingBlocks.contains(downPos.south());
            boolean downHasWestExposed = matchingBlocks.contains(downPos) && !matchingBlocks.contains(downPos.west());
            boolean downHasEastExposed = matchingBlocks.contains(downPos) && !matchingBlocks.contains(downPos.east());
            boolean upHasNorthExposed = matchingBlocks.contains(upPos) && !matchingBlocks.contains(upPos.north());
            boolean upHasSouthExposed = matchingBlocks.contains(upPos) && !matchingBlocks.contains(upPos.south());
            boolean upHasWestExposed = matchingBlocks.contains(upPos) && !matchingBlocks.contains(upPos.west());
            boolean upHasEastExposed = matchingBlocks.contains(upPos) && !matchingBlocks.contains(upPos.east());

            boolean westHasNorthExposed = matchingBlocks.contains(westPos) && !matchingBlocks.contains(westPos.north());
            boolean westHasSouthExposed = matchingBlocks.contains(westPos) && !matchingBlocks.contains(westPos.south());
            boolean eastHasNorthExposed = matchingBlocks.contains(eastPos) && !matchingBlocks.contains(eastPos.north());
            boolean eastHasSouthExposed = matchingBlocks.contains(eastPos) && !matchingBlocks.contains(eastPos.south());

            // Bottom face edges
            if (downExposed) {
                if (!northHasDownExposed) drawEdge(buffer, posMatrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, alpha);
                if (!southHasDownExposed) drawEdge(buffer, posMatrix, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, alpha);
                if (!westHasDownExposed) drawEdge(buffer, posMatrix, minX, minY, minZ, minX, minY, maxZ, r, g, b, alpha);
                if (!eastHasDownExposed) drawEdge(buffer, posMatrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, alpha);
            }

            // Top face edges
            if (upExposed) {
                if (!northHasUpExposed) drawEdge(buffer, posMatrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, alpha);
                if (!southHasUpExposed) drawEdge(buffer, posMatrix, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, alpha);
                if (!westHasUpExposed) drawEdge(buffer, posMatrix, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, alpha);
                if (!eastHasUpExposed) drawEdge(buffer, posMatrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
            }

            // North face edges
            if (northExposed) {
                if (!downExposed && !downHasNorthExposed) drawEdge(buffer, posMatrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, alpha);
                if (!upExposed && !upHasNorthExposed) drawEdge(buffer, posMatrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, alpha);
                if (!westHasNorthExposed) drawEdge(buffer, posMatrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, alpha);
                if (!eastHasNorthExposed) drawEdge(buffer, posMatrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, alpha);
            }

            // South face edges
            if (southExposed) {
                if (!downExposed && !downHasSouthExposed) drawEdge(buffer, posMatrix, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, alpha);
                if (!upExposed && !upHasSouthExposed) drawEdge(buffer, posMatrix, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, alpha);
                if (!westHasSouthExposed) drawEdge(buffer, posMatrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, alpha);
                if (!eastHasSouthExposed) drawEdge(buffer, posMatrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, alpha);
            }

            // West face edges
            if (westExposed) {
                if (!downExposed && !downHasWestExposed) drawEdge(buffer, posMatrix, minX, minY, minZ, minX, minY, maxZ, r, g, b, alpha);
                if (!upExposed && !upHasWestExposed) drawEdge(buffer, posMatrix, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, alpha);
                if (!northExposed && !northHasWestExposed) drawEdge(buffer, posMatrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, alpha);
                if (!southExposed && !southHasWestExposed) drawEdge(buffer, posMatrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, alpha);
            }

            // East face edges
            if (eastExposed) {
                if (!downExposed && !downHasEastExposed) drawEdge(buffer, posMatrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, alpha);
                if (!upExposed && !upHasEastExposed) drawEdge(buffer, posMatrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
                if (!northExposed && !northHasEastExposed) drawEdge(buffer, posMatrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, alpha);
                if (!southExposed && !southHasEastExposed) drawEdge(buffer, posMatrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, alpha);
            }
        }

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        //? if is1_21_11 {
        immediate.draw(RenderLayers.lines());
        //?} else {
        /*immediate.draw(RenderLayer.getLines());
        *///?}
    }

    private static void drawEdge(VertexConsumer buffer, Matrix4f posMatrix,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float r, float g, float b, float alpha) {
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

        buffer.vertex(posMatrix, x1, y1, z1).color(r, g, b, alpha).normal(nx, ny, nz);
        buffer.vertex(posMatrix, x2, y2, z2).color(r, g, b, alpha).normal(nx, ny, nz);
    }
}
