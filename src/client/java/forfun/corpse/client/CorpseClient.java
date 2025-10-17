package forfun.corpse.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorpseClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CorpseClient");
    private static final Pattern CORPSE_LOOT_PATTERN = Pattern.compile("\\s(.+) CORPSE LOOT!\\s");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[CorpseClient] Initializing Corpse ESP Mod");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getcorpse")
                .executes(context -> {
                    CorpseESP.getCorpseInfo();
                    return 1;
                }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                CorpseESP.tick();
            }
        });

        WorldRenderEvents.LAST.register(context -> {
            CorpseESP.render(context.matrixStack(), context.camera());
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String messageText = message.getString();
            Matcher matcher = CORPSE_LOOT_PATTERN.matcher(messageText);
            if (matcher.find()) {
                CorpseESP.onCorpseClaimed();
            }
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CorpseESP.onWorldUnload();
        });

        LOGGER.info("[CorpseClient] Corpse ESP Mod initialized");
    }
}
