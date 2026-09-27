package com.dunesrelics.client;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.registry.ModItems;
import com.dunesrelics.registry.ModParticles;
import com.dunesrelics.sandstorm.Sandstorm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side of desert and volcanic weather: sandstorms (ochre fog, wind-blown sand, the howl of the wind)
 * and ashfall (grey fog and drifting ash; during eruptions an orange glow, falling embers and distant rumbling).
 * Amber goggles cut through most of the fog.
 */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID, value = Dist.CLIENT)
public final class SandstormClient {
    private static final float STORM_FOG_DISTANCE = 20.0F;
    private static float intensity;
    private static float intensityO;
    /** How much of the current storm is volcanic ash (0) versus desert sand (1). */
    private static float ashMix;
    /** 0..1, how strongly a volcano is erupting around the player. */
    private static float eruption;
    private static SandstormSound sound;

    private SandstormClient() {}

    public static float getIntensity(float partialTick) {
        return Mth.lerp(partialTick, intensityO, intensity);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            intensity = intensityO = 0.0F;
            sound = null;
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        intensityO = intensity;
        BlockPos pos = BlockPos.containing(player.getEyePosition());
        float target = 0.0F;
        float rain = level.getRainLevel(1.0F);
        boolean sandy = Sandstorm.isSandstormBiome(level, pos);
        boolean ashen = com.dunesrelics.sandstorm.VolcanicWeather.isAshfallBiome(level, pos);
        if (rain > 0.0F && (sandy || ashen)) {
            float openness = level.canSeeSky(pos) ? 1.0F : level.getBrightness(LightLayer.SKY, pos) / 15.0F * 0.5F;
            target = rain * openness * (ashen ? 0.75F : 1.0F);
        }
        intensity += (target - intensity) * 0.04F;
        if (intensity < 0.001F) {
            intensity = 0.0F;
        }
        if (sandy || ashen) {
            ashMix += ((ashen ? 1.0F : 0.0F) - ashMix) * 0.1F;
        }
        float eruptionTarget = ashen ? level.getThunderLevel(1.0F) : 0.0F;
        eruption += (eruptionTarget - eruption) * 0.03F;

        if (intensity > 0.05F) {
            if (ashMix > 0.5F) {
                spawnAsh(level, player, level.random);
            } else {
                spawnSand(level, player, level.random);
            }
            if (sound == null || !minecraft.getSoundManager().isActive(sound)) {
                sound = new SandstormSound();
                minecraft.getSoundManager().play(sound);
            }
        }
    }

    private static void spawnAsh(ClientLevel level, LocalPlayer player, RandomSource random) {
        int count = Mth.ceil(intensity * 30.0F);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5D) * 24.0D;
            double y = player.getY() + random.nextDouble() * 12.0D - 2.0D;
            double z = player.getZ() + (random.nextDouble() - 0.5D) * 24.0D;
            pos.set(x, y, z);
            if (!level.canSeeSky(pos) || !level.getBlockState(pos).isAir()) {
                continue;
            }
            level.addParticle(random.nextInt(3) == 0 ? net.minecraft.core.particles.ParticleTypes.WHITE_ASH
                    : net.minecraft.core.particles.ParticleTypes.ASH, x, y, z, 0.0D, -0.05D, 0.0D);
            if (eruption > 0.2F && random.nextFloat() < eruption * 0.15F) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_LAVA, x, y + 6.0D, z, 0.0D, 0.0D, 0.0D);
            }
        }
        if (eruption > 0.3F && random.nextInt(160) == 0) {
            // A distant rumble from the volcano.
            level.playLocalSound(player.getX() + random.nextGaussian() * 40.0D, player.getY() + 20.0D,
                    player.getZ() + random.nextGaussian() * 40.0D, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.WEATHER, 2.5F * eruption, 0.3F + random.nextFloat() * 0.2F, false);
        }
    }

    private static void spawnSand(ClientLevel level, LocalPlayer player, RandomSource random) {
        // The wind slowly veers over the course of the day.
        float angle = (level.getDayTime() % 24000L) / 24000.0F * (float) Math.PI * 2.0F;
        double windX = Mth.cos(angle) * 0.55D;
        double windZ = Mth.sin(angle) * 0.55D;
        int count = Mth.ceil(intensity * 24.0F);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5D) * 28.0D - windX * 10.0D;
            double y = player.getY() + random.nextDouble() * 9.0D - 2.0D;
            double z = player.getZ() + (random.nextDouble() - 0.5D) * 28.0D - windZ * 10.0D;
            pos.set(x, y, z);
            if (!level.canSeeSky(pos) || !level.getBlockState(pos).isAir()) {
                continue;
            }
            double gust = 0.8D + random.nextDouble() * 0.5D;
            level.addParticle(ModParticles.SAND_GUST.get(), x, y, z, windX * gust, -0.02D, windZ * gust);
        }
    }

    private static float fogStrength(Entity camera, float partialTick) {
        float strength = getIntensity(partialTick);
        if (strength <= 0.0F || !(camera instanceof LivingEntity living)) {
            return strength;
        }
        if (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)) {
            return 0.0F;
        }
        if (living.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.AMBER_GOGGLES.get())) {
            strength *= 0.2F;
        }
        return strength;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float strength = fogStrength(event.getCamera().getEntity(), (float) event.getPartialTick());
        if (strength <= 0.01F) {
            return;
        }
        float far = Mth.lerp(strength, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), STORM_FOG_DISTANCE));
        float near = Mth.lerp(strength, event.getNearPlaneDistance(), 0.0F);
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(near);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float strength = fogStrength(event.getCamera().getEntity(), (float) event.getPartialTick()) * 0.9F;
        if (strength <= 0.01F) {
            return;
        }
        // Dim the dust at night so storms are not glowing.
        float light = Mth.clamp(event.getRed() + event.getGreen() + event.getBlue(), 0.25F, 1.0F);
        float r = Mth.lerp(ashMix, 0.80F, Mth.lerp(eruption, 0.36F, 0.55F));
        float g = Mth.lerp(ashMix, 0.66F, Mth.lerp(eruption, 0.34F, 0.26F));
        float b = Mth.lerp(ashMix, 0.45F, Mth.lerp(eruption, 0.33F, 0.16F));
        event.setRed(Mth.lerp(strength, event.getRed(), r * light));
        event.setGreen(Mth.lerp(strength, event.getGreen(), g * light));
        event.setBlue(Mth.lerp(strength, event.getBlue(), b * light));
    }
}
