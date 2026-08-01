package forfun.miningqol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Empties the SkyBlock material stash by looping the supercraft trick:
 *   /recipe enchanted [material] -> click the enchanted item -> shift-left-click the
 *   golden pickaxe (supercraft all) -> left-click it -> /viewstash material -> click
 *   the chest (pull stash into the freed inventory space) -> repeat.
 * Stops on toggle, on a step timing out, or when chat reports the stash is empty.
 */
public class EmptyStashManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EmptyStash");

    public enum Material {
        COAL("Coal", "coal", "Enchanted Coal", "coal", 0xFF3A3A3A),
        REDSTONE("Redstone", "redstone dust", "Enchanted Redstone Dust", "redstone", 0xFFCC2222),
        LAPIS("Lapis", "lapis lazuli", "Enchanted Lapis Lazuli", "lapis lazuli", 0xFF2255CC),
        HARDSTONE("Hardstone", "hard stone", "Enchanted Hard Stone", "hard stone", 0xFFB8B8B8);

        public final String displayName;
        public final String recipeArg;
        public final String enchantedName;
        public final String stashName; // the regular item's name in the stash GUI
        public final int swatchColor;

        Material(String displayName, String recipeArg, String enchantedName, String stashName, int swatchColor) {
            this.displayName = displayName;
            this.recipeArg = recipeArg;
            this.enchantedName = enchantedName;
            this.stashName = stashName;
            this.swatchColor = swatchColor;
        }
    }

    private enum State {
        IDLE,
        SEND_RECIPE,
        WAIT_RECIPE_GUI,
        WAIT_CRAFT_GUI,
        CLICK_PICKAXE,      // plain left click after the shift click
        SEND_VIEWSTASH,
        WAIT_STASH_GUI
    }

    private static final int GUI_TIMEOUT_TICKS = 100; // 5s per step before giving up

    private static boolean debug = false;
    private static Material material = Material.COAL;
    private static int actionDelay = 4; // ticks between clicks
    private static State state = State.IDLE;
    private static int cooldown = 0;
    private static int waitTicks = 0;
    private static int lastContainerId = -1;
    private static int pickaxeSlot = -1;
    private static int cycles = 0;

    public static Material getMaterial() { return material; }
    public static void setMaterial(Material m) { material = m == null ? Material.COAL : m; }
    public static void setMaterialByName(String name) {
        try {
            material = Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            material = Material.COAL;
        }
    }
    public static int getActionDelay() { return actionDelay; }
    public static void setActionDelay(int delay) { actionDelay = Math.max(1, Math.min(10, delay)); }

    public static boolean isRunning() { return state != State.IDLE; }
    public static void setDebug(boolean value) { debug = value; }
    public static boolean isDebug() { return debug; }

    public static void toggle() {
        if (isRunning()) stop("\u00A7cEmpty Stash stopped.");
        else start();
    }

    public static void start() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        cycles = 0;
        state = State.SEND_RECIPE;
        cooldown = 0;
        sendMessage("\u00A7aEmpty Stash started \u00A77(" + material.displayName + ") \u00A78— run \u00A7f/emptystash\u00A78 again to stop.");
    }

    public static void stop(String message) {
        if (state == State.IDLE) return;
        state = State.IDLE;
        if (message != null) sendMessage(message + " \u00A77(" + cycles + " cycles)");
    }

    /** Wired from CheatBootstrap's game-message hook. */
    public static void onChatMessage(String text) {
        if (state == State.IDLE) return;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("stash") && (lower.contains("is currently empty") || lower.contains("is now empty")
                || lower.contains("nothing to see here"))) {
            stop("\u00A7aStash is empty!");
        }
    }

    public static void tick() {
        if (state == State.IDLE) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            stop("\u00A7cEmpty Stash stopped (left world).");
            return;
        }

        // The user bailing out with Escape stops the loop immediately.
        if (InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
            stop("\u00A7cEmpty Stash stopped (Escape).");
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        switch (state) {
            case SEND_RECIPE -> {
                lastContainerId = currentContainerId(client);
                client.player.connection.sendCommand("recipe enchanted " + material.recipeArg);
                enterWait(State.WAIT_RECIPE_GUI);
            }
            case WAIT_RECIPE_GUI -> {
                // Some flows open the craft view (with the supercraft pickaxe) directly.
                Slot pickaxe = findGuiSlotByItem(client, Items.GOLDEN_PICKAXE, true);
                if (pickaxe != null && guiHasExactName(client, material.enchantedName)) {
                    debug("recipe GUI already is the craft view, skipping ahead");
                    state = State.WAIT_CRAFT_GUI;
                    return;
                }
                Slot slot = findNewGuiSlotByName(client, material.enchantedName);
                if (slot != null) {
                    debug("clicking '" + material.enchantedName + "' at slot " + slot.index);
                    lastContainerId = currentContainerId(client);
                    click(client, slot.index, 0, ContainerInput.PICKUP);
                    enterWait(State.WAIT_CRAFT_GUI);
                } else {
                    timeoutCheck("recipe GUI");
                }
            }
            case WAIT_CRAFT_GUI -> {
                // Hypixel often refreshes the same window in place instead of reopening,
                // so do NOT require a new container id here — just the pickaxe appearing.
                Slot slot = findGuiSlotByItem(client, Items.GOLDEN_PICKAXE, false);
                if (slot != null && !guiHasExactName(client, material.enchantedName)) {
                    stop("\u00A7cWrong recipe opened (expected " + material.enchantedName + ") — not crafting.");
                    return;
                }
                if (slot != null) {
                    debug("shift-clicking supercraft pickaxe at slot " + slot.index);
                    pickaxeSlot = slot.index;
                    click(client, pickaxeSlot, 0, ContainerInput.QUICK_MOVE); // shift-left: supercraft all
                    state = State.CLICK_PICKAXE;
                    cooldown = actionDelay;
                } else {
                    timeoutCheck("craft GUI");
                }
            }
            case CLICK_PICKAXE -> {
                if (client.screen instanceof AbstractContainerScreen<?>) {
                    click(client, pickaxeSlot, 0, ContainerInput.PICKUP);
                }
                state = State.SEND_VIEWSTASH;
                cooldown = actionDelay;
            }
            case SEND_VIEWSTASH -> {
                debug("sending /viewstash material");
                lastContainerId = currentContainerId(client);
                client.player.connection.sendCommand("viewstash material");
                enterWait(State.WAIT_STASH_GUI);
            }
            case WAIT_STASH_GUI -> {
                Slot slot = findNewGuiSlotByItem(client, Items.CHEST);
                if (slot != null) {
                    // The stash GUI is open — if the regular material is gone, we're done.
                    if (!stashHasRegularMaterial(client)) {
                        stop("\u00A7aStash has no more " + material.displayName + "!");
                        return;
                    }
                    debug("clicking stash chest at slot " + slot.index);
                    click(client, slot.index, 0, ContainerInput.PICKUP);
                    cycles++;
                    state = State.SEND_RECIPE;
                    cooldown = actionDelay * 2;
                } else {
                    timeoutCheck("stash GUI");
                }
            }
            default -> {}
        }
    }

    private static void enterWait(State next) {
        state = next;
        waitTicks = 0;
        cooldown = actionDelay;
    }

    private static void timeoutCheck(String what) {
        waitTicks++;
        if (waitTicks > GUI_TIMEOUT_TICKS) {
            stop("\u00A7cEmpty Stash stopped — timed out waiting for the " + what + ".");
        }
    }

    private static int currentContainerId(Minecraft client) {
        if (client.screen instanceof AbstractContainerScreen<?> screen) {
            return screen.getMenu().containerId;
        }
        return -1;
    }

    /** A container slot (not player inventory) in a GUI newer than the one we acted in.
     *  EXACT name match — "Enchanted Coal" must not match "Enchanted Coal Block". */
    private static Slot findNewGuiSlotByName(Minecraft client, String exactName) {
        AbstractContainerScreen<?> screen = newGui(client);
        if (screen == null) return null;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == client.player.getInventory()) continue;
            if (slot.getItem().isEmpty()) continue;
            String name = slot.getItem().getHoverName().getString().trim();
            if (name.equalsIgnoreCase(exactName)) return slot;
        }
        return null;
    }

    /** True if the open GUI (any id) has a non-player slot named exactly this. */
    private static boolean guiHasExactName(Minecraft client, String exactName) {
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return false;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == client.player.getInventory()) continue;
            if (slot.getItem().isEmpty()) continue;
            if (slot.getItem().getHoverName().getString().trim().equalsIgnoreCase(exactName)) return true;
        }
        return false;
    }

    private static Slot findNewGuiSlotByItem(Minecraft client, net.minecraft.world.item.Item item) {
        return findGuiSlotByItem(client, item, true);
    }

    private static Slot findGuiSlotByItem(Minecraft client, net.minecraft.world.item.Item item, boolean requireNewGui) {
        AbstractContainerScreen<?> screen;
        if (requireNewGui) {
            screen = newGui(client);
        } else {
            screen = client.screen instanceof AbstractContainerScreen<?> s2 ? s2 : null;
        }
        if (screen == null) return null;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == client.player.getInventory()) continue;
            if (slot.getItem().getItem() == item) return slot;
        }
        return null;
    }

    private static AbstractContainerScreen<?> newGui(Minecraft client) {
        if (client.screen instanceof AbstractContainerScreen<?> screen
                && screen.getMenu().containerId != lastContainerId) {
            return screen;
        }
        return null;
    }

    private static void click(Minecraft client, int slotIndex, int button, ContainerInput type) {
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return;
        client.gameMode.handleContainerInput(screen.getMenu().containerId, slotIndex, button, type, client.player);
    }

    /** True if the open GUI still shows the regular (non-enchanted) material. */
    private static boolean stashHasRegularMaterial(Minecraft client) {
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return false;
        String needle = material.stashName.toLowerCase(Locale.ROOT);
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == client.player.getInventory()) continue;
            if (slot.getItem().isEmpty()) continue;
            String name = slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(needle) && !name.contains("enchanted")) return true;
        }
        return false;
    }

    private static void debug(String msg) {
        if (debug) sendMessage("\u00A78[debug] \u00A77" + msg);
        LOGGER.info("[EmptyStash] {}", msg);
    }

    private static void sendMessage(String msg) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            MqoChat.log(Component.literal("\u00A76[MQO] " + msg));
        }
    }
}
