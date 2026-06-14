package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;

public class InShaftClickManager {
    private static boolean enabled = false;
    private static boolean showToggleMessage = true;
    private static boolean inSequence = false;
    private static int sequenceStep = 0;
    private static int sequenceTickCounter = 0;
    private static boolean firstEnable = true;
    private static int miningSlot = 0;
    private static boolean enableRodSwap = true;
    private static int secondDrillSlot = 3;
    private static int mainDrillDelay = 3;
    private static int secondDrillDelay = 3;
    private static int coldThreshold = 50;
    private static boolean wasColdMode = false;
    private static boolean wasOnCooldown = true;

    // Internal timer for more responsive triggering
    private static int internalTickCounter = 0;
    private static int targetCooldownTicks = 0;
    private static boolean timerActive = false;
    private static boolean waitingForCooldownStart = false;

    // Prevent double triggering
    private static long lastSequenceEndTime = 0;
    private static final long MIN_SEQUENCE_INTERVAL_MS = 5000;

    public static void toggle() {
        enabled = !enabled;
        if (showToggleMessage) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.literal(
                    enabled ? "\u00A76[MQO] \u00A7aIn Shaft Click enabled" : "\u00A76[MQO] \u00A7cIn Shaft Click disabled"), false);
            }
        }
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
            boolean abilityIsReady = !PickaxeCooldownHUD.isOnCooldown();
            double currentCooldown = PickaxeCooldownHUD.getCurrentCooldown();

            if (firstEnable || abilityIsReady) {
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                firstEnable = false;
                waitingForCooldownStart = false;
                timerActive = false;
            } else if (currentCooldown > 0) {
                targetCooldownTicks = (int) (currentCooldown * 20) + 20;
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
        if (firstEnable) return 0;
        if (timerActive && targetCooldownTicks > 0) {
            int remainingTicks = targetCooldownTicks - internalTickCounter;
            return Math.max(0, remainingTicks / 20.0);
        }
        return PickaxeCooldownHUD.getInterpolatedCooldown();
    }

    public static void setShowToggleMessage(boolean value) { showToggleMessage = value; }
    public static boolean isShowToggleMessage() { return showToggleMessage; }

    public static void setMiningSlot(int slot) { miningSlot = slot; }
    public static int getMiningSlot() { return miningSlot; }

    public static void setEnableRodSwap(boolean value) { enableRodSwap = value; }
    public static boolean isRodSwapEnabled() { return enableRodSwap; }

    public static void setSecondDrillSlot(int slot) { secondDrillSlot = slot; }
    public static int getSecondDrillSlot() { return secondDrillSlot; }

    public static void setMainDrillDelay(int ticks) { mainDrillDelay = ticks; }
    public static int getMainDrillDelay() { return mainDrillDelay; }

    public static void setSecondDrillDelay(int ticks) { secondDrillDelay = ticks; }
    public static int getSecondDrillDelay() { return secondDrillDelay; }

    public static void setColdThreshold(int value) { coldThreshold = value; }
    public static int getColdThreshold() { return coldThreshold; }

    private static boolean isColdMode() {
        return ColdTracker.getCold() >= coldThreshold;
    }

    /**
     * Returns the slot that should be used for mining right now.
     * In cold mode, mines on the second drill slot instead.
     */
    private static int getActiveMiningSlot() {
        return isColdMode() ? secondDrillSlot : miningSlot;
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
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        boolean currentlyOnCooldown = PickaxeCooldownHUD.isOnCooldown();
        double scoreboardCooldown = PickaxeCooldownHUD.getCurrentCooldown();

        long currentTime = System.currentTimeMillis();
        boolean canStartNewSequence = (currentTime - lastSequenceEndTime) >= MIN_SEQUENCE_INTERVAL_MS;

        if (waitingForCooldownStart) {
            if (currentlyOnCooldown && scoreboardCooldown > 10.0) {
                waitingForCooldownStart = false;
                targetCooldownTicks = (int) (scoreboardCooldown * 20) + 20;
                internalTickCounter = 0;
                timerActive = true;
            }
            wasOnCooldown = currentlyOnCooldown;

            if (enabled) {
                int currentSlot = getSelectedSlot(client);
                client.options.attackKey.setPressed(currentSlot == getActiveMiningSlot());
            }
            return;
        }

        if (currentlyOnCooldown && !wasOnCooldown && !timerActive && canStartNewSequence) {
            targetCooldownTicks = (int) (scoreboardCooldown * 20) + 20;
            internalTickCounter = 0;
            timerActive = true;
        }

        if (timerActive && !inSequence) {
            internalTickCounter++;
        }

        if (!inSequence && enabled && timerActive && canStartNewSequence) {
            if (internalTickCounter >= targetCooldownTicks) {
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                timerActive = false;
            }
        }

        wasOnCooldown = currentlyOnCooldown;

        if (!enabled) return;

        // Detect cold threshold crossing and auto-swap to the correct slot
        boolean coldMode = isColdMode();
        if (coldMode != wasColdMode) {
            wasColdMode = coldMode;
            if (!inSequence) {
                int targetSlot = getActiveMiningSlot();
                setSelectedSlot(client, targetSlot);
            }
        }

        int currentSlot = getSelectedSlot(client);

        if (inSequence) {
            client.options.attackKey.setPressed(false);
            handleSequence(client);
        } else {
            client.options.attackKey.setPressed(currentSlot == getActiveMiningSlot());
        }
    }

    /**
     * Ability sequence:
     * rod -> active mining slot -> wait -> second drill -> wait -> right click -> back to active mining slot
     *
     * getActiveMiningSlot() returns secondDrillSlot when cold >= threshold,
     * so the sequence naturally skips the swap when in cold mode.
     */
    private static void handleSequence(MinecraftClient client) {
        sequenceTickCounter++;
        int activeSlot = getActiveMiningSlot();

        switch (sequenceStep) {
            case 0: // Switch to rod or skip
                if (enableRodSwap) {
                    int rodSlot = findFishingRodSlot(client);
                    if (rodSlot != -1) {
                        setSelectedSlot(client, rodSlot);
                        sequenceStep = 1;
                    } else {
                        sequenceStep = 3;
                    }
                } else {
                    sequenceStep = 3;
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

            case 3: // Switch to active mining slot
                setSelectedSlot(client, activeSlot);
                sequenceStep = 4;
                sequenceTickCounter = 0;
                break;

            case 4: // Wait mainDrillDelay ticks
                if (sequenceTickCounter >= mainDrillDelay) {
                    sequenceStep = 5;
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

            case 8: // Switch back to active mining slot
                setSelectedSlot(client, activeSlot);
                sequenceStep = 9;
                sequenceTickCounter = 0;
                break;

            case 9: // End
                inSequence = false;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                lastSequenceEndTime = System.currentTimeMillis();
                timerActive = false;
                waitingForCooldownStart = true;
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
