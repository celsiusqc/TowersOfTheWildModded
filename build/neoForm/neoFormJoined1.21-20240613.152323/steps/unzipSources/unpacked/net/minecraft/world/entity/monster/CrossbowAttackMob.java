package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface CrossbowAttackMob extends RangedAttackMob {
    void setChargingCrossbow(boolean pChargingCrossbow);

    @Nullable
    LivingEntity getTarget();

    void onCrossbowAttackPerformed();

    default void performCrossbowAttack(LivingEntity pUser, float pVelocity) {
        InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(pUser, item -> item instanceof CrossbowItem);
        ItemStack itemstack = pUser.getItemInHand(interactionhand);
        if (itemstack.getItem() instanceof CrossbowItem crossbowitem) {
            crossbowitem.performShooting(
                pUser.level(), pUser, interactionhand, itemstack, pVelocity, (float)(14 - pUser.level().getDifficulty().getId() * 4), this.getTarget()
            );
        }

        this.onCrossbowAttackPerformed();
    }
}
