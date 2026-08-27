package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CheatHooks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports every container click the client sends (the player's and the mod's alike) to the
 * cheat seam, so Auto Forge can record a craft by watching the player click through it.
 * Legit builds leave the hook null and this does nothing.
 */
@Mixin(MultiPlayerGameMode.class)
public class ContainerClickMixin {
    @Inject(method = "handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"))
    private void miningqol$onContainerClick(int containerId, int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
        CheatHooks.ContainerClickListener hook = CheatHooks.onContainerClick;
        if (hook != null) hook.onContainerClick(containerId, slot, button, input);
    }
}
