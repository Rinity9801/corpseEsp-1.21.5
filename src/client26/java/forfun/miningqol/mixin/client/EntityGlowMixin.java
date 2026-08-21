package forfun.miningqol.mixin.client;

import forfun.miningqol.client.EntityGlowESP;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void miningqol$forceTrackedEntityGlow(CallbackInfoReturnable<Boolean> cir) {
        if (EntityGlowESP.shouldGlow((Entity) (Object) this)) cir.setReturnValue(true);
    }
}
