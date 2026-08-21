package forfun.miningqol.client.utils.render;

/** Carries chams metadata from entity state extraction to model submission. */
public interface ChamsRenderState {
    boolean miningqol$isChams();

    void miningqol$setChams(boolean chams);

    java.util.UUID miningqol$getUuid();

    void miningqol$setUuid(java.util.UUID uuid);
}
