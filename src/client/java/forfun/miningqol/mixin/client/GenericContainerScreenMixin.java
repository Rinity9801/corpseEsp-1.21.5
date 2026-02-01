package forfun.miningqol.mixin.client;

import forfun.miningqol.client.sacks.SackTracker;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GenericContainerScreen screen = (GenericContainerScreen) (Object) this;
        String title = screen.getTitle().getString();

        // Check if this is the Enchanted Mining Sack
        if (title.contains("Enchanted Mining Sack")) {
            // Parse sack contents (will be cached, so multiple calls are fine)
            SackTracker.parseEnchantedMiningSack(screen);
        }
    }
}
