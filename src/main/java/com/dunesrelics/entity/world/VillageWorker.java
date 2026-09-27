package com.dunesrelics.entity.world;

import com.dunesrelics.memory.Blueprint;
import com.dunesrelics.memory.Construction;
import com.dunesrelics.memory.Gathering;
import com.dunesrelics.memory.Materials;
import com.dunesrelics.memory.Names;
import com.dunesrelics.memory.WorldMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A villager who works for the whole village. Builders put up the buildings the village decides it needs, block by
 * block from the ground up, and put back whatever was burnt, blown up or broken; lumberjacks fell the trees around
 * the village and plant new ones; quarrymen dig a pit outside it. Wood and stone go into the village's stock and the
 * builders spend it. They work by day, go back to the bell at night, and trade in their materials.
 */
public class VillageWorker extends AbstractVillager {
    public enum Job { BUILDER, LUMBERJACK, QUARRYMAN }

    private final Job job;
    @Nullable
    private BlockPos homeBell;
    private int nameIndex = -1;

    public VillageWorker(EntityType<? extends AbstractVillager> type, Level level, Job job) {
        super(type, level);
        this.job = job;
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
        // registerGoals runs before the job is known, so the job's own work is added here
        this.goalSelector.addGoal(3, switch (job) {
            case BUILDER -> new BuildGoal(this);
            case LUMBERJACK -> new ChopGoal(this);
            case QUARRYMAN -> new QuarryGoal(this);
        });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.6D, 0.7D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, AbstractIllager.class, 10.0F, 0.6D, 0.7D));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.6D));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new ReturnHomeGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public Job getJob() {
        return this.job;
    }

    // ------------------------------------------------------------------------------------------ home and name

    @Nullable
    public BlockPos getHomeBell() {
        return this.homeBell;
    }

    public void setHomeBell(BlockPos bell) {
        this.homeBell = bell.immutable();
        this.restrictTo(this.homeBell, 64);
    }

    public int getNameIndex() {
        if (this.nameIndex < 0) {
            this.nameIndex = Names.randomVillager(this.random);
        }
        return this.nameIndex;
    }

    public Component getWorkerName() {
        return Component.translatable(Names.villagerKey(this.getNameIndex()));
    }

    @Nullable
    WorldMemory.VillageRecord village() {
        if (!(this.level() instanceof ServerLevel level) || this.homeBell == null) {
            return null;
        }
        return WorldMemory.get(level).villageAt(this.homeBell);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.homeBell == null && this.tickCount % 100 == 0 && this.level() instanceof ServerLevel level) {
            // one who came from a spawn egg settles in the nearest village the world knows
            WorldMemory.VillageRecord nearest = WorldMemory.get(level).nearestVillage(this.blockPosition(), 64.0D);
            if (nearest != null) {
                this.setHomeBell(nearest.bell);
            }
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        this.getNameIndex();
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    // ------------------------------------------------------------------------------------------ talking and trading

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                player.awardStat(Stats.TALKED_TO_VILLAGER);
            }
            if (!this.level().isClientSide) {
                this.tellWork(player);
                if (!this.getOffers().isEmpty()) {
                    this.setTradingPlayer(player);
                    this.openTradingScreen(player, this.getDisplayName(), 1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** "Builder Miron: building a house for Quiet Creek, 120 blocks to go." */
    private void tellWork(Player player) {
        WorldMemory.VillageRecord record = this.village();
        String prefix = "entity.dunesrelics.village_worker.";
        Component line;
        if (record == null) {
            line = Component.translatable(prefix + "idle");
        } else if (this.job == Job.LUMBERJACK) {
            line = Component.translatable(prefix + "lumber", record.wood);
        } else if (this.job == Job.QUARRYMAN) {
            line = Component.translatable(prefix + "quarry", record.stone);
        } else if (record.waitingFor != null && Construction.hasWork(record)) {
            line = Component.translatable(prefix + (record.waitingFor == Materials.Kind.WOOD ? "waiting_wood" : "waiting_stone"));
        } else if (!record.repairs.isEmpty()) {
            line = Component.translatable(prefix + "repairing", record.repairs.size());
        } else if (!record.sites.isEmpty()) {
            Blueprint site = record.sites.get(0);
            line = Component.translatable(prefix + "building", Component.translatable("project.dunesrelics." + site.project),
                    Component.translatable(record.nameKey()), site.remaining());
        } else {
            line = Component.translatable(prefix + "resting", Component.translatable(record.nameKey()));
        }
        player.displayClientMessage(Component.translatable("villager.dunesrelics.says", this.getWorkerName(), this.getTypeName(), line), true);
    }

    private static MerchantOffer sell(ItemStack goods, int emeralds, int uses) {
        return new MerchantOffer(new ItemStack(Items.EMERALD, emeralds), goods, uses, 2, 0.05F);
    }

    private static MerchantOffer buy(ItemStack wanted, int emeralds, int uses) {
        return new MerchantOffer(wanted, new ItemStack(Items.EMERALD, emeralds), uses, 2, 0.05F);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        switch (this.job) {
            case BUILDER -> {
                offers.add(sell(new ItemStack(Items.OAK_PLANKS, 24), 1, 16));
                offers.add(sell(new ItemStack(Items.STONE_BRICKS, 16), 1, 16));
                offers.add(sell(new ItemStack(Items.BRICKS, 12), 2, 12));
                offers.add(sell(new ItemStack(Items.GLASS, 12), 2, 12));
                offers.add(sell(new ItemStack(Items.SCAFFOLDING, 16), 1, 12));
                offers.add(sell(new ItemStack(Items.LANTERN, 4), 2, 8));
                offers.add(buy(new ItemStack(Items.IRON_INGOT, 4), 1, 12));
            }
            case LUMBERJACK -> {
                offers.add(sell(new ItemStack(Items.OAK_LOG, 16), 1, 16));
                offers.add(sell(new ItemStack(Items.SPRUCE_LOG, 16), 1, 16));
                offers.add(sell(new ItemStack(Items.BIRCH_LOG, 16), 1, 16));
                offers.add(sell(new ItemStack(Items.CHARCOAL, 8), 1, 12));
                offers.add(sell(new ItemStack(Items.OAK_SAPLING, 4), 1, 8));
                offers.add(buy(new ItemStack(Items.APPLE, 8), 1, 12));
            }
            case QUARRYMAN -> {
                offers.add(sell(new ItemStack(Items.COBBLESTONE, 32), 1, 16));
                offers.add(sell(new ItemStack(Items.STONE, 16), 1, 16));
                offers.add(sell(new ItemStack(Items.GRAVEL, 16), 1, 12));
                offers.add(sell(new ItemStack(Items.COAL, 6), 1, 12));
                offers.add(sell(new ItemStack(Items.FLINT, 8), 1, 12));
                offers.add(buy(new ItemStack(Items.BREAD, 6), 1, 12));
            }
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(),
                    3 + this.random.nextInt(4)));
        }
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    // ------------------------------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.homeBell != null) {
            tag.put("HomeBell", NbtUtils.writeBlockPos(this.homeBell));
        }
        tag.putInt("NameIndex", this.getNameIndex());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeBell")) {
            this.setHomeBell(NbtUtils.readBlockPos(tag.getCompound("HomeBell")));
        }
        this.nameIndex = tag.contains("NameIndex") ? tag.getInt("NameIndex") : -1;
    }

    // ------------------------------------------------------------------------------------------ work

    static boolean isWorkTime(Level level) {
        long time = level.getDayTime() % 24000L;
        return time >= 500L && time < 11500L;
    }

    /** What a builder holds while he walks to a block: the block itself (or a bucket for water). */
    static ItemStack carried(BlockState state) {
        if (state.isAir()) {
            return new ItemStack(Items.IRON_SHOVEL);
        }
        if (!state.getFluidState().isEmpty() && state.getBlock().asItem() == Items.AIR) {
            return new ItemStack(Items.WATER_BUCKET);
        }
        return new ItemStack(state.getBlock().asItem());
    }

    /** Common to all work: walk up to a spot, returning true once there (or close enough after trying a while). */
    abstract static class WorkGoal extends Goal {
        protected final VillageWorker worker;
        private int repath;
        private int stuck;

        WorkGoal(VillageWorker worker) {
            this.worker = worker;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        protected void resetWalk() {
            this.repath = 0;
            this.stuck = 0;
        }

        /** Walks towards {@code pos}; true once within {@code reach} (or, after ten seconds, within {@code far}). */
        protected boolean reach(BlockPos pos, double reach, double far) {
            Vec3 center = Vec3.atCenterOf(pos);
            this.worker.getLookControl().setLookAt(center.x, center.y, center.z);
            double distance = this.worker.distanceToSqr(center);
            if (distance <= reach * reach) {
                this.worker.getNavigation().stop();
                return true;
            }
            if (--this.repath <= 0) {
                this.repath = 20;
                this.worker.getNavigation().moveTo(center.x, pos.getY(), center.z, 0.55D);
            }
            return ++this.stuck >= 200 && distance <= far * far;
        }

        protected boolean gaveUp() {
            return this.stuck > 600;
        }
    }

    /** A builder: walks to the next block of the village's work and lays it; repairs come first. */
    static class BuildGoal extends WorkGoal {
        @Nullable
        private Construction.Task task;
        private int cooldown;

        BuildGoal(VillageWorker worker) {
            super(worker);
        }

        @Override
        public boolean canUse() {
            if (this.worker.isTrading() || this.worker.getRandom().nextInt(10) != 0 || !isWorkTime(this.worker.level())
                    || !(this.worker.level() instanceof ServerLevel level)) {
                return false;
            }
            WorldMemory.VillageRecord record = this.worker.village();
            if (record == null || !Construction.hasWork(record)) {
                return false;
            }
            this.task = Construction.nextTask(level, WorldMemory.get(level), record);
            return this.task != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.task != null && isWorkTime(this.worker.level()) && !this.worker.isTrading();
        }

        @Override
        public void start() {
            this.resetWalk();
            this.cooldown = 10;
            if (this.task != null) {
                this.worker.setItemSlot(EquipmentSlot.MAINHAND, carried(this.task.state()));
            }
        }

        @Override
        public void stop() {
            this.task = null;
            this.worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.worker.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.task == null || !(this.worker.level() instanceof ServerLevel level)) {
                return;
            }
            WorldMemory.VillageRecord record = this.worker.village();
            if (record == null) {
                this.task = null;
                return;
            }
            BlockPos pos = this.task.pos();
            if (!this.reach(pos, 4.0D, 24.0D)) {
                return;
            }
            if (this.worker.getBoundingBox().intersects(new AABB(pos))) {
                // he is standing where the block goes: step aside first
                Vec3 away = DefaultRandomPos.getPosAway(this.worker, 4, 2, Vec3.atCenterOf(pos));
                if (away != null) {
                    this.worker.getNavigation().moveTo(away.x, away.y, away.z, 0.6D);
                }
                return;
            }
            if (--this.cooldown > 0) {
                return;
            }
            this.cooldown = 8 + this.worker.getRandom().nextInt(8);
            this.worker.swing(InteractionHand.MAIN_HAND);
            WorldMemory memory = WorldMemory.get(level);
            if (Construction.place(level, memory, record, this.task)) {
                this.resetWalk();
                this.task = Construction.nextTask(level, memory, record);
                if (this.task != null) {
                    this.worker.setItemSlot(EquipmentSlot.MAINHAND, carried(this.task.state()));
                }
            }
        }
    }

    /** A lumberjack: fells a natural tree near the village and plants a sapling where it stood. */
    static class ChopGoal extends WorkGoal {
        @Nullable
        private Gathering.Tree tree;
        private int swings;
        private int cooldown;

        ChopGoal(VillageWorker worker) {
            super(worker);
        }

        @Override
        public boolean canUse() {
            if (this.worker.isTrading() || this.worker.getRandom().nextInt(20) != 0 || !isWorkTime(this.worker.level())
                    || !(this.worker.level() instanceof ServerLevel level)) {
                return false;
            }
            WorldMemory.VillageRecord record = this.worker.village();
            if (record == null || record.wood >= Gathering.woodWanted(record)) {
                return false;
            }
            this.tree = Gathering.findTree(level, WorldMemory.get(level), record, this.worker.getRandom());
            return this.tree != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.tree != null && isWorkTime(this.worker.level()) && !this.worker.isTrading() && !this.gaveUp();
        }

        @Override
        public void start() {
            this.resetWalk();
            this.swings = 0;
            this.cooldown = 10;
            this.worker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }

        @Override
        public void stop() {
            this.tree = null;
            this.worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.worker.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.tree == null || !(this.worker.level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos base = this.tree.base();
            if (!this.reach(base, 2.5D, 5.0D)) {
                return;
            }
            if (--this.cooldown > 0) {
                return;
            }
            this.cooldown = 12;
            if (!level.getBlockState(base).is(net.minecraft.tags.BlockTags.LOGS)) {
                // somebody else got to it first
                this.tree = null;
                return;
            }
            this.worker.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, base, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, this.tree.log()), base.getX() + 0.5D, base.getY() + 0.8D,
                    base.getZ() + 0.5D, 6, 0.3D, 0.3D, 0.3D, 0.05D);
            if (++this.swings >= 5) {
                WorldMemory.VillageRecord record = this.worker.village();
                int logs = Gathering.fell(level, this.tree);
                if (record != null) {
                    record.wood += logs;
                    WorldMemory.get(level).setDirty();
                }
                this.tree = null;
            }
        }
    }

    /** A quarryman: digs the village's pit down course by course from its rim. */
    static class QuarryGoal extends WorkGoal {
        @Nullable
        private BlockPos target;
        private int cooldown;

        QuarryGoal(VillageWorker worker) {
            super(worker);
        }

        @Override
        public boolean canUse() {
            if (this.worker.isTrading() || this.worker.getRandom().nextInt(20) != 0 || !isWorkTime(this.worker.level())
                    || !(this.worker.level() instanceof ServerLevel level)) {
                return false;
            }
            WorldMemory.VillageRecord record = this.worker.village();
            if (record == null || record.stone >= Gathering.stoneWanted(record)) {
                return false;
            }
            WorldMemory memory = WorldMemory.get(level);
            if (record.quarry == null) {
                record.quarry = Gathering.findQuarry(level, memory, record, this.worker.getRandom());
                record.quarryNext = 0;
                if (record.quarry == null) {
                    return false;
                }
                Gathering.fenceQuarry(level, record.quarry);
                memory.record(level.getDayTime() / 24000L, "chronicle.dunesrelics.village.quarry", "#" + record.nameKey());
                memory.setDirty();
            }
            this.target = this.nextBlock(level, record);
            return this.target != null;
        }

        @Nullable
        private BlockPos nextBlock(ServerLevel level, WorldMemory.VillageRecord record) {
            for (int skipped = 0; skipped < 60 && record.quarry != null; skipped++) {
                BlockPos pos = Gathering.pitBlock(record.quarry, record.quarryNext);
                if (pos == null) {
                    // the pit is as deep as it goes: next time, a new one
                    record.quarry = null;
                    return null;
                }
                if (!level.isLoaded(pos)) {
                    return null;
                }
                if (!level.getBlockState(pos).isAir()) {
                    return pos;
                }
                record.quarryNext++;
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && isWorkTime(this.worker.level()) && !this.worker.isTrading() && !this.gaveUp();
        }

        @Override
        public void start() {
            this.resetWalk();
            this.cooldown = 10;
            this.worker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        }

        @Override
        public void stop() {
            this.target = null;
            this.worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.worker.getNavigation().stop();
        }

        @Override
        public void tick() {
            WorldMemory.VillageRecord record = this.worker.village();
            if (this.target == null || record == null || record.quarry == null || !(this.worker.level() instanceof ServerLevel level)) {
                this.target = null;
                return;
            }
            // he works from the rim, on the side of the ladder
            BlockPos rim = record.quarry.offset(Gathering.PIT / 2, 1, -2);
            if (!this.reach(rim, 3.0D, 8.0D)) {
                return;
            }
            this.worker.getLookControl().setLookAt(Vec3.atCenterOf(this.target));
            if (--this.cooldown > 0) {
                return;
            }
            this.cooldown = 10 + this.worker.getRandom().nextInt(6);
            this.worker.swing(InteractionHand.MAIN_HAND);
            WorldMemory memory = WorldMemory.get(level);
            if (Gathering.dig(level, record, record.quarry, this.target)) {
                record.quarryNext++;
                if (record.stone >= Gathering.stoneWanted(record)) {
                    this.target = null;
                } else {
                    this.target = this.nextBlock(level, record);
                }
            } else {
                // water, lava or something built in the way: this pit is finished
                record.quarry = null;
                this.target = null;
            }
            memory.setDirty();
        }
    }

    /** Back to the bell for the night, or when he has wandered off. */
    static class ReturnHomeGoal extends Goal {
        private final VillageWorker worker;

        ReturnHomeGoal(VillageWorker worker) {
            this.worker = worker;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos bell = this.worker.homeBell;
            if (bell == null || this.worker.getRandom().nextInt(20) != 0) {
                return false;
            }
            double range = isWorkTime(this.worker.level()) ? 60.0D : 6.0D;
            return this.worker.blockPosition().distSqr(bell) > range * range;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.worker.getNavigation().isDone();
        }

        @Override
        public void start() {
            BlockPos bell = this.worker.homeBell;
            if (bell != null) {
                this.worker.getNavigation().moveTo(bell.getX() + 0.5D, bell.getY(), bell.getZ() + 0.5D, 0.5D);
            }
        }
    }
}
