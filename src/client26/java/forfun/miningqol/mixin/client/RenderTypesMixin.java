package forfun.miningqol.mixin.client;

import forfun.miningqol.client.utils.render.ChamsRenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Remembers the texture associated with vanilla model render types for the hidden pass. */
@Mixin(RenderTypes.class)
public class RenderTypesMixin {
    @Inject(method = "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("RETURN"))
    private static void miningqol$noteEntityTranslucent(Identifier texture, boolean affectsOutline,
                                                        CallbackInfoReturnable<RenderType> cir) {
        ChamsRenderLayer.noteTexture(cir.getReturnValue(), texture);
    }

    @Inject(method = "entityCutout(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("RETURN"))
    private static void miningqol$noteEntityCutout(Identifier texture, boolean affectsOutline,
                                                   CallbackInfoReturnable<RenderType> cir) {
        ChamsRenderLayer.noteTexture(cir.getReturnValue(), texture);
    }

    @Inject(method = "entityCutoutZOffset(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("RETURN"))
    private static void miningqol$noteEntityCutoutZOffset(Identifier texture, boolean affectsOutline,
                                                          CallbackInfoReturnable<RenderType> cir) {
        ChamsRenderLayer.noteTexture(cir.getReturnValue(), texture);
    }

    @Inject(method = "entityCutoutCull", at = @At("RETURN"))
    private static void miningqol$noteEntityCutoutCull(Identifier texture,
                                                       CallbackInfoReturnable<RenderType> cir) {
        ChamsRenderLayer.noteTexture(cir.getReturnValue(), texture);
    }

    @Inject(method = "entitySolid", at = @At("RETURN"))
    private static void miningqol$noteEntitySolid(Identifier texture,
                                                  CallbackInfoReturnable<RenderType> cir) {
        ChamsRenderLayer.noteTexture(cir.getReturnValue(), texture);
    }

    @Inject(method = "armorCutoutNoCull", at = @At("RETURN"))
    private static void miningqol$noteArmor(Identifier texture,
                                            CallbackInfoReturnable<RenderType> cir) {
        ChamsRenderLayer.noteTexture(cir.getReturnValue(), texture);
    }

    @Inject(method = "armorTranslucent", at = @At("RETURN"))
    private static void miningqol$noteArmorTranslucent(Identifier texture,
                                                       CallbackInfoReturnable<RenderType> cir) {
        ChamsRenderLayer.noteTexture(cir.getReturnValue(), texture);
    }
}
