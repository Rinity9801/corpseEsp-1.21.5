package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the HOTM tree (/hotm) and toggles the active mining ability by clicking the
 * ability slot, then closes the GUI. 26.1.2 port of the 1.21.x AbilitySwitchManager.
 */
public class AbilitySwitchManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AbilitySwitchManager");

    private static boolean running = false;
    private static int state = 0;
    private static int tickCounter = 0;

    // Row 4, column 3 = slot index (3 * 9) + 2 = 29
    private static final int SLOT_CHECK = (4 - 1) * 9 + (3 - 1); // 29
    // Row 4, column 7 = slot index (3 * 9) + 6 = 33
    private static final int SLOT_ALT = (4 - 1) * 9 + (7 - 1);   // 33

    private static final int STATE_IDLE = 0;
    private static final int STATE_SEND_HOTM = 1;
    private static final int STATE_WAIT_GUI = 2;
    private static final int STATE_DELAY_BEFORE_CLICK = 3;
    private static final int STATE_CLICK_SLOT = 4;
    private static final int STATE_DELAY_AFTER_CLICK = 5;
    private static final int STATE_CLOSE_GUI = 6;
    private static final int STATE_DONE = 7;

    public static void toggle() {
        if (running) {
            stop();
        } else {
            start();
        }
    }

    public static void start() {
        if (running) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        running = true;
        state = STATE_SEND_HOTM;
        tickCounter = 0;

        LOGGER.info("[AbilitySwitch] Starting ability switch");
        client.player.sendSystemMessage(Component.literal("§6[MQO] §aSwitching mining ability..."));
    }

    public static void stop() {
        running = false;
        state = STATE_IDLE;
        tickCounter = 0;
    }

    public static boolean isRunning() {
        return running;
    }

    public static void tick() {
        if (!running) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            stop();
            return;
        }

        tickCounter++;

        switch (state) {
            case STATE_SEND_HOTM:
                client.player.connection.sendCommand("hotm");
                state = STATE_WAIT_GUI;
                tickCounter = 0;
                break;

            case STATE_WAIT_GUI:
                if (isHotmOpen(client)) {
                    state = STATE_DELAY_BEFORE_CLICK;
                    tickCounter = 0;
                } else if (tickCounter >= 60) {
                    LOGGER.warn("[AbilitySwitch] HOTM GUI didn't open in time");
                    client.player.sendSystemMessage(Component.literal("§6[MQO] §cHOTM GUI didn't open in time"));
                    stop();
                }
                break;

            case STATE_DELAY_BEFORE_CLICK:
                if (tickCounter >= 3) {
                    state = STATE_CLICK_SLOT;
                    tickCounter = 0;
                }
                break;

            case STATE_CLICK_SLOT:
                if (client.screen instanceof ContainerScreen screen) {
                    var menu = screen.getMenu();
                    if (SLOT_CHECK < menu.slots.size()) {
                        ItemStack stack = menu.slots.get(SLOT_CHECK).getItem();
                        int targetSlot;
                        if (stack.getItem() == Items.EMERALD_BLOCK) {
                            // Already active, click the other one
                            targetSlot = SLOT_ALT;
                        } else {
                            targetSlot = SLOT_CHECK;
                        }

                        client.gameMode.handleContainerInput(
                            menu.containerId,
                            targetSlot,
                            0,
                            ContainerInput.PICKUP,
                            client.player
                        );
                        LOGGER.info("[AbilitySwitch] Clicked slot {}", targetSlot);
                        state = STATE_DELAY_AFTER_CLICK;
                        tickCounter = 0;
                    } else {
                        LOGGER.warn("[AbilitySwitch] Slot out of range");
                        stop();
                    }
                } else {
                    LOGGER.warn("[AbilitySwitch] GUI closed unexpectedly");
                    stop();
                }
                break;

            case STATE_DELAY_AFTER_CLICK:
                if (tickCounter >= 5) {
                    state = STATE_CLOSE_GUI;
                    tickCounter = 0;
                }
                break;

            case STATE_CLOSE_GUI:
                if (client.screen != null) {
                    client.screen.onClose();
                }
                state = STATE_DONE;
                tickCounter = 0;
                break;

            case STATE_DONE:
                client.player.sendSystemMessage(Component.literal("§6[MQO] §aMining ability switched!"));
                LOGGER.info("[AbilitySwitch] Complete");
                running = false;
                state = STATE_IDLE;
                break;
        }
    }

    private static boolean isHotmOpen(Minecraft client) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;
        String title = screen.getTitle().getString();
        return title.contains("Heart of the Mountain");
    }
}
