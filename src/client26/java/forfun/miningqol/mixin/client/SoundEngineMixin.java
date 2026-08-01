package forfun.miningqol.mixin.client;

import forfun.miningqol.client.SoundBlocker;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Single choke point for client sound playback — {@code playDelayed} and the tickable-sound
 * queue both end up here, so scanning and blocking only need this one hook.
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    // NOT at HEAD: play() resolves the instance itself (SoundInstance.resolve, then
    // getSound), and before that runs getPitch()/getVolume() NPE on the null sound field.
    // INVOKE_ASSIGN puts us just after the resolve+getSound store — sound populated,
    // stack empty, and still ahead of the channel acquisition, so cancelling is silent.
    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)"
                + "Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At(value = "INVOKE_ASSIGN",
                     target = "Lnet/minecraft/client/resources/sounds/SoundInstance;"
                            + "getSound()Lnet/minecraft/client/resources/sounds/Sound;",
                     ordinal = 0),
            cancellable = true)
    private void miningqol$scanAndBlock(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (SoundBlocker.handle(instance)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
