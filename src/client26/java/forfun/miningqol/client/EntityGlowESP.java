package forfun.miningqol.client;

import forfun.miningqol.client.utils.render.EntityCullingCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared vanilla-outline and Prisma custom-glow bridge for Shaft and Corpse ESP. */
public final class EntityGlowESP {
    private static boolean customGlowThisFrame = false;

    private EntityGlowESP() {}

    public static boolean isTarget(Entity entity) {
        return ShaftESP.isGlowTarget(entity) || CorpseESP.isGlowTarget(entity);
    }

    public static EntityEspMode modeFor(Entity entity) {
        if (CorpseESP.isGlowTarget(entity)) return CorpseESP.getRenderMode();
        if (ShaftESP.isGlowTarget(entity)) return ShaftESP.getRenderMode();
        return EntityEspMode.BOX;
    }

    public static boolean shouldGlow(Entity entity) {
        return isTarget(entity);
    }

    public static boolean shouldUseCustomGlow(Entity entity) {
        return modeFor(entity) == EntityEspMode.CUSTOM_GLOW;
    }

    public static boolean shouldChams(Entity entity) {
        return shouldUseCustomGlow(entity) && !isDirectlyVisible(entity);
    }

    public static int getOutlineColor(Entity entity) {
        float[] color = CorpseESP.isGlowTarget(entity)
            ? CorpseESP.getEspColor(entity)
            : ShaftESP.getEspColor(entity);
        int r = Math.round(clamp(color[0]) * 255.0f);
        int g = Math.round(clamp(color[1]) * 255.0f);
        int b = Math.round(clamp(color[2]) * 255.0f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static void markCustomGlowFrame() {
        customGlowThisFrame = true;
    }

    public static boolean consumeCustomGlowFrame() {
        boolean value = customGlowThisFrame;
        customGlowThisFrame = false;
        return value;
    }

    public static void forceVisible(Entity entity) {
        EntityCullingCompat.forceVisible(entity);
    }

    private static boolean isDirectlyVisible(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return false;
        Vec3 from = client.player.getEyePosition();
        return rayClear(client, from, entity.getEyePosition())
            && rayClear(client, from, entity.getBoundingBox().getCenter());
    }

    private static boolean rayClear(Minecraft client, Vec3 from, Vec3 to) {
        return client.level.clip(new ClipContext(
            from, to,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            client.player)).getType() == HitResult.Type.MISS;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
