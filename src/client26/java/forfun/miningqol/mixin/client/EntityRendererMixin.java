package forfun.miningqol.mixin.client;

import forfun.miningqol.client.EntityGlowESP;
import forfun.miningqol.client.utils.render.ChamsRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void miningqol$applyEspRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        if (EntityGlowESP.shouldGlow(entity)) {
            state.outlineColor = EntityGlowESP.getOutlineColor(entity);
            if (EntityGlowESP.shouldUseCustomGlow(entity)) EntityGlowESP.markCustomGlowFrame();
        }
        if (state instanceof ChamsRenderState chams) {
            chams.miningqol$setChams(EntityGlowESP.shouldChams(entity));
            chams.miningqol$setUuid(entity.getUUID());
        }
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void miningqol$keepEspTargetInFrustum(T entity, Frustum frustum,
                                                  double x, double y, double z,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (EntityGlowESP.isTarget(entity)) cir.setReturnValue(true);
    }
}
