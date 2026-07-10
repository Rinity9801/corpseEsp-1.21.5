package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CommClaimManager;
import forfun.miningqol.client.hotm.HotmChestScreen;
import forfun.miningqol.client.hotm.HotmPresetScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyInput;)Z", at = @At("HEAD"), cancellable = true)
    private void miningqol$onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        //? if isCheat {
        forfun.miningqol.client.MiningqolClient.tryHandleInvClickKey(input);
        //?}
        // While a comm-claim runs, swallow the player's keys so they can't derail it (Esc aborts).
        if (CommClaimManager.isRunning() && CommClaimManager.isBlockInput()) {
            if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                CommClaimManager.stop();
            } else {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z", at = @At("HEAD"), cancellable = true)
    private void miningqol$blockMouse(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (CommClaimManager.isRunning() && CommClaimManager.isBlockInput()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    private void miningqol$hideGui(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (CommClaimManager.isRunning() && CommClaimManager.isHideGui()) {
            ci.cancel();
        }
    }
}
