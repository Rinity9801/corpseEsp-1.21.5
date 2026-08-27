package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CommissionHUD;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Feeds container slot updates to the Commission HUD (SkyblockCollectionTracker's approach).
 *
 * <p>When a commission is claimed, Hypixel replaces that slot's item with the new commission.
 * Hooking the slot update itself — rather than re-reading the menu each frame and merging with
 * the tab list — is what makes a claim show up the instant the packet lands. {@code setItem}
 * covers single-slot packets; {@code initializeContents} covers the full-contents packet sent
 * when the menu (re)opens.
 */
@Mixin(AbstractContainerMenu.class)
public class ContainerMenuSlotMixin {
    @Inject(method = "setItem(IILnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void miningqol$onSetItem(int slot, int stateId, ItemStack stack, CallbackInfo ci) {
        CommissionHUD.onMenuSlotSet((AbstractContainerMenu) (Object) this, slot, stack);
    }

    @Inject(method = "initializeContents(ILjava/util/List;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void miningqol$onContents(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
        CommissionHUD.onMenuContents((AbstractContainerMenu) (Object) this);
    }
}
