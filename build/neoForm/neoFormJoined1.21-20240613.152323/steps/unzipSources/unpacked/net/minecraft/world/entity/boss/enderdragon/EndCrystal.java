package net.minecraft.world.entity.boss.enderdragon;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.dimension.end.EndDragonFight;

public class EndCrystal extends Entity {
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_BEAM_TARGET = SynchedEntityData.defineId(
        EndCrystal.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
    );
    private static final EntityDataAccessor<Boolean> DATA_SHOW_BOTTOM = SynchedEntityData.defineId(EndCrystal.class, EntityDataSerializers.BOOLEAN);
    public int time;

    public EndCrystal(EntityType<? extends EndCrystal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.blocksBuilding = true;
        this.time = this.random.nextInt(100000);
    }

    public EndCrystal(Level pLevel, double pX, double pY, double pZ) {
        this(EntityType.END_CRYSTAL, pLevel);
        this.setPos(pX, pY, pZ);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(DATA_BEAM_TARGET, Optional.empty());
        pBuilder.define(DATA_SHOW_BOTTOM, true);
    }

    @Override
    public void tick() {
        this.time++;
        this.checkInsideBlocks();
        this.handlePortal();
        if (this.level() instanceof ServerLevel) {
            BlockPos blockpos = this.blockPosition();
            if (((ServerLevel)this.level()).getDragonFight() != null && this.level().getBlockState(blockpos).isAir()) {
                this.level().setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level(), blockpos));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        if (this.getBeamTarget() != null) {
            pCompound.put("beam_target", NbtUtils.writeBlockPos(this.getBeamTarget()));
        }

        pCompound.putBoolean("ShowBottom", this.showsBottom());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        NbtUtils.readBlockPos(pCompound, "beam_target").ifPresent(this::setBeamTarget);
        if (pCompound.contains("ShowBottom", 1)) {
            this.setShowBottom(pCompound.getBoolean("ShowBottom"));
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isInvulnerableTo(pSource)) {
            return false;
        } else if (pSource.getEntity() instanceof EnderDragon) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level().isClientSide) {
                this.remove(Entity.RemovalReason.KILLED);
                if (!pSource.is(DamageTypeTags.IS_EXPLOSION)) {
                    DamageSource damagesource = pSource.getEntity() != null ? this.damageSources().explosion(this, pSource.getEntity()) : null;
                    this.level().explode(this, damagesource, null, this.getX(), this.getY(), this.getZ(), 6.0F, false, Level.ExplosionInteraction.BLOCK);
                }

                this.onDestroyedBy(pSource);
            }

            return true;
        }
    }

    @Override
    public void kill() {
        this.onDestroyedBy(this.damageSources().generic());
        super.kill();
    }

    private void onDestroyedBy(DamageSource pSource) {
        if (this.level() instanceof ServerLevel) {
            EndDragonFight enddragonfight = ((ServerLevel)this.level()).getDragonFight();
            if (enddragonfight != null) {
                enddragonfight.onCrystalDestroyed(this, pSource);
            }
        }
    }

    public void setBeamTarget(@Nullable BlockPos p_31053_) {
        this.getEntityData().set(DATA_BEAM_TARGET, Optional.ofNullable(p_31053_));
    }

    @Nullable
    public BlockPos getBeamTarget() {
        return this.getEntityData().get(DATA_BEAM_TARGET).orElse(null);
    }

    public void setShowBottom(boolean pShowBottom) {
        this.getEntityData().set(DATA_SHOW_BOTTOM, pShowBottom);
    }

    public boolean showsBottom() {
        return this.getEntityData().get(DATA_SHOW_BOTTOM);
    }

    /**
     * Checks if the entity is in range to render.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return super.shouldRenderAtSqrDistance(pDistance) || this.getBeamTarget() != null;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.END_CRYSTAL);
    }
}
