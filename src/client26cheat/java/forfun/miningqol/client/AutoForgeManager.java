package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto Forge: when "The Forge" GUI is opened, draws a craft picker beside it
 * (via CheatHooks.containerGuiOverlay). Picking a craft clicks through the
 * menu chain on the real containers:
 *
 *   book -> category (Refining / Forging / nether star for keys) -> recipe item
 *        -> confirmation slot (row 4, col 5, 1-based)
 *
 * Item names are matched case-insensitively with color codes stripped.
 * /autoforge debug dumps the open container's slots to chat for tuning.
 */
public class AutoForgeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoForgeManager");

    private static final String FORGE_TITLE = "the forge";

    // ===== Craft definitions =====

    /** The "confirmation slot": (row 4, col 5) 1-based -> 0-based index. */
    private static final int CONFIRM_SLOT = 3 * 9 + 4;

    private static final int STEP_BOOK = 0;    // first book item in the container
    private static final int STEP_STAR = 1;    // first nether star in the container
    private static final int STEP_NAME = 2;    // first item whose name contains the needle
    private static final int STEP_CONFIRM = 3; // fixed confirmation slot (once populated)
    private static final int STEP_RECORDED = 4; // an item the player clicked while recording

    private static class Step {
        final int type;
        final String needle;
        /** Recorded steps only: the menu title the click was made in, and the slot it was in. */
        final String title;
        final int slot;
        Step(int type, String needle) { this(type, needle, null, -1); }
        Step(int type, String needle, String title, int slot) {
            this.type = type;
            this.needle = needle;
            this.title = title;
            this.slot = slot;
        }
        String describe() {
            return switch (type) {
                case STEP_BOOK -> "the book";
                case STEP_STAR -> "the nether star";
                case STEP_NAME, STEP_RECORDED -> "\"" + needle + "\"";
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
        new Craft("Perfect Plate", book(), name("forging"), name("perfect plate"), confirm()),
        new Craft("Umber Key", book(), star(), name("umber key"), confirm()),
        new Craft("Tungsten Key", book(), star(), name("tungsten key"), confirm()),
        new Craft("Skeleton Key", book(), star(), name("skeleton key"), confirm()),
    };

    // ===== Recorded crafts =====

    /** One click the player made while recording: which menu, what item, which slot. */
    public record RecordedStep(String title, String item, int slot) {}

    /**
     * A craft learnt by watching the player click through it once. Replayed by finding each
     * step's item by name in the open menu (the slot only breaks ties), so it survives Hypixel
     * shuffling the layout as long as the names hold.
     */
    public static final class RecordedCraft {
        public String label;
        public final List<RecordedStep> steps;
        /** Whether it gets a button on the picker; hidden ones are kept, just not shown. */
        public boolean shown = true;

        public RecordedCraft(String label, List<RecordedStep> steps) {
            this.label = label;
            this.steps = new ArrayList<>(steps);
        }

        public String summary() {
            List<String> names = new ArrayList<>();
            for (RecordedStep step : steps) names.add(step.item());
            return String.join(" → ", names);
        }
    }

    private static final List<RecordedCraft> recordedCrafts = new ArrayList<>();
    private static boolean recording = false;
    /** Set from the settings GUI: start recording the next time The Forge opens. */
    private static boolean recordArmed = false;
    private static final List<RecordedStep> recordingSteps = new ArrayList<>();

    /** Built-ins, then recorded; rebuilt whenever the recorded list changes. */
    private static List<Craft> crafts = new ArrayList<>(Arrays.asList(CRAFTS));
    /** Index in {@link #crafts} where the recorded ones start — they get their own colour. */
    private static int recordedStart = CRAFTS.length;

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

    // Picker layout: a vanilla-styled side panel to the LEFT of the (standard 176px wide) chest GUI.
    private static final int PANEL_W = 150;
    private static final int PANEL_GAP = 6; // gap between the panel and the chest GUI edge
    private static final int HEADER_H = 22;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 4;
    private static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HOVER_SPRITE = Identifier.withDefaultNamespace("widget/button_highlighted");
    /**
     * The chest window texture, nine-sliced, so the panel is whatever the resource pack makes
     * the forge's own window look like. The plain body patch comes from the title strip
     * (rows 8-16), which carries no slot squares.
     */
    private static final Identifier CHEST_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int CHEST_TEX_SIZE = 256;
    private static final int CHEST_W = 176;
    private static final int CHEST_H = 222;
    private static final int SLICE = 8;
    private static final int TITLE_COLOR = 0x404040;

    private AutoForgeManager() {}

    private static Craft toCraft(RecordedCraft recorded) {
        Step[] steps = new Step[recorded.steps.size()];
        for (int i = 0; i < steps.length; i++) {
            RecordedStep step = recorded.steps.get(i);
            steps[i] = new Step(STEP_RECORDED, clean(step.item()), clean(step.title()), step.slot());
        }
        String label = recorded.label == null || recorded.label.isBlank()
            ? (recorded.steps.isEmpty() ? "Recorded" : recorded.steps.get(recorded.steps.size() - 1).item())
            : recorded.label;
        return new Craft(label, steps);
    }

    /** Built-in crafts switched off in the settings, by label. */
    private static final java.util.Set<String> hiddenBuiltins = new java.util.HashSet<>();

    private static void rebuildCrafts() {
        List<Craft> list = new ArrayList<>();
        for (Craft builtin : CRAFTS) {
            if (!hiddenBuiltins.contains(builtin.label)) list.add(builtin);
        }
        recordedStart = list.size();
        for (RecordedCraft recorded : recordedCrafts) {
            if (recorded.shown && !recorded.steps.isEmpty()) list.add(toCraft(recorded));
        }
        crafts = list;
    }

    /** Labels of every built-in craft, for the settings' show/hide toggles. */
    public static List<String> builtinLabels() {
        List<String> out = new ArrayList<>();
        for (Craft builtin : CRAFTS) out.add(builtin.label);
        return out;
    }

    public static boolean isBuiltinShown(String label) { return !hiddenBuiltins.contains(label); }

    public static void setBuiltinShown(String label, boolean shown) {
        if (shown) hiddenBuiltins.remove(label); else hiddenBuiltins.add(label);
        rebuildCrafts();
    }

    public static List<String> exportHiddenBuiltins() { return new ArrayList<>(hiddenBuiltins); }

    public static void importHiddenBuiltins(List<String> stored) {
        hiddenBuiltins.clear();
        if (stored != null) {
            for (String label : stored) if (label != null) hiddenBuiltins.add(label);
        }
        rebuildCrafts();
    }

    public static List<RecordedCraft> getRecordedCrafts() { return recordedCrafts; }

    public static void removeRecordedCraft(RecordedCraft recorded) {
        recordedCrafts.remove(recorded);
        rebuildCrafts();
    }

    /** Call after renaming a recorded craft so the picker picks the change up. */
    public static void refreshRecordedCrafts() { rebuildCrafts(); }

    public static boolean isRecording() { return recording; }
    public static boolean isRecordArmed() { return recordArmed; }
    public static int recordingStepCount() { return recordingSteps.size(); }

    /** Settings GUI: the next time The Forge opens, record the craft the player clicks through. */
    public static void armRecording() {
        recordArmed = !recordArmed;
        Minecraft client = Minecraft.getInstance();
        if (recordArmed) {
            msg(client, "§aRecording armed §7— open The Forge and click through the craft as normal.");
        } else {
            msg(client, "§7Recording disarmed.");
        }
    }

    private static void startRecording(Minecraft client) {
        recording = true;
        recordArmed = false;
        recordingSteps.clear();
        dismissed = true; // the picker gets out of the way while the player clicks through
        msg(client, "§a● Recording §7— click through the craft as normal. It saves itself when The Forge reopens.");
        LOGGER.info("[AutoForge] Recording started");
    }

    private static void finishRecording(Minecraft client) {
        recording = false;
        if (recordingSteps.isEmpty()) {
            msg(client, "§7Recording cancelled — nothing was clicked.");
            return;
        }
        // Name it after the thing crafted: the last click is usually the confirm button, so the
        // item picked just before it is the recipe. Renamable in the settings either way.
        RecordedStep last = recordingSteps.get(recordingSteps.size() - 1);
        RecordedStep named = recordingSteps.size() >= 2 && clean(last.item()).contains("confirm")
            ? recordingSteps.get(recordingSteps.size() - 2) : last;
        RecordedCraft craft = new RecordedCraft(named.item(), recordingSteps);
        recordedCrafts.add(craft);
        rebuildCrafts();
        recordingSteps.clear();
        msg(client, "§aSaved §6" + craft.label + " §7(" + craft.steps.size() + " clicks). Rename it in the settings if you like.");
        LOGGER.info("[AutoForge] Recorded {}: {}", craft.label, craft.summary());
        MiningqolClient.saveConfig();
    }

    /**
     * Every container click the client sends (CheatHooks.onContainerClick). While recording,
     * a left/right click on a menu item becomes a step; the mod's own clicks and hotbar swaps
     * are ignored.
     */
    public static void onContainerClick(int containerId, int slot, int button, ContainerInput input) {
        if (!recording || running || input != ContainerInput.PICKUP) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !(client.screen instanceof ContainerScreen screen)) return;
        AbstractContainerMenu menu = screen.getMenu();
        if (menu.containerId != containerId || slot < 0 || slot >= containerSlotCount(menu)) return;
        ItemStack stack = menu.slots.get(slot).getItem();
        if (stack.isEmpty()) return;
        String item = stack.getHoverName().getString().replaceAll("§.", "").trim();
        String title = screen.getTitle().getString().replaceAll("§.", "").trim();
        recordingSteps.add(new RecordedStep(title, item, slot));
        msg(client, "§7" + recordingSteps.size() + ". §f" + item);
    }

    public static List<String> exportRecordedCrafts() {
        List<String> out = new ArrayList<>();
        for (RecordedCraft recorded : recordedCrafts) {
            StringBuilder sb = new StringBuilder(sanitize(recorded.label)).append('|');
            for (int i = 0; i < recorded.steps.size(); i++) {
                RecordedStep step = recorded.steps.get(i);
                if (i > 0) sb.append(';');
                sb.append(sanitize(step.title())).append('>').append(sanitize(step.item())).append('>').append(step.slot());
            }
            sb.append('|').append(recorded.shown ? 1 : 0);
            out.add(sb.toString());
        }
        return out;
    }

    public static void importRecordedCrafts(List<String> stored) {
        recordedCrafts.clear();
        if (stored != null) {
            for (String raw : stored) {
                if (raw == null) continue;
                String[] fields = raw.split("\\|", -1);
                if (fields.length < 2) continue;
                List<RecordedStep> steps = new ArrayList<>();
                for (String part : fields[1].split(";")) {
                    String[] f = part.split(">", -1);
                    if (f.length != 3) continue;
                    int slot;
                    try { slot = Integer.parseInt(f[2]); } catch (NumberFormatException e) { continue; }
                    steps.add(new RecordedStep(f[0], f[1], slot));
                }
                if (steps.isEmpty()) continue;
                RecordedCraft recorded = new RecordedCraft(fields[0], steps);
                if (fields.length >= 3) recorded.shown = !"0".equals(fields[2]);
                recordedCrafts.add(recorded);
            }
        }
        rebuildCrafts();
    }

    /** The config line uses | ; > as separators; none belong in an item name anyway. */
    private static String sanitize(String value) {
        return nullToEmpty(value).replace('|', ' ').replace(';', ' ').replace('>', ' ');
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }

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
        boolean forge = screen instanceof ContainerScreen cs
            && clean(cs.getTitle().getString()).contains(FORGE_TITLE);
        if (recording) {
            // The confirm click reopens The Forge — that is the end of the craft. Closing the
            // GUI ends it too, with whatever was clicked so far.
            if (screen == null || (forge && recordingSteps.size() >= 2)) finishRecording(client);
        }
        menuScreen = null;
        if (!enabled) return;
        if (forge) {
            menuScreen = (ContainerScreen) screen;
            dismissed = false;
            if (recordArmed && !recording) startRecording(client);
        }
    }

    private static boolean isMenuVisible(Screen screen) {
        return enabled && !running && !recording && menuScreen != null && screen == menuScreen && !dismissed;
    }

    // ===== Picker overlay (rendered via CheatHooks.containerGuiOverlay) =====

    /** Mid-craft status card, drawn INSTEAD of the container visuals (returns true to skip them). */
    public static boolean renderReplacing(Screen screen, GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        if (!running || !(screen instanceof ContainerScreen)) return false;
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        int bw = 200, bh = 50;
        int bx = (w - bw) / 2, by = (h - bh) / 2;
        vanillaPanel(ctx, bx, by, bw, bh);
        ctx.text(font, "Auto Forge", bx + 8, by + 8, TITLE_COLOR, false);
        String progress = runCount > 1 ? " (" + (runsDone + 1) + "/" + runCount + ")" : "";
        ctx.text(font, "Starting: " + craft.label + progress, bx + 8, by + 22, TITLE_COLOR, false);
        ctx.text(font, "Esc to cancel", bx + 8, by + 34, 0x808080, false);
        return true;
    }

    /** Craft picker side panel, drawn ON TOP of the vanilla forge GUI (no dim, GUI stays usable). */
    public static void renderOnTop(Screen screen, GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();

        if (recording && screen instanceof ContainerScreen) {
            // Small banner top-centre so the menu underneath stays fully usable.
            int bw = 230, bh = 32;
            int bx = (w - bw) / 2, by = 6;
            vanillaPanel(ctx, bx, by, bw, bh);
            ctx.text(font, "§c●§r Recording " + recordingSteps.size() + " click"
                + (recordingSteps.size() == 1 ? "" : "s"), bx + 8, by + 7, TITLE_COLOR, false);
            ctx.text(font, "Saves when The Forge reopens, or on close", bx + 8, by + 19, 0x808080, false);
            return;
        }
        if (!isMenuVisible(screen)) return;

        int px = panelX(w);
        int py = panelY(h);
        int panelH = panelHeight();
        vanillaPanel(ctx, px, py, PANEL_W, panelH);
        ctx.text(font, "Auto Forge", px + 8, py + 7, TITLE_COLOR, false);

        int n = crafts.size();
        for (int i = 0; i < buttonCount(); i++) {
            int[] r = buttonRect(w, h, i);
            boolean hover = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
            String label;
            if (i < n) {
                label = trim(font, crafts.get(i).label, r[2] - 8);
            } else if (i == n) {
                label = "§c●§r Record a craft";
            } else if (i == n + 1) {
                label = "Delay: " + tickDelay + "t §7(L+ R-)";
            } else if (i == n + 2) {
                label = "Amount: " + runCount + "x §7(L+ R-)";
            } else {
                label = "Hide";
            }
            vanillaButton(ctx, font, r[0], r[1], r[2], r[3], label, hover);
        }
    }

    /** The container window, built from the pack's chest texture: corners, stretched edges, plain body. */
    private static void vanillaPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        int s = SLICE;
        int innerW = w - 2 * s;
        int innerH = h - 2 * s;
        int right = CHEST_W - s;   // u of the right-hand corner column
        int bottom = CHEST_H - s;  // v of the bottom corner row
        // body: the plain title strip, stretched
        patch(ctx, x + s, y + s, innerW, innerH, s, s, CHEST_W - 2 * s, s);
        // edges
        patch(ctx, x + s, y, innerW, s, s, 0, CHEST_W - 2 * s, s);               // top
        patch(ctx, x + s, y + h - s, innerW, s, s, bottom, CHEST_W - 2 * s, s);  // bottom
        patch(ctx, x, y + s, s, innerH, 0, s, s, s);                             // left
        patch(ctx, x + w - s, y + s, s, innerH, right, s, s, s);                 // right
        // corners
        patch(ctx, x, y, s, s, 0, 0, s, s);
        patch(ctx, x + w - s, y, s, s, right, 0, s, s);
        patch(ctx, x, y + h - s, s, s, 0, bottom, s, s);
        patch(ctx, x + w - s, y + h - s, s, s, right, bottom, s, s);
    }

    /** Draws texture region (u, v, uw, vh) stretched into (x, y, w, h). */
    private static void patch(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int u, int v, int uw, int vh) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, CHEST_TEXTURE, x, y, u, v, w, h, uw, vh, CHEST_TEX_SIZE, CHEST_TEX_SIZE);
    }

    /** A real vanilla button: the widget sprite, white shadowed label, highlighted when hovered. */
    private static void vanillaButton(GuiGraphicsExtractor ctx, Font font, int x, int y, int w, int h,
                                      String label, boolean hover) {
        ctx.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? BUTTON_HOVER_SPRITE : BUTTON_SPRITE, x, y, w, h);
        ctx.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, hover ? 0xFFFFFFA0 : 0xFFFFFFFF, true);
    }

    /** Clips a label to the button, so a long custom name cannot spill outside it. */
    private static String trim(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String cut = text;
        while (cut.length() > 1 && font.width(cut + "…") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    /** Craft buttons + record + delay changer + amount changer + hide. */
    private static int buttonCount() {
        return crafts.size() + 4;
    }

    private static int panelHeight() {
        return HEADER_H + buttonCount() * (BTN_H + BTN_GAP) + 4;
    }

    /** Panel sits just left of a standard 176px-wide chest GUI, clamped on-screen. */
    private static int panelX(int screenW) {
        return Math.max(4, screenW / 2 - 88 - PANEL_GAP - PANEL_W);
    }

    private static int panelY(int screenH) {
        return Math.max(4, (screenH - panelHeight()) / 2);
    }

    /** Rect {x, y, w, h} of button i (crafts, then record, delay changer, amount, hide). */
    private static int[] buttonRect(int screenW, int screenH, int i) {
        int px = panelX(screenW);
        int py = panelY(screenH);
        return new int[]{px + 8, py + HEADER_H + i * (BTN_H + BTN_GAP), PANEL_W - 16, BTN_H};
    }

    /** Returns true when the click was consumed (caller must block it from the vanilla screen). */
    public static boolean handleMouseClick(Screen screen, double mx, double my, int button) {
        if (!(screen instanceof ContainerScreen)) return false;
        if (running) return true; // swallow everything mid-craft
        if (recording) return false; // the player's clicks are the recording
        if (!isMenuVisible(screen)) return false;

        Minecraft client = Minecraft.getInstance();
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        int n = crafts.size();
        for (int i = 0; i < buttonCount(); i++) {
            int[] r = buttonRect(w, h, i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                if (i < n) {
                    startCraft(client, i);
                } else if (i == n) {
                    startRecording(client);
                } else if (i == n + 1) {
                    setTickDelay(tickDelay + (button == 1 ? -1 : 1)); // L+ R-
                } else if (i == n + 2) {
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
        craft = crafts.get(index);
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
        if (step.type == STEP_RECORDED) {
            // Only when this is the menu the click was recorded in — the same item name can
            // sit in the previous menu too (the recipe in the list and on its confirm page).
            Minecraft client = Minecraft.getInstance();
            String title = client.screen == null ? "" : clean(client.screen.getTitle().getString());
            return title.equals(step.title) && findStep(menu, step) != -1;
        }
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
        if (step.type == STEP_RECORDED) {
            // Exact name first (the recorded slot breaks ties), then a looser contains match:
            // Hypixel decorates names with counts and colours that come and go.
            int exact = -1;
            int contains = -1;
            for (int i = 0; i < count; i++) {
                ItemStack stack = menu.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                String name = clean(stack.getHoverName().getString());
                if (name.equals(step.needle)) {
                    if (i == step.slot) return i;
                    if (exact == -1) exact = i;
                } else if (contains == -1 && !step.needle.isEmpty() && name.contains(step.needle)) {
                    contains = i;
                }
            }
            return exact != -1 ? exact : contains;
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
            MqoChat.log(Component.literal("§6[AutoForge] " + s));
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
            + " §7(id " + menu.containerId + ", " + count + " container slots)");
        for (int i = 0; i < count; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            var loreComp = stack.get(DataComponents.LORE);
            if (fullLore) {
                MqoChat.log(Component.literal(
                    "§7#" + i + " §f" + stack.getHoverName().getString()
                        + (lacksMaterials(stack) ? " §c[MISSING MATERIALS]" : "")));
                if (loreComp != null) {
                    for (var line : loreComp.lines()) {
                        MqoChat.log(Component.literal("§8    " + line.getString()));
                    }
                }
            } else {
                String lore = "";
                if (loreComp != null && !loreComp.lines().isEmpty()) {
                    lore = " §8| " + loreComp.lines().get(0).getString();
                }
                MqoChat.log(Component.literal(
                    "§7#" + i + " §f" + stack.getHoverName().getString() + lore));
            }
        }
    }
}
