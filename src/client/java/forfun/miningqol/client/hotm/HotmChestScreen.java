package forfun.miningqol.client.hotm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;

/**
 * A fake chest GUI that displays the HOTM tree.
 * Uses a 6-row chest (54 slots). Shows 5 content rows + 1 control row.
 * Page 1 shows tree rows 0-4, Page 2 shows rows 5-9.
 */
public class HotmChestScreen {

    private static int currentPage = 0; // 0 = rows 0-4, 1 = rows 5-9

    public static void open() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        currentPage = 1;
        openPage(client);
    }

    private static void openPage(MinecraftClient client) {
        SimpleInventory inventory = new SimpleInventory(54);
        fillInventory(inventory);

        GenericContainerScreenHandler handler = new GenericContainerScreenHandler(
            ScreenHandlerType.GENERIC_9X6,
            0, // syncId - 0 for client-only
            client.player.getInventory(),
            inventory,
            6
        );

        String title = "HOTM Config (Page " + (currentPage + 1) + "/2) - Tokens: "
            + HotmManager.getTree().getUsedTokens() + "/" + HotmManager.getTree().getTotalTokens();

        client.send(() -> client.setScreen(
            new GenericContainerScreen(handler, client.player.getInventory(), Text.literal(title))
        ));
    }

    private static void fillInventory(SimpleInventory inventory) {
        HotmTree tree = HotmManager.getTree();
        int startRow = currentPage * 5;

        // Fill content rows 0-4 (tree rows startRow to startRow+4)
        for (int visRow = 0; visRow < 5; visRow++) {
            int treeRow = startRow + visRow;
            for (int col = 0; col < 9; col++) {
                int slot = visRow * 9 + col;
                HotmNode node = tree.getNodeAt(treeRow, col);
                if (node != null) {
                    HotmNode.State state = tree.getState(node);
                    ItemStack stack = new ItemStack(node.getItemForState(state));
                    stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                        Text.literal(getItemName(node, state)));
                    inventory.setStack(slot, stack);
                } else {
                    // Empty slot - gray stained glass pane
                    ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                    pane.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
                    inventory.setStack(slot, pane);
                }
            }
        }

        // Control row (row 5, slots 45-53)
        // Close button (slot 45)
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
            Text.literal("\u00A7cClose"));
        inventory.setStack(45, close);

        // Page switch (slot 8 - top right)
        ItemStack arrow = new ItemStack(Items.ARROW);
        String pageText = currentPage == 0 ? "\u00A7ePage 2 \u00A77(Rows 6-10)" : "\u00A7ePage 1 \u00A77(Rows 1-5)";
        arrow.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
            Text.literal(pageText));
        inventory.setStack(8, arrow);

        // Presets button (slot 46)
        ItemStack presets = new ItemStack(Items.BOOK);
        presets.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
            Text.literal("\u00A7dPresets"));
        inventory.setStack(46, presets);

        // Token info (slot 47)
        ItemStack info = new ItemStack(Items.NETHER_STAR);
        info.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
            Text.literal("\u00A7eTokens: " + tree.getUsedTokens() + "/" + tree.getTotalTokens()));
        inventory.setStack(47, info);

        // Delay control (slot 48) - left click +1, right click -1
        ItemStack delay = new ItemStack(Items.CLOCK);
        delay.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
            Text.literal("\u00A7eDelay: \u00A7f" + AutoHotmManager.getTickDelay() + " ticks\n\u00A77Left +1 | Right -1"));
        inventory.setStack(48, delay);

        // Save button (slot 49) - opens name input
        ItemStack save = new ItemStack(Items.EMERALD);
        save.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
            Text.literal("\u00A7aSave Preset"));
        inventory.setStack(49, save);

        // Fill remaining control row with glass panes
        for (int i = 45; i < 54; i++) {
            if (inventory.getStack(i).isEmpty()) {
                ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
                pane.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
                inventory.setStack(i, pane);
            }
        }
    }

    private static String getItemName(HotmNode node, HotmNode.State state) {
        String stateStr = switch (state) {
            case NOT_CLICKED -> "\u00A78[OFF]";
            case DISABLED -> "\u00A7c[DISABLED]";
            case LEVEL_1 -> "\u00A7a[LEVEL 1]";
            case MAXED -> "\u00A7b[MAXED]";
            case CHOSEN -> "\u00A7d[CHOSEN]";
        };
        String typeStr = node.getType() == HotmNode.Type.ABILITY ? "\u00A75[Ability] " : "";
        return typeStr + "\u00A7f" + node.getDisplayName() + " " + stateStr;
    }

    /**
     * Called when a slot is clicked in the HOTM chest screen.
     * Returns true if the click was handled.
     */
    public static boolean handleClick(int slotIndex, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;

        // Control row clicks
        if (slotIndex == 45) {
            // Close
            client.send(() -> client.currentScreen.close());
            return true;
        }
        if (slotIndex == 8) {
            // Switch page
            currentPage = currentPage == 0 ? 1 : 0;
            openPage(client);
            return true;
        }
        if (slotIndex == 46) {
            // Presets
            HotmPresetScreen.open();
            return true;
        }
        if (slotIndex == 48) {
            // Delay control: left click +1, right click -1
            int current = AutoHotmManager.getTickDelay();
            if (button == 0) {
                AutoHotmManager.setTickDelay(current + 1);
            } else if (button == 1) {
                AutoHotmManager.setTickDelay(current - 1);
            }
            openPage(client);
            return true;
        }
        if (slotIndex == 49) {
            // Save - open name input screen
            client.send(() -> client.setScreen(new forfun.miningqol.client.gui.HotmSaveScreen()));
            return true;
        }

        // Content area clicks (slots 0-44)
        if (slotIndex < 0 || slotIndex >= 45) return true; // Ignore other control slots

        int visRow = slotIndex / 9;
        int col = slotIndex % 9;
        int treeRow = currentPage * 5 + visRow;

        HotmTree tree = HotmManager.getTree();
        HotmNode node = tree.getNodeAt(treeRow, col);
        if (node == null) return true; // Clicked empty/glass pane

        // Cycle state
        HotmNode.State current = tree.getState(node);
        HotmNode.State next = getNextState(node, current);
        tree.setState(node, next);

        // Refresh the screen
        openPage(client);
        return true;
    }

    private static HotmNode.State getNextState(HotmNode node, HotmNode.State current) {
        if (node.getType() == HotmNode.Type.PERK) {
            return switch (current) {
                case NOT_CLICKED -> HotmNode.State.LEVEL_1;
                case LEVEL_1 -> HotmNode.State.MAXED;
                case MAXED -> HotmNode.State.DISABLED;
                case DISABLED -> HotmNode.State.NOT_CLICKED;
                default -> HotmNode.State.NOT_CLICKED;
            };
        } else {
            return switch (current) {
                case NOT_CLICKED -> HotmNode.State.CHOSEN;
                case CHOSEN -> HotmNode.State.DISABLED;
                case DISABLED -> HotmNode.State.NOT_CLICKED;
                default -> HotmNode.State.NOT_CLICKED;
            };
        }
    }

    public static boolean isHotmScreen(String title) {
        return title != null && title.startsWith("HOTM Config");
    }

    public static int getCurrentPage() {
        return currentPage;
    }
}
