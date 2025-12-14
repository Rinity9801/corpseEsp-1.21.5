package forfun.miningqol.mixin.client;

import forfun.miningqol.client.NameHider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;


@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(
        method = "getTooltip",
        at = @At("RETURN"),
        cancellable = true
    )
    private void modifyTooltip(Item.TooltipContext context, net.minecraft.entity.player.PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        if (!NameHider.isEnabled()) {
            return;
        }

        List<Text> tooltip = cir.getReturnValue();
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }

        
        List<Text> modifiedTooltip = new java.util.ArrayList<>();
        for (Text line : tooltip) {
            modifiedTooltip.add(NameHider.processTextRecursive(line));
        }

        cir.setReturnValue(modifiedTooltip);
    }
}
