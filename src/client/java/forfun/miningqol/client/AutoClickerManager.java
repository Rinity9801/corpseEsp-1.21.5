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

    // Fire the sequence when the pickaxe cooldown hits 0. Armed while a cooldown is active.
    private static boolean armed = false;
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
            armed = false;
            waitingForCooldownStart = false;

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
                waitingForCooldownStart = false;
                armed = false;
            } else {
                // Ability is on cooldown — arm so we fire when the HUD's interpolated cooldown hits 0
                armed = true;
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

        boolean currentlyOnCooldown = PickaxeCooldownHUD.isOnCooldown();
        double interpolatedCooldown = PickaxeCooldownHUD.getInterpolatedCooldown();

        long currentTime = System.currentTimeMillis();
        boolean canStartNewSequence = (currentTime - lastSequenceEndTime) >= MIN_SEQUENCE_INTERVAL_MS;

        // After a sequence, wait for the cooldown to refresh before re-arming.
        // The cooldown starts mid-sequence (when the drill right-click goes through),
        // so wasOnCooldown is already true at sequence end — there's no rising edge
        // to wait for. Detect the refresh by waiting for the HUD to report a high
        // cooldown (>5s, well below Maniac Miner's ~16-30s but above any residual
        // from the previous cycle).
        if (waitingForCooldownStart) {
            if (currentlyOnCooldown && interpolatedCooldown > 5.0) {
                waitingForCooldownStart = false;
                armed = true;
            } else if (!currentlyOnCooldown && wasOnCooldown) {
                // Ability became ready without a refresh (sequence failed to trigger).
                // Exit the waiting state so the rising-edge branch below can re-arm.
                waitingForCooldownStart = false;
            }
            wasOnCooldown = currentlyOnCooldown;

            // Still allow mining while waiting for next cooldown
            if (enabled) {
                int currentSlot = getSelectedSlot(client);
                client.options.attackKey.setPressed(currentSlot == expectedSlot);
            }
            return;
        }

        // Arm on the rising edge of a new cooldown (e.g. enabled while ready, then mined manually)
        if (currentlyOnCooldown && !wasOnCooldown && !armed && canStartNewSequence) {
            armed = true;
        }

        // Fire when the HUD's interpolated cooldown reaches 0
        if (!inSequence && enabled && armed && canStartNewSequence && interpolatedCooldown <= 0) {
            inSequence = true;
            sequenceStep = 0;
            sequenceTickCounter = 0;
            armed = false;
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
                armed = false;
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
