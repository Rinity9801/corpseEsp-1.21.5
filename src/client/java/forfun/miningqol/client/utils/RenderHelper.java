package forfun.miningqol.client.utils;

//? if is1_21_11 {
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Replacement for DebugRenderer.drawBox which was removed in 1.21.11.
 */
public class RenderHelper {

    public static void drawBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                               double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ,
                               float red, float green, float blue, float alpha) {
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayers.debugQuads());
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        float x0 = (float) minX;
        float y0 = (float) minY;
        float z0 = (float) minZ;
        float x1 = (float) maxX;
        float y1 = (float) maxY;
        float z1 = (float) maxZ;

        // Bottom face
        buffer.vertex(posMatrix, x0, y0, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y0, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y0, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x0, y0, z1).color(red, green, blue, alpha);

        // Top face
        buffer.vertex(posMatrix, x0, y1, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x0, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y1, z0).color(red, green, blue, alpha);

        // North face (z = min)
        buffer.vertex(posMatrix, x0, y0, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x0, y1, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y1, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y0, z0).color(red, green, blue, alpha);

        // South face (z = max)
        buffer.vertex(posMatrix, x0, y0, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y0, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x0, y1, z1).color(red, green, blue, alpha);

        // West face (x = min)
        buffer.vertex(posMatrix, x0, y0, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x0, y0, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x0, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x0, y1, z0).color(red, green, blue, alpha);

        // East face (x = max)
        buffer.vertex(posMatrix, x1, y0, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y1, z0).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(posMatrix, x1, y0, z1).color(red, green, blue, alpha);
    }
}
//?} else {
/*public class RenderHelper {}
*///?}
