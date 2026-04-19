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
    private static int mainDrillDelay = 3;
    private static int secondDrillDelay = 3;
    private static boolean wasOnCooldown = true;

    // Internal timer for more responsive triggering
    private static int internalTickCounter = 0;
    private static int targetCooldownTicks = 0;
    private static boolean timerActive = false;
    private static boolean waitingForCooldownStart = false; // Wait for next cooldown cycle

    // Prevent double triggering
    private static long lastSequenceEndTime = 0;
    private static final long MIN_SEQUENCE_INTERVAL_MS = 5000; // 5 seconds minimum between sequences

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) {
            inSequence = false;
            sequenceStep = 0;
            sequenceTickCounter = 0;
            timerActive = false;
            waitingForCooldownStart = false;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.options.attackKey.setPressed(false);
                client.options.useKey.setPressed(false);
            }
        } else {
            // Check if ability is currently ready
            boolean abilityIsReady = !PickaxeCooldownHUD.isOnCooldown();
            double currentCooldown = PickaxeCooldownHUD.getCurrentCooldown();

            if (firstEnable || abilityIsReady) {
                // If first enable OR ability is ready, trigger immediately
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                firstEnable = false;
                waitingForCooldownStart = false;
                timerActive = false;
            } else if (currentCooldown > 0) {
                // Ability is on cooldown, start tracking it
                targetCooldownTicks = (int) (currentCooldown * 20) + 20; // Add 1 second buffer
                internalTickCounter = 0;
                timerActive = true;
                waitingForCooldownStart = false;
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

    public static void setMainDrillDelay(int ticks) {
        mainDrillDelay = ticks;
    }

    public static int getMainDrillDelay() {
        return mainDrillDelay;
    }

    public static void setSecondDrillDelay(int ticks) {
        secondDrillDelay = ticks;
    }

    public static int getSecondDrillDelay() {
        return secondDrillDelay;
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

        // Check if enough time has passed since last sequence
        long currentTime = System.currentTimeMillis();
        boolean canStartNewSequence = (currentTime - lastSequenceEndTime) >= MIN_SEQUENCE_INTERVAL_MS;

        // After sequence ends, we wait for a NEW cooldown to start before tracking again
        if (waitingForCooldownStart) {
            if (currentlyOnCooldown && scoreboardCooldown > 10.0) {
                // New cooldown started, begin tracking
                waitingForCooldownStart = false;
                targetCooldownTicks = (int) (scoreboardCooldown * 20) + 20; // Add 1 second buffer
                internalTickCounter = 0;
                timerActive = true;
            }
            wasOnCooldown = currentlyOnCooldown;

            // Still allow mining while waiting for next cooldown
            if (enabled) {
                int currentSlot = getSelectedSlot(client);
                client.options.attackKey.setPressed(currentSlot == expectedSlot);
            }
            return;
        }

        // When cooldown starts, capture the duration and start our internal timer
        if (currentlyOnCooldown && !wasOnCooldown && !timerActive && canStartNewSequence) {
            targetCooldownTicks = (int) (scoreboardCooldown * 20) + 20; // Add 1 second buffer
            internalTickCounter = 0;
            timerActive = true;
        }

        // Update internal timer
        if (timerActive && !inSequence) {
            internalTickCounter++;
        }

        // Trigger based on our internal timer (more responsive)
        if (!inSequence && enabled && timerActive && canStartNewSequence) {
            if (internalTickCounter >= targetCooldownTicks) {
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                timerActive = false;
            }
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

        // Sequence:
        // 0: Switch to rod (or skip if no rod swap)
        // 1: Wait 2 ticks for rod switch
        // 2: Right click rod (2 ticks)
        // 3: Switch to main drill
        // 4: Wait mainDrillDelay ticks
        // 5: If second drill: switch to second drill; else: right click main drill
        // 6: Wait secondDrillDelay ticks (second drill only)
        // 7: Right click second drill (second drill only)
        // 8: Switch back to main drill (second drill only)
        // 9: End

        switch (sequenceStep) {
            case 0:
                // Switch to rod or skip
                if (enableRodSwap) {
                    int rodSlot = findFishingRodSlot(client);
                    if (rodSlot != -1) {
                        setSelectedSlot(client, rodSlot);
                        sequenceStep = 1;
                    } else {
                        sequenceStep = 3; // No rod found, skip to main drill
                    }
                } else {
                    sequenceStep = 3; // Rod swap disabled, skip to main drill
                }
                sequenceTickCounter = 0;
                break;

            case 1: // Wait before rod right click
                if (sequenceTickCounter >= 2) {
                    sequenceStep = 2;
                    sequenceTickCounter = 0;
                }
                break;

            case 2: // Right click rod
                client.options.useKey.setPressed(true);
                if (sequenceTickCounter >= 2) {
                    client.options.useKey.setPressed(false);
                    sequenceStep = 3;
                    sequenceTickCounter = 0;
                }
                break;

            case 3: // Switch to main drill
                setSelectedSlot(client, expectedSlot);
                sequenceStep = 4;
                sequenceTickCounter = 0;
                break;

            case 4: // Wait mainDrillDelay ticks
                if (sequenceTickCounter >= mainDrillDelay) {
                    if (enableSecondDrill) {
                        sequenceStep = 5; // Go to second drill
                    } else {
                        sequenceStep = 50; // Right click main drill ability
                    }
                    sequenceTickCounter = 0;
                }
                break;

            case 50: // Right click main drill ability (no second drill)
                client.options.useKey.setPressed(true);
                if (sequenceTickCounter >= 2) {
                    client.options.useKey.setPressed(false);
                    sequenceStep = 9; // End
                    sequenceTickCounter = 0;
                }
                break;

            case 5: // Switch to second drill
                setSelectedSlot(client, secondDrillSlot);
                sequenceStep = 6;
                sequenceTickCounter = 0;
                break;

            case 6: // Wait secondDrillDelay ticks
                if (sequenceTickCounter >= secondDrillDelay) {
                    sequenceStep = 7;
                    sequenceTickCounter = 0;
                }
                break;

            case 7: // Right click second drill
                client.options.useKey.setPressed(true);
                if (sequenceTickCounter >= 2) {
                    client.options.useKey.setPressed(false);
                    sequenceStep = 8;
                    sequenceTickCounter = 0;
                }
                break;

            case 8: // Switch back to main drill
                setSelectedSlot(client, expectedSlot);
                sequenceStep = 9;
                sequenceTickCounter = 0;
                break;

            case 9: // End sequence
                inSequence = false;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                lastSequenceEndTime = System.currentTimeMillis();
                timerActive = false;
                waitingForCooldownStart = true; // Wait for next cooldown cycle
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
