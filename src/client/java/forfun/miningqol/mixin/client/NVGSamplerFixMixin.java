package forfun.miningqol.mixin.client;

import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.meowing.vexel.utils.render.NVGRenderer;

/**
 * Fix NanoVG text rendering in 1.21.11.
 * MC 1.21.11 has an active sampler object on texture unit 0 that
 * overrides NanoVG's font atlas texture sampling parameters.
 * Unbinding the sampler before NanoVG renders fixes text.
 */
@Mixin(value = NVGRenderer.class, remap = false)
public class NVGSamplerFixMixin {

    @Inject(method = "beginFrame", at = @At("RETURN"))
    private void unbindSampler(float width, float height, CallbackInfo ci) {
        GL33C.glBindSampler(0, 0);
    }
}
