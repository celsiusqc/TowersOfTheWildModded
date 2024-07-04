package net.minecraft.world.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ThrownExperienceBottle extends ThrowableItemProjectile {
    public ThrownExperienceBottle(EntityType<? extends ThrownExperienceBottle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThrownExperienceBottle(Level pLevel, LivingEntity pShooter) {
        super(EntityType.EXPERIENCE_BOTTLE, pShooter, pLevel);
    }

    public ThrownExperienceBottle(Level pLevel, double pX, double pY, double pZ) {
        super(EntityType.EXPERIENCE_BOTTLE, pX, pY, pZ, pLevel);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EXPERIENCE_BOTTLE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07;
    }

    /**
     * Called when this EntityFireball hits a block or entity.
     */
    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (this.level() instanceof ServerLevel) {
            this.level().levelEvent(2002, this.blockPosition(), PotionContents.getColor(Potions.WATER));
            int i = 3 + this.level().random.nextInt(5) + this.level().random.nextInt(5);
            ExperienceOrb.award((ServerLevel)this.level(), this.position(), i);
            this.discard();
        }
    }
}
