package forfun.miningqol.client.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import forfun.miningqol.mixin.client.RenderPipelinesInvoker;
import forfun.miningqol.mixin.client.RenderTypeInvoker;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/** Render layers used by MiningQOL's targeted block overlay. */
public final class BlockOverlayRenderTypes {
    private static final RenderPipeline FILLED_PIPELINE = registerFilled("block_overlay_filled", false);
    private static final RenderPipeline FILLED_PHASE_PIPELINE = registerFilled("block_overlay_filled_phase", true);
    private static final RenderPipeline LINES_PIPELINE = registerLines("block_overlay_lines", false);
    private static final RenderPipeline LINES_PHASE_PIPELINE = registerLines("block_overlay_lines_phase", true);

    public static final RenderType FILLED = create("miningqol_block_overlay_filled", FILLED_PIPELINE);
    public static final RenderType FILLED_PHASE = create("miningqol_block_overlay_filled_phase", FILLED_PHASE_PIPELINE);
    public static final RenderType LINES = create("miningqol_block_overlay_lines", LINES_PIPELINE);
    public static final RenderType LINES_PHASE = create("miningqol_block_overlay_lines_phase", LINES_PHASE_PIPELINE);

    private BlockOverlayRenderTypes() {}

    private static RenderPipeline registerFilled(String path, boolean phase) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesInvoker.miningqol$getDebugFilledSnippet())
            .withLocation(id(path))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES);
        if (phase) builder.withDepthStencilState(Optional.empty());
        return RenderPipelinesInvoker.miningqol$register(builder.build());
    }

    private static RenderPipeline registerLines(String path, boolean phase) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesInvoker.miningqol$getLinesSnippet())
            .withLocation(id(path));
        if (phase) builder.withDepthStencilState(Optional.empty());
        return RenderPipelinesInvoker.miningqol$register(builder.build());
    }

    private static RenderType create(String name, RenderPipeline pipeline) {
        return RenderTypeInvoker.miningqol$create(
            name,
            RenderSetup.builder(pipeline).createRenderSetup());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("miningqol", "pipeline/" + path);
    }
}
