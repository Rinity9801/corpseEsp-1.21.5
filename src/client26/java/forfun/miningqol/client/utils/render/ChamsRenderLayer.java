package forfun.miningqol.client.utils.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import forfun.miningqol.mixin.client.RenderTypeInvoker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Prisma-style second entity pass that draws only model fragments hidden behind scene depth. */
public final class ChamsRenderLayer {
    private static final Map<RenderType, Identifier> LAYER_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<RenderType, RenderType> HIDDEN_LAYERS = new ConcurrentHashMap<>();
    private static RenderPipeline hiddenPipeline;
    private static java.util.UUID headChamsUuid;

    private ChamsRenderLayer() {}

    public static void beginHeadChams(java.util.UUID uuid) {
        headChamsUuid = uuid;
    }

    public static void endHeadChams() {
        headChamsUuid = null;
    }

    public static java.util.UUID headChamsUuid() {
        return headChamsUuid;
    }

    public static void noteTexture(RenderType layer, Identifier texture) {
        if (layer != null && texture != null) LAYER_TEXTURES.putIfAbsent(layer, texture);
    }

    public static RenderType hiddenFor(RenderType source) {
        Identifier texture = LAYER_TEXTURES.get(source);
        if (texture == null) return null;
        return HIDDEN_LAYERS.computeIfAbsent(source, ignored -> RenderTypeInvoker.miningqol$create(
            "miningqol_chams_hidden",
            RenderSetup.builder(pipeline())
                .withTexture("Sampler0", texture)
                .useOverlay()
                .sortOnUpload()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup()));
    }

    private static RenderPipeline pipeline() {
        if (hiddenPipeline != null) return hiddenPipeline;

        RenderPipeline source = RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE;
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("miningqol", "pipeline/chams_hidden"))
            .withVertexShader(source.getVertexShader())
            .withFragmentShader(source.getFragmentShader())
            .withVertexFormat(source.getVertexFormat(), source.getVertexFormatMode())
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
            .withColorTargetState(source.getColorTargetState())
            .withPolygonMode(source.getPolygonMode())
            .withCull(source.isCull());

        for (String sampler : source.getSamplers()) builder.withSampler(sampler);
        for (RenderPipeline.UniformDescription uniform : source.getUniforms()) {
            if (uniform.textureFormat() != null) {
                builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
            } else {
                builder.withUniform(uniform.name(), uniform.type());
            }
        }
        source.getShaderDefines().values().forEach((key, value) -> {
            try {
                builder.withShaderDefine(key, Integer.parseInt(value));
            } catch (NumberFormatException first) {
                try {
                    builder.withShaderDefine(key, Float.parseFloat(value));
                } catch (NumberFormatException ignored) {
                }
            }
        });
        source.getShaderDefines().flags().forEach(builder::withShaderDefine);
        hiddenPipeline = builder.build();
        return hiddenPipeline;
    }
}
