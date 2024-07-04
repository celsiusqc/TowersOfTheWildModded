package net.minecraft.world.entity.boss.wither;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WitherBoss extends Monster implements PowerableMob, RangedAttackMob {
    private static final EntityDataAccessor<Integer> DATA_TARGET_A = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_B = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_C = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
    private static final List<EntityDataAccessor<Integer>> DATA_TARGETS = ImmutableList.of(DATA_TARGET_A, DATA_TARGET_B, DATA_TARGET_C);
    private static final EntityDataAccessor<Integer> DATA_ID_INV = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
    private static final int INVULNERABLE_TICKS = 220;
    private final float[] xRotHeads = new float[2];
    private final float[] yRotHeads = new float[2];
    private final float[] xRotOHeads = new float[2];
    private final float[] yRotOHeads = new float[2];
    private final int[] nextHeadUpdate = new int[2];
    private final int[] idleHeadUpdates = new int[2];
    private int destroyBlocksTick;
    private final ServerBossEvent bossEvent = (ServerBossEvent)new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS
        )
        .setDarkenScreen(true);
    private static final Predicate<LivingEntity> LIVING_ENTITY_SELECTOR = p_348303_ -> !p_348303_.getType().is(EntityTypeTags.WITHER_FRIENDS)
            && p_348303_.attackable();
    private static final TargetingConditions TARGETING_CONDITIONS = TargetingConditions.forCombat().range(20.0).selector(LIVING_ENTITY_SELECTOR);

    public WitherBoss(EntityType<? extends WitherBoss> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setHealth(this.getMaxHealth());
        this.xpReward = 50;
    }

    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel);
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(true);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new WitherBoss.WitherDoNothingGoal());
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 20.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, false, false, LIVING_ENTITY_SELECTOR));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_TARGET_A, 0);
        pBuilder.define(DATA_TARGET_B, 0);
        pBuilder.define(DATA_TARGET_C, 0);
        pBuilder.define(DATA_ID_INV, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Invul", this.getInvulnerableTicks());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setInvulnerableTicks(pCompound.getInt("Invul"));
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component pName) {
        super.setCustomName(pName);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public void aiStep() {
        Vec3 vec3 = this.getDeltaMovement().multiply(1.0, 0.6, 1.0);
        if (!this.level().isClientSide && this.getAlternativeTarget(0) > 0) {
            Entity entity = this.level().getEntity(this.getAlternativeTarget(0));
            if (entity != null) {
                double d0 = vec3.y;
                if (this.getY() < entity.getY() || !this.isPowered() && this.getY() < entity.getY() + 5.0) {
                    d0 = Math.max(0.0, d0);
                    d0 += 0.3 - d0 * 0.6F;
                }

                vec3 = new Vec3(vec3.x, d0, vec3.z);
                Vec3 vec31 = new Vec3(entity.getX() - this.getX(), 0.0, entity.getZ() - this.getZ());
                if (vec31.horizontalDistanceSqr() > 9.0) {
                    Vec3 vec32 = vec31.normalize();
                    vec3 = vec3.add(vec32.x * 0.3 - vec3.x * 0.6, 0.0, vec32.z * 0.3 - vec3.z * 0.6);
                }
            }
        }

        this.setDeltaMovement(vec3);
        if (vec3.horizontalDistanceSqr() > 0.05) {
            this.setYRot((float)Mth.atan2(vec3.z, vec3.x) * (180.0F / (float)Math.PI) - 90.0F);
        }

        super.aiStep();

        for (int i = 0; i < 2; i++) {
            this.yRotOHeads[i] = this.yRotHeads[i];
            this.xRotOHeads[i] = this.xRotHeads[i];
        }

        for (int j = 0; j < 2; j++) {
            int k = this.getAlternativeTarget(j + 1);
            Entity entity1 = null;
            if (k > 0) {
                entity1 = this.level().getEntity(k);
            }

            if (entity1 != null) {
                double d9 = this.getHeadX(j + 1);
                double d1 = this.getHeadY(j + 1);
                double d3 = this.getHeadZ(j + 1);
                double d4 = entity1.getX() - d9;
                double d5 = entity1.getEyeY() - d1;
                double d6 = entity1.getZ() - d3;
                double d7 = Math.sqrt(d4 * d4 + d6 * d6);
                float f1 = (float)(Mth.atan2(d6, d4) * 180.0F / (float)Math.PI) - 90.0F;
                float f2 = (float)(-(Mth.atan2(d5, d7) * 180.0F / (float)Math.PI));
                this.xRotHeads[j] = this.rotlerp(this.xRotHeads[j], f2, 40.0F);
                this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], f1, 10.0F);
            } else {
                this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], this.yBodyRot, 10.0F);
            }
        }

        boolean flag = this.isPowered();

        for (int l = 0; l < 3; l++) {
            double d8 = this.getHeadX(l);
            double d10 = this.getHeadY(l);
            double d2 = this.getHeadZ(l);
            float f = 0.3F * this.getScale();
            this.level()
                .addParticle(
                    ParticleTypes.SMOKE,
                    d8 + this.random.nextGaussian() * (double)f,
                    d10 + this.random.nextGaussian() * (double)f,
                    d2 + this.random.nextGaussian() * (double)f,
                    0.0,
                    0.0,
                    0.0
                );
            if (flag && this.level().random.nextInt(4) == 0) {
                this.level()
                    .addParticle(
                        ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.7F, 0.7F, 0.5F),
                        d8 + this.random.nextGaussian() * (double)f,
                        d10 + this.random.nextGaussian() * (double)f,
                        d2 + this.random.nextGaussian() * (double)f,
                        0.0,
                        0.0,
                        0.0
                    );
            }
        }

        if (this.getInvulnerableTicks() > 0) {
            float f3 = 3.3F * this.getScale();

            for (int i1 = 0; i1 < 3; i1++) {
                this.level()
                    .addParticle(
                        ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.7F, 0.7F, 0.9F),
                        this.getX() + this.random.nextGaussian(),
                        this.getY() + (double)(this.random.nextFloat() * f3),
                        this.getZ() + this.random.nextGaussian(),
                        0.0,
                        0.0,
                        0.0
                    );
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        if (this.getInvulnerableTicks() > 0) {
            int j = this.getInvulnerableTicks() - 1;
            this.bossEvent.setProgress(1.0F - (float)j / 220.0F);
            if (j <= 0) {
                this.level().explode(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, Level.ExplosionInteraction.MOB);
                if (!this.isSilent()) {
                    this.level().globalLevelEvent(1023, this.blockPosition(), 0);
                }
            }

            this.setInvulnerableTicks(j);
            if (this.tickCount % 10 == 0) {
                this.heal(10.0F);
            }
        } else {
            super.customServerAiStep();

            for (int i = 1; i < 3; i++) {
                if (this.tickCount >= this.nextHeadUpdate[i - 1]) {
                    this.nextHeadUpdate[i - 1] = this.tickCount + 10 + this.random.nextInt(10);
                    if ((this.level().getDifficulty() == Difficulty.NORMAL || this.level().getDifficulty() == Difficulty.HARD)
                        && this.idleHeadUpdates[i - 1]++ > 15) {
                        float f = 10.0F;
                        float f1 = 5.0F;
                        double d0 = Mth.nextDouble(this.random, this.getX() - 10.0, this.getX() + 10.0);
                        double d1 = Mth.nextDouble(this.random, this.getY() - 5.0, this.getY() + 5.0);
                        double d2 = Mth.nextDouble(this.random, this.getZ() - 10.0, this.getZ() + 10.0);
                        this.performRangedAttack(i + 1, d0, d1, d2, true);
                        this.idleHeadUpdates[i - 1] = 0;
                    }

                    int k = this.getAlternativeTarget(i);
                    if (k > 0) {
                        LivingEntity livingentity = (LivingEntity)this.level().getEntity(k);
                        if (livingentity != null
                            && this.canAttack(livingentity)
                            && !(this.distanceToSqr(livingentity) > 900.0)
                            && this.hasLineOfSight(livingentity)) {
                            this.performRangedAttack(i + 1, livingentity);
                            this.nextHeadUpdate[i - 1] = this.tickCount + 40 + this.random.nextInt(20);
                            this.idleHeadUpdates[i - 1] = 0;
                        } else {
                            this.setAlternativeTarget(i, 0);
                        }
                    } else {
                        List<LivingEntity> list = this.level()
                            .getNearbyEntities(LivingEntity.class, TARGETING_CONDITIONS, this, this.getBoundingBox().inflate(20.0, 8.0, 20.0));
                        if (!list.isEmpty()) {
                            LivingEntity livingentity1 = list.get(this.random.nextInt(list.size()));
                            this.setAlternativeTarget(i, livingentity1.getId());
                        }
                    }
                }
            }

            if (this.getTarget() != null) {
                this.setAlternativeTarget(0, this.getTarget().getId());
            } else {
                this.setAlternativeTarget(0, 0);
            }

            if (this.destroyBlocksTick > 0) {
                this.destroyBlocksTick--;
                if (this.destroyBlocksTick == 0 && net.neoforged.neoforge.event.EventHooks.canEntityGrief(this.level(), this)) {
                    boolean flag = false;
                    int l = Mth.floor(this.getBbWidth() / 2.0F + 1.0F);
                    int i1 = Mth.floor(this.getBbHeight());

                    for (BlockPos blockpos : BlockPos.betweenClosed(
                        this.getBlockX() - l, this.getBlockY(), this.getBlockZ() - l, this.getBlockX() + l, this.getBlockY() + i1, this.getBlockZ() + l
                    )) {
                        BlockState blockstate = this.level().getBlockState(blockpos);
                        if (blockstate.canEntityDestroy(this.level(), blockpos, this) && net.neoforged.neoforge.event.EventHooks.onEntityDestroyBlock(this, blockpos, blockstate)) {
                            flag = this.level().destroyBlock(blockpos, true, this) || flag;
                        }
                    }

                    if (flag) {
                        this.level().levelEvent(null, 1022, this.blockPosition(), 0);
                    }
                }
            }

            if (this.tickCount % 20 == 0) {
                this.heal(1.0F);
            }

            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    /**
     * @deprecated Forge: Use {@link BlockState#canEntityDestroy(
     *             net.minecraft.world.level.BlockGetter, BlockPos, Entity)} instead.
     */
    @Deprecated
    public static boolean canDestroy(BlockState pState) {
        return !pState.isAir() && !pState.is(BlockTags.WITHER_IMMUNE);
    }

    public void makeInvulnerable() {
        this.setInvulnerableTicks(220);
        this.bossEvent.setProgress(0.0F);
        this.setHealth(this.getMaxHealth() / 3.0F);
    }

    @Override
    public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier) {
    }

    /**
     * Add the given player to the list of players tracking this entity. For instance, a player may track a boss in order to view its associated boss bar.
     */
    @Override
    public void startSeenByPlayer(ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        this.bossEvent.addPlayer(pPlayer);
    }

    /**
     * Removes the given player from the list of players tracking this entity. See {@link Entity#addTrackingPlayer} for more information on tracking.
     */
    @Override
    public void stopSeenByPlayer(ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        this.bossEvent.removePlayer(pPlayer);
    }

    private double getHeadX(int pHead) {
        if (pHead <= 0) {
            return this.getX();
        } else {
            float f = (this.yBodyRot + (float)(180 * (pHead - 1))) * (float) (Math.PI / 180.0);
            float f1 = Mth.cos(f);
            return this.getX() + (double)f1 * 1.3 * (double)this.getScale();
        }
    }

    private double getHeadY(int pHead) {
        float f = pHead <= 0 ? 3.0F : 2.2F;
        return this.getY() + (double)(f * this.getScale());
    }

    private double getHeadZ(int pHead) {
        if (pHead <= 0) {
            return this.getZ();
        } else {
            float f = (this.yBodyRot + (float)(180 * (pHead - 1))) * (float) (Math.PI / 180.0);
            float f1 = Mth.sin(f);
            return this.getZ() + (double)f1 * 1.3 * (double)this.getScale();
        }
    }

    private float rotlerp(float pAngle, float p_31444_, float p_31445_) {
        float f = Mth.wrapDegrees(p_31444_ - pAngle);
        if (f > p_31445_) {
            f = p_31445_;
        }

        if (f < -p_31445_) {
            f = -p_31445_;
        }

        return pAngle + f;
    }

    private void performRangedAttack(int pHead, LivingEntity pTarget) {
        this.performRangedAttack(
            pHead,
            pTarget.getX(),
            pTarget.getY() + (double)pTarget.getEyeHeight() * 0.5,
            pTarget.getZ(),
            pHead == 0 && this.random.nextFloat() < 0.001F
        );
    }

    /**
     * Launches a Wither skull toward (par2, par4, par6)
     */
    private void performRangedAttack(int pHead, double pX, double pY, double pZ, boolean pIsDangerous) {
        if (!this.isSilent()) {
            this.level().levelEvent(null, 1024, this.blockPosition(), 0);
        }

        double d0 = this.getHeadX(pHead);
        double d1 = this.getHeadY(pHead);
        double d2 = this.getHeadZ(pHead);
        double d3 = pX - d0;
        double d4 = pY - d1;
        double d5 = pZ - d2;
        Vec3 vec3 = new Vec3(d3, d4, d5);
        WitherSkull witherskull = new WitherSkull(this.level(), this, vec3.normalize());
        witherskull.setOwner(this);
        if (pIsDangerous) {
            witherskull.setDangerous(true);
        }

        witherskull.setPosRaw(d0, d1, d2);
        this.level().addFreshEntity(witherskull);
    }

    /**
     * Attack the specified entity using a ranged attack.
     */
    @Override
    public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {
        this.performRangedAttack(0, pTarget);
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isInvulnerableTo(pSource)) {
            return false;
        } else if (pSource.is(DamageTypeTags.WITHER_IMMUNE_TO) || pSource.getEntity() instanceof WitherBoss) {
            return false;
        } else if (this.getInvulnerableTicks() > 0 && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            if (this.isPowered()) {
                Entity entity = pSource.getDirectEntity();
                if (entity instanceof AbstractArrow || entity instanceof WindCharge) {
                    return false;
                }
            }

            Entity entity1 = pSource.getEntity();
            if (entity1 != null && entity1.getType().is(EntityTypeTags.WITHER_FRIENDS)) {
                return false;
            } else {
                if (this.destroyBlocksTick <= 0) {
                    this.destroyBlocksTick = 20;
                }

                for (int i = 0; i < this.idleHeadUpdates.length; i++) {
                    this.idleHeadUpdates[i] = this.idleHeadUpdates[i] + 3;
                }

                return super.hurt(pSource, pAmount);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel p_348644_, DamageSource p_31464_, boolean p_31466_) {
        super.dropCustomDeathLoot(p_348644_, p_31464_, p_31466_);
        ItemEntity itementity = this.spawnAtLocation(Items.NETHER_STAR);
        if (itementity != null) {
            itementity.setExtendedLifetime();
        }
    }

    @Override
    public void checkDespawn() {
        if (net.neoforged.neoforge.event.EventHooks.checkMobDespawn(this)) return;
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            this.discard();
        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    public boolean addEffect(MobEffectInstance pEffectInstance, @Nullable Entity pEntity) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.MOVEMENT_SPEED, 0.6F)
            .add(Attributes.FLYING_SPEED, 0.6F)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 4.0);
    }

    public float getHeadYRot(int pHead) {
        return this.yRotHeads[pHead];
    }

    public float getHeadXRot(int pHead) {
        return this.xRotHeads[pHead];
    }

    public int getInvulnerableTicks() {
        return this.entityData.get(DATA_ID_INV);
    }

    public void setInvulnerableTicks(int pInvulnerableTicks) {
        this.entityData.set(DATA_ID_INV, pInvulnerableTicks);
    }

    /**
     * Returns the target entity ID if present, or -1 if not
     *
     * @param pHead The target offset, should be from 0-2
     */
    public int getAlternativeTarget(int pHead) {
        return this.entityData.get(DATA_TARGETS.get(pHead));
    }

    /**
     * Updates the target entity ID
     */
    public void setAlternativeTarget(int pTargetOffset, int pNewId) {
        this.entityData.set(DATA_TARGETS.get(pTargetOffset), pNewId);
    }

    @Override
    public boolean isPowered() {
        return this.getHealth() <= this.getMaxHealth() / 2.0F;
    }

    @Override
    protected boolean canRide(Entity pEntity) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean p_352936_) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance pPotioneffect) {
        return pPotioneffect.is(MobEffects.WITHER) ? false : super.canBeAffected(pPotioneffect);
    }

    class WitherDoNothingGoal extends Goal {
        public WitherDoNothingGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return WitherBoss.this.getInvulnerableTicks() > 0;
        }
    }
}
