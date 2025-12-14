package forfun.miningqol.client.utils.waypoint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class NamedWaypoint extends Waypoint {
    private final String name;
    private final boolean showDistance;

    public NamedWaypoint(BlockPos pos, float[] colorComponents, String name, boolean showDistance) {
        super(pos, colorComponents);
        this.name = name;
        this.showDistance = showDistance;
    }

    @Override
    public void render(MatrixStack matrices, Camera camera) {
        super.render(matrices, camera);
    }
}
