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
 * Client side of sandstorms: thick ochre fog, wind-blown sand and the howl of the wind.
 * Amber goggles cut through most of the fog.
 */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID, value = Dist.CLIENT)
public final class SandstormClient {
    private static final float STORM_FOG_DISTANCE = 20.0F;
    private static float intensity;
    private static float intensityO;
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
        if (rain > 0.0F && Sandstorm.isSandstormBiome(level, pos)) {
            float openness = level.canSeeSky(pos) ? 1.0F : level.getBrightness(LightLayer.SKY, pos) / 15.0F * 0.5F;
            target = rain * openness;
        }
        intensity += (target - intensity) * 0.04F;
        if (intensity < 0.001F) {
            intensity = 0.0F;
        }

        if (intensity > 0.05F) {
            spawnSand(level, player, level.random);
            if (sound == null || !minecraft.getSoundManager().isActive(sound)) {
                sound = new SandstormSound();
                minecraft.getSoundManager().play(sound);
            }
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
        event.setRed(Mth.lerp(strength, event.getRed(), 0.80F * light));
        event.setGreen(Mth.lerp(strength, event.getGreen(), 0.66F * light));
        event.setBlue(Mth.lerp(strength, event.getBlue(), 0.45F * light));
    }
}
