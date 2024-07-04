package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public class ArrowItem extends Item implements ProjectileItem {
    public ArrowItem(Item.Properties pProperties) {
        super(pProperties);
    }

    public AbstractArrow createArrow(Level pLevel, ItemStack pStack, LivingEntity pShooter, @Nullable ItemStack p_344832_) {
        return new Arrow(pLevel, pShooter, pStack.copyWithCount(1), p_344832_);
    }

    @Override
    public Projectile asProjectile(Level pLevel, Position pPos, ItemStack pStack, Direction pDirection) {
        Arrow arrow = new Arrow(pLevel, pPos.x(), pPos.y(), pPos.z(), pStack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }

    /**
     * Called to determine if this arrow will be infinite when fired. If an arrow is infinite, then the arrow will never be consumed (regardless of enchantments).
     * <p>
     * Only called on the logical server.
     *
     * @param ammo The ammo stack (containing this item)
     * @param bow  The bow stack
     * @param livingEntity The entity who is firing the bow
     * @return True if the arrow is infinite
     */
    public boolean isInfinite(ItemStack ammo, ItemStack bow, net.minecraft.world.entity.LivingEntity livingEntity) {
        return false;
    }
}
