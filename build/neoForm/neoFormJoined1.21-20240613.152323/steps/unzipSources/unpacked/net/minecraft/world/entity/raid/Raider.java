package net.minecraft.world.entity.raid;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PathfindToRaidGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public abstract class Raider extends PatrollingMonster {
    protected static final EntityDataAccessor<Boolean> IS_CELEBRATING = SynchedEntityData.defineId(Raider.class, EntityDataSerializers.BOOLEAN);
    static final Predicate<ItemEntity> ALLOWED_ITEMS = p_352842_ -> !p_352842_.hasPickUpDelay()
            && p_352842_.isAlive()
            && ItemStack.matches(p_352842_.getItem(), Raid.getLeaderBannerInstance(p_352842_.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
    @Nullable
    protected Raid raid;
    private int wave;
    private boolean canJoinRaid;
    private int ticksOutsideRaid;

    protected Raider(EntityType<? extends Raider> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new Raider.ObtainRaidLeaderBannerGoal<>(this));
        this.goalSelector.addGoal(3, new PathfindToRaidGoal<>(this));
        this.goalSelector.addGoal(4, new Raider.RaiderMoveThroughVillageGoal(this, 1.05F, 1));
        this.goalSelector.addGoal(5, new Raider.RaiderCelebration(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(IS_CELEBRATING, false);
    }

    public abstract void applyRaidBuffs(ServerLevel p_348605_, int p_37844_, boolean p_37845_);

    public boolean canJoinRaid() {
        return this.canJoinRaid;
    }

    public void setCanJoinRaid(boolean pCanJoinRaid) {
        this.canJoinRaid = pCanJoinRaid;
    }

    @Override
    public void aiStep() {
        if (this.level() instanceof ServerLevel && this.isAlive()) {
            Raid raid = this.getCurrentRaid();
            if (this.canJoinRaid()) {
                if (raid == null) {
                    if (this.level().getGameTime() % 20L == 0L) {
                        Raid raid1 = ((ServerLevel)this.level()).getRaidAt(this.blockPosition());
                        if (raid1 != null && Raids.canJoinRaid(this, raid1)) {
                            raid1.joinRaid(raid1.getGroupsSpawned(), this, null, true);
                        }
                    }
                } else {
                    LivingEntity livingentity = this.getTarget();
                    if (livingentity != null && (livingentity.getType() == EntityType.PLAYER || livingentity.getType() == EntityType.IRON_GOLEM)) {
                        this.noActionTime = 0;
                    }
                }
            }
        }

        super.aiStep();
    }

    @Override
    protected void updateNoActionTime() {
        this.noActionTime += 2;
    }

    /**
     * Called when the mob's health reaches 0.
     */
    @Override
    public void die(DamageSource pCause) {
        if (this.level() instanceof ServerLevel) {
            Entity entity = pCause.getEntity();
            Raid raid = this.getCurrentRaid();
            if (raid != null) {
                if (this.isPatrolLeader()) {
                    raid.removeLeader(this.getWave());
                }

                if (entity != null && entity.getType() == EntityType.PLAYER) {
                    raid.addHeroOfTheVillage(entity);
                }

                raid.removeFromRaid(this, false);
            }
        }

        super.die(pCause);
    }

    @Override
    public boolean canJoinPatrol() {
        return !this.hasActiveRaid();
    }

    public void setCurrentRaid(@Nullable Raid pRaid) {
        this.raid = pRaid;
    }

    @Nullable
    public Raid getCurrentRaid() {
        return this.raid;
    }

    public boolean isCaptain() {
        ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
        boolean flag = !itemstack.isEmpty()
            && ItemStack.matches(itemstack, Raid.getLeaderBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
        boolean flag1 = this.isPatrolLeader();
        return flag && flag1;
    }

    public boolean hasRaid() {
        return !(this.level() instanceof ServerLevel serverlevel)
            ? false
            : this.getCurrentRaid() != null || serverlevel.getRaidAt(this.blockPosition()) != null;
    }

    public boolean hasActiveRaid() {
        return this.getCurrentRaid() != null && this.getCurrentRaid().isActive();
    }

    public void setWave(int pWave) {
        this.wave = pWave;
    }

    public int getWave() {
        return this.wave;
    }

    public boolean isCelebrating() {
        return this.entityData.get(IS_CELEBRATING);
    }

    public void setCelebrating(boolean pCelebrating) {
        this.entityData.set(IS_CELEBRATING, pCelebrating);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Wave", this.wave);
        pCompound.putBoolean("CanJoinRaid", this.canJoinRaid);
        if (this.raid != null) {
            pCompound.putInt("RaidId", this.raid.getId());
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.wave = pCompound.getInt("Wave");
        this.canJoinRaid = pCompound.getBoolean("CanJoinRaid");
        if (pCompound.contains("RaidId", 3)) {
            if (this.level() instanceof ServerLevel) {
                this.raid = ((ServerLevel)this.level()).getRaids().get(pCompound.getInt("RaidId"));
            }

            if (this.raid != null) {
                this.raid.addWaveMob(this.wave, this, false);
                if (this.isPatrolLeader()) {
                    this.raid.setLeader(this.wave, this);
                }
            }
        }
    }

    /**
     * Tests if this entity should pick up a weapon or an armor piece. Entity drops current weapon or armor if the new one is better.
     */
    @Override
    protected void pickUpItem(ItemEntity pItemEntity) {
        ItemStack itemstack = pItemEntity.getItem();
        boolean flag = this.hasActiveRaid() && this.getCurrentRaid().getLeader(this.getWave()) != null;
        if (this.hasActiveRaid()
            && !flag
            && ItemStack.matches(itemstack, Raid.getLeaderBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)))) {
            EquipmentSlot equipmentslot = EquipmentSlot.HEAD;
            ItemStack itemstack1 = this.getItemBySlot(equipmentslot);
            double d0 = (double)this.getEquipmentDropChance(equipmentslot);
            if (!itemstack1.isEmpty() && (double)Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
                this.spawnAtLocation(itemstack1);
            }

            this.onItemPickup(pItemEntity);
            this.setItemSlot(equipmentslot, itemstack);
            this.take(pItemEntity, itemstack.getCount());
            pItemEntity.discard();
            this.getCurrentRaid().setLeader(this.getWave(), this);
            this.setPatrolLeader(true);
        } else {
            super.pickUpItem(pItemEntity);
        }
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return this.getCurrentRaid() == null ? super.removeWhenFarAway(pDistanceToClosestPlayer) : false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.getCurrentRaid() != null;
    }

    public int getTicksOutsideRaid() {
        return this.ticksOutsideRaid;
    }

    public void setTicksOutsideRaid(int pTicksOutsideRaid) {
        this.ticksOutsideRaid = pTicksOutsideRaid;
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.hasActiveRaid()) {
            this.getCurrentRaid().updateBossbar();
        }

        return super.hurt(pSource, pAmount);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData) {
        this.setCanJoinRaid(this.getType() != EntityType.WITCH || pSpawnType != MobSpawnType.NATURAL);
        return super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
    }

    public abstract SoundEvent getCelebrateSound();

    protected class HoldGroundAttackGoal extends Goal {
        private final Raider mob;
        private final float hostileRadiusSqr;
        public final TargetingConditions shoutTargeting = TargetingConditions.forNonCombat().range(8.0).ignoreLineOfSight().ignoreInvisibilityTesting();

        public HoldGroundAttackGoal(AbstractIllager pMob, float pHostileRadiusSquare) {
            this.mob = pMob;
            this.hostileRadiusSqr = pHostileRadiusSquare * pHostileRadiusSquare;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = this.mob.getLastHurtByMob();
            return this.mob.getCurrentRaid() == null
                && this.mob.isPatrolling()
                && this.mob.getTarget() != null
                && !this.mob.isAggressive()
                && (livingentity == null || livingentity.getType() != EntityType.PLAYER);
        }

        @Override
        public void start() {
            super.start();
            this.mob.getNavigation().stop();

            for (Raider raider : this.mob
                .level()
                .getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0))) {
                raider.setTarget(this.mob.getTarget());
            }
        }

        @Override
        public void stop() {
            super.stop();
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null) {
                for (Raider raider : this.mob
                    .level()
                    .getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0))) {
                    raider.setTarget(livingentity);
                    raider.setAggressive(true);
                }

                this.mob.setAggressive(true);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null) {
                if (this.mob.distanceToSqr(livingentity) > (double)this.hostileRadiusSqr) {
                    this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
                    if (this.mob.random.nextInt(50) == 0) {
                        this.mob.playAmbientSound();
                    }
                } else {
                    this.mob.setAggressive(true);
                }

                super.tick();
            }
        }
    }

    public class ObtainRaidLeaderBannerGoal<T extends Raider> extends Goal {
        private final T mob;

        public ObtainRaidLeaderBannerGoal(T pMob) {
            this.mob = pMob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Raid raid = this.mob.getCurrentRaid();
            if (this.mob.hasActiveRaid()
                && !this.mob.getCurrentRaid().isOver()
                && this.mob.canBeLeader()
                && !ItemStack.matches(
                    this.mob.getItemBySlot(EquipmentSlot.HEAD),
                    Raid.getLeaderBannerInstance(this.mob.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN))
                )) {
                Raider raider = raid.getLeader(this.mob.getWave());
                if (raider == null || !raider.isAlive()) {
                    List<ItemEntity> list = this.mob
                        .level()
                        .getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(16.0, 8.0, 16.0), Raider.ALLOWED_ITEMS);
                    if (!list.isEmpty()) {
                        return this.mob.getNavigation().moveTo(list.get(0), 1.15F);
                    }
                }

                return false;
            } else {
                return false;
            }
        }

        @Override
        public void tick() {
            if (this.mob.getNavigation().getTargetPos().closerToCenterThan(this.mob.position(), 1.414)) {
                List<ItemEntity> list = this.mob
                    .level()
                    .getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(4.0, 4.0, 4.0), Raider.ALLOWED_ITEMS);
                if (!list.isEmpty()) {
                    this.mob.pickUpItem(list.get(0));
                }
            }
        }
    }

    public class RaiderCelebration extends Goal {
        private final Raider mob;

        RaiderCelebration(Raider pMob) {
            this.mob = pMob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Raid raid = this.mob.getCurrentRaid();
            return this.mob.isAlive() && this.mob.getTarget() == null && raid != null && raid.isLoss();
        }

        @Override
        public void start() {
            this.mob.setCelebrating(true);
            super.start();
        }

        @Override
        public void stop() {
            this.mob.setCelebrating(false);
            super.stop();
        }

        @Override
        public void tick() {
            if (!this.mob.isSilent() && this.mob.random.nextInt(this.adjustedTickDelay(100)) == 0) {
                Raider.this.makeSound(Raider.this.getCelebrateSound());
            }

            if (!this.mob.isPassenger() && this.mob.random.nextInt(this.adjustedTickDelay(50)) == 0) {
                this.mob.getJumpControl().jump();
            }

            super.tick();
        }
    }

    static class RaiderMoveThroughVillageGoal extends Goal {
        private final Raider raider;
        private final double speedModifier;
        private BlockPos poiPos;
        private final List<BlockPos> visited = Lists.newArrayList();
        private final int distanceToPoi;
        private boolean stuck;

        public RaiderMoveThroughVillageGoal(Raider pRaider, double pSpeedModifier, int pDistanceToPoi) {
            this.raider = pRaider;
            this.speedModifier = pSpeedModifier;
            this.distanceToPoi = pDistanceToPoi;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            this.updateVisited();
            return this.isValidRaid() && this.hasSuitablePoi() && this.raider.getTarget() == null;
        }

        private boolean isValidRaid() {
            return this.raider.hasActiveRaid() && !this.raider.getCurrentRaid().isOver();
        }

        private boolean hasSuitablePoi() {
            ServerLevel serverlevel = (ServerLevel)this.raider.level();
            BlockPos blockpos = this.raider.blockPosition();
            Optional<BlockPos> optional = serverlevel.getPoiManager()
                .getRandom(p_219843_ -> p_219843_.is(PoiTypes.HOME), this::hasNotVisited, PoiManager.Occupancy.ANY, blockpos, 48, this.raider.random);
            if (optional.isEmpty()) {
                return false;
            } else {
                this.poiPos = optional.get().immutable();
                return true;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.raider.getNavigation().isDone()
                ? false
                : this.raider.getTarget() == null
                    && !this.poiPos.closerToCenterThan(this.raider.position(), (double)(this.raider.getBbWidth() + (float)this.distanceToPoi))
                    && !this.stuck;
        }

        @Override
        public void stop() {
            if (this.poiPos.closerToCenterThan(this.raider.position(), (double)this.distanceToPoi)) {
                this.visited.add(this.poiPos);
            }
        }

        @Override
        public void start() {
            super.start();
            this.raider.setNoActionTime(0);
            this.raider.getNavigation().moveTo((double)this.poiPos.getX(), (double)this.poiPos.getY(), (double)this.poiPos.getZ(), this.speedModifier);
            this.stuck = false;
        }

        @Override
        public void tick() {
            if (this.raider.getNavigation().isDone()) {
                Vec3 vec3 = Vec3.atBottomCenterOf(this.poiPos);
                Vec3 vec31 = DefaultRandomPos.getPosTowards(this.raider, 16, 7, vec3, (float) (Math.PI / 10));
                if (vec31 == null) {
                    vec31 = DefaultRandomPos.getPosTowards(this.raider, 8, 7, vec3, (float) (Math.PI / 2));
                }

                if (vec31 == null) {
                    this.stuck = true;
                    return;
                }

                this.raider.getNavigation().moveTo(vec31.x, vec31.y, vec31.z, this.speedModifier);
            }
        }

        private boolean hasNotVisited(BlockPos p_37943_) {
            for (BlockPos blockpos : this.visited) {
                if (Objects.equals(p_37943_, blockpos)) {
                    return false;
                }
            }

            return true;
        }

        private void updateVisited() {
            if (this.visited.size() > 2) {
                this.visited.remove(0);
            }
        }
    }
}