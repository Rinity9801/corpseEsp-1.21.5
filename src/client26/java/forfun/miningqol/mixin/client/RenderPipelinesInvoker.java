package forfun.miningqol.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesInvoker {
    @Accessor("DEBUG_FILLED_SNIPPET")
    static RenderPipeline.Snippet miningqol$getDebugFilledSnippet() {
        throw new AssertionError();
    }

    @Accessor("LINES_SNIPPET")
    static RenderPipeline.Snippet miningqol$getLinesSnippet() {
        throw new AssertionError();
    }

    @Invoker("register")
    static RenderPipeline miningqol$register(RenderPipeline pipeline) {
        throw new AssertionError();
    }
}
