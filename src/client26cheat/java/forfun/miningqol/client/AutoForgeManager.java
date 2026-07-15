package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto Forge: when "The Forge" GUI is opened near the forge room, draws a craft
 * picker beside it (via CheatHooks.containerGuiOverlay). Picking a craft clicks
 * through the menu chain on the real containers:
 *
 *   book -> category (Refining / Forging / nether star for keys) -> recipe item
 *        -> confirmation slot (row 4, col 5, 1-based)
 *
 * Item names are matched case-insensitively with color codes stripped.
 * /autoforge debug dumps the open container's slots to chat for tuning.
 */
public class AutoForgeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoForgeManager");

    // The Forge room block (-23, 151, -48); picker only triggers within this radius.
    private static final Vec3 FORGE_POS = new Vec3(-22.5, 151.5, -47.5);
    private static final double FORGE_RADIUS = 7.0;
    private static final String FORGE_TITLE = "the forge";

    // ===== Craft definitions =====

    /** The "confirmation slot": (row 4, col 5) 1-based -> 0-based index. */
    private static final int CONFIRM_SLOT = 3 * 9 + 4;

    private static final int STEP_BOOK = 0;    // first book item in the container
    private static final int STEP_STAR = 1;    // first nether star in the container
    private static final int STEP_NAME = 2;    // first item whose name contains the needle
    private static final int STEP_CONFIRM = 3; // fixed confirmation slot (once populated)

    private static class Step {
        final int type;
        final String needle;
        Step(int type, String needle) { this.type = type; this.needle = needle; }
        String describe() {
            return switch (type) {
                case STEP_BOOK -> "the book";
                case STEP_STAR -> "the nether star";
                case STEP_NAME -> "\"" + needle + "\"";
                default -> "the confirmation slot";
            };
        }
    }

    private static class Craft {
        final String label;
        final Step[] steps;
        Craft(String label, Step... steps) { this.label = label; this.steps = steps; }
    }

    private static Step book() { return new Step(STEP_BOOK, null); }
    private static Step star() { return new Step(STEP_STAR, null); }
    private static Step name(String needle) { return new Step(STEP_NAME, needle); }
    private static Step confirm() { return new Step(STEP_CONFIRM, null); }

    /**
     * Signs that the player can't afford the craft — checked on the lore of every
     * item the machine is about to click, and on chat messages while running.
     * Without this the chain keeps clicking into menus the server rejects.
     */
    private static final String[] MISSING_MARKERS = {
        "you don't have the required items", // confirm-button lore when short on materials
        "not enough", "requirements not met", "you don't have", "you do not have",
        "missing", "can't afford", "cannot afford",
    };

    private static final Craft[] CRAFTS = {
        new Craft("Refined Umber", book(), name("refining"), name("refined umber"), confirm()),
        new Craft("Umber Plate", book(), name("forging"), name("umber plate"), confirm()),
        new Craft("Refined Tungsten", book(), name("refining"), name("refined tungsten"), confirm()),
        new Craft("Tungsten Plate", book(), name("forging"), name("tungsten plate"), confirm()),
        new Craft("Umber Key", book(), star(), name("umber key"), confirm()),
        new Craft("Tungsten Key", book(), star(), name("tungsten key"), confirm()),
        new Craft("Skeleton Key", book(), star(), name("skeleton key"), confirm()),
    };

    private static boolean enabled = true;
    private static int tickDelay = 3;
    private static int runCount = 1; // how many times to run the whole chain per click

    // Picker overlay state
    private static Screen lastScreen = null;
    private static ContainerScreen menuScreen = null;
    private static boolean dismissed = false;

    // Craft state machine
    private static final int STATE_STEP = 1;
    private static final int STATE_WAIT_GUI = 2;
    private static boolean running = false;
    private static Craft craft = null;
    private static int stepIndex = 0;
    private static int runsDone = 0;
    private static int state = 0;
    private static int tickCounter = 0;
    private static int noGuiTicks = 0;
    private static int waitContainerId = -1;

    // Picker layout: a side panel to the right of the (standard 176px wide) chest GUI.
    private static final int PANEL_W = 150;
    private static final int PANEL_GAP = 10; // gap between the chest GUI edge and the panel
    private static final int HEADER_H = 24;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 5;

    private AutoForgeManager() {}

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static int getTickDelay() { return tickDelay; }
    public static void setTickDelay(int delay) { tickDelay = Math.max(1, Math.min(10, delay)); }
    public static int getRunCount() { return runCount; }
    public static void setRunCount(int count) { runCount = Math.max(1, Math.min(7, count)); }
    public static boolean isRunning() { return running; }

    public static void tick(Minecraft client) {
        Screen screen = client.screen;
        if (screen != lastScreen) {
            lastScreen = screen;
            onScreenChange(client, screen);
        }
        if (running) runCraft(client);
    }

    private static void onScreenChange(Minecraft client, Screen screen) {
        if (running) return; // craft machine owns the GUI chain while running
        menuScreen = null;
        if (!enabled) return;
        if (screen instanceof ContainerScreen cs
                && clean(cs.getTitle().getString()).contains(FORGE_TITLE)
                && isNearForge(client)) {
            menuScreen = cs;
            dismissed = false;
        }
    }

    private static boolean isNearForge(Minecraft client) {
        return client.player != null && client.player.position().distanceTo(FORGE_POS) <= FORGE_RADIUS;
    }

    private static boolean isMenuVisible(Screen screen) {
        return enabled && !running && menuScreen != null && screen == menuScreen && !dismissed;
    }

    // ===== Picker overlay (rendered via CheatHooks.containerGuiOverlay) =====

    /** Mid-craft status card, drawn INSTEAD of the container visuals (returns true to skip them). */
    public static boolean renderReplacing(Screen screen, GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        if (!running || !(screen instanceof ContainerScreen)) return false;
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        int bw = 200, bh = 46;
        int bx = (w - bw) / 2, by = (h - bh) / 2;
        ctx.fill(bx, by, bx + bw, by + bh, 0xF0101018);
        ctx.outline(bx, by, bw, bh, 0xFF404050);
        ctx.centeredText(font, "§6Auto Forge", w / 2, by + 8, 0xFFFFFFFF);
        String progress = runCount > 1 ? " §7(" + (runsDone + 1) + "/" + runCount + ")" : "";
        ctx.centeredText(font, "§fStarting: " + craft.label + progress, w / 2, by + 20, 0xFFFFFFFF);
        ctx.centeredText(font, "§8Esc to cancel", w / 2, by + 32, 0xFFFFFFFF);
        return true;
    }

    /** Craft picker side panel, drawn ON TOP of the vanilla forge GUI (no dim, GUI stays usable). */
    public static void renderOnTop(Screen screen, GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        if (!isMenuVisible(screen)) return;
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        int px = panelX(w);
        int py = panelY(h);
        int panelH = panelHeight();
        ctx.fill(px, py, px + PANEL_W, py + panelH, 0xF0101018);
        ctx.outline(px, py, PANEL_W, panelH, 0xFF404050);
        ctx.centeredText(font, "§6Auto Forge", px + PANEL_W / 2, py + 8, 0xFFFFFFFF);

        for (int i = 0; i < buttonCount(); i++) {
            int[] r = buttonRect(w, h, i);
            boolean hover = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
            ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hover ? 0xE0303040 : 0xC0202028);
            ctx.outline(r[0], r[1], r[2], r[3], hover ? 0xFFFFFFFF : 0xFF505060);
            String label;
            if (i < CRAFTS.length) {
                label = "§e" + CRAFTS[i].label;
            } else if (i == CRAFTS.length) {
                label = "§eDelay: §f" + tickDelay + "t §7(L+ R-)";
            } else if (i == CRAFTS.length + 1) {
                label = "§eAmount: §f" + runCount + "x §7(L+ R-)";
            } else {
                label = "§7Hide";
            }
            ctx.centeredText(font, label, r[0] + r[2] / 2, r[1] + (r[3] - 8) / 2, 0xFFFFFFFF);
        }
    }

    /** Craft buttons + delay changer + amount changer + hide. */
    private static int buttonCount() {
        return CRAFTS.length + 3;
    }

    private static int panelHeight() {
        return HEADER_H + buttonCount() * (BTN_H + BTN_GAP) + 4;
    }

    /** Panel sits just right of a standard 176px-wide chest GUI, clamped on-screen. */
    private static int panelX(int screenW) {
        return Math.min(screenW / 2 + 88 + PANEL_GAP, screenW - PANEL_W - 4);
    }

    private static int panelY(int screenH) {
        return (screenH - panelHeight()) / 2;
    }

    /** Rect {x, y, w, h} of button i (crafts, then delay changer, then hide). */
    private static int[] buttonRect(int screenW, int screenH, int i) {
        int px = panelX(screenW);
        int py = panelY(screenH);
        return new int[]{px + 8, py + HEADER_H + i * (BTN_H + BTN_GAP), PANEL_W - 16, BTN_H};
    }

    /** Returns true when the click was consumed (caller must block it from the vanilla screen). */
    public static boolean handleMouseClick(Screen screen, double mx, double my, int button) {
        if (!(screen instanceof ContainerScreen)) return false;
        if (running) return true; // swallow everything mid-craft
        if (!isMenuVisible(screen)) return false;

        Minecraft client = Minecraft.getInstance();
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        for (int i = 0; i < buttonCount(); i++) {
            int[] r = buttonRect(w, h, i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                if (i < CRAFTS.length) {
                    startCraft(client, i);
                } else if (i == CRAFTS.length) {
                    setTickDelay(tickDelay + (button == 1 ? -1 : 1)); // L+ R-
                } else if (i == CRAFTS.length + 1) {
                    setRunCount(runCount + (button == 1 ? -1 : 1)); // L+ R-
                } else {
                    dismissed = true; // hide the panel for this menu
                }
                return true;
            }
        }
        // Swallow clicks on the panel background; everything else goes to the real GUI.
        int panelH = panelHeight();
        int px = panelX(w);
        int py = panelY(h);
        return mx >= px && mx < px + PANEL_W && my >= py && my < py + panelH;
    }

    /** True while player input (other than Esc) should be swallowed on this screen. */
    public static boolean shouldBlockKeys(Screen screen) {
        return running && screen instanceof ContainerScreen;
    }

    public static void onEscape() {
        if (running) stop("Cancelled");
    }

    // ===== Craft state machine =====

    private static void startCraft(Minecraft client, int index) {
        craft = CRAFTS[index];
        running = true;
        stepIndex = 0;
        runsDone = 0;
        state = STATE_STEP;
        tickCounter = 0;
        noGuiTicks = 0;
        msg(client, "§7Starting §6" + craft.label + (runCount > 1 ? " §7×" + runCount : "") + "§7...");
        LOGGER.info("[AutoForge] Starting {} x{}", craft.label, runCount);
    }

    public static void stop(String reason) {
        if (!running) return;
        running = false;
        state = 0;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            msg(client, "§c" + reason);
        }
    }

    private static void runCraft(Minecraft client) {
        if (client.player == null) {
            running = false;
            return;
        }
        if (!(client.screen instanceof ContainerScreen screen)) {
            if (++noGuiTicks > 40) stop("Menu closed");
            return;
        }
        noGuiTicks = 0;
        tickCounter++;
        AbstractContainerMenu menu = screen.getMenu();

        switch (state) {
            case STATE_STEP: {
                if (tickCounter < Math.max(tickDelay, 2)) break;
                Step step = craft.steps[stepIndex];
                int slot = findStep(menu, step);
                if (slot != -1) {
                    if (lacksMaterials(menu.slots.get(slot).getItem())) {
                        stop("Not enough materials for " + craft.label
                            + (runsDone > 0 ? " (started " + runsDone + ")" : ""));
                        break;
                    }
                    click(client, menu, slot);
                    stepIndex++;
                    if (stepIndex >= craft.steps.length) {
                        onRunComplete(client, menu);
                    } else {
                        waitForNewGui(menu);
                    }
                } else if (tickCounter >= 80) {
                    stop("Couldn't find " + step.describe() + " in this menu — run /autoforge debug here");
                }
                break;
            }

            case STATE_WAIT_GUI:
                if (menu.containerId != waitContainerId) {
                    state = STATE_STEP;
                    tickCounter = 0;
                } else if (tickCounter >= 15 && nextStepFindableInPlace(menu)) {
                    // Server updated the same window instead of opening a new one.
                    state = STATE_STEP;
                    tickCounter = 0;
                } else if (tickCounter >= 60) {
                    stop("Menu didn't change after clicking " + craft.steps[stepIndex - 1].describe());
                }
                break;
        }
    }

    private static void waitForNewGui(AbstractContainerMenu menu) {
        waitContainerId = menu.containerId;
        state = STATE_WAIT_GUI;
        tickCounter = 0;
    }

    /** In-place fallback for name-matched steps only (fixed/typed slots would false-positive). */
    private static boolean nextStepFindableInPlace(AbstractContainerMenu menu) {
        Step step = craft.steps[stepIndex];
        return step.type == STEP_NAME && findStep(menu, step) != -1;
    }

    /** One full chain finished (confirm clicked); loop again or wrap up. */
    private static void onRunComplete(Minecraft client, AbstractContainerMenu menu) {
        runsDone++;
        LOGGER.info("[AutoForge] Started {} ({}/{})", craft.label, runsDone, runCount);
        if (runsDone >= runCount) {
            msg(client, "§aStarted §6" + craft.label + "§a" + (runCount > 1 ? " ×" + runCount : "") + "!");
            running = false;
            state = 0;
            return;
        }
        msg(client, "§aStarted §6" + craft.label + "§a (" + runsDone + "/" + runCount + ")");
        // The confirm click reopens The Forge — wait for it, then start over at the book.
        stepIndex = 0;
        waitForNewGui(menu);
    }

    /** Chat safety net: the server rejecting a craft mid-chain aborts the run. */
    public static void onChatMessage(String message) {
        if (!running) return;
        String s = clean(message);
        for (String marker : MISSING_MARKERS) {
            if (s.contains(marker)) {
                stop("Server rejected the craft (missing materials?)"
                    + (runsDone > 0 ? " — started " + runsDone + "/" + runCount : ""));
                return;
            }
        }
    }

    private static boolean lacksMaterials(ItemStack stack) {
        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (var line : lore.lines()) {
            String s = clean(line.getString());
            for (String marker : MISSING_MARKERS) {
                if (s.contains(marker)) return true;
            }
        }
        return false;
    }

    // ===== Container helpers =====

    private static int containerSlotCount(AbstractContainerMenu menu) {
        return Math.max(0, menu.slots.size() - 36); // exclude player inventory
    }

    private static int findStep(AbstractContainerMenu menu, Step step) {
        int count = containerSlotCount(menu);
        if (step.type == STEP_CONFIRM) {
            // Wait until the confirm menu is tall enough and the slot has loaded.
            if (CONFIRM_SLOT < count && !menu.slots.get(CONFIRM_SLOT).getItem().isEmpty()) {
                return CONFIRM_SLOT;
            }
            return -1;
        }
        for (int i = 0; i < count; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            boolean match = switch (step.type) {
                case STEP_BOOK -> stack.getItem() == Items.BOOK;
                case STEP_STAR -> stack.getItem() == Items.NETHER_STAR;
                case STEP_NAME -> clean(stack.getHoverName().getString()).contains(step.needle);
                default -> false;
            };
            if (match) return i;
        }
        return -1;
    }

    private static void click(Minecraft client, AbstractContainerMenu menu, int slot) {
        if (slot < menu.slots.size()) {
            client.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.PICKUP, client.player);
            LOGGER.info("[AutoForge] Clicked slot {}", slot);
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("§.", "").trim().toLowerCase();
    }

    private static void msg(Minecraft client, String s) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("§6[AutoForge] " + s));
        }
    }

    /**
     * /autoforge debug [full] — dump the open container's slots to chat for tuning
     * the matchers. "full" prints every lore line instead of just the first.
     */
    public static void debugDump(boolean fullLore) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (!(client.screen instanceof ContainerScreen screen)) {
            msg(client, "§cNo container GUI open");
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        int count = containerSlotCount(menu);
        msg(client, "§fTitle: §e" + screen.getTitle().getString()
            + " §7(id " + menu.containerId + ", " + count + " container slots, near forge: " + isNearForge(client) + ")");
        for (int i = 0; i < count; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            var loreComp = stack.get(DataComponents.LORE);
            if (fullLore) {
                client.player.sendSystemMessage(Component.literal(
                    "§7#" + i + " §f" + stack.getHoverName().getString()
                        + (lacksMaterials(stack) ? " §c[MISSING MATERIALS]" : "")));
                if (loreComp != null) {
                    for (var line : loreComp.lines()) {
                        client.player.sendSystemMessage(Component.literal("§8    " + line.getString()));
                    }
                }
            } else {
                String lore = "";
                if (loreComp != null && !loreComp.lines().isEmpty()) {
                    lore = " §8| " + loreComp.lines().get(0).getString();
                }
                client.player.sendSystemMessage(Component.literal(
                    "§7#" + i + " §f" + stack.getHoverName().getString() + lore));
            }
        }
    }
}
