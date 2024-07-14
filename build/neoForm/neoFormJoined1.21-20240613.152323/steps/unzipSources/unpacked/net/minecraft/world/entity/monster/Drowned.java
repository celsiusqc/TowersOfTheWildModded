package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class Drowned extends Zombie implements RangedAttackMob {
    public static final float NAUTILUS_SHELL_CHANCE = 0.03F;
    boolean searchingForLand;
    protected final WaterBoundPathNavigation waterNavigation;
    protected final GroundPathNavigation groundNavigation;

    public Drowned(EntityType<? extends Drowned> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new Drowned.DrownedMoveControl(this);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.waterNavigation = new WaterBoundPathNavigation(this, pLevel);
        this.groundNavigation = new GroundPathNavigation(this, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new Drowned.DrownedGoToWaterGoal(this, 1.0));
        this.goalSelector.addGoal(2, new Drowned.DrownedTridentAttackGoal(this, 1.0, 40, 10.0F));
        this.goalSelector.addGoal(2, new Drowned.DrownedAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new Drowned.DrownedGoToBeachGoal(this, 1.0));
        this.goalSelector.addGoal(6, new Drowned.DrownedSwimUpGoal(this, 1.0, this.level().getSeaLevel()));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Drowned.class).setAlertOthers(ZombifiedPiglin.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::okTarget));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Axolotl.class, true, false));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData) {
        pSpawnGroupData = super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
        if (this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty() && pLevel.getRandom().nextFloat() < 0.03F) {
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
            this.setGuaranteedDrop(EquipmentSlot.OFFHAND);
        }

        return pSpawnGroupData;
    }

    public static boolean checkDrownedSpawnRules(
        EntityType<Drowned> pDrowned, ServerLevelAccessor pServerLevel, MobSpawnType pMobSpawnType, BlockPos pPos, RandomSource pRandom
    ) {
        if (!pServerLevel.getFluidState(pPos.below()).is(FluidTags.WATER) && !MobSpawnType.isSpawner(pMobSpawnType)) {
            return false;
        } else {
            Holder<Biome> holder = pServerLevel.getBiome(pPos);
            boolean flag = pServerLevel.getDifficulty() != Difficulty.PEACEFUL
                && (MobSpawnType.ignoresLightRequirements(pMobSpawnType) || isDarkEnoughToSpawn(pServerLevel, pPos, pRandom))
                && (MobSpawnType.isSpawner(pMobSpawnType) || pServerLevel.getFluidState(pPos).is(FluidTags.WATER));
            if (flag && MobSpawnType.isSpawner(pMobSpawnType)) {
                return true;
            } else {
                return holder.is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)
                    ? pRandom.nextInt(15) == 0 && flag
                    : pRandom.nextInt(40) == 0 && isDeepEnoughToSpawn(pServerLevel, pPos) && flag;
            }
        }
    }

    private static boolean isDeepEnoughToSpawn(LevelAccessor pLevel, BlockPos pPos) {
        return pPos.getY() < pLevel.getSeaLevel() - 5;
    }

    @Override
    protected boolean supportsBreakDoorGoal() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.DROWNED_AMBIENT_WATER : SoundEvents.DROWNED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return this.isInWater() ? SoundEvents.DROWNED_HURT_WATER : SoundEvents.DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isInWater() ? SoundEvents.DROWNED_DEATH_WATER : SoundEvents.DROWNED_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.DROWNED_STEP;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DROWNED_SWIM;
    }

    @Override
    protected ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
        if ((double)pRandom.nextFloat() > 0.9) {
            int i = pRandom.nextInt(16);
            if (i < 10) {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            } else {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
            }
        }
    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack pCandidate, ItemStack pExisting) {
        if (pExisting.is(Items.NAUTILUS_SHELL)) {
            return false;
        } else if (pExisting.is(Items.TRIDENT)) {
            return pCandidate.is(Items.TRIDENT) ? pCandidate.getDamageValue() < pExisting.getDamageValue() : false;
        } else {
            return pCandidate.is(Items.TRIDENT) ? true : super.canReplaceCurrentItem(pCandidate, pExisting);
        }
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader pLevel) {
        return pLevel.isUnobstructed(this);
    }

    public boolean okTarget(@Nullable LivingEntity p_32396_) {
        return p_32396_ != null ? !this.level().isDay() || p_32396_.isInWater() : false;
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.isSwimming();
    }

    boolean wantsToSwim() {
        if (this.searchingForLand) {
            return true;
        } else {
            LivingEntity livingentity = this.getTarget();
            return livingentity != null && livingentity.isInWater();
        }
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.isControlledByLocalInstance() && this.isInWater() && this.wantsToSwim()) {
            this.moveRelative(0.01F, pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        } else {
            super.travel(pTravelVector);
        }
    }

    @Override
    public void updateSwimming() {
        if (!this.level().isClientSide) {
            if (this.isEffectiveAi() && this.isInWater() && this.wantsToSwim()) {
                this.navigation = this.waterNavigation;
                this.setSwimming(true);
            } else {
                this.navigation = this.groundNavigation;
                this.setSwimming(false);
            }
        }
    }

    @Override
    public boolean isVisuallySwimming() {
        return this.isSwimming();
    }

    protected boolean closeToNextPos() {
        Path path = this.getNavigation().getPath();
        if (path != null) {
            BlockPos blockpos = path.getTarget();
            if (blockpos != null) {
                double d0 = this.distanceToSqr((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
                if (d0 < 4.0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Attack the specified entity using a ranged attack.
     */
    @Override
    public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {
        ThrownTrident throwntrident = new ThrownTrident(this.level(), this, new ItemStack(Items.TRIDENT));
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getY(0.3333333333333333) - throwntrident.getY();
        double d2 = pTarget.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        throwntrident.shoot(d0, d1 + d3 * 0.2F, d2, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(throwntrident);
    }

    public void setSearchingForLand(boolean pSearchingForLand) {
        this.searchingForLand = pSearchingForLand;
    }

    static class DrownedAttackGoal extends ZombieAttackGoal {
        private final Drowned drowned;

        public DrownedAttackGoal(Drowned pDrowned, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
            super(pDrowned, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
            this.drowned = pDrowned;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.drowned.okTarget(this.drowned.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.drowned.okTarget(this.drowned.getTarget());
        }
    }

    static class DrownedGoToBeachGoal extends MoveToBlockGoal {
        private final Drowned drowned;

        public DrownedGoToBeachGoal(Drowned pDrowned, double pSpeedModifier) {
            super(pDrowned, pSpeedModifier, 8, 2);
            this.drowned = pDrowned;
        }

        @Override
        public boolean canUse() {
            return super.canUse()
                && !this.drowned.level().isDay()
                && this.drowned.isInWater()
                && this.drowned.getY() >= (double)(this.drowned.level().getSeaLevel() - 3);
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse();
        }

        /**
         * Return {@code true} to set given position as destination
         */
        @Override
        protected boolean isValidTarget(LevelReader pLevel, BlockPos pPos) {
            BlockPos blockpos = pPos.above();
            return pLevel.isEmptyBlock(blockpos) && pLevel.isEmptyBlock(blockpos.above())
                ? pLevel.getBlockState(pPos).entityCanStandOn(pLevel, pPos, this.drowned)
                : false;
        }

        @Override
        public void start() {
            this.drowned.setSearchingForLand(false);
            this.drowned.navigation = this.drowned.groundNavigation;
            super.start();
        }

        @Override
        public void stop() {
            super.stop();
        }
    }

    static class DrownedGoToWaterGoal extends Goal {
        private final PathfinderMob mob;
        private double wantedX;
        private double wantedY;
        private double wantedZ;
        private final double speedModifier;
        private final Level level;

        public DrownedGoToWaterGoal(PathfinderMob pMob, double pSpeedModifier) {
            this.mob = pMob;
            this.speedModifier = pSpeedModifier;
            this.level = pMob.level();
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.level.isDay()) {
                return false;
            } else if (this.mob.isInWater()) {
                return false;
            } else {
                Vec3 vec3 = this.getWaterPos();
                if (vec3 == null) {
                    return false;
                } else {
                    this.wantedX = vec3.x;
                    this.wantedY = vec3.y;
                    this.wantedZ = vec3.z;
                    return true;
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !this.mob.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
        }

        @Nullable
        private Vec3 getWaterPos() {
            RandomSource randomsource = this.mob.getRandom();
            BlockPos blockpos = this.mob.blockPosition();

            for (int i = 0; i < 10; i++) {
                BlockPos blockpos1 = blockpos.offset(randomsource.nextInt(20) - 10, 2 - randomsource.nextInt(8), randomsource.nextInt(20) - 10);
                if (this.level.getBlockState(blockpos1).is(Blocks.WATER)) {
                    return Vec3.atBottomCenterOf(blockpos1);
                }
            }

            return null;
        }
    }

    static class DrownedMoveControl extends MoveControl {
        private final Drowned drowned;

        public DrownedMoveControl(Drowned pDrowned) {
            super(pDrowned);
            this.drowned = pDrowned;
        }

        @Override
        public void tick() {
            LivingEntity livingentity = this.drowned.getTarget();
            if (this.drowned.wantsToSwim() && this.drowned.isInWater()) {
                if (livingentity != null && livingentity.getY() > this.drowned.getY() || this.drowned.searchingForLand) {
                    this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0.0, 0.002, 0.0));
                }

                if (this.operation != MoveControl.Operation.MOVE_TO || this.drowned.getNavigation().isDone()) {
                    this.drowned.setSpeed(0.0F);
                    return;
                }

                double d0 = this.wantedX - this.drowned.getX();
                double d1 = this.wantedY - this.drowned.getY();
                double d2 = this.wantedZ - this.drowned.getZ();
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                d1 /= d3;
                float f = (float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) - 90.0F;
                this.drowned.setYRot(this.rotlerp(this.drowned.getYRot(), f, 90.0F));
                this.drowned.yBodyRot = this.drowned.getYRot();
                float f1 = (float)(this.speedModifier * this.drowned.getAttributeValue(Attributes.MOVEMENT_SPEED));
                float f2 = Mth.lerp(0.125F, this.drowned.getSpeed(), f1);
                this.drowned.setSpeed(f2);
                this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add((double)f2 * d0 * 0.005, (double)f2 * d1 * 0.1, (double)f2 * d2 * 0.005));
            } else {
                if (!this.drowned.onGround()) {
                    this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0.0, -0.008, 0.0));
                }

                super.tick();
            }
        }
    }

    static class DrownedSwimUpGoal extends Goal {
        private final Drowned drowned;
        private final double speedModifier;
        private final int seaLevel;
        private boolean stuck;

        public DrownedSwimUpGoal(Drowned pDrowned, double pSpeedModifier, int pSeaLevel) {
            this.drowned = pDrowned;
            this.speedModifier = pSpeedModifier;
            this.seaLevel = pSeaLevel;
        }

        @Override
        public boolean canUse() {
            return !this.drowned.level().isDay() && this.drowned.isInWater() && this.drowned.getY() < (double)(this.seaLevel - 2);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && !this.stuck;
        }

        @Override
        public void tick() {
            if (this.drowned.getY() < (double)(this.seaLevel - 1) && (this.drowned.getNavigation().isDone() || this.drowned.closeToNextPos())) {
                Vec3 vec3 = DefaultRandomPos.getPosTowards(
                    this.drowned, 4, 8, new Vec3(this.drowned.getX(), (double)(this.seaLevel - 1), this.drowned.getZ()), (float) (Math.PI / 2)
                );
                if (vec3 == null) {
                    this.stuck = true;
                    return;
                }

                this.drowned.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, this.speedModifier);
            }
        }

        @Override
        public void start() {
            this.drowned.setSearchingForLand(true);
            this.stuck = false;
        }

        @Override
        public void stop() {
            this.drowned.setSearchingForLand(false);
        }
    }

    static class DrownedTridentAttackGoal extends RangedAttackGoal {
        private final Drowned drowned;

        public DrownedTridentAttackGoal(RangedAttackMob pRangedAttackMob, double pSpeedModifier, int pAttackInterval, float pAttackRadius) {
            super(pRangedAttackMob, pSpeedModifier, pAttackInterval, pAttackRadius);
            this.drowned = (Drowned)pRangedAttackMob;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.drowned.getMainHandItem().is(Items.TRIDENT);
        }

        @Override
        public void start() {
            super.start();
            this.drowned.setAggressive(true);
            this.drowned.startUsingItem(InteractionHand.MAIN_HAND);
        }

        @Override
        public void stop() {
            super.stop();
            this.drowned.stopUsingItem();
            this.drowned.setAggressive(false);
        }
    }
}