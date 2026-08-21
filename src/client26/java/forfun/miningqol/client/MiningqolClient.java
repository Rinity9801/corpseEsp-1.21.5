package forfun.miningqol.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import forfun.miningqol.client.config.MiningConfig;
import forfun.miningqol.client.waypoints.OrderedWaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiningqolClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningQOL");
    private static final Pattern CORPSE_LOOT_PATTERN = Pattern.compile("\\s(.+) CORPSE LOOT!\\s");

    private static MiningConfig config;

    // Registered here (base) so both legit and cheat share one keybind category;
    // CheatBootstrap reuses MINING_CATEGORY instead of registering its own.
    public static KeyMapping.Category MINING_CATEGORY;
    private static KeyMapping abilitySwitchKey;

    @Override
    public void onInitializeClient() {
        config = MiningConfig.load();

        MINING_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("miningqol", "category"));
        abilitySwitchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.miningqol.ability_switch", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, MINING_CATEGORY));

        // Cheat-only wiring (keybinds, clicker ticks, comm claim, config hooks) lives in
        // CheatBootstrap, which only exists in the -cheat variant's source tree.
        try {
            Class.forName("forfun.miningqol.client.CheatBootstrap").getMethod("init").invoke(null);
            LOGGER.info("[MiningQOL] Cheat features enabled");
        } catch (ClassNotFoundException e) {
            // legit build — no cheat features
        } catch (Exception e) {
            LOGGER.error("[MiningQOL] Failed to init cheat features", e);
        }

        // Local-only external ESP feed (local/esp/). Absent from every released build.
        try {
            Class.forName("forfun.miningqol.client.EspBootstrap").getMethod("init").invoke(null);
            LOGGER.info("[MiningQOL] External ESP feed enabled (local build)");
        } catch (ClassNotFoundException e) {
            // released build — no external ESP
        } catch (Exception e) {
            LOGGER.error("[MiningQOL] Failed to init external ESP feed", e);
        }
        config.applyToGame();
        CommissionHUD.register();
        BlockOverlay.init();
        PickaxeCooldownHUD.register();
        LobbyFinderHUD.register();
        OrderedWaypointManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            OrderedWaypointManager.tick();
            while (abilitySwitchKey.consumeClick()) AbilitySwitchManager.toggle();
            if (client.level != null && client.player != null) {
                CorpseESP.tick();
                ShaftESP.tick();
                PickaxeCooldownHUD.tick();
                CommandKeybindManager.tick(client);
                LobbyFinder.tick();
                ColdTracker.tick();
                FiletWarning.tick();
                AbilitySwitchManager.tick();
                EfficientMinerOverlay.tick();
                // Ticked, not driven from the render pass: the HUD stops rendering while any screen
                // is open, which is exactly when the Royal Pigeon menu needs reading.
                CommissionHUD.tick();
            }
        });

        ClientTickEvents.START_LEVEL_TICK.register(level -> LobbyFinder.onWorldChange());

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) {
                return;   // action bar; nothing here reads it
            }
            String messageText = message.getString();
            PickaxeCooldownHUD.onGameMessage(messageText);

            Matcher corpseMatcher = CORPSE_LOOT_PATTERN.matcher(messageText);
            if (corpseMatcher.find()) {
                CorpseESP.onCorpseClaimed();
            }

            CommTracker.onChatMessage(messageText);

            if (CheatHooks.onGameMessage != null) {
                CheatHooks.onGameMessage.accept(messageText);
            }
        });

        // /sho load <route> -> follow up with /sho skipto 1 (ported from 1.21.11)
        ClientSendMessageEvents.COMMAND.register(command -> {
            if (config.autoSkipShoLoad && command.startsWith("sho load ")) {
                net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                if (client.player != null) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(100); // let the load command process first
                            client.execute(() -> {
                                if (client.player != null) {
                                    client.player.connection.sendCommand("sho skipto 1");
                                }
                            });
                        } catch (InterruptedException e) {
                            LOGGER.error("Failed to auto-skip sho load", e);
                        }
                    }, "MiningQOL-ShoSkip").start();
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CorpseESP.onWorldUnload();
            ShaftESP.onWorldUnload();
            if (CheatHooks.onStopping != null) {
                CheatHooks.onStopping.run();
            }
            config.loadFromGame();
            config.save();
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("miningconfig")
                .executes(context -> {
                    // Defer to the next client-thread task drain: we're mid-command on the
                    // render thread and the chat screen closes right after this returns.
                    // (Vexel's display() uses knit's TimeScheduler, which calls setScreen
                    // from a timer thread and trips fabric-screen-api — don't use it.)
                    net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                    client.schedule(() -> client.setScreen(new forfun.miningqol.client.gui.VexelMainScreen()));
                    return 1;
                }));
            dispatcher.register(ClientCommands.literal("commtrack")
                .then(ClientCommands.literal("reset")
                    .executes(context -> {
                        CommTracker.reset();
                        return 1;
                    })));
            dispatcher.register(ClientCommands.literal("commhuddebug")
                .executes(context -> {
                    CommissionHUD.debugDump();
                    return 1;
                }));
            // Separate command because a command can't be run while the menu is open — this prints
            // the slots captured on tick the last time the Royal Pigeon was up.
            dispatcher.register(ClientCommands.literal("commhudslots")
                .executes(context -> {
                    CommissionHUD.dumpMenuSlots();
                    return 1;
                }));
            // Sound scanner: /soundscan to capture what's playing, /soundblock to silence it.
            dispatcher.register(ClientCommands.literal("soundscan")
                .executes(context -> {
                    SoundBlocker.toggleScanning();
                    return 1;
                })
                .then(ClientCommands.literal("on")
                    .executes(context -> {
                        SoundBlocker.setScanning(true);
                        return 1;
                    }))
                .then(ClientCommands.literal("off")
                    .executes(context -> {
                        SoundBlocker.setScanning(false);
                        return 1;
                    }))
                .then(ClientCommands.literal("list")
                    .executes(context -> {
                        SoundBlocker.listCaptured();
                        return 1;
                    }))
                .then(ClientCommands.literal("clear")
                    .executes(context -> {
                        SoundBlocker.clearCaptured();
                        return 1;
                    }))
                .then(ClientCommands.literal("verbose")
                    .executes(context -> {
                        SoundBlocker.setVerbose(!SoundBlocker.isVerbose());
                        return 1;
                    }))
                .then(ClientCommands.literal("status")
                    .executes(context -> {
                        SoundBlocker.status();
                        return 1;
                    }))
                .then(ClientCommands.literal("block")
                    .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            SoundBlocker.blockCaptured(IntegerArgumentType.getInteger(context, "number"), false);
                            return 1;
                        })
                        .then(ClientCommands.literal("all")
                            .executes(context -> {
                                SoundBlocker.blockCaptured(IntegerArgumentType.getInteger(context, "number"), true);
                                return 1;
                            })))));
            dispatcher.register(ClientCommands.literal("soundblock")
                .executes(context -> {
                    SoundBlocker.listRules();
                    return 1;
                })
                .then(ClientCommands.literal("add")
                    .then(ClientCommands.argument("rule", StringArgumentType.greedyString())
                        .executes(context -> {
                            SoundBlocker.addRule(StringArgumentType.getString(context, "rule"));
                            return 1;
                        })))
                .then(ClientCommands.literal("remove")
                    .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            SoundBlocker.removeRule(IntegerArgumentType.getInteger(context, "number"));
                            return 1;
                        })))
                .then(ClientCommands.literal("list")
                    .executes(context -> {
                        SoundBlocker.listRules();
                        return 1;
                    }))
                .then(ClientCommands.literal("clear")
                    .executes(context -> {
                        SoundBlocker.clearRules();
                        return 1;
                    }))
                .then(ClientCommands.literal("toggle")
                    .executes(context -> {
                        SoundBlocker.setBlockingEnabled(!SoundBlocker.isBlockingEnabled());
                        SoundBlocker.listRules();
                        saveConfig();
                        return 1;
                    })));
            dispatcher.register(ClientCommands.literal("getcorpse")
                .executes(context -> {
                    CorpseESP.getCorpseInfo();
                    return 1;
                }));
            dispatcher.register(ClientCommands.literal("getcold")
                .executes(context -> {
                    net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                    if (client.player != null) {
                        MqoChat.reply(Component.literal(
                            "\u00A76[MQO] \u00A7fCurrent Cold: \u00A7b" + ColdTracker.getCold()));
                    }
                    return 1;
                }));
            dispatcher.register(ClientCommands.literal("lobbyfind")
                .then(ClientCommands.literal("add")
                    .executes(context -> {
                        LobbyFinder.addBlock();
                        return 1;
                    }))
                .then(ClientCommands.literal("remove")
                    .executes(context -> {
                        LobbyFinder.removeBlock();
                        return 1;
                    }))
                .then(ClientCommands.literal("clear")
                    .executes(context -> {
                        LobbyFinder.clearAll();
                        return 1;
                    }))
                .then(ClientCommands.literal("list")
                    .executes(context -> {
                        LobbyFinder.listBlocks();
                        return 1;
                    })));
            dispatcher.register(ClientCommands.literal("mqo")
                .then(ClientCommands.literal("add")
                    .executes(context -> {
                        OrderedWaypointManager.add();
                        return 1;
                    }))
                .then(ClientCommands.literal("insert")
                    .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            OrderedWaypointManager.insert(IntegerArgumentType.getInteger(context, "number"));
                            return 1;
                        })))
                .then(ClientCommands.literal("remove")
                    .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            OrderedWaypointManager.remove(IntegerArgumentType.getInteger(context, "number"));
                            return 1;
                        })))
                .then(ClientCommands.literal("move")
                    .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            OrderedWaypointManager.move(IntegerArgumentType.getInteger(context, "number"));
                            return 1;
                        })))
                .then(ClientCommands.literal("change")
                    .then(ClientCommands.argument("first", IntegerArgumentType.integer(1))
                        .then(ClientCommands.argument("second", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                OrderedWaypointManager.swap(
                                    IntegerArgumentType.getInteger(context, "first"),
                                    IntegerArgumentType.getInteger(context, "second"));
                                return 1;
                            }))))
                .then(ClientCommands.literal("edit")
                    .executes(context -> {
                        OrderedWaypointManager.toggleEditMode();
                        return 1;
                    }))
                .then(ClientCommands.literal("ether")
                    .executes(context -> {
                        OrderedWaypointManager.listEtherwarps();
                        return 1;
                    })
                    .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            OrderedWaypointManager.toggleEtherwarp(IntegerArgumentType.getInteger(context, "number"));
                            return 1;
                        })))
                .then(ClientCommands.literal("skip")
                    .executes(context -> {
                        OrderedWaypointManager.skip(1);
                        return 1;
                    })
                    .then(ClientCommands.argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            OrderedWaypointManager.skip(IntegerArgumentType.getInteger(context, "amount"));
                            return 1;
                        })))
                .then(ClientCommands.literal("skipto")
                    .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            OrderedWaypointManager.skipTo(IntegerArgumentType.getInteger(context, "number"));
                            return 1;
                        })))
                .then(ClientCommands.literal("save")
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            OrderedWaypointManager.save(StringArgumentType.getString(context, "name"));
                            return 1;
                        })))
                .then(ClientCommands.literal("load")
                    .executes(context -> {
                        OrderedWaypointManager.loadFromClipboard();
                        return 1;
                    })
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            OrderedWaypointManager.load(StringArgumentType.getString(context, "name"));
                            return 1;
                        })))
                .then(ClientCommands.literal("unload")
                    .executes(context -> {
                        OrderedWaypointManager.unload();
                        return 1;
                    }))
                .then(ClientCommands.literal("delete")
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            OrderedWaypointManager.deleteRoute(StringArgumentType.getString(context, "name"));
                            return 1;
                        })))
                .then(ClientCommands.literal("list")
                    .executes(context -> {
                        OrderedWaypointManager.listRoutes();
                        return 1;
                    }))
                .then(ClientCommands.literal("info")
                    .executes(context -> {
                        OrderedWaypointManager.info();
                        return 1;
                    }))
                .then(ClientCommands.literal("toggle")
                    .executes(context -> {
                        OrderedWaypointManager.toggle();
                        return 1;
                    }))
                .then(ClientCommands.literal("export")
                    .executes(context -> {
                        OrderedWaypointManager.export();
                        return 1;
                    })));
        });
    }

    public static MiningConfig getConfig() {
        return config;
    }

    /** Snapshots current in-game state and writes it out — for settings changed mid-session. */
    public static void saveConfig() {
        if (config == null) return;
        config.loadFromGame();
        config.save();
    }
}
