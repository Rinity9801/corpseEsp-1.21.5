package forfun.miningqol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import forfun.miningqol.client.utils.render.BlockOverlayRenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Configurable replacement for the vanilla targeted-block outline. */
public final class BlockOverlay {
    private static final float[] DEFAULT_COLOR = {0.0f, 134.0f / 255.0f, 1.0f};

    private static boolean registered;
    private static boolean enabled;
    private static Mode mode = Mode.FILLED_OUTLINE;
    private static float[] fillColor = DEFAULT_COLOR.clone();
    private static float fillAlpha = 50.0f / 255.0f;
    private static float[] outlineColor = DEFAULT_COLOR.clone();
    private static float lineWidth = 2.5f;
    private static boolean phase;
    private static boolean hideDuringEtherwarp;

    private BlockOverlay() {}

    public enum Mode {
        OUTLINE("Outline"),
        FILL("Fill"),
        FILLED_OUTLINE("Filled Outline");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static void init() {
        if (registered) return;
        registered = true;
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(BlockOverlay::beforeBlockOutline);
    }

    private static boolean beforeBlockOutline(LevelRenderContext context, BlockOutlineRenderState outlineState) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.options.hideGui) return true;
        if (hideDuringEtherwarp && shouldHideForEtherwarp(client)) return false;
        if (client.level == null) return true;

        render(context, outlineState, client);
        return false;
    }

    private static void render(LevelRenderContext context, BlockOutlineRenderState outlineState, Minecraft client) {
        BlockPos pos = outlineState.pos();
        BlockState state = client.level.getBlockState(pos);
        VoxelShape shape = state.isAir() ? Shapes.block() : state.getShape(client.level, pos);
        if (shape.isEmpty()) return;

        double minX = pos.getX() + shape.min(Direction.Axis.X);
        double minY = pos.getY() + shape.min(Direction.Axis.Y);
        double minZ = pos.getZ() + shape.min(Direction.Axis.Z);
        double maxX = pos.getX() + shape.max(Direction.Axis.X);
        double maxY = pos.getY() + shape.max(Direction.Axis.Y);
        double maxZ = pos.getZ() + shape.max(Direction.Axis.Z);

        Vec3 camera = client.gameRenderer.getMainCamera().position();
        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = matrices.last();

        if (mode == Mode.FILL || mode == Mode.FILLED_OUTLINE) {
            VertexConsumer fill = context.bufferSource().getBuffer(
                phase ? BlockOverlayRenderTypes.FILLED_PHASE : BlockOverlayRenderTypes.FILLED);
            addFilledBox(fill, pose, minX, minY, minZ, maxX, maxY, maxZ,
                fillColor[0], fillColor[1], fillColor[2], fillAlpha);
        }

        if (mode == Mode.OUTLINE || mode == Mode.FILLED_OUTLINE) {
            VertexConsumer lines = context.bufferSource().getBuffer(
                phase ? BlockOverlayRenderTypes.LINES_PHASE : BlockOverlayRenderTypes.LINES);
            addLineBox(lines, pose, minX, minY, minZ, maxX, maxY, maxZ,
                outlineColor[0], outlineColor[1], outlineColor[2], 1.0f, lineWidth);
        }

        matrices.popPose();
    }

    private static void addFilledBox(VertexConsumer buffer, PoseStack.Pose pose,
                                     double x1, double y1, double z1,
                                     double x2, double y2, double z2,
                                     float red, float green, float blue, float alpha) {
        float minX = (float) x1 - 0.002f;
        float minY = (float) y1 - 0.002f;
        float minZ = (float) z1 - 0.002f;
        float maxX = (float) x2 + 0.002f;
        float maxY = (float) y2 + 0.002f;
        float maxZ = (float) z2 + 0.002f;

        quad(buffer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        quad(buffer, pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        quad(buffer, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        quad(buffer, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        quad(buffer, pose, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
        quad(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
    }

    private static void addLineBox(VertexConsumer buffer, PoseStack.Pose pose,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   float red, float green, float blue, float alpha, float width) {
        float minX = (float) x1 - 0.002f;
        float minY = (float) y1 - 0.002f;
        float minZ = (float) z1 - 0.002f;
        float maxX = (float) x2 + 0.002f;
        float maxY = (float) y2 + 0.002f;
        float maxZ = (float) z2 + 0.002f;

        line(buffer, pose, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha, width);
        line(buffer, pose, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha, width);
        line(buffer, pose, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha, width);
        line(buffer, pose, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha, width);
        line(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha, width);
        line(buffer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha, width);
        line(buffer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha, width);
        line(buffer, pose, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha, width);
        line(buffer, pose, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha, width);
        line(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha, width);
        line(buffer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha, width);
        line(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha, width);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float red, float green, float blue, float alpha) {
        vertex(buffer, pose, x1, y1, z1, red, green, blue, alpha);
        vertex(buffer, pose, x2, y2, z2, red, green, blue, alpha);
        vertex(buffer, pose, x3, y3, z3, red, green, blue, alpha);
        vertex(buffer, pose, x1, y1, z1, red, green, blue, alpha);
        vertex(buffer, pose, x3, y3, z3, red, green, blue, alpha);
        vertex(buffer, pose, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        buffer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float red, float green, float blue, float alpha, float width) {
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        if (normal.lengthSquared() > 0.0f) normal.normalize();
        buffer.addVertex(pose, x1, y1, z1)
            .setColor(red, green, blue, alpha)
            .setNormal(pose, normal)
            .setLineWidth(width);
        buffer.addVertex(pose, x2, y2, z2)
            .setColor(red, green, blue, alpha)
            .setNormal(pose, normal)
            .setLineWidth(width);
    }

    private static boolean shouldHideForEtherwarp(Minecraft client) {
        if (client.player == null || !client.player.isCrouching()) return false;
        ItemStack stack = client.player.getMainHandItem();
        CustomData customData = stack.getComponents().get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;

        CompoundTag tag = customData.copyTag();
        String id = tag.getString("id").orElse("");
        boolean teleportSword = id.equals("ASPECT_OF_THE_VOID") || id.equals("ASPECT_OF_THE_END");
        return teleportSword && tag.getByte("ethermerge").orElse((byte) 0) == 1;
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static Mode getMode() { return mode; }
    public static void setMode(Mode value) { mode = value == null ? Mode.FILLED_OUTLINE : value; }
    public static void cycleMode() { mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length]; }
    public static float[] getFillColor() { return fillColor.clone(); }
    public static void setFillColor(float red, float green, float blue) { fillColor = color(red, green, blue); }
    public static float getFillAlpha() { return fillAlpha; }
    public static void setFillAlpha(float value) { fillAlpha = clamp(value); }
    public static float[] getOutlineColor() { return outlineColor.clone(); }
    public static void setOutlineColor(float red, float green, float blue) { outlineColor = color(red, green, blue); }
    public static float getLineWidth() { return lineWidth; }
    public static void setLineWidth(float value) { lineWidth = Math.max(1.0f, Math.min(10.0f, value)); }
    public static boolean isPhase() { return phase; }
    public static void setPhase(boolean value) { phase = value; }
    public static boolean isHideDuringEtherwarp() { return hideDuringEtherwarp; }
    public static void setHideDuringEtherwarp(boolean value) { hideDuringEtherwarp = value; }

    private static float[] color(float red, float green, float blue) {
        return new float[]{clamp(red), clamp(green), clamp(blue)};
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
