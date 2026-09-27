package com.dunesrelics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** A grain of sand carried by the wind. It keeps the velocity it was spawned with and fades out. */
public class SandGustParticle extends TextureSheetParticle {
    protected SandGustParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.xd = xd + (this.random.nextDouble() - 0.5D) * 0.05D;
        this.yd = yd + (this.random.nextDouble() - 0.5D) * 0.03D;
        this.zd = zd + (this.random.nextDouble() - 0.5D) * 0.05D;
        this.friction = 0.99F;
        this.gravity = 0.01F;
        this.lifetime = 30 + this.random.nextInt(40);
        this.quadSize = 0.04F + this.random.nextFloat() * 0.08F;
        float shade = 0.8F + this.random.nextFloat() * 0.2F;
        this.rCol = shade;
        this.gCol = shade * 0.86F;
        this.bCol = shade * 0.62F;
        this.alpha = 0.9F;
        this.hasPhysics = true;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float remaining = 1.0F - (float) this.age / this.lifetime;
        this.alpha = Math.min(0.9F, remaining * 2.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                 double xd, double yd, double zd) {
            return new SandGustParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
