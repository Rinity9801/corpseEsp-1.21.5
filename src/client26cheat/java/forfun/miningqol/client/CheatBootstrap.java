package forfun.miningqol.client;

import com.mojang.blaze3d.platform.InputConstants;
import forfun.miningqol.client.config.MiningConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Cheat-only wiring for 26.1.2, invoked reflectively from MiningqolClient.
 * Everything cheat lives behind this class so the legit variant (which simply
 * doesn't compile this source tree) never references it.
 */
public final class CheatBootstrap {
    private static KeyMapping toggleAutoClickerKey;
    private static KeyMapping toggleShaftClickerKey;
    private static KeyMapping toggleInShaftClickKey;

    private CheatBootstrap() {}

    public static void init() {
        // Reuse the category registered by the base MiningqolClient (runs first).
        KeyMapping.Category category = MiningqolClient.MINING_CATEGORY;

        toggleAutoClickerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.miningqol.toggle_coalclick", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        toggleShaftClickerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.miningqol.toggle_shaftclick", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category));
        toggleInShaftClickKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.miningqol.toggle_inshaftclick", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, category));

        AutoClickerHUD.register();
        forfun.miningqol.client.hotm.HotmManager.init();
        forfun.miningqol.client.gui.CheatGui.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleAutoClickerKey.consumeClick()) AutoClickerManager.toggle();
            while (toggleShaftClickerKey.consumeClick()) ShaftClickerManager.toggle();
            while (toggleInShaftClickKey.consumeClick()) InShaftClickManager.toggle();

            if (client.level != null && client.player != null) {
                AutoClickerManager.tick();
                CommClaimManager.checkAutoTrigger(client);
                CommClaimManager.tick();
                InShaftClickManager.tick();
                ShaftClickerManager.tick();
                EmptyStashManager.tick();
                forfun.miningqol.client.hotm.AutoHotmManager.tick();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("claimcomms")
                .executes(context -> {
                    CommClaimManager.start();
                    return 1;
                })
                .then(ClientCommands.literal("stop")
                    .executes(context -> {
                        CommClaimManager.stop();
                        return 1;
                    })));
            dispatcher.register(ClientCommands.literal("emptystash")
                .executes(context -> {
                    EmptyStashManager.toggle();
                    return 1;
                })
                .then(ClientCommands.literal("stop")
                    .executes(context -> {
                        EmptyStashManager.stop("\u00A7cEmpty Stash stopped.");
                        return 1;
                    }))
                .then(ClientCommands.literal("debug")
                    .executes(context -> {
                        EmptyStashManager.setDebug(!EmptyStashManager.isDebug());
                        return 1;
                    })));
            dispatcher.register(ClientCommands.literal("hotmconfig")
                .executes(context -> {
                    forfun.miningqol.client.hotm.HotmChestScreen.open();
                    return 1;
                }));
            dispatcher.register(ClientCommands.literal("commclaimdebug")
                .executes(context -> {
                    CommClaimManager.setDebug(!CommClaimManager.isDebug());
                    return 1;
                }));
        });

        CheatHooks.onGameMessage = messageText -> {
            // A commission just completed — fast-poll the tab list so the auto-claim
            // fires as soon as possible.
            if (messageText.toLowerCase().contains("commission complete")) {
                CommClaimManager.onCommissionComplete(messageText);
            }
            EmptyStashManager.onChatMessage(messageText);
            forfun.miningqol.client.hotm.AutoHotmManager.onChatMessage(messageText);
        };

        CheatHooks.onStopping = () -> {
            AutoClickerManager.cleanup();
            InShaftClickManager.cleanup();
            ShaftClickerManager.cleanup();
        };

        CheatHooks.applyConfig = () -> {
            MiningConfig config = MiningqolClient.getConfig();
            if (config == null) return;
            // Never restore enabled state for clickers - always start disabled for safety
            AutoClickerManager.setMiningSlot(config.autoClickerMiningSlot);
            AutoClickerManager.setEnableRodSwap(config.autoClickerRodSwap);
            AutoClickerManager.setEnableSecondDrill(config.autoClickerSecondDrill);
            AutoClickerManager.setSecondDrillSlot(config.autoClickerSecondDrillSlot);
            AutoClickerManager.setMainDrillDelay(config.autoClickerMainDrillDelay);
            AutoClickerManager.setSecondDrillDelay(config.autoClickerSecondDrillDelay);
            AutoClickerHUD.setEnabled(config.autoClickerHudEnabled);

            InShaftClickManager.setMiningSlot(config.coldClickerMiningSlot);
            InShaftClickManager.setEnableRodSwap(config.coldClickerRodSwap);
            InShaftClickManager.setSecondDrillSlot(config.coldClickerSecondDrillSlot);
            InShaftClickManager.setMainDrillDelay(config.coldClickerMainDrillDelay);
            InShaftClickManager.setSecondDrillDelay(config.coldClickerSecondDrillDelay);
            InShaftClickManager.setColdThreshold(config.coldClickerColdThreshold);
            InShaftClickManager.setShowToggleMessage(config.coldClickerShowToggleMessage);

            ShaftClickerManager.setMiningSlot(config.shaftClickerMiningSlot);
            ShaftClickerManager.setShowToggleMessage(config.shaftClickerShowToggleMessage);

            CommClaimManager.setBatPersonSlot(config.commClaimBatPersonSlot);
            CommClaimManager.setDivanSlot(config.commClaimDivanSlot);
            CommClaimManager.setRefinedToolSlot(config.commClaimRefinedToolSlot);
            CommClaimManager.setTickDelay(config.commClaimTickDelay);
            CommClaimManager.setGuiWaitDelay(config.commClaimGuiWaitDelay);
            CommClaimManager.setAutoTrigger(config.commClaimAutoTrigger);
            CommClaimManager.setWardrobeSwap(config.commClaimWardrobeSwap);
            CommClaimManager.setBatchMining(config.commClaimBatchMining);

            EmptyStashManager.setMaterialByName(config.emptyStashMaterial);
            EmptyStashManager.setActionDelay(config.emptyStashDelay);
        };

        CheatHooks.storeConfig = () -> {
            MiningConfig config = MiningqolClient.getConfig();
            if (config == null) return;
            config.autoClickerMiningSlot = AutoClickerManager.getMiningSlot();
            config.autoClickerRodSwap = AutoClickerManager.isRodSwapEnabled();
            config.autoClickerSecondDrill = AutoClickerManager.isSecondDrillEnabled();
            config.autoClickerSecondDrillSlot = AutoClickerManager.getSecondDrillSlot();
            config.autoClickerMainDrillDelay = AutoClickerManager.getMainDrillDelay();
            config.autoClickerSecondDrillDelay = AutoClickerManager.getSecondDrillDelay();
            config.autoClickerHudEnabled = AutoClickerHUD.isEnabled();

            config.coldClickerMiningSlot = InShaftClickManager.getMiningSlot();
            config.coldClickerRodSwap = InShaftClickManager.isRodSwapEnabled();
            config.coldClickerSecondDrillSlot = InShaftClickManager.getSecondDrillSlot();
            config.coldClickerMainDrillDelay = InShaftClickManager.getMainDrillDelay();
            config.coldClickerSecondDrillDelay = InShaftClickManager.getSecondDrillDelay();
            config.coldClickerColdThreshold = InShaftClickManager.getColdThreshold();
            config.coldClickerShowToggleMessage = InShaftClickManager.isShowToggleMessage();

            config.shaftClickerMiningSlot = ShaftClickerManager.getMiningSlot();
            config.shaftClickerShowToggleMessage = ShaftClickerManager.isShowToggleMessage();

            config.commClaimBatPersonSlot = CommClaimManager.getBatPersonSlot();
            config.commClaimDivanSlot = CommClaimManager.getDivanSlot();
            config.commClaimRefinedToolSlot = CommClaimManager.getRefinedToolSlot();
            config.commClaimTickDelay = CommClaimManager.getTickDelay();
            config.commClaimGuiWaitDelay = CommClaimManager.getGuiWaitDelay();
            config.commClaimAutoTrigger = CommClaimManager.isAutoTrigger();
            config.commClaimWardrobeSwap = CommClaimManager.isWardrobeSwap();
            config.commClaimBatchMining = CommClaimManager.isBatchMining();

            config.emptyStashMaterial = EmptyStashManager.getMaterial().name();
            config.emptyStashDelay = EmptyStashManager.getActionDelay();
        };
    }
}
