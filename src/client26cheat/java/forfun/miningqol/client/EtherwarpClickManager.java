package forfun.miningqol.client;

import forfun.miningqol.client.waypoints.OrderedWaypoint;
import forfun.miningqol.client.waypoints.OrderedWaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;

/**
 * Fires the etherwarp right-click for route waypoints marked with /mqo ether.
 *
 * <p>The player does the aiming: while they sneak and their crosshair rests on (or right next to)
 * the marked target waypoint with an AOTV/AOTE in hand, this presses use for a few ticks. The
 * key is HELD rather than tapped — a single-tick press sometimes doesn't register on Hypixel —
 * and vanilla's handleKeybinds does the actual startUseItem, exactly like a real click.
 */
public final class EtherwarpClickManager {
    /** Etherwarp reaches 57 blocks fully tuned; a little slack for the eye-height offset. */
    private static final double RAY_RANGE = 61.0;
    /** Aimed block must land within this many blocks of the waypoint (squared distance). */
    private static final double HIT_TOLERANCE_SQ = 4.0;
    private static final int HOLD_TICKS = 3;
    /** Ticks to wait after a click before considering another, so a failed warp doesn't spam. */
    private static final int COOLDOWN_TICKS = 15;

    private static boolean enabled = true;
    private static int holdTicks = 0;
    private static int cooldownTicks = 0;

    private EtherwarpClickManager() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();

        if (holdTicks > 0) {
            if (--holdTicks == 0) {
                client.options.keyUse.setDown(false);
                cooldownTicks = COOLDOWN_TICKS;
            }
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (!enabled || client.player == null || client.level == null || client.screen != null) return;
        if (!OrderedWaypointManager.isEnabled()) return;
        if (!client.player.isShiftKeyDown()) return;

        OrderedWaypoint target = OrderedWaypointManager.getNextWaypoint();
        if (target == null || !target.isEtherwarp()) return;

        // Only fire holding a transmission item, so sneaking near the waypoint can't
        // right-click whatever else happens to be in hand.
        String held = client.player.getMainHandItem().getHoverName().getString().toLowerCase(Locale.ROOT);
        if (!held.contains("aspect of the")) return;

        HitResult hit = client.player.pick(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) return;
        if (blockHit.getBlockPos().distSqr(target.getPosition()) > HIT_TOLERANCE_SQ) return;

        client.options.keyUse.setDown(true);
        holdTicks = HOLD_TICKS;
    }

    public static void cleanup() {
        if (holdTicks > 0) {
            Minecraft.getInstance().options.keyUse.setDown(false);
            holdTicks = 0;
        }
    }
}
