package forfun.miningqol.client;

import net.minecraft.client.renderer.state.level.CameraRenderState;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Null-safe seams for the local-only external ESP feed.
 *
 * <p>The feed itself lives in {@code local/esp/}, which is compiled in only when that
 * directory is present — so it never ships in a release. Everything in the released tree
 * talks to it through here and simply does nothing when it's absent, the same way
 * {@link CheatHooks} keeps cheat features out of the legit build.
 */
public final class EspHooks {
    public static Runnable enableFeed;
    public static BooleanSupplier overlayConnected;
    public static Consumer<CameraRenderState> onRender;

    private EspHooks() {}

    /** True when the local feed module is compiled in — gates its settings rows. */
    public static boolean isPresent() {
        return enableFeed != null;
    }

    /** True only when an overlay app is actually attached. */
    public static boolean isOverlayConnected() {
        return overlayConnected != null && overlayConnected.getAsBoolean();
    }

    public static void enableFeed() {
        if (enableFeed != null) enableFeed.run();
    }

    public static void render(CameraRenderState state) {
        Consumer<CameraRenderState> hook = onRender;
        if (hook != null) hook.accept(state);
    }
}
