package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> QUICKSAND = ResourceKey.create(Registries.DAMAGE_TYPE, DunesRelics.id("quicksand"));

    private ModDamageTypes() {}

    public static DamageSource quicksand(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(QUICKSAND));
    }
}
