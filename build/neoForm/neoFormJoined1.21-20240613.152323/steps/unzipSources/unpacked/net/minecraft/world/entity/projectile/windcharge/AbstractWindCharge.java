package net.minecraft.world.entity.projectile.windcharge;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractWindCharge extends AbstractHurtingProjectile implements ItemSupplier {
    public static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(
        true, false, Optional.empty(), BuiltInRegistries.BLOCK.getTag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
    );
    public static final double JUMP_SCALE = 0.25;

    public AbstractWindCharge(EntityType<? extends AbstractWindCharge> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.accelerationPower = 0.0;
    }

    public AbstractWindCharge(
        EntityType<? extends AbstractWindCharge> pEntityType, Level pLevel, Entity pOwner, double pX, double pY, double pZ
    ) {
        super(pEntityType, pX, pY, pZ, pLevel);
        this.setOwner(pOwner);
        this.accelerationPower = 0.0;
    }

    AbstractWindCharge(
        EntityType<? extends AbstractWindCharge> p_326232_, double p_326236_, double p_326440_, double p_326413_, Vec3 p_347459_, Level p_326449_
    ) {
        super(p_326232_, p_326236_, p_326440_, p_326413_, p_347459_, p_326449_);
        this.accelerationPower = 0.0;
    }

    @Override
    protected AABB makeBoundingBox() {
        float f = this.getType().getDimensions().width() / 2.0F;
        float f1 = this.getType().getDimensions().height();
        float f2 = 0.15F;
        return new AABB(
            this.position().x - (double)f,
            this.position().y - 0.15F,
            this.position().z - (double)f,
            this.position().x + (double)f,
            this.position().y - 0.15F + (double)f1,
            this.position().z + (double)f
        );
    }

    @Override
    public boolean canCollideWith(Entity pEntity) {
        return pEntity instanceof AbstractWindCharge ? false : super.canCollideWith(pEntity);
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        if (pTarget instanceof AbstractWindCharge) {
            return false;
        } else {
            return pTarget.getType() == EntityType.END_CRYSTAL ? false : super.canHitEntity(pTarget);
        }
    }

    /**
     * Called when the arrow hits an entity
     */
    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        if (!this.level().isClientSide) {
            LivingEntity livingentity = this.getOwner() instanceof LivingEntity livingentity1 ? livingentity1 : null;
            Entity entity = pResult.getEntity();
            if (livingentity != null) {
                livingentity.setLastHurtMob(entity);
            }

            DamageSource damagesource = this.damageSources().windCharge(this, livingentity);
            if (entity.hurt(damagesource, 1.0F) && entity instanceof LivingEntity livingentity2) {
                EnchantmentHelper.doPostAttackEffects((ServerLevel)this.level(), livingentity2, damagesource);
            }

            this.explode(this.position());
        }
    }

    /**
     * Adds to the current velocity of the entity, and sets {@link #isAirBorne} to true.
     */
    @Override
    public void push(double pX, double pY, double pZ) {
    }

    protected abstract void explode(Vec3 p_352265_);

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        if (!this.level().isClientSide) {
            Vec3i vec3i = pResult.getDirection().getNormal();
            Vec3 vec3 = Vec3.atLowerCornerOf(vec3i).multiply(0.25, 0.25, 0.25);
            Vec3 vec31 = pResult.getLocation().add(vec3);
            this.explode(vec31);
            this.discard();
        }
    }

    /**
     * Called when this EntityFireball hits a block or entity.
     */
    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected float getLiquidInertia() {
        return this.getInertia();
    }

    @Nullable
    @Override
    protected ParticleOptions getTrailParticle() {
        return null;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && this.getBlockY() > this.level().getMaxBuildHeight() + 30) {
            this.explode(this.position());
            this.discard();
        } else {
            super.tick();
        }
    }

    @Override
    public boolean hurt(DamageSource p_352147_, float p_352222_) {
        return false;
    }
}
