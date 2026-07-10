package forfun.miningqol.client;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Bridge between the shared 26.1.2 client and cheat-only features. The cheat
 * source tree (src/client26cheat) isn't stonecutter-gated, so the shared code
 * must never reference cheat classes directly — CheatBootstrap (cheat-only)
 * fills these in at init, and legit builds simply leave them null.
 */
public final class CheatHooks {
    /** Applies cheat config fields to the cheat managers (applyToGame side). */
    public static Runnable applyConfig = null;
    /** Reads cheat manager state back into config fields (loadFromGame side). */
    public static Runnable storeConfig = null;
    /** Raw game chat messages (non-overlay), stripped to plain text. */
    public static Consumer<String> onGameMessage = null;
    /** Client shutting down — release held keys etc. */
    public static Runnable onStopping = null;
    /** True while an open container GUI should be hidden (e.g. mid comm-claim with Hide GUI on). */
    public static BooleanSupplier hideContainerGui = null;

    private CheatHooks() {}
}
