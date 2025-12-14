package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;

public class AutoClickerManager {
    private static boolean enabled = false;
    private static boolean inSequence = false;
    private static int sequenceStep = 0;
    private static int sequenceTickCounter = 0;
    private static boolean firstEnable = true;
    private static int expectedSlot = 0;
    private static boolean enableRodSwap = true;
    private static boolean enableSecondDrill = false;
    private static int secondDrillSlot = 3;
    private static boolean wasOnCooldown = true;

    // Internal timer for more responsive triggering
    private static int internalTickCounter = 0;
    private static int targetCooldownTicks = 0;
    private static boolean timerActive = false;

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) {
            inSequence = false;
            sequenceStep = 0;
            sequenceTickCounter = 0;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.options.attackKey.setPressed(false);
                client.options.useKey.setPressed(false);
            }
        } else {
            // Check if ability is currently ready
            boolean abilityIsReady = !PickaxeCooldownHUD.isOnCooldown();

            if (firstEnable || abilityIsReady) {
                // If first enable OR ability is ready, trigger immediately
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                firstEnable = false;
            }
            wasOnCooldown = PickaxeCooldownHUD.isOnCooldown();
        }
    }

    public static void setEnabled(boolean value) {
        if (enabled != value) {
            toggle();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double getRemainingSeconds() {
        if (firstEnable) {
            return 0;
        }
        // Use internal timer when active
        if (timerActive && targetCooldownTicks > 0) {
            int remainingTicks = targetCooldownTicks - internalTickCounter;
            return Math.max(0, remainingTicks / 20.0);
        }
        // Fall back to scoreboard when timer not active
        return PickaxeCooldownHUD.getInterpolatedCooldown();
    }

    public static void setMiningSlot(int slot) {
        expectedSlot = slot;
    }

    public static int getMiningSlot() {
        return expectedSlot;
    }

    public static void setEnableRodSwap(boolean value) {
        enableRodSwap = value;
    }

    public static boolean isRodSwapEnabled() {
        return enableRodSwap;
    }

    public static void setEnableSecondDrill(boolean value) {
        enableSecondDrill = value;
    }

    public static boolean isSecondDrillEnabled() {
        return enableSecondDrill;
    }

    public static void setSecondDrillSlot(int slot) {
        secondDrillSlot = slot;
    }

    public static int getSecondDrillSlot() {
        return secondDrillSlot;
    }

    private static int findFishingRodSlot(MinecraftClient client) {
        if (client.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof FishingRodItem) {
                return i;
            }
        }
        return -1;
    }

    private static int getSelectedSlot(MinecraftClient client) {
        if (client.player == null) return 0;
        return client.player.getInventory().getSelectedSlot();
    }

    private static void setSelectedSlot(MinecraftClient client, int slot) {
        if (client.player == null) return;
        client.player.getInventory().setSelectedSlot(slot);
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        // Get scoreboard cooldown info
        boolean currentlyOnCooldown = PickaxeCooldownHUD.isOnCooldown();
        double scoreboardCooldown = PickaxeCooldownHUD.getCurrentCooldown();
        double interpolatedCooldown = PickaxeCooldownHUD.getInterpolatedCooldown();

        // When cooldown starts, capture the duration and start our internal timer
        if (currentlyOnCooldown && !wasOnCooldown) {
            targetCooldownTicks = (int) (scoreboardCooldown * 20);
            internalTickCounter = 0;
            timerActive = true;
        }

        // Fallback: if on cooldown but timer not active, sync it
        if (currentlyOnCooldown && !timerActive && !inSequence && scoreboardCooldown > 0) {
            targetCooldownTicks = (int) (scoreboardCooldown * 20);
            internalTickCounter = 0;
            timerActive = true;
        }

        // Update internal timer
        if (timerActive && !inSequence) {
            internalTickCounter++;
        }

        // Trigger based on our internal timer (more responsive)
        if (!inSequence && enabled && timerActive) {
            if (internalTickCounter >= targetCooldownTicks) {
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                timerActive = false;
            }
        }

        // Fallback trigger: if scoreboard says ready and no timer active, trigger
        if (!inSequence && enabled && !timerActive && !currentlyOnCooldown && interpolatedCooldown <= 0 && wasOnCooldown) {
            inSequence = true;
            sequenceStep = 0;
            sequenceTickCounter = 0;
        }

        wasOnCooldown = currentlyOnCooldown;

        if (!enabled) {
            return;
        }

        int currentSlot = getSelectedSlot(client);

        if (inSequence) {
            client.options.attackKey.setPressed(false);
            handleManiacMinerSequence(client);
        } else {
            client.options.attackKey.setPressed(currentSlot == expectedSlot);
        }
    }

    private static void handleManiacMinerSequence(MinecraftClient client) {
        sequenceTickCounter++;

        switch (sequenceStep) {
            case 0:
                if (enableRodSwap) {
                    int rodSlot = findFishingRodSlot(client);
                    if (rodSlot != -1) {
                        setSelectedSlot(client, rodSlot);
                        sequenceStep++;
                    } else {
                        sequenceStep = 4;
                    }
                } else {
                    sequenceStep = 4;
                }
                sequenceTickCounter = 0;
                break;

            case 1, 5:
                if (sequenceTickCounter >= 2) {
                    sequenceStep++;
                    sequenceTickCounter = 0;
                }
                break;

            case 2, 6, 10:
                client.options.useKey.setPressed(true);
                if (sequenceTickCounter >= 3) {
                    client.options.useKey.setPressed(false);
                    sequenceStep++;
                    sequenceTickCounter = 0;
                }
                break;

            case 3, 7, 9:
                if (sequenceTickCounter >= 3) {
                    sequenceStep++;
                    sequenceTickCounter = 0;
                }
                break;

            case 4:
                if (enableSecondDrill) {
                    setSelectedSlot(client, secondDrillSlot);
                    sequenceStep++;
                } else {
                    sequenceStep = 8;
                }
                sequenceTickCounter = 0;
                break;

            case 8:
                setSelectedSlot(client, expectedSlot);
                sequenceStep++;
                sequenceTickCounter = 0;
                break;

            case 11:
                inSequence = false;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                break;
        }
    }

    public static void cleanup() {
        if (enabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.options.attackKey.setPressed(false);
            client.options.useKey.setPressed(false);
        }
    }
}
