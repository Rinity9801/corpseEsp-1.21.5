package forfun.miningqol.client;

import forfun.miningqol.client.config.MiningConfig;
import forfun.miningqol.client.gui.VexelMainScreen;
import forfun.miningqol.client.profit.BlockTracker;
import forfun.miningqol.client.profit.GemstoneTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import forfun.miningqol.client.profit.ProfitDebugger;
import forfun.miningqol.client.collection.CollectionTracker;
import forfun.miningqol.client.sacks.CoalValueCommand;

//? if isCheat {
import forfun.miningqol.client.hotm.HotmManager;
//?}
import forfun.miningqol.client.waypoints.OrderedWaypointManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiningqolClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningqolClient");
    private static final Pattern CORPSE_LOOT_PATTERN = Pattern.compile("\\s(.+) CORPSE LOOT!\\s");
    private static final Pattern PRISTINE_PATTERN = Pattern.compile("PRISTINE! You found . Flawed (.+) Gemstone x(\\d+)!");
    private static MiningConfig config;
    //? if isCheat {
    private static KeyBinding toggleAutoClickerKey;
    private static KeyBinding toggleShaftClickerKey;
    private static KeyBinding toggleInShaftClickKey;
    private static KeyBinding commClaimKey;
    private static KeyBinding invClickKey;
    //?}
    private static KeyBinding abilitySwitchKey;
    private static KeyBinding radialKey;


    @Override
    public void onInitializeClient() {
        LOGGER.info("[MiningqolClient] Initializing MiningQOL Mod");

        config = MiningConfig.load();
        config.applyToGame();

        PickaxeCooldownHUD.register();
        RollingMinerCooldown.register();

        OrderedWaypointManager.init();
        //? if isCheat {
        HotmManager.init();
        //?}

        // Create category once and reuse for all keybinds
        KeyBinding.Category miningqolCategory = KeyBinding.Category.create(Identifier.of("miningqol", "category"));

        //? if isCheat {
        toggleAutoClickerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miningqol.toggle_coalclick",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            miningqolCategory
        ));

        toggleShaftClickerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miningqol.toggle_shaftclick",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            miningqolCategory
        ));

        toggleInShaftClickKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miningqol.toggle_inshaftclick",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            miningqolCategory
        ));

        commClaimKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miningqol.comm_claim",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            miningqolCategory
        ));

        invClickKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miningqol.inv_click",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            miningqolCategory
        ));
        //?}

        abilitySwitchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miningqol.ability_switch",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            miningqolCategory
        ));

        radialKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miningqol.radial",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            miningqolCategory
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getcorpse")
                .executes(context -> {
                    CorpseESP.getCorpseInfo();
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("miningconfig")
                .executes(context -> {
                    MinecraftClient.getInstance().send(() -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.setScreen(new VexelMainScreen());
                    });
                    return 1;
                })
                //? if isCheat {
                .then(ClientCommandManager.literal("hotm")
                    .executes(context -> {
                        forfun.miningqol.client.hotm.HotmChestScreen.open();
                        return 1;
                    }))
                //?}
                );
            //? if isCheat {
            dispatcher.register(ClientCommandManager.literal("hotmconfig")
                .executes(context -> {
                    forfun.miningqol.client.hotm.HotmChestScreen.open();
                    return 1;
                }));
            //?}
            dispatcher.register(ClientCommandManager.literal("getstand")
                .executes(context -> {
                    getArmorStandData(context.getSource());
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("getcold")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        int cold = ColdTracker.getCold();
                        client.player.sendMessage(Text.literal("§6[MQO] §fCurrent Cold: §b" + cold), false);
                    }
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("shaftdebug")
                .executes(context -> {
                    dumpNearbyEntities();
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("profitreset")
                .executes(context -> {
                    GemstoneTracker.reset();
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("profitdebug")
                .executes(context -> {
                    ProfitDebugger.showCalculationDetails();
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("trackerdebug")
                .executes(context -> {
                    boolean newState = !BlockTracker.isDebugEnabled();
                    BlockTracker.setDebugEnabled(newState);
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(
                            newState ? "\u00A7aBlock tracker debug enabled" : "\u00A7cBlock tracker debug disabled"
                        ), false);
                    }
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("coalvalue")
                .executes(context -> {
                    CoalValueCommand.execute();
                    return 1;
                }));
            //? if isCheat {
            dispatcher.register(ClientCommandManager.literal("claimcomms")
                .executes(context -> {
                    if (CommClaimManager.isRunning()) {
                        CommClaimManager.stop();
                    } else {
                        CommClaimManager.start();
                    }
                    return 1;
                }));
            //?}
            dispatcher.register(ClientCommandManager.literal("radial")
                .executes(context -> {
                    RadialMenuManager.open();
                    return 1;
                })
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(1, 8))
                        .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                            .executes(context -> {
                                RadialMenuManager.setCommand(
                                    IntegerArgumentType.getInteger(context, "slot"),
                                    StringArgumentType.getString(context, "command"));
                                return 1;
                            }))))
                .then(ClientCommandManager.literal("label")
                    .then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(1, 8))
                        .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                            .executes(context -> {
                                RadialMenuManager.setLabel(
                                    IntegerArgumentType.getInteger(context, "slot"),
                                    StringArgumentType.getString(context, "text"));
                                return 1;
                            }))))
                .then(ClientCommandManager.literal("clear")
                    .then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(1, 8))
                        .executes(context -> {
                            RadialMenuManager.clear(IntegerArgumentType.getInteger(context, "slot"));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        RadialMenuManager.list();
                        return 1;
                    })));
            dispatcher.register(ClientCommandManager.literal("commclaimdebug")
                .executes(context -> {
                    boolean newState = !CommClaimManager.isDebug();
                    CommClaimManager.setDebug(newState);
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(
                            newState ? "§6[CommClaim] §aSidebar debug enabled (prints each second)"
                                     : "§6[CommClaim] §cSidebar debug disabled"), false);
                    }
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("colltrack")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(
                            "§6[MQO] §7Usage: /colltrack <material> | stop | list | move\n§7Materials: §f"
                                + CollectionTracker.materialsList()), false);
                    }
                    return 1;
                })
                .then(ClientCommandManager.literal("stop")
                    .executes(context -> {
                        CollectionTracker.stop(true);
                        return 1;
                    }))
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        CollectionTracker.dumpCollections();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("move")
                    .executes(context -> {
                        MinecraftClient.getInstance().send(() ->
                            MinecraftClient.getInstance().setScreen(
                                new forfun.miningqol.client.gui.CollectionHudPositionScreen(null)));
                        return 1;
                    }))
                .then(ClientCommandManager.argument("material", StringArgumentType.word())
                    .executes(context -> {
                        CollectionTracker.start(StringArgumentType.getString(context, "material"));
                        return 1;
                    })));
            //? if isCheat {
            dispatcher.register(ClientCommandManager.literal("invclickdelay")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§6[MQO] §7Inventory click delay: §f"
                            + InventoryClickManager.getClickDelay() + "§7 ticks"), false);
                    }
                    return 1;
                })
                .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(1, 40))
                    .executes(context -> {
                        int ticks = IntegerArgumentType.getInteger(context, "ticks");
                        InventoryClickManager.setClickDelay(ticks);
                        config.loadFromGame();
                        config.save();
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§6[MQO] §aInventory click delay set to §f"
                                + InventoryClickManager.getClickDelay() + "§a ticks"), false);
                        }
                        return 1;
                    })));
            //?}
            dispatcher.register(ClientCommandManager.literal("getplayerhead")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null) return 0;

                    net.minecraft.item.ItemStack heldItem = client.player.getMainHandStack();
                    if (heldItem.isEmpty()) {
                        client.player.sendMessage(Text.literal("§cNo item in hand!"), false);
                        return 0;
                    }

                    // Get the profile component
                    net.minecraft.component.type.ProfileComponent profile = heldItem.get(net.minecraft.component.DataComponentTypes.PROFILE);

                    if (profile == null) {
                        client.player.sendMessage(Text.literal("§cThis item has no profile data!"), false);
                        return 0;
                    }

                    // Format the profile data
                    StringBuilder sb = new StringBuilder();
                    sb.append("[minecraft:profile={");

                    // Add UUID as int array
                    java.util.UUID uuid = profile.getGameProfile().id();
                    if (uuid != null) {
                        long mostSig = uuid.getMostSignificantBits();
                        long leastSig = uuid.getLeastSignificantBits();
                        int[] uuidInts = new int[4];
                        uuidInts[0] = (int)(mostSig >> 32);
                        uuidInts[1] = (int)mostSig;
                        uuidInts[2] = (int)(leastSig >> 32);
                        uuidInts[3] = (int)leastSig;
                        sb.append("id:[I;").append(uuidInts[0]).append(",")
                          .append(uuidInts[1]).append(",")
                          .append(uuidInts[2]).append(",")
                          .append(uuidInts[3]).append("]");
                    }

                    // Add name
                    String name = profile.getGameProfile().name();
                    if (name == null) name = "";
                    sb.append(",name:\"").append(name).append("\"");

                    // Add properties
                    sb.append(",properties:[");
                    com.mojang.authlib.properties.PropertyMap properties = profile.getGameProfile().properties();
                    if (!properties.isEmpty()) {
                        boolean first = true;
                        for (com.mojang.authlib.properties.Property prop : properties.values()) {
                            if (!first) sb.append(",");
                            first = false;
                            sb.append("{name:\"").append(prop.name()).append("\"");
                            sb.append(",value:\"").append(prop.value()).append("\"}");
                        }
                    }
                    sb.append("]}]");

                    // Send to player and copy to clipboard
                    client.player.sendMessage(Text.literal("§6Profile Data: §f" + sb.toString()), false);
                    client.keyboard.setClipboard(sb.toString());
                    client.player.sendMessage(Text.literal("§aCopied to clipboard!"), false);

                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("lobbyfind")
                .then(ClientCommandManager.literal("add")
                    .executes(context -> {
                        LobbyFinder.addBlock();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("remove")
                    .executes(context -> {
                        LobbyFinder.removeBlock();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("clear")
                    .executes(context -> {
                        LobbyFinder.clearAll();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        LobbyFinder.listBlocks();
                        return 1;
                    })));
            dispatcher.register(ClientCommandManager.literal("sblines")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null || client.world == null) return 0;

                    net.minecraft.scoreboard.Scoreboard scoreboard = client.world.getScoreboard();
                    net.minecraft.scoreboard.ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);

                    if (sidebar == null) {
                        client.player.sendMessage(Text.literal("§cNo sidebar scoreboard found!"), false);
                        return 0;
                    }

                    client.player.sendMessage(Text.literal("§6=== Scoreboard: " + sidebar.getDisplayName().getString() + " ==="), false);

                    java.util.List<net.minecraft.scoreboard.ScoreboardEntry> entries = new java.util.ArrayList<>(scoreboard.getScoreboardEntries(sidebar));
                    entries.sort((a, b) -> Integer.compare(b.value(), a.value()));

                    for (int i = 0; i < entries.size(); i++) {
                        net.minecraft.scoreboard.ScoreboardEntry entry = entries.get(i);
                        String name = entry.owner();

                        // Try to get the display text from the team
                        net.minecraft.scoreboard.Team team = scoreboard.getScoreHolderTeam(name);
                        String displayText;
                        if (team != null) {
                            displayText = team.getPrefix().getString() + name + team.getSuffix().getString();
                        } else {
                            displayText = name;
                        }

                        client.player.sendMessage(Text.literal("§7[" + i + "] §f" + displayText), false);
                    }

                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("tablist")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null || client.getNetworkHandler() == null) return 0;

                    java.util.Collection<net.minecraft.client.network.PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();
                    client.player.sendMessage(Text.literal("§6=== Tab List (" + entries.size() + " entries) ==="), false);

                    int i = 0;
                    for (net.minecraft.client.network.PlayerListEntry entry : entries) {
                        String name = entry.getProfile().name();
                        Text displayName = entry.getDisplayName();
                        String display = displayName != null ? displayName.getString() : name;
                        client.player.sendMessage(Text.literal("§7[" + i + "] §f" + display + " §8(" + name + ")"), false);
                        i++;
                    }

                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("mqo")
                .then(ClientCommandManager.literal("add")
                    .executes(context -> {
                        OrderedWaypointManager.add();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("insert")
                    .then(ClientCommandManager.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int num = IntegerArgumentType.getInteger(context, "number");
                            OrderedWaypointManager.insert(num);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int num = IntegerArgumentType.getInteger(context, "number");
                            OrderedWaypointManager.remove(num);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("move")
                    .then(ClientCommandManager.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int num = IntegerArgumentType.getInteger(context, "number");
                            OrderedWaypointManager.move(num);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("change")
                    .then(ClientCommandManager.argument("first", IntegerArgumentType.integer(1))
                        .then(ClientCommandManager.argument("second", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                int first = IntegerArgumentType.getInteger(context, "first");
                                int second = IntegerArgumentType.getInteger(context, "second");
                                OrderedWaypointManager.swap(first, second);
                                return 1;
                            }))))
                .then(ClientCommandManager.literal("edit")
                    .executes(context -> {
                        OrderedWaypointManager.toggleEditMode();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("skip")
                    .executes(context -> {
                        OrderedWaypointManager.skip(1);
                        return 1;
                    })
                    .then(ClientCommandManager.argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            OrderedWaypointManager.skip(amount);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("skipto")
                    .then(ClientCommandManager.argument("number", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int num = IntegerArgumentType.getInteger(context, "number");
                            OrderedWaypointManager.skipTo(num);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("save")
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            OrderedWaypointManager.save(name);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("load")
                    .executes(context -> {
                        OrderedWaypointManager.loadFromClipboard();
                        return 1;
                    })
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            OrderedWaypointManager.load(name);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("unload")
                    .executes(context -> {
                        OrderedWaypointManager.unload();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("delete")
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            OrderedWaypointManager.deleteRoute(name);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        OrderedWaypointManager.listRoutes();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("info")
                    .executes(context -> {
                        OrderedWaypointManager.info();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("toggle")
                    .executes(context -> {
                        OrderedWaypointManager.toggle();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("export")
                    .executes(context -> {
                        OrderedWaypointManager.export();
                        return 1;
                    })));
            });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            //? if isCheat {
            while (toggleAutoClickerKey.wasPressed()) {
                AutoClickerManager.toggle();
            }

            while (toggleShaftClickerKey.wasPressed()) {
                ShaftClickerManager.toggle();
            }

            while (toggleInShaftClickKey.wasPressed()) {
                InShaftClickManager.toggle();
            }

            while (commClaimKey.wasPressed()) {
                if (CommClaimManager.isRunning()) {
                    CommClaimManager.stop();
                } else {
                    CommClaimManager.start();
                }
            }
            //?}

            while (abilitySwitchKey.wasPressed()) {
                AbilitySwitchManager.toggle();
            }

            // Radial menu: hold the key to open, release to run the highlighted option.
            if (client.player != null) {
                InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(radialKey);
                boolean held = bound.getCategory() == InputUtil.Type.KEYSYM
                    && InputUtil.isKeyPressed(client.getWindow(), bound.getCode());
                if (held && client.currentScreen == null) {
                    RadialMenuManager.open();
                } else if (!held && client.currentScreen instanceof forfun.miningqol.client.gui.RadialMenuScreen rms) {
                    rms.selectAndClose();
                }
            }

            RollingMinerCooldown.tick(client);
            if (client.world != null && client.player != null) {
                CorpseESP.tick();
                ShaftESP.tick();
                GemstoneTracker.tick();
                BlockTracker.tick();
                EfficientMinerOverlay.tick();
                PickaxeCooldownHUD.tick();
                //? if isCheat {
                AutoClickerManager.tick();
                //?}
                CommandKeybindManager.tick(client);
                LobbyFinder.tick();
                OrderedWaypointManager.tick();
                //? if isCheat {
                CommClaimManager.checkAutoTrigger(client);
                CommClaimManager.tick();
                //?}
                AbilitySwitchManager.tick();
                ColdTracker.tick();
                //? if isCheat {
                InShaftClickManager.tick();
                ShaftClickerManager.tick();
                InventoryClickManager.tick();
                //?}
                FiletWarning.tick();
                //? if isCheat {
                forfun.miningqol.client.hotm.AutoHotmManager.tick();
                //?}

            }
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_WORLD_TICK.register(world -> {
            LobbyFinder.onWorldChange();
            BlockTracker.onWorldChange();
            OrderedWaypointManager.onWorldChange();
        });

        // WorldRenderEvents.LAST removed in 1.21.10 - now handled by WorldRendererMixin

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String messageText = message.getString();
            RollingMinerCooldown.onGameMessage(messageText);

            Matcher corpseMatcher = CORPSE_LOOT_PATTERN.matcher(messageText);
            if (corpseMatcher.find()) {
                CorpseESP.onCorpseClaimed();
            }

            Matcher pristineMatcher = PRISTINE_PATTERN.matcher(messageText);
            if (pristineMatcher.find()) {
                String gemType = pristineMatcher.group(1);
                int amount = Integer.parseInt(pristineMatcher.group(2));
                GemstoneTracker.onPristineGem(gemType, amount);
            }

            //? if isCheat {
            // Forward chat to AutoHotm for "already purchased" detection
            forfun.miningqol.client.hotm.AutoHotmManager.onChatMessage(messageText);

            // A commission just completed — fast-poll the tab list so the auto-claim
            // (gated on ALL mining commissions being done) fires as soon as possible.
            if (messageText.toLowerCase().contains("commission complete")) {
                CommClaimManager.onCommissionComplete(messageText);
            }
            //?}

            // Block profit tracking is expensive on large sack messages because it
            // walks hover text and may refresh pricing, so only do it when enabled.
            if (BlockTracker.shouldProcessSackMessages()) {
                BlockTracker.onChatMessage(message);
            }
        });

        ClientSendMessageEvents.COMMAND.register((command) -> {
            if (config.autoSkipShoLoad && command.startsWith("sho load ")) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    // Schedule the skipto command to run after a short delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(100); // Wait 100ms for the load command to process
                            client.execute(() -> {
                                if (client.player != null) {
                                    client.player.networkHandler.sendChatCommand("sho skipto 1");
                                }
                            });
                        } catch (InterruptedException e) {
                            LOGGER.error("Failed to auto-skip sho load", e);
                        }
                    }).start();
                }
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ProfitTrackerHUD.render(context);
            CollectionTracker.render(context);
            CommissionHUD.render(context);
            //? if isCheat {
            AutoClickerHUD.render(context, client);
            //?}
            LobbyFinderHUD.render(context);
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CorpseESP.onWorldUnload();
            ShaftESP.onWorldUnload();
            //? if isCheat {
            AutoClickerManager.cleanup();
            InShaftClickManager.cleanup();
            ShaftClickerManager.cleanup();
            //?}

            config.loadFromGame();
            config.save();
        });

        LOGGER.info("[MiningqolClient] MiningQOL Mod initialized");
    }

    public static MiningConfig getConfig() {
        return config;
    }

    //? if isCheat {
    public static boolean tryHandleInvClickKey(net.minecraft.client.input.KeyInput input) {
        if (invClickKey != null && invClickKey.matchesKey(input)) {
            InventoryClickManager.start();
            return true;
        }
        return false;
    }
    //?}

    private static void getArmorStandData(FabricClientCommandSource source) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d eyePos = client.player.getCameraPosVec(1.0f);
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        Vec3d endPos = eyePos.add(lookVec.multiply(100.0)); // No range limit

        ArmorStandEntity closestStand = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ArmorStandEntity stand) {
                Box box = stand.getBoundingBox().expand(0.5);
                java.util.Optional<Vec3d> hit = box.raycast(eyePos, endPos);
                if (hit.isPresent()) {
                    double dist = eyePos.distanceTo(hit.get());
                    if (dist < closestDistance) {
                        closestDistance = dist;
                        closestStand = stand;
                    }
                }
            }
        }

        if (closestStand == null) {
            source.sendFeedback(Text.literal("\u00A7cNo armor stand found in view"));
            return;
        }

        source.sendFeedback(Text.literal("\u00A7a=== Armor Stand Data ==="));
        source.sendFeedback(Text.literal("\u00A7ePosition: \u00A7f" + String.format("[%.2f, %.2f, %.2f]",
            closestStand.getX(), closestStand.getY(), closestStand.getZ())));
        source.sendFeedback(Text.literal("\u00A7eCustom Name: \u00A7f" +
            (closestStand.hasCustomName() ? closestStand.getCustomName().getString() : "None")));
        source.sendFeedback(Text.literal("\u00A7eInvisible: \u00A7f" + closestStand.isInvisible()));
        source.sendFeedback(Text.literal("\u00A7eSmall: \u00A7f" + closestStand.isSmall()));
        source.sendFeedback(Text.literal("\u00A7eMarker: \u00A7f" + closestStand.isMarker()));
        source.sendFeedback(Text.literal("\u00A7eNo Gravity: \u00A7f" + closestStand.hasNoGravity()));

        source.sendFeedback(Text.literal("\u00A7e--- Equipment ---"));
        ItemStack head = closestStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        ItemStack chest = closestStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        ItemStack legs = closestStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS);
        ItemStack feet = closestStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET);
        ItemStack mainHand = closestStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.MAINHAND);
        ItemStack offHand = closestStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.OFFHAND);

        if (!head.isEmpty()) source.sendFeedback(Text.literal("\u00A77Head: \u00A7f" + head.getName().getString()));
        if (!chest.isEmpty()) source.sendFeedback(Text.literal("\u00A77Chest: \u00A7f" + chest.getName().getString()));
        if (!legs.isEmpty()) source.sendFeedback(Text.literal("\u00A77Legs: \u00A7f" + legs.getName().getString()));
        if (!feet.isEmpty()) source.sendFeedback(Text.literal("\u00A77Feet: \u00A7f" + feet.getName().getString()));
        if (!mainHand.isEmpty()) source.sendFeedback(Text.literal("\u00A77Main Hand: \u00A7f" + mainHand.getName().getString()));
        if (!offHand.isEmpty()) source.sendFeedback(Text.literal("\u00A77Off Hand: \u00A7f" + offHand.getName().getString()));
    }

    private static void dumpNearbyEntities() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        client.player.sendMessage(Text.literal("\u00A7e=== Nearby Entities (20 blocks) ==="), false);

        int count = 0;
        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (entity.squaredDistanceTo(client.player) > 400) continue; // 20 blocks

            String type = entity.getClass().getSimpleName();
            String name = entity.hasCustomName() ? entity.getCustomName().getString() : "-";
            String cleanName = name.replaceAll("\u00A7.", "").trim();

            StringBuilder info = new StringBuilder();
            info.append("\u00A76").append(type);
            info.append(" \u00A77| \u00A7f").append(cleanName);

            if (entity instanceof ArmorStandEntity stand) {
                info.append(" \u00A77| inv=").append(stand.isInvisible());
                info.append(" mkr=").append(stand.isMarker());
            }

            info.append(String.format(" \u00A78[%.0f, %.0f, %.0f]",
                entity.getX(), entity.getY(), entity.getZ()));

            client.player.sendMessage(Text.literal(info.toString()), false);
            count++;
        }

        client.player.sendMessage(Text.literal("\u00A7e=== " + count + " entities found ==="), false);
    }
}
