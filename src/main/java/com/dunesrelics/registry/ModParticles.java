package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DunesRelics.MODID);

    /** Wind-blown grains of sand, used by sandstorms and the Scepter of Sands. */
    public static final RegistryObject<SimpleParticleType> SAND_GUST = PARTICLES.register("sand_gust", () -> new SimpleParticleType(true));

    /** Foam drifting on the surface of running water. */
    public static final RegistryObject<SimpleParticleType> FOAM = PARTICLES.register("foam", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
