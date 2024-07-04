package net.minecraft.world.entity.decoration;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

public class ItemFrame extends HangingEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_ROTATION = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.INT);
    public static final int NUM_ROTATIONS = 8;
    private static final float DEPTH = 0.0625F;
    private static final float WIDTH = 0.75F;
    private static final float HEIGHT = 0.75F;
    private float dropChance = 1.0F;
    private boolean fixed;

    public ItemFrame(EntityType<? extends ItemFrame> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ItemFrame(Level pLevel, BlockPos pPos, Direction pFacingDirection) {
        this(EntityType.ITEM_FRAME, pLevel, pPos, pFacingDirection);
    }

    public ItemFrame(EntityType<? extends ItemFrame> pEntityType, Level pLevel, BlockPos pPos, Direction pDirection) {
        super(pEntityType, pLevel, pPos);
        this.setDirection(pDirection);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(DATA_ITEM, ItemStack.EMPTY);
        pBuilder.define(DATA_ROTATION, 0);
    }

    /**
     * Updates facing and bounding box based on it
     */
    @Override
    protected void setDirection(Direction pFacingDirection) {
        Validate.notNull(pFacingDirection);
        this.direction = pFacingDirection;
        if (pFacingDirection.getAxis().isHorizontal()) {
            this.setXRot(0.0F);
            this.setYRot((float)(this.direction.get2DDataValue() * 90));
        } else {
            this.setXRot((float)(-90 * pFacingDirection.getAxisDirection().getStep()));
            this.setYRot(0.0F);
        }

        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.recalculateBoundingBox();
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos p_345363_, Direction p_344892_) {
        float f = 0.46875F;
        Vec3 vec3 = Vec3.atCenterOf(p_345363_).relative(p_344892_, -0.46875);
        Direction.Axis direction$axis = p_344892_.getAxis();
        double d0 = direction$axis == Direction.Axis.X ? 0.0625 : 0.75;
        double d1 = direction$axis == Direction.Axis.Y ? 0.0625 : 0.75;
        double d2 = direction$axis == Direction.Axis.Z ? 0.0625 : 0.75;
        return AABB.ofSize(vec3, d0, d1, d2);
    }

    @Override
    public boolean survives() {
        if (this.fixed) {
            return true;
        } else if (!this.level().noCollision(this)) {
            return false;
        } else {
            BlockState blockstate = this.level().getBlockState(this.pos.relative(this.direction.getOpposite()));
            return blockstate.isSolid() || this.direction.getAxis().isHorizontal() && DiodeBlock.isDiode(blockstate)
                ? this.level().getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty()
                : false;
        }
    }

    @Override
    public void move(MoverType pType, Vec3 pPos) {
        if (!this.fixed) {
            super.move(pType, pPos);
        }
    }

    /**
     * Adds to the current velocity of the entity, and sets {@link #isAirBorne} to true.
     */
    @Override
    public void push(double pX, double pY, double pZ) {
        if (!this.fixed) {
            super.push(pX, pY, pZ);
        }
    }

    @Override
    public void kill() {
        this.removeFramedMap(this.getItem());
        super.kill();
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.fixed) {
            return !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !pSource.isCreativePlayer() ? false : super.hurt(pSource, pAmount);
        } else if (this.isInvulnerableTo(pSource)) {
            return false;
        } else if (!pSource.is(DamageTypeTags.IS_EXPLOSION) && !this.getItem().isEmpty()) {
            if (!this.level().isClientSide) {
                this.dropItem(pSource.getEntity(), false);
                this.gameEvent(GameEvent.BLOCK_CHANGE, pSource.getEntity());
                this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
            }

            return true;
        } else {
            return super.hurt(pSource, pAmount);
        }
    }

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ITEM_FRAME_REMOVE_ITEM;
    }

    /**
     * Checks if the entity is in range to render.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        double d0 = 16.0;
        d0 *= 64.0 * getViewScale();
        return pDistance < d0 * d0;
    }

    /**
     * Called when this entity is broken. Entity parameter may be null.
     */
    @Override
    public void dropItem(@Nullable Entity pBrokenEntity) {
        this.playSound(this.getBreakSound(), 1.0F, 1.0F);
        this.dropItem(pBrokenEntity, true);
        this.gameEvent(GameEvent.BLOCK_CHANGE, pBrokenEntity);
    }

    public SoundEvent getBreakSound() {
        return SoundEvents.ITEM_FRAME_BREAK;
    }

    @Override
    public void playPlacementSound() {
        this.playSound(this.getPlaceSound(), 1.0F, 1.0F);
    }

    public SoundEvent getPlaceSound() {
        return SoundEvents.ITEM_FRAME_PLACE;
    }

    private void dropItem(@Nullable Entity pEntity, boolean pDropSelf) {
        if (!this.fixed) {
            ItemStack itemstack = this.getItem();
            this.setItem(ItemStack.EMPTY);
            if (!this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                if (pEntity == null) {
                    this.removeFramedMap(itemstack);
                }
            } else {
                if (pEntity instanceof Player player && player.hasInfiniteMaterials()) {
                    this.removeFramedMap(itemstack);
                    return;
                }

                if (pDropSelf) {
                    this.spawnAtLocation(this.getFrameItemStack());
                }

                if (!itemstack.isEmpty()) {
                    itemstack = itemstack.copy();
                    this.removeFramedMap(itemstack);
                    if (this.random.nextFloat() < this.dropChance) {
                        this.spawnAtLocation(itemstack);
                    }
                }
            }
        }
    }

    /**
     * Removes the dot representing this frame's position from the map when the item frame is broken.
     */
    private void removeFramedMap(ItemStack pStack) {
        MapId mapid = this.getFramedMapId(pStack);
        if (mapid != null) {
            MapItemSavedData mapitemsaveddata = MapItem.getSavedData(mapid, this.level());
            if (mapitemsaveddata != null) {
                mapitemsaveddata.removedFromFrame(this.pos, this.getId());
                mapitemsaveddata.setDirty(true);
            }
        }

        pStack.setEntityRepresentation(null);
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    @Nullable
    public MapId getFramedMapId(ItemStack p_353029_) {
        return p_353029_.get(DataComponents.MAP_ID);
    }

    public boolean hasFramedMap() {
        return this.getItem().has(DataComponents.MAP_ID);
    }

    public void setItem(ItemStack p_31806_) {
        this.setItem(p_31806_, true);
    }

    public void setItem(ItemStack pStack, boolean pUpdateNeighbours) {
        if (!pStack.isEmpty()) {
            pStack = pStack.copyWithCount(1);
        }

        this.onItemChanged(pStack);
        this.getEntityData().set(DATA_ITEM, pStack);
        if (!pStack.isEmpty()) {
            this.playSound(this.getAddItemSound(), 1.0F, 1.0F);
        }

        if (pUpdateNeighbours && this.pos != null) {
            this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }
    }

    public SoundEvent getAddItemSound() {
        return SoundEvents.ITEM_FRAME_ADD_ITEM;
    }

    @Override
    public SlotAccess getSlot(int pSlot) {
        return pSlot == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(pSlot);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (pKey.equals(DATA_ITEM)) {
            this.onItemChanged(this.getItem());
        }
    }

    private void onItemChanged(ItemStack pItem) {
        if (!pItem.isEmpty() && pItem.getFrame() != this) {
            pItem.setEntityRepresentation(this);
        }

        this.recalculateBoundingBox();
    }

    public int getRotation() {
        return this.getEntityData().get(DATA_ROTATION);
    }

    public void setRotation(int pRotation) {
        this.setRotation(pRotation, true);
    }

    private void setRotation(int pRotation, boolean pUpdateNeighbours) {
        this.getEntityData().set(DATA_ROTATION, pRotation % 8);
        if (pUpdateNeighbours && this.pos != null) {
            this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (!this.getItem().isEmpty()) {
            pCompound.put("Item", this.getItem().save(this.registryAccess()));
            pCompound.putByte("ItemRotation", (byte)this.getRotation());
            pCompound.putFloat("ItemDropChance", this.dropChance);
        }

        pCompound.putByte("Facing", (byte)this.direction.get3DDataValue());
        pCompound.putBoolean("Invisible", this.isInvisible());
        pCompound.putBoolean("Fixed", this.fixed);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        ItemStack itemstack;
        if (pCompound.contains("Item", 10)) {
            CompoundTag compoundtag = pCompound.getCompound("Item");
            itemstack = ItemStack.parse(this.registryAccess(), compoundtag).orElse(ItemStack.EMPTY);
        } else {
            itemstack = ItemStack.EMPTY;
        }

        ItemStack itemstack1 = this.getItem();
        if (!itemstack1.isEmpty() && !ItemStack.matches(itemstack, itemstack1)) {
            this.removeFramedMap(itemstack1);
        }

        this.setItem(itemstack, false);
        if (!itemstack.isEmpty()) {
            this.setRotation(pCompound.getByte("ItemRotation"), false);
            if (pCompound.contains("ItemDropChance", 99)) {
                this.dropChance = pCompound.getFloat("ItemDropChance");
            }
        }

        this.setDirection(Direction.from3DDataValue(pCompound.getByte("Facing")));
        this.setInvisible(pCompound.getBoolean("Invisible"));
        this.fixed = pCompound.getBoolean("Fixed");
    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        boolean flag = !this.getItem().isEmpty();
        boolean flag1 = !itemstack.isEmpty();
        if (this.fixed) {
            return InteractionResult.PASS;
        } else if (!this.level().isClientSide) {
            if (!flag) {
                if (flag1 && !this.isRemoved()) {
                    if (itemstack.is(Items.FILLED_MAP)) {
                        MapItemSavedData mapitemsaveddata = MapItem.getSavedData(itemstack, this.level());
                        if (mapitemsaveddata != null && mapitemsaveddata.isTrackedCountOverLimit(256)) {
                            return InteractionResult.FAIL;
                        }
                    }

                    this.setItem(itemstack);
                    this.gameEvent(GameEvent.BLOCK_CHANGE, pPlayer);
                    itemstack.consume(1, pPlayer);
                }
            } else {
                this.playSound(this.getRotateItemSound(), 1.0F, 1.0F);
                this.setRotation(this.getRotation() + 1);
                this.gameEvent(GameEvent.BLOCK_CHANGE, pPlayer);
            }

            return InteractionResult.CONSUME;
        } else {
            return !flag && !flag1 ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
    }

    public SoundEvent getRotateItemSound() {
        return SoundEvents.ITEM_FRAME_ROTATE_ITEM;
    }

    public int getAnalogOutput() {
        return this.getItem().isEmpty() ? 0 : this.getRotation() % 8 + 1;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_352300_) {
        return new ClientboundAddEntityPacket(this, this.direction.get3DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);
        this.setDirection(Direction.from3DDataValue(pPacket.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        ItemStack itemstack = this.getItem();
        return itemstack.isEmpty() ? this.getFrameItemStack() : itemstack.copy();
    }

    protected ItemStack getFrameItemStack() {
        return new ItemStack(Items.ITEM_FRAME);
    }

    @Override
    public float getVisualRotationYInDegrees() {
        Direction direction = this.getDirection();
        int i = direction.getAxis().isVertical() ? 90 * direction.getAxisDirection().getStep() : 0;
        return (float)Mth.wrapDegrees(180 + direction.get2DDataValue() * 90 + this.getRotation() * 45 + i);
    }
}
