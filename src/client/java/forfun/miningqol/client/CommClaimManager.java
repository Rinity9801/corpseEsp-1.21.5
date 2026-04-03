package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommClaimManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CommClaimManager");

    private static boolean running = false;
    private static int state = 0;
    private static int tickCounter = 0;
    private static int tickDelay = 2; // Minimum ticks between actions
    private static int guiWaitDelay = 3; // Ticks to wait after GUI actions
    private static boolean autoTrigger = false; // Auto-trigger on commission complete message
    private static boolean wardrobeSwap = true; // Enable wardrobe armor swapping

    // Config values
    private static int batPersonSlot = 1; // 1-9, column in wardrobe row 5
    private static int divanSlot = 2; // 1-9, column in wardrobe row 5
    private static int refinedToolSlot = 0; // Hotbar slot 0-8

    // State machine states
    private static final int STATE_IDLE = 0;
    // Phase 1: Equip Bat Person armor
    private static final int STATE_OPEN_WARDROBE_1 = 1;
    private static final int STATE_WAIT_WARDROBE_1 = 2;
    private static final int STATE_DELAY_BEFORE_CLICK_BAT = 3;
    private static final int STATE_CLICK_BAT_ARMOR = 4;
    private static final int STATE_DELAY_AFTER_CLICK_BAT = 5;
    private static final int STATE_CLOSE_WARDROBE_1 = 6;
    private static final int STATE_DELAY_AFTER_CLOSE_1 = 7;
    // Phase 2: Use pigeon and switch to refined tool
    private static final int STATE_FIND_PIGEON = 8;
    private static final int STATE_DELAY_BEFORE_USE_PIGEON = 9;
    private static final int STATE_USE_PIGEON = 10;
    private static final int STATE_SWITCH_REFINED = 11; // Immediate after pigeon right-click
    // Phase 3: Claim commissions
    private static final int STATE_WAIT_PIGEON_GUI = 12;
    private static final int STATE_DELAY_AFTER_PIGEON_OPEN = 25;
    private static final int STATE_DELAY_BEFORE_CLICK_COMPLETED = 13;
    private static final int STATE_CLICK_COMPLETED = 14;
    private static final int STATE_DELAY_AFTER_CLICK_COMPLETED = 15;
    private static final int STATE_CLOSE_PIGEON = 16;
    private static final int STATE_DELAY_AFTER_CLOSE_PIGEON = 17;
    // Phase 4: Equip Divan armor
    private static final int STATE_OPEN_WARDROBE_2 = 18;
    private static final int STATE_WAIT_WARDROBE_2 = 19;
    private static final int STATE_DELAY_BEFORE_CLICK_DIVAN = 20;
    private static final int STATE_CLICK_DIVAN_ARMOR = 21;
    private static final int STATE_DELAY_AFTER_CLICK_DIVAN = 22;
    private static final int STATE_CLOSE_WARDROBE_2 = 23;
    private static final int STATE_DONE = 24;

    private static int pigeonSlot = -1;
    private static int completedClickAttempts = 0;
    private static final int MAX_COMPLETED_CLICKS = 10;

    public static void start() {
        if (running) {
            LOGGER.info("[CommClaim] Already running, ignoring start");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        running = true;
        // Skip first wardrobe phase if wardrobe swap is disabled
        state = wardrobeSwap ? STATE_OPEN_WARDROBE_1 : STATE_FIND_PIGEON;
        tickCounter = 0;
        completedClickAttempts = 0;
        pigeonSlot = -1;

        LOGGER.info("[CommClaim] Starting commission claim sequence (wardrobeSwap={})", wardrobeSwap);
        client.player.sendMessage(net.minecraft.text.Text.literal("§6[CommClaim] §aStarting commission claim..."), false);
    }

    public static void stop() {
        running = false;
        state = STATE_IDLE;
        tickCounter = 0;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.options.useKey.setPressed(false);
        }
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§6[CommClaim] §cStopped"), false);
        }
    }

    public static boolean isRunning() {
        return running;
    }

    public static void tick() {
        if (!running) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            stop();
            return;
        }

        tickCounter++;

        switch (state) {
            // ===== PHASE 1: EQUIP BAT PERSON ARMOR =====
            case STATE_OPEN_WARDROBE_1:
                client.player.networkHandler.sendChatCommand("wardrobe");
                state = STATE_WAIT_WARDROBE_1;
                tickCounter = 0;
                break;

            case STATE_WAIT_WARDROBE_1:
                if (isWardrobeOpen(client)) {
                    state = STATE_DELAY_BEFORE_CLICK_BAT;
                    tickCounter = 0;
                } else if (tickCounter >= 60) {
                    LOGGER.warn("[CommClaim] Wardrobe didn't open in time");
                    stop();
                }
                break;

            case STATE_DELAY_BEFORE_CLICK_BAT:
                if (tickCounter >= tickDelay) {
                    state = STATE_CLICK_BAT_ARMOR;
                    tickCounter = 0;
                }
                break;

            case STATE_CLICK_BAT_ARMOR:
                if (clickWardrobeSlot(client, batPersonSlot)) {
                    state = STATE_DELAY_AFTER_CLICK_BAT;
                    tickCounter = 0;
                } else {
                    stop();
                }
                break;

            case STATE_DELAY_AFTER_CLICK_BAT:
                if (tickCounter >= guiWaitDelay) {
                    state = STATE_CLOSE_WARDROBE_1;
                    tickCounter = 0;
                }
                break;

            case STATE_CLOSE_WARDROBE_1:
                if (client.currentScreen != null) {
                    client.currentScreen.close();
                }
                state = STATE_DELAY_AFTER_CLOSE_1;
                tickCounter = 0;
                break;

            case STATE_DELAY_AFTER_CLOSE_1:
                if (tickCounter >= tickDelay) {
                    state = STATE_FIND_PIGEON;
                    tickCounter = 0;
                }
                break;

            // ===== PHASE 2: USE PIGEON & SWITCH TO REFINED TOOL =====
            case STATE_FIND_PIGEON:
                pigeonSlot = findPigeonSlot(client);
                if (pigeonSlot != -1) {
                    client.player.getInventory().setSelectedSlot(pigeonSlot);
                    state = STATE_DELAY_BEFORE_USE_PIGEON;
                    tickCounter = 0;
                } else {
                    LOGGER.warn("[CommClaim] Could not find Royal Pigeon in hotbar");
                    client.player.sendMessage(net.minecraft.text.Text.literal("§6[CommClaim] §cCould not find Royal Pigeon in hotbar"), false);
                    stop();
                }
                break;

            case STATE_DELAY_BEFORE_USE_PIGEON:
                if (tickCounter >= tickDelay) {
                    state = STATE_USE_PIGEON;
                    tickCounter = 0;
                }
                break;

            case STATE_USE_PIGEON:
                // Right click the pigeon
                client.options.useKey.setPressed(true);
                if (tickCounter >= 3) {
                    client.options.useKey.setPressed(false);
                    // Immediately switch to refined tool after right-clicking pigeon
                    state = STATE_SWITCH_REFINED;
                    tickCounter = 0;
                }
                break;

            case STATE_SWITCH_REFINED:
                // Immediate switch - no delay, right after pigeon use
                client.player.getInventory().setSelectedSlot(refinedToolSlot);
                state = STATE_WAIT_PIGEON_GUI;
                tickCounter = 0;
                break;

            // ===== PHASE 3: CLAIM COMMISSIONS =====
            case STATE_WAIT_PIGEON_GUI:
                if (isPigeonGuiOpen(client)) {
                    state = STATE_DELAY_AFTER_PIGEON_OPEN;
                    tickCounter = 0;
                    completedClickAttempts = 0;
                } else if (tickCounter >= 60) {
                    LOGGER.warn("[CommClaim] Pigeon GUI didn't open");
                    stop();
                }
                break;

            case STATE_DELAY_AFTER_PIGEON_OPEN:
                if (tickCounter >= 2) {
                    state = STATE_DELAY_BEFORE_CLICK_COMPLETED;
                    tickCounter = 0;
                }
                break;

            case STATE_DELAY_BEFORE_CLICK_COMPLETED:
                if (tickCounter >= tickDelay) {
                    state = STATE_CLICK_COMPLETED;
                    tickCounter = 0;
                }
                break;

            case STATE_CLICK_COMPLETED:
                boolean clicked = clickCompletedSlots(client);
                completedClickAttempts++;

                if (!clicked || completedClickAttempts >= MAX_COMPLETED_CLICKS) {
                    state = STATE_DELAY_AFTER_CLICK_COMPLETED;
                    tickCounter = 0;
                } else {
                    // More to click - add delay between clicks
                    state = STATE_DELAY_AFTER_CLICK_COMPLETED;
                    tickCounter = 0;
                }
                break;

            case STATE_DELAY_AFTER_CLICK_COMPLETED:
                if (tickCounter >= tickDelay) {
                    // Check if we should click more or close
                    if (completedClickAttempts < MAX_COMPLETED_CLICKS && hasCompletedSlots(client)) {
                        state = STATE_CLICK_COMPLETED;
                        tickCounter = 0;
                    } else {
                        state = STATE_CLOSE_PIGEON;
                        tickCounter = 0;
                    }
                }
                break;

            case STATE_CLOSE_PIGEON:
                if (client.currentScreen != null) {
                    client.currentScreen.close();
                }
                state = STATE_DELAY_AFTER_CLOSE_PIGEON;
                tickCounter = 0;
                break;

            case STATE_DELAY_AFTER_CLOSE_PIGEON:
                if (tickCounter >= tickDelay) {
                    // Skip second wardrobe phase if wardrobe swap is disabled
                    state = wardrobeSwap ? STATE_OPEN_WARDROBE_2 : STATE_DONE;
                    tickCounter = 0;
                }
                break;

            // ===== PHASE 4: EQUIP DIVAN ARMOR =====
            case STATE_OPEN_WARDROBE_2:
                client.player.networkHandler.sendChatCommand("wardrobe");
                state = STATE_WAIT_WARDROBE_2;
                tickCounter = 0;
                break;

            case STATE_WAIT_WARDROBE_2:
                if (isWardrobeOpen(client)) {
                    state = STATE_DELAY_BEFORE_CLICK_DIVAN;
                    tickCounter = 0;
                } else if (tickCounter >= 60) {
                    LOGGER.warn("[CommClaim] Second wardrobe didn't open");
                    stop();
                }
                break;

            case STATE_DELAY_BEFORE_CLICK_DIVAN:
                if (tickCounter >= tickDelay) {
                    state = STATE_CLICK_DIVAN_ARMOR;
                    tickCounter = 0;
                }
                break;

            case STATE_CLICK_DIVAN_ARMOR:
                if (clickWardrobeSlot(client, divanSlot)) {
                    state = STATE_DELAY_AFTER_CLICK_DIVAN;
                    tickCounter = 0;
                } else {
                    stop();
                }
                break;

            case STATE_DELAY_AFTER_CLICK_DIVAN:
                if (tickCounter >= guiWaitDelay) {
                    state = STATE_CLOSE_WARDROBE_2;
                    tickCounter = 0;
                }
                break;

            case STATE_CLOSE_WARDROBE_2:
                if (client.currentScreen != null) {
                    client.currentScreen.close();
                }
                state = STATE_DONE;
                tickCounter = 0;
                break;

            case STATE_DONE:
                client.player.sendMessage(net.minecraft.text.Text.literal("§6[CommClaim] §aCommission claim complete!"), false);
                LOGGER.info("[CommClaim] Sequence complete");
                running = false;
                state = STATE_IDLE;
                break;
        }
    }

    private static boolean isWardrobeOpen(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen screen)) return false;
        String title = screen.getTitle().getString();
        return title.contains("Wardrobe");
    }

    private static boolean isPigeonGuiOpen(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen screen)) return false;
        String title = screen.getTitle().getString();
        // Royal Pigeon opens a commission-related GUI
        return title.contains("Commissions") || title.contains("Pigeon") || title.contains("Commission");
    }

    private static boolean clickWardrobeSlot(MinecraftClient client, int column) {
        if (!(client.currentScreen instanceof GenericContainerScreen screen)) return false;

        // Wardrobe row 5 is the equip row
        // Row 5 in a 6-row chest = slots 36-44 (index 4 * 9 = 36)
        // Column 1-9 maps to slots 36-44
        int slotIndex = 36 + (column - 1);

        try {
            var handler = screen.getScreenHandler();
            if (slotIndex < handler.slots.size()) {
                client.interactionManager.clickSlot(
                    handler.syncId,
                    slotIndex,
                    0,
                    SlotActionType.PICKUP,
                    client.player
                );
                LOGGER.info("[CommClaim] Clicked wardrobe slot {} (index {})", column, slotIndex);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("[CommClaim] Error clicking wardrobe slot", e);
        }
        return false;
    }

    private static int findPigeonSlot(MinecraftClient client) {
        if (client.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            // Check if it's a player head
            if (stack.getItem() != Items.PLAYER_HEAD) continue;

            // Check lore for ROYAL_PIGEON
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (var line : lore.lines()) {
                    String lineStr = line.getString();
                    if (lineStr.contains("ROYAL_PIGEON")) {
                        LOGGER.info("[CommClaim] Found Royal Pigeon in slot {}", i);
                        return i;
                    }
                }
            }

            // Also check custom name or item ID in NBT
            var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData != null) {
                String nbtStr = customData.toString();
                if (nbtStr.contains("ROYAL_PIGEON")) {
                    LOGGER.info("[CommClaim] Found Royal Pigeon (via NBT) in slot {}", i);
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean hasCompletedSlots(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen screen)) return false;

        var handler = screen.getScreenHandler();

        for (int i = 0; i < handler.slots.size() - 36; i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (var line : lore.lines()) {
                    String lineStr = line.getString().toLowerCase();
                    if (lineStr.contains("completed") || lineStr.contains("click to claim")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean clickCompletedSlots(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen screen)) return false;

        var handler = screen.getScreenHandler();

        for (int i = 0; i < handler.slots.size() - 36; i++) { // Exclude player inventory
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            // Check lore for "Completed" or "COMPLETED"
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (var line : lore.lines()) {
                    String lineStr = line.getString().toLowerCase();
                    if (lineStr.contains("completed") || lineStr.contains("click to claim")) {
                        client.interactionManager.clickSlot(
                            handler.syncId,
                            i,
                            0,
                            SlotActionType.PICKUP,
                            client.player
                        );
                        LOGGER.info("[CommClaim] Clicked completed commission slot {}", i);
                        return true; // Click one at a time
                    }
                }
            }
        }

        return false;
    }

    // Config getters/setters
    public static int getBatPersonSlot() {
        return batPersonSlot;
    }

    public static void setBatPersonSlot(int slot) {
        batPersonSlot = Math.max(1, Math.min(9, slot));
    }

    public static int getDivanSlot() {
        return divanSlot;
    }

    public static void setDivanSlot(int slot) {
        divanSlot = Math.max(1, Math.min(9, slot));
    }

    public static int getRefinedToolSlot() {
        return refinedToolSlot;
    }

    public static void setRefinedToolSlot(int slot) {
        refinedToolSlot = Math.max(0, Math.min(8, slot));
    }

    public static int getTickDelay() {
        return tickDelay;
    }

    public static void setTickDelay(int delay) {
        tickDelay = Math.max(1, Math.min(10, delay));
    }

    public static int getGuiWaitDelay() {
        return guiWaitDelay;
    }

    public static void setGuiWaitDelay(int delay) {
        guiWaitDelay = Math.max(1, Math.min(10, delay));
    }

    public static boolean isAutoTrigger() {
        return autoTrigger;
    }

    public static void setAutoTrigger(boolean enabled) {
        autoTrigger = enabled;
    }

    public static boolean isWardrobeSwap() {
        return wardrobeSwap;
    }

    public static void setWardrobeSwap(boolean enabled) {
        wardrobeSwap = enabled;
    }
}
