package forfun.miningqol.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class BlockOutlineRenderer {
    private static boolean enabled = false;
    private static OutlineMode mode = OutlineMode.BOTH;
    private static float red = 0.0f;
    private static float green = 0.0f;
    private static float blue = 0.0f;
    private static float alpha = 0.4f;

    public enum OutlineMode {
        OUTLINE_ONLY,
        FILLED,
        BOTH
    }

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setMode(OutlineMode newMode) {
        mode = newMode;
    }

    public static OutlineMode getMode() {
        return mode;
    }

    public static void setColor(float r, float g, float b, float a) {
        red = r;
        green = g;
        blue = b;
        alpha = a;
    }

    public static float getRed() {
        return red;
    }

    public static float getGreen() {
        return green;
    }

    public static float getBlue() {
        return blue;
    }

    public static float getAlpha() {
        return alpha;
    }

    public static void render(MatrixStack matrices, Camera camera) {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos blockPos = blockHit.getBlockPos();

        
        BlockState blockState = client.world.getBlockState(blockPos);
        VoxelShape shape = blockState.getOutlineShape(client.world, blockPos);

        if (shape.isEmpty()) return;

        
        boolean isTransparent = !blockState.isOpaque();
        float effectiveAlpha = isTransparent ? 0.4f : alpha;

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);

        
        for (Box box : shape.getBoundingBoxes()) {
            double minX = blockPos.getX() + box.minX - cameraX;
            double minY = blockPos.getY() + box.minY - cameraY;
            double minZ = blockPos.getZ() + box.minZ - cameraZ;
            double maxX = blockPos.getX() + box.maxX - cameraX;
            double maxY = blockPos.getY() + box.maxY - cameraY;
            double maxZ = blockPos.getZ() + box.maxZ - cameraZ;

            
            
            if (mode == OutlineMode.FILLED || mode == OutlineMode.BOTH) {
                
                float adjustedAlpha = effectiveAlpha * 0.5f;
                DebugRenderer.drawBox(matrices, immediate,
                    minX, minY, minZ,
                    maxX, maxY, maxZ,
                    red, green, blue, adjustedAlpha);
                immediate.draw();
            }

            if (mode == OutlineMode.OUTLINE_ONLY || mode == OutlineMode.BOTH) {
                
                float outlineAlpha = isTransparent ? 0.4f : (alpha * 2.0f);
                outlineAlpha = Math.max(0.0f, Math.min(1.0f, outlineAlpha)) * 0.5f; 
                double expand = 0.002;
                DebugRenderer.drawBox(matrices, immediate,
                    minX - expand, minY - expand, minZ - expand,
                    maxX + expand, maxY + expand, maxZ + expand,
                    red, green, blue, outlineAlpha);
                immediate.draw();
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }
}
