package forfun.miningqol.client.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/** Draws constant-looking wireframe boxes on a no-depth quad layer. */
public final class SeeThroughBoxRenderer {
    private static final int FULL_BRIGHT = 15728880;
    private static final float EDGE_SCREEN_SCALE = 0.0015f;
    private static final float EDGE_MIN = 0.004f;
    private static final float EDGE_MAX = 0.08f;

    private SeeThroughBoxRenderer() {}

    public static void outline(VertexConsumer quads, Matrix4f pose,
                               float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ,
                               float red, float green, float blue, float alpha) {
        edge(quads, pose, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        edge(quads, pose, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        edge(quads, pose, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        edge(quads, pose, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);
        edge(quads, pose, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        edge(quads, pose, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        edge(quads, pose, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        edge(quads, pose, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
        edge(quads, pose, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        edge(quads, pose, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        edge(quads, pose, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        edge(quads, pose, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void edge(VertexConsumer quads, Matrix4f pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float red, float green, float blue, float alpha) {
        float midX = (x1 + x2) * 0.5f;
        float midY = (y1 + y2) * 0.5f;
        float midZ = (z1 + z2) * 0.5f;
        float distance = (float) Math.sqrt(midX * midX + midY * midY + midZ * midZ);
        float thickness = Math.max(EDGE_MIN, Math.min(EDGE_MAX, EDGE_SCREEN_SCALE * distance));

        float tx = x1 == x2 ? thickness : 0.0f;
        float ty = y1 == y2 ? thickness : 0.0f;
        float tz = z1 == z2 ? thickness : 0.0f;
        box(quads, pose,
            Math.min(x1, x2) - tx, Math.min(y1, y2) - ty, Math.min(z1, z2) - tz,
            Math.max(x1, x2) + tx, Math.max(y1, y2) + ty, Math.max(z1, z2) + tz,
            red, green, blue, alpha);
    }

    private static void box(VertexConsumer buffer, Matrix4f pose,
                            float minX, float minY, float minZ,
                            float maxX, float maxY, float maxZ,
                            float red, float green, float blue, float alpha) {
        int r = (int) (red * 255.0f);
        int g = (int) (green * 255.0f);
        int b = (int) (blue * 255.0f);
        int a = (int) (alpha * 255.0f);

        quad(buffer, pose, r, g, b, a, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        quad(buffer, pose, r, g, b, a, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        quad(buffer, pose, r, g, b, a, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
        quad(buffer, pose, r, g, b, a, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        quad(buffer, pose, r, g, b, a, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
        quad(buffer, pose, r, g, b, a, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose, int red, int green, int blue, int alpha,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        buffer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x3, y3, z3).setColor(red, green, blue, alpha).setLight(FULL_BRIGHT);
        buffer.addVertex(pose, x4, y4, z4).setColor(red, green, blue, alpha).setLight(FULL_BRIGHT);
    }
}
