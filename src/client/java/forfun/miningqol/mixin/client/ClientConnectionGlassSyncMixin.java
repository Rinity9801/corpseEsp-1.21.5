package forfun.miningqol.mixin.client;

import forfun.miningqol.client.GlassSync;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionGlassSyncMixin {

    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        if (!GlassSync.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (packet instanceof BlockUpdateS2CPacket blockUpdatePacket) {
            BlockPos pos = blockUpdatePacket.getPos();
            BlockState oldState = mc.world.getBlockState(pos);
            BlockState newState = blockUpdatePacket.getState();

            // When stained glass is broken, update neighboring panes
            if (newState.isAir() && GlassSync.isStainedGlass(oldState)) {
                mc.execute(() -> {
                    for (Direction dir : Direction.Type.HORIZONTAL) {
                        BlockPos neighborPos = pos.offset(dir);
                        BlockState neighborState = mc.world.getBlockState(neighborPos);

                        if (neighborState.getBlock() instanceof PaneBlock) {
                            // Remove the connection to the broken block
                            BlockState updated = GlassSync.withoutConnection(neighborState, dir.getOpposite());

                            // If this would leave the pane with no connections, make it fully connected
                            if (!GlassSync.isConnectedPane(updated)) {
                                mc.world.setBlockState(neighborPos, GlassSync.asFullyConnected(neighborState));
                            } else {
                                mc.world.setBlockState(neighborPos, updated);
                            }
                        }
                    }
                });
            }
        }
    }
}
