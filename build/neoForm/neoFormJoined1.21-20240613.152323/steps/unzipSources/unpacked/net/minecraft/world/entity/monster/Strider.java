package net.minecraft.world.entity.monster;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class Strider extends Animal implements ItemSteerable, Saddleable {
    private static final ResourceLocation SUFFOCATING_MODIFIER_ID = ResourceLocation.withDefaultNamespace("suffocating");
    private static final AttributeModifier SUFFOCATING_MODIFIER = new AttributeModifier(
        SUFFOCATING_MODIFIER_ID, -0.34F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
    );
    private static final float SUFFOCATE_STEERING_MODIFIER = 0.35F;
    private static final float STEERING_MODIFIER = 0.55F;
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SUFFOCATING = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SADDLE_ID = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.BOOLEAN);
    private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME, DATA_SADDLE_ID);
    @Nullable
    private TemptGoal temptGoal;

    public Strider(EntityType<? extends Strider> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.blocksBuilding = true;
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, 0.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
    }

    public static boolean checkStriderSpawnRules(
        EntityType<Strider> pStrider, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom
    ) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

        do {
            blockpos$mutableblockpos.move(Direction.UP);
        } while (pLevel.getFluidState(blockpos$mutableblockpos).is(FluidTags.LAVA));

        return pLevel.getBlockState(blockpos$mutableblockpos).isAir();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_BOOST_TIME.equals(pKey) && this.level().isClientSide) {
            this.steering.onSynced();
        }

        super.onSyncedDataUpdated(pKey);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_BOOST_TIME, 0);
        pBuilder.define(DATA_SUFFOCATING, false);
        pBuilder.define(DATA_SADDLE_ID, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.steering.addAdditionalSaveData(pCompound);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.steering.readAdditionalSaveData(pCompound);
    }

    @Override
    public boolean isSaddled() {
        return this.steering.hasSaddle();
    }

    @Override
    public boolean isSaddleable() {
        return this.isAlive() && !this.isBaby();
    }

    @Override
    public void equipSaddle(ItemStack p_352201_, @Nullable SoundSource p_33878_) {
        this.steering.setSaddle(true);
        if (p_33878_ != null) {
            this.level().playSound(null, this, SoundEvents.STRIDER_SADDLE, p_33878_, 0.5F, 1.0F);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.65));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.temptGoal = new TemptGoal(this, 1.4, p_335880_ -> p_335880_.is(ItemTags.STRIDER_TEMPT_ITEMS), false);
        this.goalSelector.addGoal(3, this.temptGoal);
        this.goalSelector.addGoal(4, new Strider.StriderGoToLavaGoal(this, 1.0));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.0));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0, 60));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Strider.class, 8.0F));
    }

    public void setSuffocating(boolean pSuffocating) {
        this.entityData.set(DATA_SUFFOCATING, pSuffocating);
        AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeinstance != null) {
            if (pSuffocating) {
                attributeinstance.addOrUpdateTransientModifier(SUFFOCATING_MODIFIER);
            } else {
                attributeinstance.removeModifier(SUFFOCATING_MODIFIER_ID);
            }
        }
    }

    public boolean isSuffocating() {
        return this.entityData.get(DATA_SUFFOCATING);
    }

    @Override
    public boolean canStandOnFluid(FluidState pFluidState) {
        return pFluidState.is(FluidTags.LAVA);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity pEntity, EntityDimensions pDimensions, float pPartialTick) {
        float f = Math.min(0.25F, this.walkAnimation.speed());
        float f1 = this.walkAnimation.position();
        float f2 = 0.12F * Mth.cos(f1 * 1.5F) * 2.0F * f;
        return super.getPassengerAttachmentPoint(pEntity, pDimensions, pPartialTick).add(0.0, (double)(f2 * pPartialTick), 0.0);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader pLevel) {
        return pLevel.isUnobstructed(this);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return (LivingEntity)(this.isSaddled() && this.getFirstPassenger() instanceof Player player && player.isHolding(Items.WARPED_FUNGUS_ON_A_STICK)
            ? player
            : super.getControllingPassenger());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pLivingEntity) {
        Vec3[] avec3 = new Vec3[]{
            getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), pLivingEntity.getYRot()),
            getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), pLivingEntity.getYRot() - 22.5F),
            getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), pLivingEntity.getYRot() + 22.5F),
            getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), pLivingEntity.getYRot() - 45.0F),
            getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)pLivingEntity.getBbWidth(), pLivingEntity.getYRot() + 45.0F)
        };
        Set<BlockPos> set = Sets.newLinkedHashSet();
        double d0 = this.getBoundingBox().maxY;
        double d1 = this.getBoundingBox().minY - 0.5;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Vec3 vec3 : avec3) {
            blockpos$mutableblockpos.set(this.getX() + vec3.x, d0, this.getZ() + vec3.z);

            for (double d2 = d0; d2 > d1; d2--) {
                set.add(blockpos$mutableblockpos.immutable());
                blockpos$mutableblockpos.move(Direction.DOWN);
            }
        }

        for (BlockPos blockpos : set) {
            if (!this.level().getFluidState(blockpos).is(FluidTags.LAVA)) {
                double d3 = this.level().getBlockFloorHeight(blockpos);
                if (DismountHelper.isBlockFloorValid(d3)) {
                    Vec3 vec31 = Vec3.upFromBottomCenterOf(blockpos, d3);

                    for (Pose pose : pLivingEntity.getDismountPoses()) {
                        AABB aabb = pLivingEntity.getLocalBoundsForPose(pose);
                        if (DismountHelper.canDismountTo(this.level(), pLivingEntity, aabb.move(vec31))) {
                            pLivingEntity.setPose(pose);
                            return vec31;
                        }
                    }
                }
            }
        }

        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    @Override
    protected void tickRidden(Player pPlayer, Vec3 pTravelVector) {
        this.setRot(pPlayer.getYRot(), pPlayer.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        this.steering.tickBoost();
        super.tickRidden(pPlayer, pTravelVector);
    }

    @Override
    protected Vec3 getRiddenInput(Player pPlayer, Vec3 pTravelVector) {
        return new Vec3(0.0, 0.0, 1.0);
    }

    @Override
    protected float getRiddenSpeed(Player pPlayer) {
        return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (double)(this.isSuffocating() ? 0.35F : 0.55F) * (double)this.steering.boostFactor());
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.6F;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
        this.playSound(this.isInLava() ? SoundEvents.STRIDER_STEP_LAVA : SoundEvents.STRIDER_STEP, 1.0F, 1.0F);
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
        this.checkInsideBlocks();
        if (this.isInLava()) {
            this.resetFallDistance();
        } else {
            super.checkFallDamage(pY, pOnGround, pState, pPos);
        }
    }

    @Override
    public void tick() {
        if (this.isBeingTempted() && this.random.nextInt(140) == 0) {
            this.makeSound(SoundEvents.STRIDER_HAPPY);
        } else if (this.isPanicking() && this.random.nextInt(60) == 0) {
            this.makeSound(SoundEvents.STRIDER_RETREAT);
        }

        if (!this.isNoAi()) {
            boolean flag;
            boolean flag2;
            label36: {
                BlockState blockstate = this.level().getBlockState(this.blockPosition());
                BlockState blockstate1 = this.getBlockStateOnLegacy();
                flag = blockstate.is(BlockTags.STRIDER_WARM_BLOCKS)
                    || blockstate1.is(BlockTags.STRIDER_WARM_BLOCKS)
                    || this.getFluidHeight(FluidTags.LAVA) > 0.0;
                if (this.getVehicle() instanceof Strider strider && strider.isSuffocating()) {
                    flag2 = true;
                    break label36;
                }

                flag2 = false;
            }

            boolean flag1 = flag2;
            this.setSuffocating(!flag || flag1);
        }

        super.tick();
        this.floatStrider();
        this.checkInsideBlocks();
    }

    private boolean isBeingTempted() {
        return this.temptGoal != null && this.temptGoal.isRunning();
    }

    @Override
    protected boolean shouldPassengersInheritMalus() {
        return true;
    }

    private void floatStrider() {
        if (this.isInLava()) {
            CollisionContext collisioncontext = CollisionContext.of(this);
            if (collisioncontext.isAbove(LiquidBlock.STABLE_SHAPE, this.blockPosition(), true)
                && !this.level().getFluidState(this.blockPosition().above()).is(FluidTags.LAVA)) {
                this.setOnGround(true);
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.175F).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return !this.isPanicking() && !this.isBeingTempted() ? SoundEvents.STRIDER_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }

    @Override
    protected boolean canAddPassenger(Entity pPassenger) {
        return !this.isVehicle() && !this.isEyeInFluid(FluidTags.LAVA);
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new Strider.StriderPathNavigation(this, pLevel);
    }

    @Override
    public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
        if (pLevel.getBlockState(pPos).getFluidState().is(FluidTags.LAVA)) {
            return 10.0F;
        } else {
            return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
        }
    }

    @Nullable
    public Strider getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return EntityType.STRIDER.create(pLevel);
    }

    /**
     * Checks if the parameter is an item which this animal can be fed to breed it (wheat, carrots or seeds depending on the animal type)
     */
    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(ItemTags.STRIDER_FOOD);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (this.isSaddled()) {
            this.spawnAtLocation(Items.SADDLE);
        }
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        boolean flag = this.isFood(pPlayer.getItemInHand(pHand));
        if (!flag && this.isSaddled() && !this.isVehicle() && !pPlayer.isSecondaryUseActive()) {
            if (!this.level().isClientSide) {
                pPlayer.startRiding(this);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            InteractionResult interactionresult = super.mobInteract(pPlayer, pHand);
            if (!interactionresult.consumesAction()) {
                ItemStack itemstack = pPlayer.getItemInHand(pHand);
                return itemstack.is(Items.SADDLE) ? itemstack.interactLivingEntity(pPlayer, this, pHand) : InteractionResult.PASS;
            } else {
                if (flag && !this.isSilent()) {
                    this.level()
                        .playSound(
                            null,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            SoundEvents.STRIDER_EAT,
                            this.getSoundSource(),
                            1.0F,
                            1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
                        );
                }

                return interactionresult;
            }
        }
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, (double)(0.6F * this.getEyeHeight()), (double)(this.getBbWidth() * 0.4F));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData) {
        if (this.isBaby()) {
            return super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
        } else {
            RandomSource randomsource = pLevel.getRandom();
            if (randomsource.nextInt(30) == 0) {
                Mob mob = EntityType.ZOMBIFIED_PIGLIN.create(pLevel.getLevel());
                if (mob != null) {
                    pSpawnGroupData = this.spawnJockey(pLevel, pDifficulty, mob, new Zombie.ZombieGroupData(Zombie.getSpawnAsBabyOdds(randomsource), false));
                    mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));
                    this.equipSaddle(new ItemStack(Items.SADDLE), null);
                }
            } else if (randomsource.nextInt(10) == 0) {
                AgeableMob ageablemob = EntityType.STRIDER.create(pLevel.getLevel());
                if (ageablemob != null) {
                    ageablemob.setAge(-24000);
                    pSpawnGroupData = this.spawnJockey(pLevel, pDifficulty, ageablemob, null);
                }
            } else {
                pSpawnGroupData = new AgeableMob.AgeableMobGroupData(0.5F);
            }

            return super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
        }
    }

    private SpawnGroupData spawnJockey(ServerLevelAccessor pServerLevel, DifficultyInstance pDifficulty, Mob pJockey, @Nullable SpawnGroupData pSpawnData) {
        pJockey.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
        pJockey.finalizeSpawn(pServerLevel, pDifficulty, MobSpawnType.JOCKEY, pSpawnData);
        pJockey.startRiding(this, true);
        return new AgeableMob.AgeableMobGroupData(0.0F);
    }

    static class StriderGoToLavaGoal extends MoveToBlockGoal {
        private final Strider strider;

        StriderGoToLavaGoal(Strider pStrider, double pSpeedModifier) {
            super(pStrider, pSpeedModifier, 8, 2);
            this.strider = pStrider;
        }

        @Override
        public BlockPos getMoveToTarget() {
            return this.blockPos;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.strider.isInLava() && this.isValidTarget(this.strider.level(), this.blockPos);
        }

        @Override
        public boolean canUse() {
            return !this.strider.isInLava() && super.canUse();
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 20 == 0;
        }

        /**
         * Return {@code true} to set given position as destination
         */
        @Override
        protected boolean isValidTarget(LevelReader pLevel, BlockPos pPos) {
            return pLevel.getBlockState(pPos).is(Blocks.LAVA) && pLevel.getBlockState(pPos.above()).isPathfindable(PathComputationType.LAND);
        }
    }

    static class StriderPathNavigation extends GroundPathNavigation {
        StriderPathNavigation(Strider pStrider, Level pLevel) {
            super(pStrider, pLevel);
        }

        @Override
        protected PathFinder createPathFinder(int pMaxVisitedNodes) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, pMaxVisitedNodes);
        }

        @Override
        protected boolean hasValidPathType(PathType pPathType) {
            return pPathType != PathType.LAVA && pPathType != PathType.DAMAGE_FIRE && pPathType != PathType.DANGER_FIRE
                ? super.hasValidPathType(pPathType)
                : true;
        }

        @Override
        public boolean isStableDestination(BlockPos pPos) {
            return this.level.getBlockState(pPos).is(Blocks.LAVA) || super.isStableDestination(pPos);
        }
    }
}
