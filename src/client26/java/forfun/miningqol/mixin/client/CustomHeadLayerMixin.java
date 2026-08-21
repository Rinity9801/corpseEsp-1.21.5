package forfun.miningqol.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import forfun.miningqol.client.utils.render.ChamsRenderLayer;
import forfun.miningqol.client.utils.render.ChamsRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {
    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
        at = @At("HEAD")
    )
    private void miningqol$beginHeadChams(PoseStack matrices, SubmitNodeCollector collector, int light,
                                          LivingEntityRenderState state, float yRot, float xRot,
                                          CallbackInfo ci) {
        if (state instanceof ChamsRenderState chams && chams.miningqol$isChams()) {
            ChamsRenderLayer.beginHeadChams(chams.miningqol$getUuid());
        }
    }

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
        at = @At("RETURN")
    )
    private void miningqol$endHeadChams(PoseStack matrices, SubmitNodeCollector collector, int light,
                                        LivingEntityRenderState state, float yRot, float xRot,
                                        CallbackInfo ci) {
        ChamsRenderLayer.endHeadChams();
    }
}
