package forfun.miningqol.mixin.client;

import forfun.miningqol.client.hotm.HotmChestScreen;
import forfun.miningqol.client.hotm.HotmPresetScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        String title = screen.getTitle().getString();

        if (slot == null) return;

        if (HotmChestScreen.isHotmScreen(title)) {
            if (HotmChestScreen.handleClick(slot.id, button)) {
                ci.cancel();
            }
        } else if (HotmPresetScreen.isPresetScreen(title)) {
            if (HotmPresetScreen.handleClick(slot.id, button)) {
                ci.cancel();
            }
        }
    }
}
