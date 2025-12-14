package forfun.miningqol.mixin.client;

import forfun.miningqol.client.NameHider;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(
        method = "getPlayerName",
        at = @At("RETURN"),
        cancellable = true
    )
    private void modifyPlayerListName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!NameHider.isEnabled()) {
            return;
        }

        Text original = cir.getReturnValue();
        if (original != null) {
            cir.setReturnValue(NameHider.processTextRecursive(original));
        }
    }
}
