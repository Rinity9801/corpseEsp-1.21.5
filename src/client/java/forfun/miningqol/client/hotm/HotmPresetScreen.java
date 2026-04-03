package forfun.miningqol.client.hotm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Chest GUI that shows saved HOTM presets.
 * Click a preset to load it, then returns to the main HOTM config screen.
 */
public class HotmPresetScreen {

    private static List<String> presetNames;

    public static void open() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        presetNames = HotmTree.listPresets();

        // Use 3-row chest (27 slots) for up to 18 presets + control row
        SimpleInventory inventory = new SimpleInventory(27);
        fillInventory(inventory);

        GenericContainerScreenHandler handler = new GenericContainerScreenHandler(
            ScreenHandlerType.GENERIC_9X3,
            0,
            client.player.getInventory(),
            inventory,
            3
        );

        String title = "HOTM Presets (" + presetNames.size() + " saved)";
        client.send(() -> client.setScreen(
            new GenericContainerScreen(handler, client.player.getInventory(), Text.literal(title))
        ));
    }

    private static void fillInventory(SimpleInventory inventory) {
        // Show presets in slots 0-17 (first 2 rows)
        for (int i = 0; i < Math.min(presetNames.size(), 18); i++) {
            String name = presetNames.get(i);
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            book.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                Text.literal("\u00A7e" + name + "\n\u00A77Left click: Edit | Right click: Apply"));
            inventory.setStack(i, book);
        }

        // Fill empty preset slots with glass
        for (int i = presetNames.size(); i < 18; i++) {
            ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            pane.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            inventory.setStack(i, pane);
        }

        // Control row (slots 18-26)
        // Back button (slot 18)
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
            Text.literal("\u00A7eBack to HOTM Config"));
        inventory.setStack(18, back);

        // Fill remaining control slots
        for (int i = 19; i < 27; i++) {
            ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
            pane.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            inventory.setStack(i, pane);
        }
    }

    public static boolean handleClick(int slotIndex, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;

        // Back button
        if (slotIndex == 18) {
            HotmChestScreen.open();
            return true;
        }

        // Preset slots (0-17)
        if (slotIndex >= 0 && slotIndex < 18 && slotIndex < presetNames.size()) {
            String name = presetNames.get(slotIndex);
            HotmManager.loadPreset(name);

            if (button == 1) {
                // Right click = load and apply
                client.send(() -> client.currentScreen.close());
                client.send(() -> HotmManager.apply());
            } else {
                // Left click = load into editor
                HotmChestScreen.open();
            }
            return true;
        }

        return true; // Consume all clicks
    }

    public static boolean isPresetScreen(String title) {
        return title != null && title.startsWith("HOTM Presets");
    }
}
