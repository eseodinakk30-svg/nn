package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.Meerkat;
import com.dunesrelics.entity.Mummy;
import com.dunesrelics.entity.Pharaoh;
import com.dunesrelics.entity.Scarab;
import com.dunesrelics.entity.Scorpion;
import com.dunesrelics.entity.Vulture;
import com.dunesrelics.entity.volcanic.CinderWraith;
import com.dunesrelics.entity.volcanic.LavaCrab;
import com.dunesrelics.entity.volcanic.MagmaTitan;
import com.dunesrelics.entity.volcanic.Magmaling;
import com.dunesrelics.entity.volcanic.Salamander;
import com.dunesrelics.entity.volcanic.VolcanicBomb;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DunesRelics.MODID);

    public static final RegistryObject<EntityType<Mummy>> MUMMY = ENTITIES.register("mummy",
            () -> EntityType.Builder.of(Mummy::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8)
                    .build(DunesRelics.id("mummy").toString()));
    public static final RegistryObject<EntityType<Pharaoh>> PHARAOH = ENTITIES.register("pharaoh",
            () -> EntityType.Builder.of(Pharaoh::new, MobCategory.MONSTER).sized(0.7F, 2.25F).clientTrackingRange(10)
                    .build(DunesRelics.id("pharaoh").toString()));
    public static final RegistryObject<EntityType<Scorpion>> SCORPION = ENTITIES.register("scorpion",
            () -> EntityType.Builder.of(Scorpion::new, MobCategory.MONSTER).sized(1.2F, 0.6F).clientTrackingRange(8)
                    .build(DunesRelics.id("scorpion").toString()));
    public static final RegistryObject<EntityType<Scarab>> SCARAB = ENTITIES.register("scarab",
            () -> EntityType.Builder.of(Scarab::new, MobCategory.MONSTER).sized(0.45F, 0.3F).clientTrackingRange(8)
                    .build(DunesRelics.id("scarab").toString()));
    public static final RegistryObject<EntityType<Meerkat>> MEERKAT = ENTITIES.register("meerkat",
            () -> EntityType.Builder.of(Meerkat::new, MobCategory.CREATURE).sized(0.5F, 0.7F).clientTrackingRange(10)
                    .build(DunesRelics.id("meerkat").toString()));
    public static final RegistryObject<EntityType<Vulture>> VULTURE = ENTITIES.register("vulture",
            () -> EntityType.Builder.of(Vulture::new, MobCategory.CREATURE).sized(0.9F, 0.9F).clientTrackingRange(10)
                    .build(DunesRelics.id("vulture").toString()));

    // ---------------------------------------------------------------- Ash & Ember
    public static final RegistryObject<EntityType<MagmaTitan>> MAGMA_TITAN = ENTITIES.register("magma_titan",
            () -> EntityType.Builder.of(MagmaTitan::new, MobCategory.MONSTER).sized(1.6F, 3.4F).fireImmune().clientTrackingRange(12)
                    .build(DunesRelics.id("magma_titan").toString()));
    public static final RegistryObject<EntityType<Salamander>> SALAMANDER = ENTITIES.register("salamander",
            () -> EntityType.Builder.of(Salamander::new, MobCategory.CREATURE).sized(0.7F, 0.45F).fireImmune().clientTrackingRange(10)
                    .build(DunesRelics.id("salamander").toString()));
    public static final RegistryObject<EntityType<LavaCrab>> LAVA_CRAB = ENTITIES.register("lava_crab",
            () -> EntityType.Builder.of(LavaCrab::new, MobCategory.CREATURE).sized(0.8F, 0.5F).fireImmune().clientTrackingRange(10)
                    .build(DunesRelics.id("lava_crab").toString()));
    public static final RegistryObject<EntityType<Magmaling>> MAGMALING = ENTITIES.register("magmaling",
            () -> EntityType.Builder.of(Magmaling::new, MobCategory.MONSTER).sized(0.6F, 1.2F).fireImmune().clientTrackingRange(8)
                    .build(DunesRelics.id("magmaling").toString()));
    public static final RegistryObject<EntityType<CinderWraith>> CINDER_WRAITH = ENTITIES.register("cinder_wraith",
            () -> EntityType.Builder.of(CinderWraith::new, MobCategory.MONSTER).sized(0.6F, 1.4F).fireImmune().clientTrackingRange(8)
                    .build(DunesRelics.id("cinder_wraith").toString()));
    public static final RegistryObject<EntityType<VolcanicBomb>> VOLCANIC_BOMB = ENTITIES.register("volcanic_bomb",
            () -> EntityType.Builder.<VolcanicBomb>of(VolcanicBomb::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8)
                    .updateInterval(10).build(DunesRelics.id("volcanic_bomb").toString()));

    private ModEntities() {}
}
