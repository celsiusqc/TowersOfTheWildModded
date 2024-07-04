package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class SpectralArrow extends AbstractArrow {
    private int duration = 200;

    public SpectralArrow(EntityType<? extends SpectralArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public SpectralArrow(Level pLevel, LivingEntity pOwner, ItemStack pPickupItemStack, @Nullable ItemStack p_346408_) {
        super(EntityType.SPECTRAL_ARROW, pOwner, pLevel, pPickupItemStack, p_346408_);
    }

    public SpectralArrow(Level pLevel, double pX, double pY, double pZ, ItemStack pPickupItemStack, @Nullable ItemStack p_345907_) {
        super(EntityType.SPECTRAL_ARROW, pX, pY, pZ, pLevel, pPickupItemStack, p_345907_);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            this.level().addParticle(ParticleTypes.INSTANT_EFFECT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity pLiving) {
        super.doPostHurtEffects(pLiving);
        MobEffectInstance mobeffectinstance = new MobEffectInstance(MobEffects.GLOWING, this.duration, 0);
        pLiving.addEffect(mobeffectinstance, this.getEffectSource());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Duration")) {
            this.duration = pCompound.getInt("Duration");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Duration", this.duration);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.SPECTRAL_ARROW);
    }
}
