package forfun.miningqol.client.waypoints;

import net.minecraft.core.BlockPos;

public class OrderedWaypoint {
    private BlockPos position;
    private int index;
    /** Reached by etherwarping onto it (sneak + right-click with an AOTV/AOTE) rather than walking. */
    private boolean etherwarp;

    public OrderedWaypoint(BlockPos position, int index) {
        this.position = position;
        this.index = index;
    }

    public boolean isEtherwarp() {
        return etherwarp;
    }

    public void setEtherwarp(boolean etherwarp) {
        this.etherwarp = etherwarp;
    }

    public BlockPos getPosition() {
        return position;
    }

    public void setPosition(BlockPos position) {
        this.position = position;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double distanceTo(BlockPos other) {
        return Math.sqrt(position.distSqr(other));
    }

    public String toSaveString() {
        return index + ":" + position.getX() + "," + position.getY() + "," + position.getZ();
    }

    public static OrderedWaypoint fromSaveString(String str) {
        try {
            String[] parts = str.split(":");
            int idx = Integer.parseInt(parts[0]);
            String[] coords = parts[1].split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            int z = Integer.parseInt(coords[2]);
            return new OrderedWaypoint(new BlockPos(x, y, z), idx);
        } catch (Exception e) {
            return null;
        }
    }
}
