package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CritParticleDrop;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Rewrites the spawn y of mining crit particles — see {@link CritParticleDrop}.
 *
 * <p>ordinal 1 picks {@code y} out of the method's doubles (x, y, z, vx, vy, vz); the
 * remaining parameters are captured so the particle type and position can be checked
 * before deciding to move anything.
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @ModifyVariable(method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)"
                        + "Lnet/minecraft/client/particle/Particle;",
                    at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double miningqol$dropCritParticle(double y, ParticleOptions options,
                                              double x, double originalY, double z,
                                              double vx, double vy, double vz) {
        return CritParticleDrop.adjustY(options, x, y, z);
    }
}
