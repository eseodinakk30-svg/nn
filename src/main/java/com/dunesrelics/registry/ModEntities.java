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
import com.dunesrelics.entity.world.Cannonball;
import com.dunesrelics.entity.world.Pirate;
import com.dunesrelics.entity.world.PirateCaptain;
import com.dunesrelics.entity.world.PirateGunner;
import com.dunesrelics.entity.world.Shade;
import com.dunesrelics.entity.world.Traveler;
import com.dunesrelics.entity.world.VillageWorker;
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

    // ---------------------------------------------------------------- Living World
    public static final RegistryObject<EntityType<Pirate>> PIRATE = ENTITIES.register("pirate",
            () -> EntityType.Builder.of(Pirate::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8)
                    .build(DunesRelics.id("pirate").toString()));
    public static final RegistryObject<EntityType<PirateGunner>> PIRATE_GUNNER = ENTITIES.register("pirate_gunner",
            () -> EntityType.Builder.of(PirateGunner::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8)
                    .build(DunesRelics.id("pirate_gunner").toString()));
    public static final RegistryObject<EntityType<PirateCaptain>> PIRATE_CAPTAIN = ENTITIES.register("pirate_captain",
            () -> EntityType.Builder.of(PirateCaptain::new, MobCategory.MONSTER).sized(0.7F, 2.2F).clientTrackingRange(10)
                    .build(DunesRelics.id("pirate_captain").toString()));
    public static final RegistryObject<EntityType<Traveler>> TRAVELER = ENTITIES.register("traveler",
            () -> EntityType.Builder.of(Traveler::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(DunesRelics.id("traveler").toString()));
    public static final RegistryObject<EntityType<Shade>> SHADE = ENTITIES.register("shade",
            () -> EntityType.Builder.of(Shade::new, MobCategory.MONSTER).sized(0.6F, 2.6F).clientTrackingRange(10)
                    .build(DunesRelics.id("shade").toString()));
    public static final RegistryObject<EntityType<Cannonball>> CANNONBALL = ENTITIES.register("cannonball",
            () -> EntityType.Builder.<Cannonball>of(Cannonball::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(8)
                    .updateInterval(10).build(DunesRelics.id("cannonball").toString()));

    public static final RegistryObject<EntityType<VillageWorker>> VILLAGE_BUILDER = worker("village_builder", VillageWorker.Job.BUILDER);
    public static final RegistryObject<EntityType<VillageWorker>> LUMBERJACK = worker("lumberjack", VillageWorker.Job.LUMBERJACK);
    public static final RegistryObject<EntityType<VillageWorker>> QUARRYMAN = worker("quarryman", VillageWorker.Job.QUARRYMAN);

    private static RegistryObject<EntityType<VillageWorker>> worker(String name, VillageWorker.Job job) {
        return ENTITIES.register(name, () -> EntityType.Builder.<VillageWorker>of((type, level) -> new VillageWorker(type, level, job),
                MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build(DunesRelics.id(name).toString()));
    }

    private ModEntities() {}
}
