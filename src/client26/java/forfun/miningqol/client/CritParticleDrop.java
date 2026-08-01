package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Lowers mining crit particles while sneaking.
 *
 * <p>Ported from prisma's Cute Visuals sneak-drop. The server resolves your hit from ITS
 * idea of your crouched eye height, which doesn't match this client's — so while sneaking
 * the crit sprite renders slightly above the spot the server actually registered. Dropping
 * it by a fixed {@value #DROP} puts the sprite where the hit really landed.
 *
 * <p>Standing, the two agree and no correction is applied. The amount is a constant rather
 * than a setting: it corrects one specific mismatch, so there's a right answer rather than
 * a preference.
 */
public class CritParticleDrop {
    /** Matches prisma's SNEAK_MARKER_DROP. */
    public static final double DROP = 0.275;

    private static boolean enabled = false;

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Returns the y the particle should spawn at. */
    public static double adjustY(ParticleOptions options, double x, double y, double z) {
        if (!enabled) return y;
        if (options.getType() != ParticleTypes.CRIT && options.getType() != ParticleTypes.ENCHANTED_HIT) {
            return y;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isShiftKeyDown()) return y;
        // Mining range only (6 blocks, as prisma uses) — leaves combat crits alone.
        if (mc.player.distanceToSqr(x, y, z) > 36.0) return y;
        return y - DROP;
    }
}
