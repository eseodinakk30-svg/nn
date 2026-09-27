package com.dunesrelics.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** The looping howl of the wind during a sandstorm; follows the listener and fades with the storm. */
public class SandstormSound extends AbstractTickableSoundInstance {
    public SandstormSound() {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = 0.5F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        float intensity = SandstormClient.getIntensity(1.0F);
        if (intensity < 0.02F) {
            this.stop();
            return;
        }
        this.volume = intensity * 0.35F;
        this.pitch = 0.45F + intensity * 0.15F;
    }
}
