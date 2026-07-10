package forfun.miningqol.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import forfun.miningqol.client.waypoints.OrderedWaypointRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void miningqol$afterRenderLevel(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker,
                                            boolean renderBlockOutline, CameraRenderState cameraState,
                                            Matrix4fc viewMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor,
                                            boolean bl, ChunkSectionsToRender chunkSections, CallbackInfo ci) {
        OrderedWaypointRenderer.render(cameraState, viewMatrix);
        forfun.miningqol.client.ShaftESP.render(cameraState, viewMatrix);
        forfun.miningqol.client.CorpseESP.render(cameraState, viewMatrix);
        forfun.miningqol.client.EfficientMinerOverlay.render(cameraState, viewMatrix);
    }
}
