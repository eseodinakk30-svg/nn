package com.dunesrelics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** A fleck of foam floating on the surface with the current; it keeps its speed and fades away. */
public class FoamParticle extends TextureSheetParticle {
    protected FoamParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.xd = xd;
        this.yd = 0.0D;
        this.zd = zd;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 40 + this.random.nextInt(40);
        this.quadSize = 0.08F + this.random.nextFloat() * 0.1F;
        float shade = 0.9F + this.random.nextFloat() * 0.1F;
        this.rCol = shade;
        this.gCol = shade;
        this.bCol = 1.0F;
        this.alpha = 0.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) this.age / this.lifetime;
        this.alpha = Math.min(1.0F, Math.min(life * 5.0F, (1.0F - life) * 3.0F)) * 0.85F;
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
            return new FoamParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
