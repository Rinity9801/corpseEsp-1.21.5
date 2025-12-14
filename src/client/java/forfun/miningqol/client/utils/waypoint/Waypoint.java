package forfun.miningqol.client.utils.waypoint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class Waypoint {
    protected static final float DEFAULT_HIGHLIGHT_ALPHA = 0.3f;
    protected static final float DEFAULT_LINE_WIDTH = 5f;

    public final BlockPos pos;
    public final float[] colorComponents;
    public final float alpha;
    public final float lineWidth;
    public final boolean throughWalls;
    private boolean enabled;

    public Waypoint(BlockPos pos, float[] colorComponents) {
        this(pos, colorComponents, DEFAULT_HIGHLIGHT_ALPHA, DEFAULT_LINE_WIDTH, true, true);
    }

    public Waypoint(BlockPos pos, float[] colorComponents, float alpha, float lineWidth, boolean throughWalls, boolean enabled) {
        this.pos = pos;
        this.colorComponents = colorComponents;
        this.alpha = alpha;
        this.lineWidth = lineWidth;
        this.throughWalls = throughWalls;
        this.enabled = enabled;
    }

    public boolean shouldRender() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void render(MatrixStack matrices, Camera camera) {
        if (!shouldRender()) return;

        Vec3d cameraPos = camera.getPos();
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        double centerX = pos.getX() - cameraPos.x + 0.5;
        double centerZ = pos.getZ() - cameraPos.z + 0.5;

        try {
            double minX = pos.getX() - cameraPos.x;
            double minY = pos.getY() - cameraPos.y;
            double minZ = pos.getZ() - cameraPos.z;
            double maxX = minX + 1.0;
            double maxY = minY + 1.0;
            double maxZ = minZ + 1.0;

            if (throughWalls) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glEnable(GL11.GL_BLEND);
            }

            DebugRenderer.drawBox(matrices, immediate,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                colorComponents[0], colorComponents[1], colorComponents[2], alpha);

            immediate.draw();

            if (throughWalls) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_CULL_FACE);
            }

            double beaconWidth = 0.3;
            double beaconHeight = 20.0;

            if (throughWalls) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glEnable(GL11.GL_BLEND);
            }

            DebugRenderer.drawBox(matrices, immediate,
                centerX - beaconWidth, minY, centerZ - beaconWidth,
                centerX + beaconWidth, minY + beaconHeight, centerZ + beaconWidth,
                colorComponents[0], colorComponents[1], colorComponents[2], alpha * 0.6f);

            immediate.draw();

            if (throughWalls) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
        } catch (Exception e) {
        }
    }
}
