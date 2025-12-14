package forfun.miningqol.mixin.client;

import forfun.miningqol.client.NameHider;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text modifyChatMessage(Text message) {
        if (!NameHider.isEnabled() || message == null) {
            return message;
        }

        return NameHider.processTextRecursive(message);
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Text modifySimpleChatMessage(Text message) {
        if (!NameHider.isEnabled() || message == null) {
            return message;
        }

        return NameHider.processTextRecursive(message);
    }
}
