package forfun.miningqol.client;

/**
 * One thing worth drawing, in world coordinates — what the ESP trackers hand to
 * {@link EspFrameServer} for the external overlay.
 *
 * @param type "corpse", "littlefoot" or "mob" (the overlay picks styling from this)
 * @param rgb  packed 0xRRGGBB, so the overlay matches the in-game colours
 */
public record EspTarget(double x, double y, double z, String name, String type, int rgb) {

    public static int packColor(float[] color) {
        int r = Math.round(Math.max(0f, Math.min(1f, color[0])) * 255f);
        int g = Math.round(Math.max(0f, Math.min(1f, color[1])) * 255f);
        int b = Math.round(Math.max(0f, Math.min(1f, color[2])) * 255f);
        return (r << 16) | (g << 8) | b;
    }
}
