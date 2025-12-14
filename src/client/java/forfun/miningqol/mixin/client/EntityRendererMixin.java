package forfun.miningqol.mixin.client;

import forfun.miningqol.client.NameHider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(
        method = "renderLabelIfPresent",
        at = @At("HEAD")
    )
    private void modifyEntityName(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        if (state.displayName != null) {
            state.displayName = NameHider.processName(state.displayName);
        }
    }
}
