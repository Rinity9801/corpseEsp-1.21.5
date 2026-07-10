package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CommClaimManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CommClaimManager");

    // Auto-trigger only fires once EVERY active mining commission is done.
    // A mining commission is a sidebar line containing one of these keywords
    // and NOT containing "Slayer" (so "Glacite Walker Slayer" is excluded).
    private static final String[] MINING_COMMISSION_KEYWORDS = {
        "aquamarine", "citrine", "glacite", "onyx", "peridot", "tungsten", "umber"
    };
    private static final int CHECK_INTERVAL_TICKS = 5; // ~0.25s between tab-list checks (fallback)
    private static boolean autoTriggerLatch = false; // true once we've fired for the current batch
    private static int autoCheckCounter = 0;
    private static int debugPrintCounter = 0;
    private static int fastPollTicks = 0; // >0: check every tick (after a completion message)
    private static boolean debug = false; // when on, prints commission classification ~once/sec

    /**
     * Called from the chat listener the instant a "Commission Complete" message
     * arrives. The message names the commission (e.g. "UMBER COLLECTOR Commission
     * Complete! ..."), so we can claim immediately without waiting for the tab list
     * to catch up:
     *   - non-mining commission  -> claim now
     *   - mining commission, and all other mining commissions already done -> claim now
     *   - otherwise              -> open a fast-poll fallback window
     */
    public static void onCommissionComplete(String message) {
        // Fallback: per-tick tab checks for ~3s in case the instant path below bails.
        fastPollTicks = 60;
        autoCheckCounter = CHECK_INTERVAL_TICKS;

        if (!autoTrigger || running) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        // "Claim after each" mode: any completion fires immediately.
        if (!batchMining) {
            fireAutoClaim("commission ready");
            return;
        }

        // Batch mode: classify by the commission name (text before "Commission Complete").
        String clean = message.replaceAll("\u00A7.", "");
        int idx = clean.toLowerCase().indexOf("commission complete");
        if (idx <= 0) return; // no name in this message: leave it to the fast-poll
        String name = clean.substring(0, idx).toLowerCase();

        boolean slayer = name.contains("slayer");
        boolean isMining = false;
        if (!slayer) {
            for (String kw : MINING_COMMISSION_KEYWORDS) {
                if (name.contains(kw)) {
                    isMining = true;
                    break;
                }
            }
        }

        if (!isMining) {
            // Non-mining (or slayer) commission: claim instantly even in batch mode.
            fireAutoClaim("commission ready");
            return;
        }
        // Mining commission in batch mode: fire immediately if this was the LAST unfinished
        // mining commission — i.e. every OTHER mining commission already reads done in the tab.
        // Identifying the just-completed one by name means we don't wait for its (lagging) tab
        // entry to flip to DONE. If others are still in progress, the fast-poll handles it.
        String justCompleted = name.trim();
        int otherMiningPending = 0;
        for (String line : getTabListLines(client)) {
            String l = line.replaceAll("§.", "").trim().toLowerCase();
            if (!(l.contains("%") || l.contains("done"))) continue;
            if (l.contains("slayer")) continue;
            boolean m = false;
            for (String kw : MINING_COMMISSION_KEYWORDS) {
                if (l.contains(kw)) { m = true; break; }
            }
            if (!m) continue;
            if (!justCompleted.isEmpty() && l.contains(justCompleted)) continue; // this one — its tab lags
            if (!(l.contains("done") || l.contains("100%"))) otherMiningPending++;
        }
        if (otherMiningPending == 0) {
            fireAutoClaim("all mining commissions done");
        }
        // else: other mining commissions still in progress — fast-poll handles it.
    }

    private static void fireAutoClaim(String reason) {
        if (running || autoTriggerLatch) return;
        autoTriggerLatch = true;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("\u00A76[CommClaim] \u00A7a" + reason + " — auto-claiming..."));
        }
        start();
    }

    public static void setDebug(boolean enabled) {
        debug = enabled;
    }

    public static boolean isDebug() {
        return debug;
    }

    private static boolean running = false;
    private static int state = 0;
    private static int tickCounter = 0;
    private static int tickDelay = 2; // Minimum ticks between actions
    private static int guiWaitDelay = 3; // Ticks to wait after GUI actions
    private static boolean autoTrigger = false; // Auto-trigger on commission complete message
    private static boolean wardrobeSwap = true; // Enable wardrobe/loadout armor swapping
    private static boolean batchMining = true;  // true: wait until ALL mining commissions are done;
                                                // false: claim as soon as ANY commission is done
    private static boolean blockInput = true;   // while running, swallow the player's clicks/keys
                                                // so they can't interfere with the automated claim

    // Config values
    private static int batPersonSlot = 1; // 1-12, loadout index (equip before claim)
    private static int divanSlot = 2; // 1-12, loadout index (equip after claim)
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

    // Wardrobe phase timings (kept separate from the configurable pigeon/claim delays).
    private static final int WARDROBE_READY_TIMEOUT = 40; // max ticks to wait for the armor slot to load
    private static final int WARDROBE_EQUIP_TICKS = 2;    // wait after clicking armor before closing
    private static final int WARDROBE_POST_CLOSE_TICKS = 1; // wait after closing the wardrobe

    public static void start() {
        if (running) {
            LOGGER.info("[CommClaim] Already running, ignoring start");
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        running = true;
        // Skip first wardrobe phase if wardrobe swap is disabled
        state = wardrobeSwap ? STATE_OPEN_WARDROBE_1 : STATE_FIND_PIGEON;
        tickCounter = 0;
        completedClickAttempts = 0;
        pigeonSlot = -1;

        LOGGER.info("[CommClaim] Starting commission claim sequence (wardrobeSwap={})", wardrobeSwap);
        client.player.sendSystemMessage(Component.literal("\u00A76[CommClaim] \u00A7aStarting commission claim..."));
    }

    public static void stop() {
        running = false;
        state = STATE_IDLE;
        tickCounter = 0;

        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.options.keyUse.setDown(false);
        }
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal("\u00A76[CommClaim] \u00A7cStopped"));
        }
    }

    public static boolean isRunning() {
        return running;
    }

    /**
     * Called each client tick. When auto-trigger is enabled, starts the claim
     * sequence only once ALL active mining commissions (per
     * {@link #MINING_COMMISSION_KEYWORDS}) read as done on the sidebar. Fires
     * once per batch; re-arms when an unfinished mining commission reappears.
     */
    public static void checkAutoTrigger(Minecraft client) {
        if (running) return;
        if (!autoTrigger && !debug) return; // still scan when debugging so we can inspect
        if (client.level == null || client.player == null) return;

        // After a completion message, check every tick for ~3s; otherwise ~4x/second.
        int interval = fastPollTicks > 0 ? 1 : CHECK_INTERVAL_TICKS;
        if (fastPollTicks > 0) fastPollTicks--;
        if (++autoCheckCounter < interval) return;
        autoCheckCounter = 0;

        // Print debug at most ~once per second to avoid chat spam.
        boolean printDebug = debug && (++debugPrintCounter >= 4);
        if (printDebug) debugPrintCounter = 0;

        int miningTotal = 0;
        int miningDone = 0;
        int nonMiningDone = 0;
        int commissionLines = 0;
        List<String> tab = getTabListLines(client);
        List<String> dbg = printDebug ? new ArrayList<>() : null;
        for (String line : tab) {
            String clean = line.replaceAll("\u00A7.", "").trim();
            if (clean.isEmpty()) continue;
            String lower = clean.toLowerCase();

            // A commission line is one showing progress (a percentage) or "DONE".
            // This excludes location/HUD text like "Glacite Tunnels".
            boolean commissionLine = lower.contains("%") || lower.contains("done");
            if (!commissionLine) continue;
            commissionLines++;

            boolean slayer = lower.contains("slayer");
            boolean isMining = false;
            if (!slayer) {
                for (String kw : MINING_COMMISSION_KEYWORDS) {
                    if (lower.contains(kw)) {
                        isMining = true;
                        break;
                    }
                }
            }
            boolean isDone = lower.contains("done") || lower.contains("100%");

            if (isMining) {
                miningTotal++;
                if (isDone) miningDone++;
            } else if (isDone) {
                nonMiningDone++;
            }
            if (dbg != null) {
                String tag = isMining ? (isDone ? "\u00A7a[MINE done] " : "\u00A7e[MINE  ..] ")
                                      : (isDone ? "\u00A7b[OTHER done]" : "\u00A77[other  ..]");
                dbg.add(tag + " \u00A7f" + clean);
            }
        }

        if (dbg != null) {
            client.player.sendSystemMessage(Component.literal(
                "\u00A76[CommClaim debug] \u00A7ftab=" + tab.size() + " comm=" + commissionLines
                    + " mining=" + miningDone + "/" + miningTotal + " otherDone=" + nonMiningDone
                    + " latch=" + autoTriggerLatch));
            for (String d : dbg) {
                client.player.sendSystemMessage(Component.literal(d));
            }
        }

        if (!autoTrigger) return;

        boolean claimable;
        String reason;
        if (batchMining) {
            // Non-mining claimed instantly; mining held until EVERY mining commission is done.
            boolean allMiningDone = miningTotal > 0 && miningDone == miningTotal;
            claimable = nonMiningDone > 0 || allMiningDone;
            reason = nonMiningDone > 0
                ? (nonMiningDone + " commission(s) ready")
                : ("all " + miningTotal + " mining commissions done");
        } else {
            int done = nonMiningDone + miningDone;
            claimable = done > 0;
            reason = done + " commission(s) ready";
        }

        if (!claimable) {
            autoTriggerLatch = false; // nothing claimable yet: re-arm
            return;
        }
        fireAutoClaim(reason);
    }

    private static List<String> getTabListLines(Minecraft client) {
        List<String> result = new ArrayList<>();
        if (client.getConnection() == null) return result;

        for (PlayerInfo entry : client.getConnection().getListedOnlinePlayers()) {
            Component displayName = entry.getTabListDisplayName();
            String s = displayName != null ? displayName.getString() : entry.getProfile().name();
            if (s != null && !s.trim().isEmpty()) {
                result.add(s);
            }
        }
        return result;
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
            // ===== PHASE 1: EQUIP BAT PERSON ARMOR =====
            case STATE_OPEN_WARDROBE_1:
                client.player.connection.sendCommand("loadout");
                state = STATE_WAIT_WARDROBE_1;
                tickCounter = 0;
                break;

            case STATE_WAIT_WARDROBE_1:
                if (isLoadoutOpen(client)) {
                    // Athen-style: click the instant the slot is populated (short fallback), then
                    // close on the next tick — no fixed equip/close waits.
                    if (isLoadoutSlotReady(client, batPersonSlot) || tickCounter >= WARDROBE_READY_TIMEOUT) {
                        clickLoadoutSlot(client, batPersonSlot);
                        state = STATE_CLOSE_WARDROBE_1;
                        tickCounter = 0;
                    }
                } else if (tickCounter >= 60) {
                    LOGGER.warn("[CommClaim] Loadout menu didn't open in time");
                    stop();
                }
                break;

            case STATE_CLOSE_WARDROBE_1:
                if (client.screen != null) {
                    client.screen.onClose();
                }
                state = STATE_FIND_PIGEON;
                tickCounter = 0;
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
                    client.player.sendSystemMessage(Component.literal("\u00A76[CommClaim] \u00A7cCould not find Royal Pigeon in hotbar"));
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
                client.options.keyUse.setDown(true);
                if (tickCounter >= 3) {
                    client.options.keyUse.setDown(false);
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
                if (client.screen != null) {
                    client.screen.onClose();
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
                client.player.connection.sendCommand("loadout");
                state = STATE_WAIT_WARDROBE_2;
                tickCounter = 0;
                break;

            case STATE_WAIT_WARDROBE_2:
                if (isLoadoutOpen(client)) {
                    if (isLoadoutSlotReady(client, divanSlot) || tickCounter >= WARDROBE_READY_TIMEOUT) {
                        clickLoadoutSlot(client, divanSlot);
                        state = STATE_CLOSE_WARDROBE_2;
                        tickCounter = 0;
                    }
                } else if (tickCounter >= 60) {
                    LOGGER.warn("[CommClaim] Second loadout menu didn't open");
                    stop();
                }
                break;

            case STATE_CLOSE_WARDROBE_2:
                if (client.screen != null) {
                    client.screen.onClose();
                }
                state = STATE_DONE;
                tickCounter = 0;
                break;

            case STATE_DONE:
                client.player.sendSystemMessage(Component.literal("\u00A76[CommClaim] \u00A7aCommission claim complete!"));
                LOGGER.info("[CommClaim] Sequence complete");
                running = false;
                state = STATE_IDLE;
                break;
        }
    }

    private static boolean isLoadoutOpen(Minecraft client) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;
        String title = screen.getTitle().getString();
        return title.toLowerCase().contains("loadout");
    }

    /**
     * Hypixel now equips via /loadout: a 3x4 grid at columns 6-8, rows 2-5 (1-indexed).
     * Loadout 1-12 reads left-to-right, top-to-bottom; convert to a 0-indexed chest slot.
     */
    private static int loadoutSlotIndex(int loadout) {
        int n = Math.max(1, Math.min(12, loadout)) - 1;
        int row = 1 + n / 3; // rows 2-5 -> 0-indexed 1-4
        int col = 5 + n % 3; // columns 6-8 -> 0-indexed 5-7
        return row * 9 + col;
    }

    private static boolean isPigeonGuiOpen(Minecraft client) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;
        String title = screen.getTitle().getString();
        // Royal Pigeon opens a commission-related GUI
        return title.contains("Commissions") || title.contains("Pigeon") || title.contains("Commission");
    }

    private static boolean isLoadoutSlotReady(Minecraft client, int column) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;
        int slotIndex = loadoutSlotIndex(column);
        var menu = screen.getMenu();
        if (slotIndex >= menu.slots.size()) return false;
        return !menu.slots.get(slotIndex).getItem().isEmpty();
    }

    private static boolean clickLoadoutSlot(Minecraft client, int column) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;

        int slotIndex = loadoutSlotIndex(column);

        try {
            var menu = screen.getMenu();
            if (slotIndex < menu.slots.size()) {
                client.gameMode.handleContainerInput(
                    menu.containerId,
                    slotIndex,
                    0,
                    ContainerInput.PICKUP,
                    client.player
                );
                LOGGER.info("[CommClaim] Clicked loadout {} (slot index {})", column, slotIndex);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("[CommClaim] Error clicking loadout slot", e);
        }
        return false;
    }

    private static int findPigeonSlot(Minecraft client) {
        if (client.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            // Check if it's a player head
            if (stack.getItem() != Items.PLAYER_HEAD) continue;

            // Check lore for ROYAL_PIGEON
            var lore = stack.get(DataComponents.LORE);
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
            var customData = stack.get(DataComponents.CUSTOM_DATA);
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

    private static boolean hasCompletedSlots(Minecraft client) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;

        var menu = screen.getMenu();

        for (int i = 0; i < menu.slots.size() - 36; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            var lore = stack.get(DataComponents.LORE);
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

    private static boolean clickCompletedSlots(Minecraft client) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;

        var menu = screen.getMenu();

        for (int i = 0; i < menu.slots.size() - 36; i++) { // Exclude player inventory
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            // Check lore for "Completed" or "COMPLETED"
            var lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (var line : lore.lines()) {
                    String lineStr = line.getString().toLowerCase();
                    if (lineStr.contains("completed") || lineStr.contains("click to claim")) {
                        client.gameMode.handleContainerInput(
                            menu.containerId,
                            i,
                            0,
                            ContainerInput.PICKUP,
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
        batPersonSlot = Math.max(1, Math.min(12, slot));
    }

    public static int getDivanSlot() {
        return divanSlot;
    }

    public static void setDivanSlot(int slot) {
        divanSlot = Math.max(1, Math.min(12, slot));
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

    public static boolean isBatchMining() {
        return batchMining;
    }

    public static void setBatchMining(boolean enabled) {
        batchMining = enabled;
    }

    public static boolean isBlockInput() {
        return blockInput;
    }

    public static void setBlockInput(boolean enabled) {
        blockInput = enabled;
    }
}
