package forfun.miningqol.client.hotm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * State machine that automatically configures the in-game HOTM tree
 * to match the current HotmTree preset.
 *
 * In-game coordinate mapping (1-based):
 * - Tree rows 5-9 -> Page 1, in-game row = treeRow - 4
 * - Tree rows 0-4 -> Page 2, in-game row = treeRow + 1
 * - In-game col = treeCol + 1
 * - Slot index (0-based) = (row_1based - 1) * 9 + (col_1based - 1)
 *
 * Actions:
 * - Left click = enable/level up perk
 * - Shift+left click = buy 10 levels at once (for maxing)
 * - Right click = disable perk
 * - Page arrow at slot 8 (top-right), right-click to switch page
 * - Reset button at slot 52 (row 6, col 8 in 1-based = row 5, col 7 in 0-based)
 * - Reset confirm at slot 11 (row 2, col 3 in 1-based)
 */
public class AutoHotmManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoHotmManager");

    private static boolean running = false;
    private static int state = 0;
    private static int tickCounter = 0;
    private static int tickDelay = 5;

    // Page arrow slot (0-based): row 0, col 8 = slot 8
    private static final int PAGE_ARROW_SLOT = 8;
    // Reset button slot (0-based): row 5, col 7 = slot 52
    private static final int RESET_SLOT = 52;
    // Reset confirm slot (0-based): row 1, col 2 = slot 11
    private static final int RESET_CONFIRM_SLOT = 11;

    private static final int STATE_IDLE = 0;
    private static final int STATE_OPEN_HOTM = 1;
    private static final int STATE_WAIT_HOTM = 2;
    private static final int STATE_CHECK_RESET = 3;
    private static final int STATE_RESET_CLICK = 4;
    private static final int STATE_RESET_WAIT = 5;
    private static final int STATE_RESET_CONFIRM = 6;
    private static final int STATE_REOPEN_HOTM = 7;
    private static final int STATE_WAIT_REOPEN = 8;
    private static final int STATE_SETUP_ACTIONS = 9;
    private static final int STATE_NEXT_ACTION = 10;
    private static final int STATE_CLICK_ACTION = 11;
    private static final int STATE_MAX_LOOP = 12;
    private static final int STATE_MAX_CHECK = 13;
    private static final int STATE_SWITCH_PAGE = 14;
    private static final int STATE_WAIT_PAGE = 15;
    private static final int STATE_DISABLE_PHASE = 16;
    private static final int STATE_DISABLE_CLICK = 17;
    private static final int STATE_DONE = 18;

    // Action queue
    private static List<Action> actions = new ArrayList<>();
    private static int actionIndex = 0;
    private static List<Action> disableActions = new ArrayList<>();
    private static int disableIndex = 0;
    private static int currentPage = 1; // 1 = bottom half (rows 5-9), 2 = top half (rows 0-4)
    private static int maxRetries = 0;
    private static String lastChatMessage = "";

    private static class Action {
        final HotmNode node;
        final HotmNode.State desiredState;
        final int page;
        final int slot;

        Action(HotmNode node, HotmNode.State desiredState, int page, int slot) {
            this.node = node;
            this.desiredState = desiredState;
            this.page = page;
            this.slot = slot;
        }
    }

    public static void start() {
        if (running) {
            stop();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        running = true;
        state = STATE_OPEN_HOTM;
        tickCounter = 0;
        actionIndex = 0;
        disableIndex = 0;
        currentPage = 1;
        lastChatMessage = "";

        client.player.sendSystemMessage(Component.literal("\u00A76[MQO] \u00A7aStarting Auto-HOTM..."));
        LOGGER.info("[AutoHotm] Starting");
    }

    public static void stop() {
        running = false;
        state = STATE_IDLE;
        tickCounter = 0;
        actions.clear();
        disableActions.clear();

        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal("\u00A76[MQO] \u00A7cAuto-HOTM stopped"));
        }
    }

    public static boolean isRunning() { return running; }

    public static int getTickDelay() { return tickDelay; }
    public static void setTickDelay(int delay) { tickDelay = Math.max(1, Math.min(20, delay)); }

    public static void onChatMessage(String message) {
        if (running) lastChatMessage = message;
    }

    public static void tick() {
        if (!running) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) { stop(); return; }

        tickCounter++;

        switch (state) {
            case STATE_OPEN_HOTM:
                client.player.connection.sendCommand("hotm");
                state = STATE_WAIT_HOTM;
                tickCounter = 0;
                break;

            case STATE_WAIT_HOTM:
                if (isHotmGuiOpen(client)) {
                    state = STATE_CHECK_RESET;
                    tickCounter = 0;
                } else if (tickCounter >= 60) {
                    client.player.sendSystemMessage(Component.literal("\u00A76[MQO] \u00A7cHOTM GUI didn't open"));
                    stop();
                }
                break;

            case STATE_CHECK_RESET:
                if (tickCounter >= tickDelay) {
                    if (client.screen instanceof ContainerScreen screen) {
                        var menu = screen.getMenu();
                        var resetItem = menu.slots.get(RESET_SLOT).getItem();
                        // If reset slot is NOT black glass pane, tree needs resetting
                        if (resetItem.getItem() != Items.BLACK_STAINED_GLASS_PANE) {
                            state = STATE_RESET_CLICK;
                        } else {
                            // Tree is already reset, go straight to actions
                            state = STATE_SETUP_ACTIONS;
                        }
                        tickCounter = 0;
                    }
                }
                break;

            case STATE_RESET_CLICK:
                clickSlot(client, RESET_SLOT, 0, ContainerInput.PICKUP);
                state = STATE_RESET_WAIT;
                tickCounter = 0;
                break;

            case STATE_RESET_WAIT:
                // Wait 120 ticks (6 seconds) for the countdown
                if (tickCounter >= 120) {
                    state = STATE_RESET_CONFIRM;
                    tickCounter = 0;
                }
                break;

            case STATE_RESET_CONFIRM:
                if (tickCounter >= tickDelay) {
                    clickSlot(client, RESET_CONFIRM_SLOT, 0, ContainerInput.PICKUP);
                    state = STATE_REOPEN_HOTM;
                    tickCounter = 0;
                }
                break;

            case STATE_REOPEN_HOTM:
                if (tickCounter >= tickDelay) {
                    client.player.connection.sendCommand("hotm");
                    state = STATE_WAIT_REOPEN;
                    tickCounter = 0;
                }
                break;

            case STATE_WAIT_REOPEN:
                if (isHotmGuiOpen(client)) {
                    state = STATE_SETUP_ACTIONS;
                    tickCounter = 0;
                } else if (tickCounter >= 60) {
                    stop();
                }
                break;

            case STATE_SETUP_ACTIONS:
                buildActionQueue();
                actionIndex = 0;
                disableIndex = 0;
                state = STATE_NEXT_ACTION;
                tickCounter = 0;
                break;

            case STATE_NEXT_ACTION:
                if (tickCounter < tickDelay) break;
                if (actionIndex >= actions.size()) {
                    // All enable actions done, move to disable phase
                    state = STATE_DISABLE_PHASE;
                    tickCounter = 0;
                    break;
                }
                Action action = actions.get(actionIndex);
                // Check if we need to switch pages
                if (action.page != currentPage) {
                    state = STATE_SWITCH_PAGE;
                    tickCounter = 0;
                } else {
                    state = STATE_CLICK_ACTION;
                    tickCounter = 0;
                }
                break;

            case STATE_SWITCH_PAGE:
                if (tickCounter >= tickDelay) {
                    // Right-click the page arrow
                    clickSlot(client, PAGE_ARROW_SLOT, 1, ContainerInput.PICKUP);
                    currentPage = currentPage == 1 ? 2 : 1;
                    state = STATE_WAIT_PAGE;
                    tickCounter = 0;
                }
                break;

            case STATE_WAIT_PAGE:
                if (tickCounter >= 10) {
                    // Resume - go back to the appropriate phase to re-check
                    if (actionIndex >= actions.size()) {
                        state = STATE_DISABLE_PHASE;
                    } else {
                        state = STATE_NEXT_ACTION;
                    }
                    tickCounter = 0;
                }
                break;

            case STATE_CLICK_ACTION:
                if (tickCounter >= tickDelay) {
                    Action act = actions.get(actionIndex);
                    if (act.desiredState == HotmNode.State.MAXED) {
                        // First click to enable, then enter max loop
                        clickSlot(client, act.slot, 0, ContainerInput.PICKUP);
                        maxRetries = 0;
                        lastChatMessage = "";
                        state = STATE_MAX_LOOP;
                    } else {
                        // Single left click for LEVEL_1, CHOSEN, or DISABLED (enable first)
                        clickSlot(client, act.slot, 0, ContainerInput.PICKUP);
                        actionIndex++;
                        state = STATE_NEXT_ACTION;
                    }
                    tickCounter = 0;
                }
                break;

            case STATE_MAX_LOOP:
                if (tickCounter >= 2) {
                    Action act = actions.get(actionIndex);
                    // Shift+left click to buy 10 levels
                    clickSlot(client, act.slot, 0, ContainerInput.QUICK_MOVE);
                    state = STATE_MAX_CHECK;
                    tickCounter = 0;
                }
                break;

            case STATE_MAX_CHECK:
                if (tickCounter >= 2) {
                    Action act = actions.get(actionIndex);
                    maxRetries++;

                    // Check if maxed (diamond) or got "already purchased" message
                    if (client.screen instanceof ContainerScreen screen) {
                        var item = screen.getMenu().slots.get(act.slot).getItem();
                        if (item.getItem() == Items.DIAMOND) {
                            actionIndex++;
                            state = STATE_NEXT_ACTION;
                            tickCounter = 0;
                            break;
                        }
                    }

                    if (lastChatMessage.contains("You have already purchased this!")) {
                        actionIndex++;
                        state = STATE_NEXT_ACTION;
                        tickCounter = 0;
                        lastChatMessage = "";
                        break;
                    }

                    if (maxRetries > 50) {
                        // Safety: stop trying after 50 shift-clicks
                        LOGGER.warn("[AutoHotm] Max retries reached for {}", act.node.getDisplayName());
                        actionIndex++;
                        state = STATE_NEXT_ACTION;
                        tickCounter = 0;
                        break;
                    }

                    // Keep shift-clicking
                    state = STATE_MAX_LOOP;
                    tickCounter = 0;
                }
                break;

            case STATE_DISABLE_PHASE:
                if (tickCounter < tickDelay) break;
                if (disableIndex >= disableActions.size()) {
                    state = STATE_DONE;
                    tickCounter = 0;
                    break;
                }
                Action disableAct = disableActions.get(disableIndex);
                if (disableAct.page != currentPage) {
                    // Need to switch page for disable too
                    state = STATE_SWITCH_PAGE;
                    tickCounter = 0;
                } else {
                    state = STATE_DISABLE_CLICK;
                    tickCounter = 0;
                }
                break;

            case STATE_DISABLE_CLICK:
                if (tickCounter >= tickDelay) {
                    Action dAct = disableActions.get(disableIndex);
                    // Right-click to disable
                    clickSlot(client, dAct.slot, 1, ContainerInput.PICKUP);
                    disableIndex++;
                    state = STATE_DISABLE_PHASE;
                    tickCounter = 0;
                }
                break;

            case STATE_DONE:
                if (client.screen != null) {
                    client.screen.onClose();
                }
                client.player.sendSystemMessage(Component.literal("\u00A76[MQO] \u00A7aAuto-HOTM complete!"));
                running = false;
                state = STATE_IDLE;
                break;
        }
    }

    private static void buildActionQueue() {
        actions.clear();
        disableActions.clear();

        HotmTree tree = HotmManager.getTree();
        List<HotmNode> order = tree.getActivationOrder();

        // Split BFS results by page, preserving BFS order within each page
        List<Action> page1Enable = new ArrayList<>();
        List<Action> page2Enable = new ArrayList<>();

        for (HotmNode node : order) {
            // Skip always-enabled nodes (Core of the Mountain)
            if (node.isAlwaysEnabled()) continue;

            HotmNode.State desired = tree.getState(node);
            int page = getPageForNode(node);
            int slot = getSlotForNode(node);

            if (desired == HotmNode.State.DISABLED) {
                // Enable first (left click), then disable later (right click)
                Action enable = new Action(node, HotmNode.State.LEVEL_1, page, slot);
                if (page == 1) page1Enable.add(enable);
                else page2Enable.add(enable);
                disableActions.add(new Action(node, HotmNode.State.DISABLED, page, slot));
            } else {
                Action enable = new Action(node, desired, page, slot);
                if (page == 1) page1Enable.add(enable);
                else page2Enable.add(enable);
            }
        }

        // Page 1 first, then page 2
        actions.addAll(page1Enable);
        actions.addAll(page2Enable);

        int page1Count = 0, page2Count = 0;
        for (Action a : actions) {
            if (a.page == 1) page1Count++;
            else page2Count++;
        }

        // Debug: count all active nodes per page in tree
        HotmTree debugTree = HotmManager.getTree();
        int treePage1 = 0, treePage2 = 0;
        for (HotmNode n : HotmNode.values()) {
            if (debugTree.getState(n) != HotmNode.State.NOT_CLICKED) {
                if (n.getRow() >= 5) treePage1++;
                else treePage2++;
            }
        }

        Minecraft c = Minecraft.getInstance();
        if (c.player != null) {
            c.player.sendSystemMessage(Component.literal("\u00A76[MQO] \u00A77Tree has " + treePage1 + " active on P1, " + treePage2 + " on P2"));
            c.player.sendSystemMessage(Component.literal("\u00A76[MQO] \u00A77Actions: " + actions.size()
                + " enable (P1:" + page1Count + " P2:" + page2Count + "), "
                + disableActions.size() + " disable"));
        }
        LOGGER.info("[AutoHotm] Built {} enable actions (P1:{} P2:{}), {} disable actions",
            actions.size(), page1Count, page2Count, disableActions.size());
    }

    private static int getPageForNode(HotmNode node) {
        return node.getRow() >= 5 ? 1 : 2;
    }

    private static int getSlotForNode(HotmNode node) {
        int row0based;
        if (node.getRow() >= 5) {
            // Page 1: tree rows 5-9 -> vis rows 0-4
            row0based = node.getRow() - 5;
        } else {
            // Page 2: tree rows 0-4 -> vis rows 0-4
            row0based = node.getRow();
        }
        return row0based * 9 + node.getCol();
    }

    private static void clickSlot(Minecraft client, int slot, int button, ContainerInput actionType) {
        if (client.screen instanceof ContainerScreen screen) {
            var menu = screen.getMenu();
            if (slot < menu.slots.size()) {
                client.gameMode.handleContainerInput(menu.containerId, slot, button, actionType, client.player);
            }
        }
    }

    private static boolean isHotmGuiOpen(Minecraft client) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;
        return screen.getTitle().getString().contains("Heart of the Mountain");
    }
}
