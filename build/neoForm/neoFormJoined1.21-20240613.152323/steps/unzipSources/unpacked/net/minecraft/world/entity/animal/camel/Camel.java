package net.minecraft.world.entity.animal.camel;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Dynamic;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Camel extends AbstractHorse implements PlayerRideableJumping, Saddleable {
    public static final float BABY_SCALE = 0.45F;
    public static final int DASH_COOLDOWN_TICKS = 55;
    public static final int MAX_HEAD_Y_ROT = 30;
    private static final float RUNNING_SPEED_BONUS = 0.1F;
    private static final float DASH_VERTICAL_MOMENTUM = 1.4285F;
    private static final float DASH_HORIZONTAL_MOMENTUM = 22.2222F;
    private static final int DASH_MINIMUM_DURATION_TICKS = 5;
    private static final int SITDOWN_DURATION_TICKS = 40;
    private static final int STANDUP_DURATION_TICKS = 52;
    private static final int IDLE_MINIMAL_DURATION_TICKS = 80;
    private static final float SITTING_HEIGHT_DIFFERENCE = 1.43F;
    public static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.LONG);
    public final AnimationState sitAnimationState = new AnimationState();
    public final AnimationState sitPoseAnimationState = new AnimationState();
    public final AnimationState sitUpAnimationState = new AnimationState();
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState dashAnimationState = new AnimationState();
    private static final EntityDimensions SITTING_DIMENSIONS = EntityDimensions.scalable(EntityType.CAMEL.getWidth(), EntityType.CAMEL.getHeight() - 1.43F)
        .withEyeHeight(0.845F);
    private int dashCooldown = 0;
    private int idleAnimationTimeout = 0;

    public Camel(EntityType<? extends Camel> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new Camel.CamelMoveControl();
        this.lookControl = new Camel.CamelLookControl();
        GroundPathNavigation groundpathnavigation = (GroundPathNavigation)this.getNavigation();
        groundpathnavigation.setCanFloat(true);
        groundpathnavigation.setCanWalkOverFences(true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putLong("LastPoseTick", this.entityData.get(LAST_POSE_CHANGE_TICK));
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        long i = pCompound.getLong("LastPoseTick");
        if (i < 0L) {
            this.setPose(Pose.SITTING);
        }

        this.resetLastPoseChangeTick(i);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseHorseAttributes()
            .add(Attributes.MAX_HEALTH, 32.0)
            .add(Attributes.MOVEMENT_SPEED, 0.09F)
            .add(Attributes.JUMP_STRENGTH, 0.42F)
            .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DASH, false);
        pBuilder.define(LAST_POSE_CHANGE_TICK, 0L);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData) {
        CamelAi.initMemories(this, pLevel.getRandom());
        this.resetLastPoseChangeTickToFullStand(pLevel.getLevel().getGameTime());
        return super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
    }

    @Override
    protected Brain.Provider<Camel> brainProvider() {
        return CamelAi.brainProvider();
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> pDynamic) {
        return CamelAi.makeBrain(this.brainProvider().makeBrain(pDynamic));
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pPose) {
        return pPose == Pose.SITTING ? SITTING_DIMENSIONS.scale(this.getAgeScale()) : super.getDefaultDimensions(pPose);
    }

    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("camelBrain");
        Brain<?> brain = this.getBrain();
        ((Brain<Camel>)brain).tick((ServerLevel)this.level(), this);
        this.level().getProfiler().pop();
        this.level().getProfiler().push("camelActivityUpdate");
        CamelAi.updateActivity(this);
        this.level().getProfiler().pop();
        super.customServerAiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDashing() && this.dashCooldown < 50 && (this.onGround() || this.isInLiquid() || this.isPassenger())) {
            this.setDashing(false);
        }

        if (this.dashCooldown > 0) {
            this.dashCooldown--;
            if (this.dashCooldown == 0) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.CAMEL_DASH_READY, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }

        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }

        if (this.refuseToMove()) {
            this.clampHeadRotationToBody();
        }

        if (this.isCamelSitting() && this.isInWater()) {
            this.standUpInstantly();
        }
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.random.nextInt(40) + 80;
            this.idleAnimationState.start(this.tickCount);
        } else {
            this.idleAnimationTimeout--;
        }

        if (this.isCamelVisuallySitting()) {
            this.sitUpAnimationState.stop();
            this.dashAnimationState.stop();
            if (this.isVisuallySittingDown()) {
                this.sitAnimationState.startIfStopped(this.tickCount);
                this.sitPoseAnimationState.stop();
            } else {
                this.sitAnimationState.stop();
                this.sitPoseAnimationState.startIfStopped(this.tickCount);
            }
        } else {
            this.sitAnimationState.stop();
            this.sitPoseAnimationState.stop();
            this.dashAnimationState.animateWhen(this.isDashing(), this.tickCount);
            this.sitUpAnimationState.animateWhen(this.isInPoseTransition() && this.getPoseTime() >= 0L, this.tickCount);
        }
    }

    @Override
    protected void updateWalkAnimation(float pPartialTick) {
        float f;
        if (this.getPose() == Pose.STANDING && !this.dashAnimationState.isStarted()) {
            f = Math.min(pPartialTick * 6.0F, 1.0F);
        } else {
            f = 0.0F;
        }

        this.walkAnimation.update(f, 0.2F);
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.refuseToMove() && this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
            pTravelVector = pTravelVector.multiply(0.0, 1.0, 0.0);
        }

        super.travel(pTravelVector);
    }

    @Override
    protected void tickRidden(Player pPlayer, Vec3 pTravelVector) {
        super.tickRidden(pPlayer, pTravelVector);
        if (pPlayer.zza > 0.0F && this.isCamelSitting() && !this.isInPoseTransition()) {
            this.standUp();
        }
    }

    public boolean refuseToMove() {
        return this.isCamelSitting() || this.isInPoseTransition();
    }

    @Override
    protected float getRiddenSpeed(Player pPlayer) {
        float f = pPlayer.isSprinting() && this.getJumpCooldown() == 0 ? 0.1F : 0.0F;
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) + f;
    }

    @Override
    protected Vec2 getRiddenRotation(LivingEntity pEntity) {
        return this.refuseToMove() ? new Vec2(this.getXRot(), this.getYRot()) : super.getRiddenRotation(pEntity);
    }

    @Override
    protected Vec3 getRiddenInput(Player pPlayer, Vec3 pTravelVector) {
        return this.refuseToMove() ? Vec3.ZERO : super.getRiddenInput(pPlayer, pTravelVector);
    }

    @Override
    public boolean canJump() {
        return !this.refuseToMove() && super.canJump();
    }

    @Override
    public void onPlayerJump(int pJumpPower) {
        if (this.isSaddled() && this.dashCooldown <= 0 && this.onGround()) {
            super.onPlayerJump(pJumpPower);
        }
    }

    @Override
    public boolean canSprint() {
        return true;
    }

    @Override
    protected void executeRidersJump(float pPlayerJumpPendingScale, Vec3 pTravelVector) {
        double d0 = (double)this.getJumpPower();
        this.addDeltaMovement(
            this.getLookAngle()
                .multiply(1.0, 0.0, 1.0)
                .normalize()
                .scale((double)(22.2222F * pPlayerJumpPendingScale) * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (double)this.getBlockSpeedFactor())
                .add(0.0, (double)(1.4285F * pPlayerJumpPendingScale) * d0, 0.0)
        );
        this.dashCooldown = 55;
        this.setDashing(true);
        this.hasImpulse = true;
        net.neoforged.neoforge.common.CommonHooks.onLivingJump(this);
    }

    public boolean isDashing() {
        return this.entityData.get(DASH);
    }

    public void setDashing(boolean pDashing) {
        this.entityData.set(DASH, pDashing);
    }

    @Override
    public void handleStartJump(int pJumpPower) {
        this.makeSound(SoundEvents.CAMEL_DASH);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.setDashing(true);
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public int getJumpCooldown() {
        return this.dashCooldown;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMEL_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAMEL_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.CAMEL_HURT;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
        if (pBlock.is(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS)) {
            this.playSound(SoundEvents.CAMEL_STEP_SAND, 1.0F, 1.0F);
        } else {
            this.playSound(SoundEvents.CAMEL_STEP, 1.0F, 1.0F);
        }
    }

    /**
     * Checks if the parameter is an item which this animal can be fed to breed it (wheat, carrots or seeds depending on the animal type)
     */
    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(ItemTags.CAMEL_FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (pPlayer.isSecondaryUseActive() && !this.isBaby()) {
            this.openCustomInventoryScreen(pPlayer);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            InteractionResult interactionresult = itemstack.interactLivingEntity(pPlayer, this, pHand);
            if (interactionresult.consumesAction()) {
                return interactionresult;
            } else if (this.isFood(itemstack)) {
                return this.fedFood(pPlayer, itemstack);
            } else {
                if (this.getPassengers().size() < 2 && !this.isBaby()) {
                    this.doPlayerRide(pPlayer);
                }

                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
    }

    @Override
    public boolean handleLeashAtDistance(Entity p_352419_, float p_352358_) {
        if (p_352358_ > 6.0F && this.isCamelSitting() && !this.isInPoseTransition() && this.canCamelChangePose()) {
            this.standUp();
        }

        return true;
    }

    public boolean canCamelChangePose() {
        return this.wouldNotSuffocateAtTargetPose(this.isCamelSitting() ? Pose.STANDING : Pose.SITTING);
    }

    @Override
    protected boolean handleEating(Player pPlayer, ItemStack pStack) {
        if (!this.isFood(pStack)) {
            return false;
        } else {
            boolean flag = this.getHealth() < this.getMaxHealth();
            if (flag) {
                this.heal(2.0F);
            }

            boolean flag1 = this.isTamed() && this.getAge() == 0 && this.canFallInLove();
            if (flag1) {
                this.setInLove(pPlayer);
            }

            boolean flag2 = this.isBaby();
            if (flag2) {
                this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
                if (!this.level().isClientSide) {
                    this.ageUp(10);
                }
            }

            if (!flag && !flag1 && !flag2) {
                return false;
            } else {
                if (!this.isSilent()) {
                    SoundEvent soundevent = this.getEatingSound();
                    if (soundevent != null) {
                        this.level()
                            .playSound(
                                null,
                                this.getX(),
                                this.getY(),
                                this.getZ(),
                                soundevent,
                                this.getSoundSource(),
                                1.0F,
                                1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
                            );
                    }
                }

                this.gameEvent(GameEvent.EAT);
                return true;
            }
        }
    }

    @Override
    protected boolean canPerformRearing() {
        return false;
    }

    /**
     * Returns {@code true} if the mob is currently able to mate with the specified mob.
     */
    @Override
    public boolean canMate(Animal pOtherAnimal) {
        if (pOtherAnimal != this && pOtherAnimal instanceof Camel camel && this.canParent() && camel.canParent()) {
            return true;
        }

        return false;
    }

    @Nullable
    public Camel getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return EntityType.CAMEL.create(pLevel);
    }

    @Nullable
    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.CAMEL_EAT;
    }

    /**
     * Deals damage to the entity. This will take the armor of the entity into consideration before damaging the health bar.
     */
    @Override
    protected void actuallyHurt(DamageSource pDamageSource, float pDamageAmount) {
        this.standUpInstantly();
        super.actuallyHurt(pDamageSource, pDamageAmount);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity pEntity, EntityDimensions pDimensions, float pPartialTick) {
        int i = Math.max(this.getPassengers().indexOf(pEntity), 0);
        boolean flag = i == 0;
        float f = 0.5F;
        float f1 = (float)(this.isRemoved() ? 0.01F : this.getBodyAnchorAnimationYOffset(flag, 0.0F, pDimensions, pPartialTick));
        if (this.getPassengers().size() > 1) {
            if (!flag) {
                f = -0.7F;
            }

            if (pEntity instanceof Animal) {
                f += 0.2F;
            }
        }

        return new Vec3(0.0, (double)f1, (double)(f * pPartialTick)).yRot(-this.getYRot() * (float) (Math.PI / 180.0));
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.45F : 1.0F;
    }

    private double getBodyAnchorAnimationYOffset(boolean pFirstPassenger, float pPartialTick, EntityDimensions pDimensions, float pScale) {
        double d0 = (double)(pDimensions.height() - 0.375F * pScale);
        float f = pScale * 1.43F;
        float f1 = f - pScale * 0.2F;
        float f2 = f - f1;
        boolean flag = this.isInPoseTransition();
        boolean flag1 = this.isCamelSitting();
        if (flag) {
            int i = flag1 ? 40 : 52;
            int j;
            float f3;
            if (flag1) {
                j = 28;
                f3 = pFirstPassenger ? 0.5F : 0.1F;
            } else {
                j = pFirstPassenger ? 24 : 32;
                f3 = pFirstPassenger ? 0.6F : 0.35F;
            }

            float f4 = Mth.clamp((float)this.getPoseTime() + pPartialTick, 0.0F, (float)i);
            boolean flag2 = f4 < (float)j;
            float f5 = flag2 ? f4 / (float)j : (f4 - (float)j) / (float)(i - j);
            float f6 = f - f3 * f1;
            d0 += flag1 ? (double)Mth.lerp(f5, flag2 ? f : f6, flag2 ? f6 : f2) : (double)Mth.lerp(f5, flag2 ? f2 - f : f2 - f6, flag2 ? f2 - f6 : 0.0F);
        }

        if (flag1 && !flag) {
            d0 += (double)f2;
        }

        return d0;
    }

    @Override
    public Vec3 getLeashOffset(float pPartialTick) {
        EntityDimensions entitydimensions = this.getDimensions(this.getPose());
        float f = this.getAgeScale();
        return new Vec3(
            0.0, this.getBodyAnchorAnimationYOffset(true, pPartialTick, entitydimensions, f) - (double)(0.2F * f), (double)(entitydimensions.width() * 0.56F)
        );
    }

    @Override
    public int getMaxHeadYRot() {
        return 30;
    }

    @Override
    protected boolean canAddPassenger(Entity pPassenger) {
        return this.getPassengers().size() <= 2;
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    public boolean isCamelSitting() {
        return this.entityData.get(LAST_POSE_CHANGE_TICK) < 0L;
    }

    public boolean isCamelVisuallySitting() {
        return this.getPoseTime() < 0L != this.isCamelSitting();
    }

    public boolean isInPoseTransition() {
        long i = this.getPoseTime();
        return i < (long)(this.isCamelSitting() ? 40 : 52);
    }

    private boolean isVisuallySittingDown() {
        return this.isCamelSitting() && this.getPoseTime() < 40L && this.getPoseTime() >= 0L;
    }

    public void sitDown() {
        if (!this.isCamelSitting()) {
            this.makeSound(SoundEvents.CAMEL_SIT);
            this.setPose(Pose.SITTING);
            this.gameEvent(GameEvent.ENTITY_ACTION);
            this.resetLastPoseChangeTick(-this.level().getGameTime());
        }
    }

    public void standUp() {
        if (this.isCamelSitting()) {
            this.makeSound(SoundEvents.CAMEL_STAND);
            this.setPose(Pose.STANDING);
            this.gameEvent(GameEvent.ENTITY_ACTION);
            this.resetLastPoseChangeTick(this.level().getGameTime());
        }
    }

    public void standUpInstantly() {
        this.setPose(Pose.STANDING);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.resetLastPoseChangeTickToFullStand(this.level().getGameTime());
    }

    @VisibleForTesting
    public void resetLastPoseChangeTick(long pLastPoseChangeTick) {
        this.entityData.set(LAST_POSE_CHANGE_TICK, pLastPoseChangeTick);
    }

    private void resetLastPoseChangeTickToFullStand(long pLastPoseChangedTick) {
        this.resetLastPoseChangeTick(Math.max(0L, pLastPoseChangedTick - 52L - 1L));
    }

    public long getPoseTime() {
        return this.level().getGameTime() - Math.abs(this.entityData.get(LAST_POSE_CHANGE_TICK));
    }

    @Override
    public SoundEvent getSaddleSoundEvent() {
        return SoundEvents.CAMEL_SADDLE;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (!this.firstTick && DASH.equals(pKey)) {
            this.dashCooldown = this.dashCooldown == 0 ? 55 : this.dashCooldown;
        }

        super.onSyncedDataUpdated(pKey);
    }

    @Override
    public boolean isTamed() {
        return true;
    }

    @Override
    public void openCustomInventoryScreen(Player pPlayer) {
        if (!this.level().isClientSide) {
            pPlayer.openHorseInventory(this, this.inventory);
        }
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new Camel.CamelBodyRotationControl(this);
    }

    class CamelBodyRotationControl extends BodyRotationControl {
        public CamelBodyRotationControl(Camel pCamel) {
            super(pCamel);
        }

        @Override
        public void clientTick() {
            if (!Camel.this.refuseToMove()) {
                super.clientTick();
            }
        }
    }

    class CamelLookControl extends LookControl {
        CamelLookControl() {
            super(Camel.this);
        }

        @Override
        public void tick() {
            if (!Camel.this.hasControllingPassenger()) {
                super.tick();
            }
        }
    }

    class CamelMoveControl extends MoveControl {
        public CamelMoveControl() {
            super(Camel.this);
        }

        @Override
        public void tick() {
            if (this.operation == MoveControl.Operation.MOVE_TO
                && !Camel.this.isLeashed()
                && Camel.this.isCamelSitting()
                && !Camel.this.isInPoseTransition()
                && Camel.this.canCamelChangePose()) {
                Camel.this.standUp();
            }

            super.tick();
        }
    }
}
