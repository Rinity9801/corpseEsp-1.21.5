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

import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        // A fresh completion is new evidence — re-arm even while the tab still shows
        // stale DONE lines from the previous claim (those keep the latch stuck).
        autoTriggerLatch = false;

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

        // Mineshaft Explorer completes the moment you enter a shaft \u2014 always claim
        // instantly, never hold it for the mining batch.
        if (name.contains("mineshaft explorer")) {
            fireNotBefore = System.currentTimeMillis() + 2_500L;
            fireAutoClaim("mineshaft explorer complete");
            return;
        }

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
            if (l.contains("mineshaft explorer")) continue; // instant-claim, never batched
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

    /**
     * The tab list only carries the "Commissions:" widget on islands where
     * commissions exist. Elsewhere (hub, garden, ...) the tab still has skill
     * widget lines like "Combat 57: 95.6%" and XP bars ending in "(100%)" that
     * the line classifier would miscount as completed commissions — so no
     * widget, no auto-claim.
     */
    private static boolean tabHasCommissionsWidget(Minecraft client) {
        for (String line : getTabListLines(client)) {
            String clean = line.replaceAll("§.", "").trim().toLowerCase();
            if (clean.startsWith("commissions")) {
                return true;
            }
        }
        return false;
    }

    /** Tab skill/XP widget lines that contain a % but are not commissions. */
    private static boolean isSkillOrXpLine(String lower) {
        if (lower.contains("xp")) return true;
        if (lower.startsWith("|")) return true; // XP progress bar "||||||| (100%)"
        for (String skill : new String[]{"farming", "mining", "combat", "foraging", "fishing",
                "enchanting", "alchemy", "carpentry", "runecrafting", "taming", "social", "skill"}) {
            if (lower.startsWith(skill + " ")) return true;
        }
        return false;
    }

    // A claim that arrived while the tab was still loading (fresh server right after
    // entering a shaft) — held here and fired the moment the Commissions widget shows.
    private static int pendingFireTicks = 0;
    private static String pendingFireReason = null;

    private static void fireAutoClaim(String reason) {
        if (running || autoTriggerLatch) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        // Held without latching until the location has settled. Mineshaft Explorer
        // completes in the same instant you enter a shaft, so the sidebar still reads the
        // old area when the message lands and the shaft check below would pass on it.
        if (System.currentTimeMillis() < fireNotBefore) {
            pendingFireTicks = Math.max(pendingFireTicks, 100);
            pendingFireReason = reason;
            return;
        }
        // Mismyla cannot be called from a shaft. Hold the trigger without latching or
        // announcing, so it fires on its own once you are out — Mineshaft Explorer
        // completes the moment you enter one, which is exactly this case.
        if (hasMineshaftScoreboard(client)) {
            pendingFireTicks = Math.max(pendingFireTicks, 200);
            pendingFireReason = reason;
            return;
        }
        if (!tabHasCommissionsWidget(client)) {
            pendingFireTicks = 200; // keep trying for ~10s while the tab populates
            pendingFireReason = reason;
            return;
        }
        pendingFireTicks = 0;
        pendingFireReason = null;
        autoTriggerLatch = true;
        if (client.player != null) {
            MqoChat.log(Component.literal("\u00A76[CommClaim] \u00A7a" + reason + " — auto-claiming..."));
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
    private static int guiWaitDelay = 10; // Seconds a menu gets to appear before giving up
    private static boolean autoTrigger = false; // Auto-trigger on commission complete message
    private static boolean wardrobeSwap = true; // Enable wardrobe/loadout armor swapping
    private static boolean batchMining = true;  // true: wait until ALL mining commissions are done;
                                                // false: claim as soon as ANY commission is done
    private static boolean blockInput = true;   // while running, swallow the player's clicks/keys
                                                // so they can't interfere with the automated claim
    private static boolean hideGui = false;     // while running, hide the container GUI visuals

    // Config values
    private static int batPersonSlot = 1; // 1-12, loadout index (equip before claim)
    private static int divanSlot = 2; // 1-12, loadout index (equip after claim)
    private static int refinedToolSlot = 0; // Hotbar slot 0-8

    // State machine — millisecond driven, so a laggy server stretches waits rather
    // than desynchronising a tick counter.
    private enum State {
        IDLE,
        CALL_MISMYLA,
        OPEN_CLAIM_LOADOUT,
        WAIT_CLAIM_LOADOUT,
        CLOSE_CLAIM_LOADOUT,
        WAIT_COMMISSIONS,
        CLAIM,
        CLOSE_COMMISSIONS,
        OPEN_RETURN_LOADOUT,
        WAIT_RETURN_LOADOUT,
        CLOSE_RETURN_LOADOUT,
        DONE
    }

    private static State phase = State.IDLE;
    private static long readyAt = 0L;
    private static long timeoutAt = 0L;
    private static int claimAttempts = 0;
    private static long lastShaftCheckAt = 0L;
    private static long fireNotBefore = 0L;
    private static final int MAX_CLAIM_ATTEMPTS = 10;
    /** Brief pause after the commission menu closes before changing loadouts. */
    private static final int COMMISSION_CLOSE_DELAY_MS = 120;

    public static void start() {
        if (running) {
            LOGGER.info("[CommClaim] Already running, ignoring start");
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Mismyla cannot be called from a shaft, so the run would only time out.
        if (hasMineshaftScoreboard(client)) {
            MqoChat.log(Component.literal("\u00A76[CommClaim] \u00A7cUnavailable in mineshafts."));
            return;
        }

        running = true;
        phase = State.CALL_MISMYLA;
        // A beat before the first command so the sidebar can catch up with a location
        // change that happened in the same moment as the trigger.
        readyAt = System.currentTimeMillis() + 500L;
        timeoutAt = 0L;
        claimAttempts = 0;

        LOGGER.info("[CommClaim] Starting commission claim sequence (loadoutSwap={})", wardrobeSwap);
        MqoChat.log(Component.literal("\u00A76[CommClaim] \u00A7aStarting commission claim..."));
    }

    public static void stop() {
        running = false;
        phase = State.IDLE;
        readyAt = 0L;
        timeoutAt = 0L;
        claimAttempts = 0;

        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.options.keyUse.setDown(false);
        }
        if (client != null && client.player != null) {
            MqoChat.log(Component.literal("\u00A76[CommClaim] \u00A7cStopped"));
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

        // Retry a claim that was held for the tab to load (checked every tick).
        if (pendingFireTicks > 0) {
            pendingFireTicks--;
            if (tabHasCommissionsWidget(client)) {
                String reason = pendingFireReason != null ? pendingFireReason : "commission ready";
                pendingFireTicks = 0;
                pendingFireReason = null;
                fireAutoClaim(reason);
                return;
            }
        }

        // After a completion message, check every tick for ~3s; otherwise ~4x/second.
        int interval = fastPollTicks > 0 ? 1 : CHECK_INTERVAL_TICKS;
        if (fastPollTicks > 0) fastPollTicks--;
        if (++autoCheckCounter < interval) return;
        autoCheckCounter = 0;

        // Print debug at most ~once per second to avoid chat spam.
        boolean printDebug = debug && (++debugPrintCounter >= 4);
        if (printDebug) debugPrintCounter = 0;

        boolean commWidget = tabHasCommissionsWidget(client);

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
            if (isSkillOrXpLine(lower)) continue; // skill widget lines also carry percentages
            commissionLines++;

            boolean slayer = lower.contains("slayer");
            // Mineshaft Explorer stays non-mining even where its tab name carries a
            // mining keyword (e.g. "Glacite Mineshaft Explorer") — done = claim now.
            boolean instantComm = lower.contains("mineshaft explorer");
            boolean isMining = false;
            if (!slayer && !instantComm) {
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
            MqoChat.log(Component.literal(
                "\u00A76[CommClaim debug] \u00A7ftab=" + tab.size() + " comm=" + commissionLines
                    + " mining=" + miningDone + "/" + miningTotal + " otherDone=" + nonMiningDone
                    + " latch=" + autoTriggerLatch + " commWidget=" + commWidget));
            for (String d : dbg) {
                MqoChat.log(Component.literal(d));
            }
        }

        if (!autoTrigger) return;
        if (!commWidget) return; // not on a commission island — leave the latch as-is

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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.player.connection == null) {
            if (running) stop();
            return;
        }
        if (!running) return;

        long now = System.currentTimeMillis();
        // A warp can drop you into a shaft part way through; the rest of the run would
        // only time out there.
        if (now - lastShaftCheckAt >= 500L) {
            lastShaftCheckAt = now;
            if (hasMineshaftScoreboard(client)) {
                fail("Entered a mineshaft — claim aborted.");
                return;
            }
        }
        if (now < readyAt) return;

        switch (phase) {
            case CALL_MISMYLA -> {
                // Re-checked unguarded by the throttle: the trigger for this run is often
                // Mineshaft Explorer, whose completion message beats the sidebar update, so
                // the start-time check can pass on a location that is already stale.
                if (hasMineshaftScoreboard(client)) {
                    fail("In a mineshaft — claim aborted.");
                    return;
                }
                client.player.connection.sendCommand("call mismyla");
                client.player.getInventory().setSelectedSlot(Math.max(0, Math.min(8, refinedToolSlot)));
                if (wardrobeSwap) {
                    advance(State.OPEN_CLAIM_LOADOUT);
                } else {
                    waitForCommissions();
                }
            }
            case OPEN_CLAIM_LOADOUT -> {
                client.player.connection.sendCommand("loadout");
                phase = State.WAIT_CLAIM_LOADOUT;
                timeoutAt = now + menuTimeoutMs();
            }
            case WAIT_CLAIM_LOADOUT -> {
                if (isLoadoutOpen(client) && isLoadoutSlotReady(client, batPersonSlot)) {
                    if (!clickLoadoutSlot(client, batPersonSlot)) {
                        fail("Could not select the claim loadout.");
                        return;
                    }
                    advance(State.CLOSE_CLAIM_LOADOUT);
                } else if (now >= timeoutAt) {
                    fail("Claim loadout did not open.");
                }
            }
            case CLOSE_CLAIM_LOADOUT -> {
                closeIfOpen(client, true);
                waitForCommissions();
            }
            case WAIT_COMMISSIONS -> {
                if (isCommissionsOpen(client)) {
                    advance(State.CLAIM);
                } else if (now >= timeoutAt) {
                    fail("Queen Mismyla did not answer.");
                }
            }
            case CLAIM -> {
                if (!isCommissionsOpen(client)) {
                    // Claiming the last one closes the menu server-side. That is a normal
                    // finish, not a failure — only an empty-handed close is a problem.
                    if (claimAttempts > 0) {
                        advanceIn(State.CLOSE_COMMISSIONS, COMMISSION_CLOSE_DELAY_MS);
                    } else {
                        fail("Commissions closed unexpectedly.");
                    }
                    return;
                }
                if (claimAttempts < MAX_CLAIM_ATTEMPTS && clickCompletedCommission(client)) {
                    claimAttempts++;
                    // Match the old implementation: wait the configured action delay,
                    // then scan the current menu again without waiting for an acknowledgement.
                    advance(State.CLAIM);
                } else {
                    advance(State.CLOSE_COMMISSIONS);
                }
            }
            case CLOSE_COMMISSIONS -> {
                if (client.screen != null && isCommissionsOpen(client)) client.screen.onClose();
                if (wardrobeSwap) {
                    advanceIn(State.OPEN_RETURN_LOADOUT, COMMISSION_CLOSE_DELAY_MS);
                } else {
                    advance(State.DONE);
                }
            }
            case OPEN_RETURN_LOADOUT -> {
                client.player.connection.sendCommand("loadout");
                phase = State.WAIT_RETURN_LOADOUT;
                timeoutAt = now + menuTimeoutMs();
            }
            case WAIT_RETURN_LOADOUT -> {
                if (isLoadoutOpen(client) && isLoadoutSlotReady(client, divanSlot)) {
                    if (!clickLoadoutSlot(client, divanSlot)) {
                        fail("Could not select the return loadout.");
                        return;
                    }
                    advance(State.CLOSE_RETURN_LOADOUT);
                } else if (now >= timeoutAt) {
                    fail("Return loadout did not open.");
                }
            }
            case CLOSE_RETURN_LOADOUT -> {
                closeIfOpen(client, true);
                advance(State.DONE);
            }
            case DONE -> {
                running = false;
                phase = State.IDLE;
                MqoChat.log(Component.literal("\u00A76[CommClaim] \u00A7aFinished."));
            }
            case IDLE -> running = false;
        }
    }

    /** Milliseconds between actions, from the Action Delay slider. */
    private static long actionDelayMs() {
        return Math.max(50, Math.min(1000, tickDelay * 50L));
    }

    /** How long a menu gets to appear before the run is abandoned. */
    private static long menuTimeoutMs() {
        return Math.max(5, guiWaitDelay) * 1000L;
    }

    private static void advance(State next) {
        advanceIn(next, actionDelayMs());
    }

    private static void advanceIn(State next, long delayMs) {
        phase = next;
        readyAt = System.currentTimeMillis() + delayMs;
        timeoutAt = 0L;
    }

    private static void waitForCommissions() {
        long now = System.currentTimeMillis();
        phase = State.WAIT_COMMISSIONS;
        readyAt = now + actionDelayMs();
        timeoutAt = now + menuTimeoutMs();
    }

    /** Aborts the run with a reason, leaving no menu open behind us. */
    private static void fail(String reason) {
        LOGGER.info("[CommClaim] {}", reason);
        MqoChat.log(Component.literal("\u00A76[CommClaim] \u00A7c" + reason));
        stop();
    }

    private static void closeIfOpen(Minecraft client, boolean loadout) {
        if (client.screen == null) return;
        if (loadout ? isLoadoutOpen(client) : isCommissionsOpen(client)) client.screen.onClose();
    }

    /**
     * Delegates to the auto-party's detector rather than keeping a second copy.
     *
     * <p>That one is the version actually verified in game through /shaftid, and a
     * near-duplicate here is how a shaft guard ends up silently not firing.
     */
    static boolean hasMineshaftScoreboard(Minecraft client) {
        return forfun.miningqol.client.party.MineshaftAutoParty.isInMineshaft();
    }

    private static boolean isLoadoutOpen(Minecraft client) {
        return client.screen instanceof ContainerScreen screen
            && screen.getTitle().getString().toLowerCase(Locale.ROOT).contains("loadout");
    }

    private static boolean isCommissionsOpen(Minecraft client) {
        return client.screen instanceof ContainerScreen screen
            && screen.getTitle().getString().toLowerCase(Locale.ROOT).contains("commission");
    }

    /** Loadouts sit in a 3-wide block starting at the second row, sixth column. */
    private static int loadoutSlotIndex(int loadout) {
        int index = Math.max(1, Math.min(12, loadout)) - 1;
        return (1 + index / 3) * 9 + 5 + index % 3;
    }

    private static boolean isLoadoutSlotReady(Minecraft client, int loadout) {
        if (!(client.screen instanceof ContainerScreen screen)) return false;
        int slot = loadoutSlotIndex(loadout);
        return slot < screen.getMenu().slots.size() && !screen.getMenu().getSlot(slot).getItem().isEmpty();
    }

    private static boolean clickLoadoutSlot(Minecraft client, int loadout) {
        if (!(client.screen instanceof ContainerScreen screen)
            || client.gameMode == null || client.player == null) return false;
        int slot = loadoutSlotIndex(loadout);
        if (slot < 0 || slot >= screen.getMenu().slots.size()) return false;
        client.gameMode.handleContainerInput(
            screen.getMenu().containerId, slot, 0, ContainerInput.PICKUP, client.player);
        return true;
    }

    /** Container slots only — the trailing 36 are the player's own inventory. */
    private static int containerSlotCount(ContainerScreen screen) {
        return Math.max(0, screen.getMenu().slots.size() - 36);
    }

    /** Clicks the first completed commission in the open menu, then lets the delay pace rescans. */
    private static boolean clickCompletedCommission(Minecraft client) {
        if (!(client.screen instanceof ContainerScreen screen)
            || client.gameMode == null || client.player == null) return false;
        int limit = containerSlotCount(screen);
        for (int slot = 0; slot < limit; slot++) {
            ItemStack item = screen.getMenu().getSlot(slot).getItem();
            if (!isCompleted(item)) continue;
            client.gameMode.handleContainerInput(
                screen.getMenu().containerId, slot, 0, ContainerInput.PICKUP, client.player);
            return true;
        }
        return false;
    }

    private static boolean isCompleted(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (name.contains("completed") || name.contains("click to claim")) return true;
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component line : lore.lines()) {
            String lower = line.getString().toLowerCase(Locale.ROOT);
            if (lower.contains("completed") || lower.contains("click to claim")) return true;
        }
        return false;
    }

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
        // Repurposed from ticks to a menu timeout in seconds.
        guiWaitDelay = Math.max(5, Math.min(20, delay));
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

    public static boolean isHideGui() {
        return hideGui;
    }

    public static void setHideGui(boolean enabled) {
        hideGui = enabled;
    }
}
