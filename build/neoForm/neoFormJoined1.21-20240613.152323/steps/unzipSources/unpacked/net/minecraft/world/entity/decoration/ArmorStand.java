package net.minecraft.world.entity.decoration;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArmorStand extends LivingEntity {
    public static final int WOBBLE_TIME = 5;
    private static final boolean ENABLE_ARMS = true;
    private static final Rotations DEFAULT_HEAD_POSE = new Rotations(0.0F, 0.0F, 0.0F);
    private static final Rotations DEFAULT_BODY_POSE = new Rotations(0.0F, 0.0F, 0.0F);
    private static final Rotations DEFAULT_LEFT_ARM_POSE = new Rotations(-10.0F, 0.0F, -10.0F);
    private static final Rotations DEFAULT_RIGHT_ARM_POSE = new Rotations(-15.0F, 0.0F, 10.0F);
    private static final Rotations DEFAULT_LEFT_LEG_POSE = new Rotations(-1.0F, 0.0F, -1.0F);
    private static final Rotations DEFAULT_RIGHT_LEG_POSE = new Rotations(1.0F, 0.0F, 1.0F);
    private static final EntityDimensions MARKER_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);
    private static final EntityDimensions BABY_DIMENSIONS = EntityType.ARMOR_STAND.getDimensions().scale(0.5F).withEyeHeight(0.9875F);
    private static final double FEET_OFFSET = 0.1;
    private static final double CHEST_OFFSET = 0.9;
    private static final double LEGS_OFFSET = 0.4;
    private static final double HEAD_OFFSET = 1.6;
    public static final int DISABLE_TAKING_OFFSET = 8;
    public static final int DISABLE_PUTTING_OFFSET = 16;
    public static final int CLIENT_FLAG_SMALL = 1;
    public static final int CLIENT_FLAG_SHOW_ARMS = 4;
    public static final int CLIENT_FLAG_NO_BASEPLATE = 8;
    public static final int CLIENT_FLAG_MARKER = 16;
    public static final EntityDataAccessor<Byte> DATA_CLIENT_FLAGS = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Rotations> DATA_HEAD_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_BODY_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_LEFT_ARM_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_RIGHT_ARM_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_LEFT_LEG_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_RIGHT_LEG_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    private static final Predicate<Entity> RIDABLE_MINECARTS = p_31582_ -> p_31582_ instanceof AbstractMinecart
            && ((AbstractMinecart)p_31582_).canBeRidden();
    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
    private boolean invisible;
    /**
     * After punching the stand, the cooldown before you can punch it again without breaking it.
     */
    public long lastHit;
    private int disabledSlots;
    private Rotations headPose = DEFAULT_HEAD_POSE;
    private Rotations bodyPose = DEFAULT_BODY_POSE;
    private Rotations leftArmPose = DEFAULT_LEFT_ARM_POSE;
    private Rotations rightArmPose = DEFAULT_RIGHT_ARM_POSE;
    private Rotations leftLegPose = DEFAULT_LEFT_LEG_POSE;
    private Rotations rightLegPose = DEFAULT_RIGHT_LEG_POSE;

    public ArmorStand(EntityType<? extends ArmorStand> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ArmorStand(Level pLevel, double pX, double pY, double pZ) {
        this(EntityType.ARMOR_STAND, pLevel);
        this.setPos(pX, pY, pZ);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivingAttributes().add(Attributes.STEP_HEIGHT, 0.0);
    }

    @Override
    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    private boolean hasPhysics() {
        return !this.isMarker() && !this.isNoGravity();
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && this.hasPhysics();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_CLIENT_FLAGS, (byte)0);
        pBuilder.define(DATA_HEAD_POSE, DEFAULT_HEAD_POSE);
        pBuilder.define(DATA_BODY_POSE, DEFAULT_BODY_POSE);
        pBuilder.define(DATA_LEFT_ARM_POSE, DEFAULT_LEFT_ARM_POSE);
        pBuilder.define(DATA_RIGHT_ARM_POSE, DEFAULT_RIGHT_ARM_POSE);
        pBuilder.define(DATA_LEFT_LEG_POSE, DEFAULT_LEFT_LEG_POSE);
        pBuilder.define(DATA_RIGHT_LEG_POSE, DEFAULT_RIGHT_LEG_POSE);
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return this.handItems;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.armorItems;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot pSlot) {
        switch (pSlot.getType()) {
            case HAND:
                return this.handItems.get(pSlot.getIndex());
            case HUMANOID_ARMOR:
                return this.armorItems.get(pSlot.getIndex());
            default:
                return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canUseSlot(EquipmentSlot pSlot) {
        return pSlot != EquipmentSlot.BODY;
    }

    @Override
    public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
        this.verifyEquippedItem(pStack);
        switch (pSlot.getType()) {
            case HAND:
                this.onEquipItem(pSlot, this.handItems.set(pSlot.getIndex(), pStack), pStack);
                break;
            case HUMANOID_ARMOR:
                this.onEquipItem(pSlot, this.armorItems.set(pSlot.getIndex(), pStack), pStack);
        }
    }

    @Override
    public boolean canTakeItem(ItemStack pItemstack) {
        EquipmentSlot equipmentslot = this.getEquipmentSlotForItem(pItemstack);
        return this.getItemBySlot(equipmentslot).isEmpty() && !this.isDisabled(equipmentslot);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        ListTag listtag = new ListTag();

        for (ItemStack itemstack : this.armorItems) {
            listtag.add(itemstack.saveOptional(this.registryAccess()));
        }

        pCompound.put("ArmorItems", listtag);
        ListTag listtag1 = new ListTag();

        for (ItemStack itemstack1 : this.handItems) {
            listtag1.add(itemstack1.saveOptional(this.registryAccess()));
        }

        pCompound.put("HandItems", listtag1);
        pCompound.putBoolean("Invisible", this.isInvisible());
        pCompound.putBoolean("Small", this.isSmall());
        pCompound.putBoolean("ShowArms", this.isShowArms());
        pCompound.putInt("DisabledSlots", this.disabledSlots);
        pCompound.putBoolean("NoBasePlate", this.isNoBasePlate());
        if (this.isMarker()) {
            pCompound.putBoolean("Marker", this.isMarker());
        }

        pCompound.put("Pose", this.writePose());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("ArmorItems", 9)) {
            ListTag listtag = pCompound.getList("ArmorItems", 10);

            for (int i = 0; i < this.armorItems.size(); i++) {
                CompoundTag compoundtag = listtag.getCompound(i);
                this.armorItems.set(i, ItemStack.parseOptional(this.registryAccess(), compoundtag));
            }
        }

        if (pCompound.contains("HandItems", 9)) {
            ListTag listtag1 = pCompound.getList("HandItems", 10);

            for (int j = 0; j < this.handItems.size(); j++) {
                CompoundTag compoundtag2 = listtag1.getCompound(j);
                this.handItems.set(j, ItemStack.parseOptional(this.registryAccess(), compoundtag2));
            }
        }

        this.setInvisible(pCompound.getBoolean("Invisible"));
        this.setSmall(pCompound.getBoolean("Small"));
        this.setShowArms(pCompound.getBoolean("ShowArms"));
        this.disabledSlots = pCompound.getInt("DisabledSlots");
        this.setNoBasePlate(pCompound.getBoolean("NoBasePlate"));
        this.setMarker(pCompound.getBoolean("Marker"));
        this.noPhysics = !this.hasPhysics();
        CompoundTag compoundtag1 = pCompound.getCompound("Pose");
        this.readPose(compoundtag1);
    }

    private void readPose(CompoundTag pCompound) {
        ListTag listtag = pCompound.getList("Head", 5);
        this.setHeadPose(listtag.isEmpty() ? DEFAULT_HEAD_POSE : new Rotations(listtag));
        ListTag listtag1 = pCompound.getList("Body", 5);
        this.setBodyPose(listtag1.isEmpty() ? DEFAULT_BODY_POSE : new Rotations(listtag1));
        ListTag listtag2 = pCompound.getList("LeftArm", 5);
        this.setLeftArmPose(listtag2.isEmpty() ? DEFAULT_LEFT_ARM_POSE : new Rotations(listtag2));
        ListTag listtag3 = pCompound.getList("RightArm", 5);
        this.setRightArmPose(listtag3.isEmpty() ? DEFAULT_RIGHT_ARM_POSE : new Rotations(listtag3));
        ListTag listtag4 = pCompound.getList("LeftLeg", 5);
        this.setLeftLegPose(listtag4.isEmpty() ? DEFAULT_LEFT_LEG_POSE : new Rotations(listtag4));
        ListTag listtag5 = pCompound.getList("RightLeg", 5);
        this.setRightLegPose(listtag5.isEmpty() ? DEFAULT_RIGHT_LEG_POSE : new Rotations(listtag5));
    }

    private CompoundTag writePose() {
        CompoundTag compoundtag = new CompoundTag();
        if (!DEFAULT_HEAD_POSE.equals(this.headPose)) {
            compoundtag.put("Head", this.headPose.save());
        }

        if (!DEFAULT_BODY_POSE.equals(this.bodyPose)) {
            compoundtag.put("Body", this.bodyPose.save());
        }

        if (!DEFAULT_LEFT_ARM_POSE.equals(this.leftArmPose)) {
            compoundtag.put("LeftArm", this.leftArmPose.save());
        }

        if (!DEFAULT_RIGHT_ARM_POSE.equals(this.rightArmPose)) {
            compoundtag.put("RightArm", this.rightArmPose.save());
        }

        if (!DEFAULT_LEFT_LEG_POSE.equals(this.leftLegPose)) {
            compoundtag.put("LeftLeg", this.leftLegPose.save());
        }

        if (!DEFAULT_RIGHT_LEG_POSE.equals(this.rightLegPose)) {
            compoundtag.put("RightLeg", this.rightLegPose.save());
        }

        return compoundtag;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity pEntity) {
    }

    @Override
    protected void pushEntities() {
        for (Entity entity : this.level().getEntities(this, this.getBoundingBox(), RIDABLE_MINECARTS)) {
            if (this.distanceToSqr(entity) <= 0.2) {
                entity.push(this);
            }
        }
    }

    /**
     * Applies the given player interaction to this Entity.
     */
    @Override
    public InteractionResult interactAt(Player pPlayer, Vec3 pVec, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (this.isMarker() || itemstack.is(Items.NAME_TAG)) {
            return InteractionResult.PASS;
        } else if (pPlayer.isSpectator()) {
            return InteractionResult.SUCCESS;
        } else if (pPlayer.level().isClientSide) {
            return InteractionResult.CONSUME;
        } else {
            EquipmentSlot equipmentslot = this.getEquipmentSlotForItem(itemstack);
            if (itemstack.isEmpty()) {
                EquipmentSlot equipmentslot1 = this.getClickedSlot(pVec);
                EquipmentSlot equipmentslot2 = this.isDisabled(equipmentslot1) ? equipmentslot : equipmentslot1;
                if (this.hasItemInSlot(equipmentslot2) && this.swapItem(pPlayer, equipmentslot2, itemstack, pHand)) {
                    return InteractionResult.SUCCESS;
                }
            } else {
                if (this.isDisabled(equipmentslot)) {
                    return InteractionResult.FAIL;
                }

                if (equipmentslot.getType() == EquipmentSlot.Type.HAND && !this.isShowArms()) {
                    return InteractionResult.FAIL;
                }

                if (this.swapItem(pPlayer, equipmentslot, itemstack, pHand)) {
                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.PASS;
        }
    }

    private EquipmentSlot getClickedSlot(Vec3 pVector) {
        EquipmentSlot equipmentslot = EquipmentSlot.MAINHAND;
        boolean flag = this.isSmall();
        double d0 = pVector.y / (double)(this.getScale() * this.getAgeScale());
        EquipmentSlot equipmentslot1 = EquipmentSlot.FEET;
        if (d0 >= 0.1 && d0 < 0.1 + (flag ? 0.8 : 0.45) && this.hasItemInSlot(equipmentslot1)) {
            equipmentslot = EquipmentSlot.FEET;
        } else if (d0 >= 0.9 + (flag ? 0.3 : 0.0) && d0 < 0.9 + (flag ? 1.0 : 0.7) && this.hasItemInSlot(EquipmentSlot.CHEST)) {
            equipmentslot = EquipmentSlot.CHEST;
        } else if (d0 >= 0.4 && d0 < 0.4 + (flag ? 1.0 : 0.8) && this.hasItemInSlot(EquipmentSlot.LEGS)) {
            equipmentslot = EquipmentSlot.LEGS;
        } else if (d0 >= 1.6 && this.hasItemInSlot(EquipmentSlot.HEAD)) {
            equipmentslot = EquipmentSlot.HEAD;
        } else if (!this.hasItemInSlot(EquipmentSlot.MAINHAND) && this.hasItemInSlot(EquipmentSlot.OFFHAND)) {
            equipmentslot = EquipmentSlot.OFFHAND;
        }

        return equipmentslot;
    }

    private boolean isDisabled(EquipmentSlot pSlot) {
        return (this.disabledSlots & 1 << pSlot.getFilterFlag()) != 0 || pSlot.getType() == EquipmentSlot.Type.HAND && !this.isShowArms();
    }

    private boolean swapItem(Player pPlayer, EquipmentSlot pSlot, ItemStack pStack, InteractionHand pHand) {
        ItemStack itemstack = this.getItemBySlot(pSlot);
        if (!itemstack.isEmpty() && (this.disabledSlots & 1 << pSlot.getFilterFlag() + 8) != 0) {
            return false;
        } else if (itemstack.isEmpty() && (this.disabledSlots & 1 << pSlot.getFilterFlag() + 16) != 0) {
            return false;
        } else if (pPlayer.hasInfiniteMaterials() && itemstack.isEmpty() && !pStack.isEmpty()) {
            this.setItemSlot(pSlot, pStack.copyWithCount(1));
            return true;
        } else if (pStack.isEmpty() || pStack.getCount() <= 1) {
            this.setItemSlot(pSlot, pStack);
            pPlayer.setItemInHand(pHand, itemstack);
            return true;
        } else if (!itemstack.isEmpty()) {
            return false;
        } else {
            this.setItemSlot(pSlot, pStack.split(1));
            return true;
        }
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isRemoved()) {
            return false;
        } else if (this.level() instanceof ServerLevel serverlevel) {
            if (pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                this.kill();
                return false;
            } else if (this.isInvulnerableTo(pSource) || this.invisible || this.isMarker()) {
                return false;
            } else if (pSource.is(DamageTypeTags.IS_EXPLOSION)) {
                this.brokenByAnything(serverlevel, pSource);
                this.kill();
                return false;
            } else if (pSource.is(DamageTypeTags.IGNITES_ARMOR_STANDS)) {
                if (this.isOnFire()) {
                    this.causeDamage(serverlevel, pSource, 0.15F);
                } else {
                    this.igniteForSeconds(5.0F);
                }

                return false;
            } else if (pSource.is(DamageTypeTags.BURNS_ARMOR_STANDS) && this.getHealth() > 0.5F) {
                this.causeDamage(serverlevel, pSource, 4.0F);
                return false;
            } else {
                boolean flag1 = pSource.is(DamageTypeTags.CAN_BREAK_ARMOR_STAND);
                boolean flag = pSource.is(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS);
                if (!flag1 && !flag) {
                    return false;
                } else {
                    if (pSource.getEntity() instanceof Player player && !player.getAbilities().mayBuild) {
                        return false;
                    }

                    if (pSource.isCreativePlayer()) {
                        this.playBrokenSound();
                        this.showBreakingParticles();
                        this.kill();
                        return true;
                    } else {
                        long i = serverlevel.getGameTime();
                        if (i - this.lastHit > 5L && !flag) {
                            serverlevel.broadcastEntityEvent(this, (byte)32);
                            this.gameEvent(GameEvent.ENTITY_DAMAGE, pSource.getEntity());
                            this.lastHit = i;
                        } else {
                            this.brokenByPlayer(serverlevel, pSource);
                            this.showBreakingParticles();
                            this.kill();
                        }

                        return true;
                    }
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Handler for {@link World#setEntityState}
     */
    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == 32) {
            if (this.level().isClientSide) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_HIT, this.getSoundSource(), 0.3F, 1.0F, false);
                this.lastHit = this.level().getGameTime();
            }
        } else {
            super.handleEntityEvent(pId);
        }
    }

    /**
     * Checks if the entity is in range to render.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        double d0 = this.getBoundingBox().getSize() * 4.0;
        if (Double.isNaN(d0) || d0 == 0.0) {
            d0 = 4.0;
        }

        d0 *= 64.0;
        return pDistance < d0 * d0;
    }

    private void showBreakingParticles() {
        if (this.level() instanceof ServerLevel) {
            ((ServerLevel)this.level())
                .sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()),
                    this.getX(),
                    this.getY(0.6666666666666666),
                    this.getZ(),
                    10,
                    (double)(this.getBbWidth() / 4.0F),
                    (double)(this.getBbHeight() / 4.0F),
                    (double)(this.getBbWidth() / 4.0F),
                    0.05
                );
        }
    }

    private void causeDamage(ServerLevel p_348633_, DamageSource p_31649_, float p_31650_) {
        float f = this.getHealth();
        f -= p_31650_;
        if (f <= 0.5F) {
            this.brokenByAnything(p_348633_, p_31649_);
            this.kill();
        } else {
            this.setHealth(f);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, p_31649_.getEntity());
        }
    }

    private void brokenByPlayer(ServerLevel p_348677_, DamageSource p_31647_) {
        ItemStack itemstack = new ItemStack(Items.ARMOR_STAND);
        itemstack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        Block.popResource(this.level(), this.blockPosition(), itemstack);
        this.brokenByAnything(p_348677_, p_31647_);
    }

    private void brokenByAnything(ServerLevel p_348553_, DamageSource p_31654_) {
        this.playBrokenSound();
        this.dropAllDeathLoot(p_348553_, p_31654_);

        for (int i = 0; i < this.handItems.size(); i++) {
            ItemStack itemstack = this.handItems.get(i);
            if (!itemstack.isEmpty()) {
                Block.popResource(this.level(), this.blockPosition().above(), itemstack);
                this.handItems.set(i, ItemStack.EMPTY);
            }
        }

        for (int j = 0; j < this.armorItems.size(); j++) {
            ItemStack itemstack1 = this.armorItems.get(j);
            if (!itemstack1.isEmpty()) {
                Block.popResource(this.level(), this.blockPosition().above(), itemstack1);
                this.armorItems.set(j, ItemStack.EMPTY);
            }
        }
    }

    private void playBrokenSound() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_BREAK, this.getSoundSource(), 1.0F, 1.0F);
    }

    @Override
    protected float tickHeadTurn(float pYRot, float pAnimStep) {
        this.yBodyRotO = this.yRotO;
        this.yBodyRot = this.getYRot();
        return 0.0F;
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.hasPhysics()) {
            super.travel(pTravelVector);
        }
    }

    /**
     * Set the body Y rotation of the entity.
     */
    @Override
    public void setYBodyRot(float pOffset) {
        this.yBodyRotO = this.yRotO = pOffset;
        this.yHeadRotO = this.yHeadRot = pOffset;
    }

    /**
     * Sets the head's Y rotation of the entity.
     */
    @Override
    public void setYHeadRot(float pRotation) {
        this.yBodyRotO = this.yRotO = pRotation;
        this.yHeadRotO = this.yHeadRot = pRotation;
    }

    @Override
    public void tick() {
        super.tick();
        Rotations rotations = this.entityData.get(DATA_HEAD_POSE);
        if (!this.headPose.equals(rotations)) {
            this.setHeadPose(rotations);
        }

        Rotations rotations1 = this.entityData.get(DATA_BODY_POSE);
        if (!this.bodyPose.equals(rotations1)) {
            this.setBodyPose(rotations1);
        }

        Rotations rotations2 = this.entityData.get(DATA_LEFT_ARM_POSE);
        if (!this.leftArmPose.equals(rotations2)) {
            this.setLeftArmPose(rotations2);
        }

        Rotations rotations3 = this.entityData.get(DATA_RIGHT_ARM_POSE);
        if (!this.rightArmPose.equals(rotations3)) {
            this.setRightArmPose(rotations3);
        }

        Rotations rotations4 = this.entityData.get(DATA_LEFT_LEG_POSE);
        if (!this.leftLegPose.equals(rotations4)) {
            this.setLeftLegPose(rotations4);
        }

        Rotations rotations5 = this.entityData.get(DATA_RIGHT_LEG_POSE);
        if (!this.rightLegPose.equals(rotations5)) {
            this.setRightLegPose(rotations5);
        }
    }

    @Override
    protected void updateInvisibilityStatus() {
        this.setInvisible(this.invisible);
    }

    @Override
    public void setInvisible(boolean pInvisible) {
        this.invisible = pInvisible;
        super.setInvisible(pInvisible);
    }

    @Override
    public boolean isBaby() {
        return this.isSmall();
    }

    @Override
    public void kill() {
        this.remove(Entity.RemovalReason.KILLED);
        this.gameEvent(GameEvent.ENTITY_DIE);
    }

    @Override
    public boolean ignoreExplosion(Explosion pExplosion) {
        return this.isInvisible();
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return this.isMarker() ? PushReaction.IGNORE : super.getPistonPushReaction();
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return this.isMarker();
    }

    private void setSmall(boolean pSmall) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 1, pSmall));
    }

    public boolean isSmall() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 1) != 0;
    }

    public void setShowArms(boolean pShowArms) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 4, pShowArms));
    }

    public boolean isShowArms() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 4) != 0;
    }

    public void setNoBasePlate(boolean pNoBasePlate) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 8, pNoBasePlate));
    }

    public boolean isNoBasePlate() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 8) != 0;
    }

    /**
     * Marker defines where if true, the size is 0 and will not be rendered or intractable.
     */
    private void setMarker(boolean pMarker) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 16, pMarker));
    }

    public boolean isMarker() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 16) != 0;
    }

    private byte setBit(byte pOldBit, int pOffset, boolean pValue) {
        if (pValue) {
            pOldBit = (byte)(pOldBit | pOffset);
        } else {
            pOldBit = (byte)(pOldBit & ~pOffset);
        }

        return pOldBit;
    }

    public void setHeadPose(Rotations pHeadPose) {
        this.headPose = pHeadPose;
        this.entityData.set(DATA_HEAD_POSE, pHeadPose);
    }

    public void setBodyPose(Rotations pBodyPose) {
        this.bodyPose = pBodyPose;
        this.entityData.set(DATA_BODY_POSE, pBodyPose);
    }

    public void setLeftArmPose(Rotations pLeftArmPose) {
        this.leftArmPose = pLeftArmPose;
        this.entityData.set(DATA_LEFT_ARM_POSE, pLeftArmPose);
    }

    public void setRightArmPose(Rotations pRightArmPose) {
        this.rightArmPose = pRightArmPose;
        this.entityData.set(DATA_RIGHT_ARM_POSE, pRightArmPose);
    }

    public void setLeftLegPose(Rotations pLeftLegPose) {
        this.leftLegPose = pLeftLegPose;
        this.entityData.set(DATA_LEFT_LEG_POSE, pLeftLegPose);
    }

    public void setRightLegPose(Rotations pRightLegPose) {
        this.rightLegPose = pRightLegPose;
        this.entityData.set(DATA_RIGHT_LEG_POSE, pRightLegPose);
    }

    public Rotations getHeadPose() {
        return this.headPose;
    }

    public Rotations getBodyPose() {
        return this.bodyPose;
    }

    public Rotations getLeftArmPose() {
        return this.leftArmPose;
    }

    public Rotations getRightArmPose() {
        return this.rightArmPose;
    }

    public Rotations getLeftLegPose() {
        return this.leftLegPose;
    }

    public Rotations getRightLegPose() {
        return this.rightLegPose;
    }

    @Override
    public boolean isPickable() {
        return super.isPickable() && !this.isMarker();
    }

    /**
     * Called when a player attacks an entity. If this returns true the attack will not happen.
     */
    @Override
    public boolean skipAttackInteraction(Entity pEntity) {
        return pEntity instanceof Player && !this.level().mayInteract((Player)pEntity, this.blockPosition());
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.ARMOR_STAND_FALL, SoundEvents.ARMOR_STAND_FALL);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.ARMOR_STAND_HIT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }

    @Override
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_CLIENT_FLAGS.equals(pKey)) {
            this.refreshDimensions();
            this.blocksBuilding = !this.isMarker();
        }

        super.onSyncedDataUpdated(pKey);
    }

    @Override
    public boolean attackable() {
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pPose) {
        return this.getDimensionsMarker(this.isMarker());
    }

    private EntityDimensions getDimensionsMarker(boolean pIsMarker) {
        if (pIsMarker) {
            return MARKER_DIMENSIONS;
        } else {
            return this.isBaby() ? BABY_DIMENSIONS : this.getType().getDimensions();
        }
    }

    @Override
    public Vec3 getLightProbePosition(float pPartialTicks) {
        if (this.isMarker()) {
            AABB aabb = this.getDimensionsMarker(false).makeBoundingBox(this.position());
            BlockPos blockpos = this.blockPosition();
            int i = Integer.MIN_VALUE;

            for (BlockPos blockpos1 : BlockPos.betweenClosed(
                BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ), BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ)
            )) {
                int j = Math.max(this.level().getBrightness(LightLayer.BLOCK, blockpos1), this.level().getBrightness(LightLayer.SKY, blockpos1));
                if (j == 15) {
                    return Vec3.atCenterOf(blockpos1);
                }

                if (j > i) {
                    i = j;
                    blockpos = blockpos1.immutable();
                }
            }

            return Vec3.atCenterOf(blockpos);
        } else {
            return super.getLightProbePosition(pPartialTicks);
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.ARMOR_STAND);
    }

    @Override
    public boolean canBeSeenByAnyone() {
        return !this.isInvisible() && !this.isMarker();
    }
}
