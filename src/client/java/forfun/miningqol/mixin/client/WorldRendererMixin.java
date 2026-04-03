package forfun.miningqol.mixin.client;

import forfun.miningqol.client.BlockOutlineRenderer;
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.EfficientMinerOverlay;
import forfun.miningqol.client.ShaftESP;
import forfun.miningqol.client.waypoints.OrderedWaypointRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderWorld(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, GpuBufferSlice bufferSlice, Vector4f vector4f, boolean lastParam, CallbackInfo ci) {
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(matrix4f);

        CorpseESP.render(matrices, camera);
        ShaftESP.render(matrices, camera);
        BlockOutlineRenderer.render(matrices, camera);
        EfficientMinerOverlay.render(matrices, camera);
        OrderedWaypointRenderer.render(matrices, camera);
    }
}
