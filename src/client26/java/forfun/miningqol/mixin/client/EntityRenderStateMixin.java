package forfun.miningqol.mixin.client;

import forfun.miningqol.client.utils.render.ChamsRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements ChamsRenderState {
    @Unique
    private boolean miningqol$chams;
    @Unique
    private java.util.UUID miningqol$uuid;

    @Override
    public boolean miningqol$isChams() {
        return miningqol$chams;
    }

    @Override
    public void miningqol$setChams(boolean chams) {
        miningqol$chams = chams;
    }

    @Override
    public java.util.UUID miningqol$getUuid() {
        return miningqol$uuid;
    }

    @Override
    public void miningqol$setUuid(java.util.UUID uuid) {
        miningqol$uuid = uuid;
    }
}
