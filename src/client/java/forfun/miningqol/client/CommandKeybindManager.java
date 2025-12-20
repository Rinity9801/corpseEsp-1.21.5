package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class CommandKeybindManager {
    private static final Map<Integer, String> keybindCommands = new HashMap<>();
    private static final Map<Integer, Boolean> keyStates = new HashMap<>();

    // Special codes for scroll wheel (beyond mouse buttons)
    public static final int SCROLL_UP = GLFW.GLFW_KEY_LAST + 100;
    public static final int SCROLL_DOWN = GLFW.GLFW_KEY_LAST + 101;

    // Pending scroll events (set by scroll callback, consumed by tick)
    private static volatile boolean pendingScrollUp = false;
    private static volatile boolean pendingScrollDown = false;

    public static void registerKeybind(int keyCode, String command) {
        keybindCommands.put(keyCode, command);
        keyStates.put(keyCode, false);
    }

    public static void removeKeybind(int keyCode) {
        keybindCommands.remove(keyCode);
        keyStates.remove(keyCode);
    }

    public static void clearAll() {
        keybindCommands.clear();
        keyStates.clear();
    }

    public static Map<Integer, String> getAllKeybinds() {
        return new HashMap<>(keybindCommands);
    }

    // Called from scroll callback mixin
    public static void onScroll(double vertical) {
        if (vertical > 0) {
            pendingScrollUp = true;
        } else if (vertical < 0) {
            pendingScrollDown = true;
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        // Don't process keybinds when a screen is open (GUI, chat, etc.)
        if (client.currentScreen != null) {
            // Clear pending scroll events when screen is open
            pendingScrollUp = false;
            pendingScrollDown = false;
            return;
        }

        for (Map.Entry<Integer, String> entry : keybindCommands.entrySet()) {
            int keyCode = entry.getKey();
            String command = entry.getValue();

            boolean isPressed;
            if (keyCode == SCROLL_UP) {
                isPressed = pendingScrollUp;
            } else if (keyCode == SCROLL_DOWN) {
                isPressed = pendingScrollDown;
            } else if (keyCode > GLFW.GLFW_KEY_LAST) {
                int mouseButton = keyCode - GLFW.GLFW_KEY_LAST - 1;
                isPressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), mouseButton) == GLFW.GLFW_PRESS;
            } else {
                isPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), keyCode);
            }

            boolean wasPressed = keyStates.getOrDefault(keyCode, false);

            if (isPressed && !wasPressed) {
                executeCommand(client, command);
            }

            keyStates.put(keyCode, isPressed);
        }

        // Clear scroll events after processing
        pendingScrollUp = false;
        pendingScrollDown = false;
    }

    private static void executeCommand(MinecraftClient client, String command) {
        if (client.player == null) return;

        if (command.startsWith("/")) {
            client.player.networkHandler.sendChatCommand(command.substring(1));
        } else {
            client.player.networkHandler.sendChatMessage(command);
        }
    }

    public static String getKeyName(int keyCode) {
        if (keyCode == SCROLL_UP) {
            return "Scroll Up";
        } else if (keyCode == SCROLL_DOWN) {
            return "Scroll Down";
        } else if (keyCode > GLFW.GLFW_KEY_LAST) {
            int mouseButton = keyCode - GLFW.GLFW_KEY_LAST - 1;
            return "Mouse " + (mouseButton + 1);
        }
        return InputUtil.Type.KEYSYM.createFromCode(keyCode).getTranslationKey();
    }

    public static boolean isScrollCode(int keyCode) {
        return keyCode == SCROLL_UP || keyCode == SCROLL_DOWN;
    }
}
