package forfun.miningqol.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/** Grid snapping for the HUD move editors — hold Shift to place freely. */
final class HudDragSnap {
    private static final int GRID = 10;

    private HudDragSnap() {}

    static int snap(int value) {
        if (shiftHeld()) {
            return value;
        }
        return Math.round(value / (float) GRID) * GRID;
    }

    private static boolean shiftHeld() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT)
            || InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT);
    }
}
