package forfun.miningqol.client.hotm;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class HotmManager {
    private static final HotmTree tree = new HotmTree();

    public static HotmTree getTree() { return tree; }

    public static void init() {
        HotmTree.initPresetDir();
        tree.load();
    }

    public static void save() {
        tree.save();
        sendMessage("\u00A7aSaved HOTM config");
    }

    public static void savePreset(String name) {
        tree.savePreset(name);
        sendMessage("\u00A7aSaved preset '\u00A7e" + name + "\u00A7a'");
    }

    public static void loadPreset(String name) {
        if (tree.loadPreset(name)) {
            sendMessage("\u00A7aLoaded preset '\u00A7e" + name + "\u00A7a' (" + tree.getUsedTokens() + " tokens)");
        } else {
            sendMessage("\u00A7cPreset '" + name + "' not found");
        }
    }

    public static void deletePreset(String name) {
        HotmTree.deletePreset(name);
        sendMessage("\u00A7cDeleted preset '\u00A7e" + name + "\u00A7c'");
    }

    public static void apply() {
        AutoHotmManager.start();
    }

    private static void sendMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("\u00A76[MQO] " + message), false);
        }
    }
}
