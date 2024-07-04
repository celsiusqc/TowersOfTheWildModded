package net.minecraft.world.entity;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ExperienceOrb extends Entity {
    private static final int LIFETIME = 6000;
    private static final int ENTITY_SCAN_PERIOD = 20;
    private static final int MAX_FOLLOW_DIST = 8;
    private static final int ORB_GROUPS_PER_AREA = 40;
    private static final double ORB_MERGE_DISTANCE = 0.5;
    private int age;
    private int health = 5;
    public int value;
    private int count = 1;
    private Player followingPlayer;

    public ExperienceOrb(Level pLevel, double pX, double pY, double pZ, int pValue) {
        this(EntityType.EXPERIENCE_ORB, pLevel);
        this.setPos(pX, pY, pZ);
        this.setYRot((float)(this.random.nextDouble() * 360.0));
        this.setDeltaMovement(
            (this.random.nextDouble() * 0.2F - 0.1F) * 2.0, this.random.nextDouble() * 0.2 * 2.0, (this.random.nextDouble() * 0.2F - 0.1F) * 2.0
        );
        this.value = pValue;
    }

    public ExperienceOrb(EntityType<? extends ExperienceOrb> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        if (this.isEyeInFluid(FluidTags.WATER)) {
            this.setUnderwaterMovement();
        } else {
            this.applyGravity();
        }

        if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
            this.setDeltaMovement(
                (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.2F),
                0.2F,
                (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.2F)
            );
        }

        if (!this.level().noCollision(this.getBoundingBox())) {
            this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
        }

        if (this.tickCount % 20 == 1) {
            this.scanForEntities();
        }

        if (this.followingPlayer != null && (this.followingPlayer.isSpectator() || this.followingPlayer.isDeadOrDying())) {
            this.followingPlayer = null;
        }

        if (this.followingPlayer != null) {
            Vec3 vec3 = new Vec3(
                this.followingPlayer.getX() - this.getX(),
                this.followingPlayer.getY() + (double)this.followingPlayer.getEyeHeight() / 2.0 - this.getY(),
                this.followingPlayer.getZ() - this.getZ()
            );
            double d0 = vec3.lengthSqr();
            if (d0 < 64.0) {
                double d1 = 1.0 - Math.sqrt(d0) / 8.0;
                this.setDeltaMovement(this.getDeltaMovement().add(vec3.normalize().scale(d1 * d1 * 0.1)));
            }
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        float f = 0.98F;
        if (this.onGround()) {
            BlockPos pos = getBlockPosBelowThatAffectsMyMovement();
            f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply((double)f, 0.98, (double)f));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, -0.9, 1.0));
        }

        this.age++;
        if (this.age >= 6000) {
            this.discard();
        }
    }

    @Override
    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.999999F);
    }

    private void scanForEntities() {
        if (this.followingPlayer == null || this.followingPlayer.distanceToSqr(this) > 64.0) {
            this.followingPlayer = this.level().getNearestPlayer(this, 8.0);
        }

        if (this.level() instanceof ServerLevel) {
            for (ExperienceOrb experienceorb : this.level()
                .getEntities(EntityTypeTest.forClass(ExperienceOrb.class), this.getBoundingBox().inflate(0.5), this::canMerge)) {
                this.merge(experienceorb);
            }
        }
    }

    public static void award(ServerLevel pLevel, Vec3 pPos, int pAmount) {
        while (pAmount > 0) {
            int i = getExperienceValue(pAmount);
            pAmount -= i;
            if (!tryMergeToExisting(pLevel, pPos, i)) {
                pLevel.addFreshEntity(new ExperienceOrb(pLevel, pPos.x(), pPos.y(), pPos.z(), i));
            }
        }
    }

    private static boolean tryMergeToExisting(ServerLevel pLevel, Vec3 pPos, int pAmount) {
        AABB aabb = AABB.ofSize(pPos, 1.0, 1.0, 1.0);
        int i = pLevel.getRandom().nextInt(40);
        List<ExperienceOrb> list = pLevel.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), aabb, p_147081_ -> canMerge(p_147081_, i, pAmount));
        if (!list.isEmpty()) {
            ExperienceOrb experienceorb = list.get(0);
            experienceorb.count++;
            experienceorb.age = 0;
            return true;
        } else {
            return false;
        }
    }

    private boolean canMerge(ExperienceOrb p_147087_) {
        return p_147087_ != this && canMerge(p_147087_, this.getId(), this.value);
    }

    private static boolean canMerge(ExperienceOrb pOrb, int pAmount, int pOther) {
        return !pOrb.isRemoved() && (pOrb.getId() - pAmount) % 40 == 0 && pOrb.value == pOther;
    }

    private void merge(ExperienceOrb pOrb) {
        this.count = this.count + pOrb.count;
        this.age = Math.min(this.age, pOrb.age);
        pOrb.discard();
    }

    private void setUnderwaterMovement() {
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x * 0.99F, Math.min(vec3.y + 5.0E-4F, 0.06F), vec3.z * 0.99F);
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (this.isInvulnerableTo(pSource)) {
            return false;
        } else if (this.level().isClientSide) {
            return true;
        } else {
            this.markHurt();
            this.health = (int)((float)this.health - pAmount);
            if (this.health <= 0) {
                this.discard();
            }

            return true;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putShort("Health", (short)this.health);
        pCompound.putShort("Age", (short)this.age);
        pCompound.putShort("Value", (short)this.value);
        pCompound.putInt("Count", this.count);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        this.health = pCompound.getShort("Health");
        this.age = pCompound.getShort("Age");
        this.value = pCompound.getShort("Value");
        this.count = Math.max(pCompound.getInt("Count"), 1);
    }

    /**
     * Called by a player entity when they collide with an entity
     */
    @Override
    public void playerTouch(Player pEntity) {
        if (pEntity instanceof ServerPlayer serverplayer) {
            if (pEntity.takeXpDelay == 0) {
                if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerXpEvent.PickupXp(pEntity, this)).isCanceled()) return;
                pEntity.takeXpDelay = 2;
                pEntity.take(this, 1);
                int i = this.repairPlayerItems(serverplayer, this.value);
                if (i > 0) {
                    pEntity.giveExperiencePoints(i);
                }

                this.count--;
                if (this.count == 0) {
                    this.discard();
                }
            }
        }
    }

    private int repairPlayerItems(ServerPlayer p_344821_, int p_147094_) {
        Optional<EnchantedItemInUse> optional = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, p_344821_, ItemStack::isDamaged);
        if (optional.isPresent()) {
            ItemStack itemstack = optional.get().itemStack();
            int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(p_344821_.serverLevel(), itemstack, (int) (p_147094_ * itemstack.getXpRepairRatio()));
            int j = Math.min(i, itemstack.getDamageValue());
            itemstack.setDamageValue(itemstack.getDamageValue() - j);
            if (j > 0) {
                int k = p_147094_ - j * p_147094_ / i;
                if (k > 0) {
                    return this.repairPlayerItems(p_344821_, k);
                }
            }

            return 0;
        } else {
            return p_147094_;
        }
    }

    public int getValue() {
        return this.value;
    }

    public int getIcon() {
        if (this.value >= 2477) {
            return 10;
        } else if (this.value >= 1237) {
            return 9;
        } else if (this.value >= 617) {
            return 8;
        } else if (this.value >= 307) {
            return 7;
        } else if (this.value >= 149) {
            return 6;
        } else if (this.value >= 73) {
            return 5;
        } else if (this.value >= 37) {
            return 4;
        } else if (this.value >= 17) {
            return 3;
        } else if (this.value >= 7) {
            return 2;
        } else {
            return this.value >= 3 ? 1 : 0;
        }
    }

    /**
     * Get a fragment of the maximum experience points value for the supplied value of experience points value.
     */
    public static int getExperienceValue(int pExpValue) {
        if (pExpValue >= 2477) {
            return 2477;
        } else if (pExpValue >= 1237) {
            return 1237;
        } else if (pExpValue >= 617) {
            return 617;
        } else if (pExpValue >= 307) {
            return 307;
        } else if (pExpValue >= 149) {
            return 149;
        } else if (pExpValue >= 73) {
            return 73;
        } else if (pExpValue >= 37) {
            return 37;
        } else if (pExpValue >= 17) {
            return 17;
        } else if (pExpValue >= 7) {
            return 7;
        } else {
            return pExpValue >= 3 ? 3 : 1;
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_352351_) {
        return new ClientboundAddExperienceOrbPacket(this, p_352351_);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }
}
