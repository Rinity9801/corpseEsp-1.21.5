package forfun.miningqol.client.utils.render;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;

/** Optional compatibility bridge that keeps tracked ESP entities visible to EntityCulling. */
public final class EntityCullingCompat {
    private static boolean resolved;
    private static Class<?> cullable;
    private static Method setCulled;
    private static Method setTimeout;

    private EntityCullingCompat() {}

    public static void forceVisible(Entity entity) {
        if (!resolved) {
            resolved = true;
            try {
                cullable = Class.forName("dev.tr7zw.entityculling.versionless.access.Cullable");
                setCulled = cullable.getMethod("setCulled", boolean.class);
                setTimeout = cullable.getMethod("setTimeout");
            } catch (Exception ignored) {
                cullable = null;
            }
        }
        if (cullable == null || !cullable.isInstance(entity)) return;
        try {
            setCulled.invoke(entity, false);
            setTimeout.invoke(entity);
        } catch (Exception ignored) {
        }
    }
}
