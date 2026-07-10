package forfun.miningqol.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Heatmap overlay that highlights the best blocks to mine (clay / red sandstone)
 * by clustering. 26.1.2 port of the 1.21.x EfficientMinerOverlay — draws filled
 * see-through boxes (like {@link ShaftESP}) instead of debug line boxes.
 */
public class EfficientMinerOverlay {
    private static final int FULL_BRIGHT = 15728880;

    private static boolean enabled = false;
    private static boolean useOldHeatmap = false;
    private static final List<BlockData> blocks = new ArrayList<>();

    private static final String[] TARGET_BLOCKS = { "clay", "red_sandstone" };
    private static final String[] AIR_TYPES = { "air", "cave_air", "void_air", "snow" };

    private static class BlockData {
        final int x, y, z, priority;
        BlockData(int x, int y, int z, int priority) {
            this.x = x; this.y = y; this.z = z; this.priority = priority;
        }
    }

    public static void setEnabled(boolean enable) { enabled = enable; }
    public static boolean isEnabled() { return enabled; }
    public static void setUseOldHeatmap(boolean useOld) { useOldHeatmap = useOld; }
    public static boolean isUsingOldHeatmap() { return useOldHeatmap; }

    public static void tick() {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        blocks.clear();

        int pX = (int) Math.floor(client.player.getX());
        int pY = (int) Math.floor(client.player.getY());
        int pZ = (int) Math.floor(client.player.getZ());

        for (int x = pX - 6; x <= pX + 6; x++) {
            for (int y = pY - 6; y <= pY + 6; y++) {
                for (int z = pZ - 6; z <= pZ + 6; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isTargetBlock(client.level, pos) && isVisible(client.level, pos)) {
                        blocks.add(new BlockData(x, y, z, calculatePriority(client.level, pos)));
                    }
                }
            }
        }
    }

    public static void render(CameraRenderState cameraState, Matrix4fc viewMatrix) {
        if (!enabled || blocks.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        Vec3 cam = cameraState.pos;
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        Matrix4f pose = new Matrix4f(viewMatrix);
        VertexConsumer quads = buffers.getBuffer(RenderTypes.textBackgroundSeeThrough());

        for (BlockData block : blocks) {
            float[] color = getColorForPriority(block.priority);
            float alpha = (0.1f + (block.priority / 10.0f)) * 0.5f;

            float minX = (float) (block.x - cam.x);
            float minY = (float) (block.y - cam.y - 0.001);
            float minZ = (float) (block.z - cam.z);
            float maxX = (float) (block.x + 1.001 - cam.x);
            float maxY = (float) (block.y + 1.002 - cam.y);
            float maxZ = (float) (block.z + 1.001 - cam.z);

            box(quads, pose, minX, minY, minZ, maxX, maxY, maxZ, color[0], color[1], color[2], alpha);
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

        quad(buffer, pose, r, g, b, a, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        quad(buffer, pose, r, g, b, a, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        quad(buffer, pose, r, g, b, a, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
        quad(buffer, pose, r, g, b, a, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        quad(buffer, pose, r, g, b, a, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
        quad(buffer, pose, r, g, b, a, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose, int r, int g, int b, int a,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        buffer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a).setLight(FULL_BRIGHT);
    }

    private static boolean isTargetBlock(ClientLevel level, BlockPos pos) {
        String id = level.getBlockState(pos).getBlock().getDescriptionId();
        for (String target : TARGET_BLOCKS) {
            if (id.contains(target)) return true;
        }
        return false;
    }

    private static boolean isVisible(ClientLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock().getDescriptionId().contains("bedrock")) return false;

        BlockPos[] adjacent = { pos.above(), pos.below(), pos.east(), pos.west(), pos.north(), pos.south() };
        for (BlockPos adjPos : adjacent) {
            String id = level.getBlockState(adjPos).getBlock().getDescriptionId();
            for (String airType : AIR_TYPES) {
                if (id.contains(airType)) return true;
            }
        }
        return false;
    }

    private static int calculatePriority(ClientLevel level, BlockPos pos) {
        int priority = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (isTargetBlock(level, pos.offset(dx, dy, dz))) priority++;
                }
            }
        }
        return Math.min(priority, 9);
    }

    private static float[] getColorForPriority(int priority) {
        if (priority >= 10) priority = 1;

        if (useOldHeatmap) {
            return switch (priority) {
                case 1 -> new float[]{20/255f, 90/255f, 38/255f};
                case 2 -> new float[]{42/255f, 230/255f, 92/255f};
                case 3 -> new float[]{180/255f, 252/255f, 69/255f};
                case 4 -> new float[]{180/255f, 177/255f, 31/255f};
                case 5 -> new float[]{180/255f, 31/255f, 45/255f};
                case 6 -> new float[]{212/255f, 57/255f, 229/255f};
                case 7 -> new float[]{89/255f, 33/255f, 95/255f};
                case 8 -> new float[]{62/255f, 56/255f, 216/255f};
                default -> new float[]{0f, 0f, 0f};
            };
        } else {
            if (priority < 3) {
                return new float[]{20/255f, 90/255f, 38/255f};
            } else if (priority < 5) {
                return new float[]{145/255f, 23/255f, 23/255f};
            } else if (priority < 7) {
                return new float[]{104/255f, 210/255f, 249/255f};
            } else {
                return new float[]{49/255f, 41/255f, 165/255f};
            }
        }
    }
}
