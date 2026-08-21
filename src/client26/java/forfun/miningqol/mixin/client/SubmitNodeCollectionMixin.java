package forfun.miningqol.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import forfun.miningqol.client.utils.render.ChamsRenderLayer;
import forfun.miningqol.client.utils.render.ChamsRenderState;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a fullbright GREATER-depth submission for custom-glow targets hidden by blocks. */
@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {
    @Unique
    private static boolean miningqol$resubmitting;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(
        method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("TAIL")
    )
    private void miningqol$submitHiddenChamsPass(Model model, Object state, PoseStack matrices,
                                                 RenderType layer, int light, int overlay,
                                                 int outlineColor, TextureAtlasSprite sprite, int order,
                                                 ModelFeatureRenderer.CrumblingOverlay crumbling,
                                                 CallbackInfo ci) {
        if (miningqol$resubmitting) return;
        if (state instanceof ChamsRenderState chams && chams.miningqol$isChams()) {
            // The render-state flag covers normal body and armor submissions.
        } else if (ChamsRenderLayer.headChamsUuid() == null) {
            return;
        }

        RenderType hidden = ChamsRenderLayer.hiddenFor(layer);
        if (hidden == null) return;
        miningqol$resubmitting = true;
        try {
            ((SubmitNodeCollection) (Object) this)
                .submitModel(model, state, matrices, hidden, light, overlay,
                    outlineColor, sprite, order, crumbling);
        } finally {
            miningqol$resubmitting = false;
        }
    }
}
