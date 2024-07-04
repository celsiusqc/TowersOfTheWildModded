package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class ProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
    private final ProjectileItem projectileItem;
    private final ProjectileItem.DispenseConfig dispenseConfig;

    public ProjectileDispenseBehavior(Item pProjectile) {
        if (pProjectile instanceof ProjectileItem projectileitem) {
            this.projectileItem = projectileitem;
            this.dispenseConfig = projectileitem.createDispenseConfig();
        } else {
            throw new IllegalArgumentException(pProjectile + " not instance of " + ProjectileItem.class.getSimpleName());
        }
    }

    @Override
    public ItemStack execute(BlockSource pBlockSource, ItemStack pItem) {
        Level level = pBlockSource.level();
        Direction direction = pBlockSource.state().getValue(DispenserBlock.FACING);
        Position position = this.dispenseConfig.positionFunction().getDispensePosition(pBlockSource, direction);
        Projectile projectile = this.projectileItem.asProjectile(level, position, pItem, direction);
        this.projectileItem
            .shoot(
                projectile,
                (double)direction.getStepX(),
                (double)direction.getStepY(),
                (double)direction.getStepZ(),
                this.dispenseConfig.power(),
                this.dispenseConfig.uncertainty()
            );
        level.addFreshEntity(projectile);
        pItem.shrink(1);
        return pItem;
    }

    @Override
    protected void playSound(BlockSource pBlockSource) {
        pBlockSource.level().levelEvent(this.dispenseConfig.overrideDispenseEvent().orElse(1002), pBlockSource.pos(), 0);
    }
}
