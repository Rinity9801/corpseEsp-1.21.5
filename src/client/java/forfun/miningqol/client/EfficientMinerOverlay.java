package forfun.miningqol.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class EfficientMinerOverlay {
    private static boolean enabled = false;
    private static boolean useOldHeatmap = false;
    private static final List<BlockData> blocks = new ArrayList<>();

    private static final String[] TARGET_BLOCKS = {
        "minecraft:clay",
        "minecraft:red_sandstone"
    };

    private static final String[] AIR_TYPES = {
        "minecraft:air",
        "minecraft:cave_air",
        "minecraft:void_air",
        "minecraft:snow"
    };

    private static class BlockData {
        int x, y, z;
        int priority;

        BlockData(int x, int y, int z, int priority) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.priority = priority;
        }
    }

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setUseOldHeatmap(boolean useOld) {
        useOldHeatmap = useOld;
    }

    public static boolean isUsingOldHeatmap() {
        return useOldHeatmap;
    }

    public static void tick() {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        blocks.clear();

        int pX = (int) Math.floor(client.player.getX());
        int pY = (int) Math.floor(client.player.getY());
        int pZ = (int) Math.floor(client.player.getZ());

        for (int x = pX - 6; x <= pX + 6; x++) {
            for (int y = pY - 6; y <= pY + 6; y++) {
                for (int z = pZ - 6; z <= pZ + 6; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isTargetBlock(client.world, pos) && isVisible(client.world, pos)) {
                        int priority = calculatePriority(client.world, pos);
                        blocks.add(new BlockData(x, y, z, priority));
                    }
                }
            }
        }
    }

    public static void render(MatrixStack matrices, net.minecraft.client.render.Camera camera) {
        if (!enabled || blocks.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        for (BlockData block : blocks) {
            float[] color = getColorForPriority(block.priority);
            float alpha = 0.1f + (block.priority / 10.0f);

            double minX = block.x - cameraX;
            double minY = block.y - cameraY - 0.001;
            double minZ = block.z - cameraZ;
            double maxX = block.x + 1.001 - cameraX;
            double maxY = block.y + 1.002 - cameraY;
            double maxZ = block.z + 1.001 - cameraZ;

            
            DebugRenderer.drawBox(matrices, immediate,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                color[0], color[1], color[2], alpha * 0.5f);
            immediate.draw();
        }
    }

    private static boolean isTargetBlock(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        String blockId = state.getBlock().getTranslationKey();

        for (String target : TARGET_BLOCKS) {
            if (blockId.contains(target.replace("minecraft:", ""))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVisible(World world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock().getTranslationKey().contains("bedrock")) {
            return false;
        }

        
        BlockPos[] adjacent = {
            pos.up(),    
            pos.down(),  
            pos.east(),  
            pos.west(),  
            pos.north(), 
            pos.south()  
        };

        for (BlockPos adjPos : adjacent) {
            String blockId = world.getBlockState(adjPos).getBlock().getTranslationKey();
            for (String airType : AIR_TYPES) {
                if (blockId.contains(airType.replace("minecraft:", ""))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int calculatePriority(World world, BlockPos pos) {
        int priority = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    if (isTargetBlock(world, checkPos)) {
                        priority++;
                    }
                }
            }
        }

        return Math.min(priority, 9);
    }

    private static float[] getColorForPriority(int priority) {
        if (priority >= 10) priority = 1;

        if (useOldHeatmap) {
            
            return switch (priority) {
                case 1 -> new float[]{20/255f, 90/255f, 38/255f};    
                case 2 -> new float[]{42/255f, 230/255f, 92/255f};   
                case 3 -> new float[]{180/255f, 252/255f, 69/255f};  
                case 4 -> new float[]{180/255f, 177/255f, 31/255f};  
                case 5 -> new float[]{180/255f, 31/255f, 45/255f};   
                case 6 -> new float[]{212/255f, 57/255f, 229/255f};  
                case 7 -> new float[]{89/255f, 33/255f, 95/255f};    
                case 8 -> new float[]{62/255f, 56/255f, 216/255f};   
                default -> new float[]{0f, 0f, 0f};
            };
        } else {
            
            if (priority < 3) {
                return new float[]{20/255f, 90/255f, 38/255f};      
            } else if (priority < 5) {
                return new float[]{145/255f, 23/255f, 23/255f};     
            } else if (priority < 7) {
                return new float[]{104/255f, 210/255f, 249/255f};   
            } else {
                return new float[]{49/255f, 41/255f, 165/255f};     
            }
        }
    }
}
