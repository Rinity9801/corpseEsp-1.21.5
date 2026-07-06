package forfun.miningqol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class CommandKeybindManager {
    private static final Map<Integer, String> keybindCommands = new HashMap<>();
    private static final Map<Integer, Boolean> keyStates = new HashMap<>();

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

    public static void tick(Minecraft client) {
        if (client.player == null) return;

        // Don't process keybinds when a screen is open (GUI, chat, etc.)
        if (client.screen != null) return;

        for (Map.Entry<Integer, String> entry : keybindCommands.entrySet()) {
            int keyCode = entry.getKey();
            String command = entry.getValue();

            boolean isPressed;
            if (keyCode > GLFW.GLFW_KEY_LAST) {
                int mouseButton = keyCode - GLFW.GLFW_KEY_LAST - 1;
                isPressed = GLFW.glfwGetMouseButton(client.getWindow().handle(), mouseButton) == GLFW.GLFW_PRESS;
            } else {
                isPressed = InputConstants.isKeyDown(client.getWindow(), keyCode);
            }

            boolean wasPressed = keyStates.getOrDefault(keyCode, false);

            if (isPressed && !wasPressed) {
                executeCommand(client, command);
            }

            keyStates.put(keyCode, isPressed);
        }
    }

    private static void executeCommand(Minecraft client, String command) {
        if (client.player == null) return;

        if (command.startsWith("/")) {
            client.player.connection.sendCommand(command.substring(1));
        } else {
            client.player.connection.sendChat(command);
        }
    }

    public static String getKeyName(int keyCode) {
        if (keyCode > GLFW.GLFW_KEY_LAST) {
            int mouseButton = keyCode - GLFW.GLFW_KEY_LAST - 1;
            return "Mouse " + (mouseButton + 1);
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getName();
    }
}
