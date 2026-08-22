package forfun.miningqol.client.utils.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.OptionalInt;

/** Premultiplied outline composite used by the Prisma-style custom halo. */
public final class OutlineHaloBlit {
    private static final RenderPipeline OUTLINE_HALO_BLIT = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("miningqol", "pipeline/outline_halo_blit"))
        .withVertexShader("core/screenquad")
        .withFragmentShader("core/blit_screen")
        .withSampler("InSampler")
        .withColorTargetState(new ColorTargetState(
            Optional.of(new BlendFunction(
                SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA,
                SourceFactor.ZERO, DestFactor.ONE)),
            ColorTargetState.WRITE_COLOR))
        .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
        .build();

    private OutlineHaloBlit() {}

    public static void blit(RenderTarget source, GpuTextureView target) {
        RenderSystem.assertOnRenderThread();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass pass = encoder.createRenderPass(
            () -> "Sybau outline halo blit", target, OptionalInt.empty())) {
            pass.setPipeline(OUTLINE_HALO_BLIT);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture(
                "InSampler",
                source.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(0, 3);
        }
    }
}
