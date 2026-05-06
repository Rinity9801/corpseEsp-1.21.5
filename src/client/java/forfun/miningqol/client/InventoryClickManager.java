package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Clicks the top-left 4 slots of the player's main inventory (slots 9-12 in
 * PlayerScreenHandler) with a fixed delay between clicks. Requires a
 * HandledScreen to already be open.
 */
public class InventoryClickManager {
    private static final int[] TARGET_SLOTS = {9, 10, 11, 12};

    private static final int STATE_IDLE = 0;
    private static final int STATE_CLICK = 1;

    private static int clickDelayTicks = 3;
    private static int state = STATE_IDLE;
    private static int tickCounter = 0;
    private static int clickIndex = 0;

    public static int getClickDelay() {
        return clickDelayTicks;
    }

    public static void setClickDelay(int ticks) {
        clickDelayTicks = Math.max(1, Math.min(40, ticks));
    }

    public static void start() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state != STATE_IDLE) return;
        if (!(client.currentScreen instanceof HandledScreen<?>)) return;

        state = STATE_CLICK;
        tickCounter = clickDelayTicks; // first click fires on the next tick
        clickIndex = 0;
    }

    public static boolean isRunning() {
        return state != STATE_IDLE;
    }

    public static void tick() {
        if (state != STATE_CLICK) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) {
            state = STATE_IDLE;
            return;
        }

        tickCounter++;
        if (tickCounter < clickDelayTicks) return;
        tickCounter = 0;

        if (clickIndex >= TARGET_SLOTS.length) {
            state = STATE_IDLE;
            return;
        }

        if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
            state = STATE_IDLE;
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        int slot = TARGET_SLOTS[clickIndex];
        if (slot < handler.slots.size()) {
            client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
        }
        clickIndex++;
    }
}
