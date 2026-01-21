package forfun.miningqol.mixin.client;

import forfun.miningqol.client.profit.BlockTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class ClientPlayerInventoryMixin {

    @Inject(method = "setStack", at = @At("HEAD"))
    private void onSetStack(int slot, ItemStack stack, CallbackInfo ci) {
        PlayerInventory inventory = (PlayerInventory)(Object)this;

        // Only track for client player
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || inventory.player != client.player) {
            return;
        }

        // Get current stack in slot
        ItemStack currentStack = inventory.getStack(slot);

        // If we're adding items (not removing)
        if (!stack.isEmpty()) {
            String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
            int newCount = stack.getCount();
            int oldCount = currentStack.isEmpty() ? 0 :
                          (ItemStack.areItemsEqual(currentStack, stack) ? currentStack.getCount() : 0);

            int added = newCount - oldCount;
            if (added > 0) {
                BlockTracker.onInventoryItemAdd(itemId, stack.getName().getString(), added);
            }
        }
    }
}
