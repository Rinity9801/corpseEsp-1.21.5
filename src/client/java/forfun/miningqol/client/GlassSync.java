package forfun.miningqol.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlassSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("GlassSync");
    private static boolean enabled = false;

    public static void setEnabled(boolean enabled) {
        GlassSync.enabled = enabled;
        LOGGER.info("GlassSync " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isStainedGlass(BlockState state) {
        Block block = state.getBlock();
        return block instanceof StainedGlassBlock || block instanceof StainedGlassPaneBlock;
    }

    public static boolean isConnectedPane(BlockState state) {
        if (!(state.getBlock() instanceof PaneBlock)) return false;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (state.get(PaneBlock.FACING_PROPERTIES.get(dir))) {
                return true;
            }
        }
        return false;
    }

    public static BlockState withoutConnection(BlockState state, Direction dir) {
        return state.with(PaneBlock.FACING_PROPERTIES.get(dir), false);
    }

    public static BlockState asFullyConnected(BlockState state) {
        BlockState result = state;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            result = result.with(PaneBlock.FACING_PROPERTIES.get(dir), true);
        }
        return result;
    }
}
