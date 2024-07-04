package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
    private static final int DEFAULT_ACCURACY = 6;

    @Override
    public final ItemStack dispense(BlockSource pBlockSource, ItemStack pItem) {
        ItemStack itemstack = this.execute(pBlockSource, pItem);
        this.playSound(pBlockSource);
        this.playAnimation(pBlockSource, pBlockSource.state().getValue(DispenserBlock.FACING));
        return itemstack;
    }

    protected ItemStack execute(BlockSource pBlockSource, ItemStack pItem) {
        Direction direction = pBlockSource.state().getValue(DispenserBlock.FACING);
        Position position = DispenserBlock.getDispensePosition(pBlockSource);
        ItemStack itemstack = pItem.split(1);
        spawnItem(pBlockSource.level(), itemstack, 6, direction, position);
        return pItem;
    }

    public static void spawnItem(Level pLevel, ItemStack pStack, int pSpeed, Direction pFacing, Position pPosition) {
        double d0 = pPosition.x();
        double d1 = pPosition.y();
        double d2 = pPosition.z();
        if (pFacing.getAxis() == Direction.Axis.Y) {
            d1 -= 0.125;
        } else {
            d1 -= 0.15625;
        }

        ItemEntity itementity = new ItemEntity(pLevel, d0, d1, d2, pStack);
        double d3 = pLevel.random.nextDouble() * 0.1 + 0.2;
        itementity.setDeltaMovement(
            pLevel.random.triangle((double)pFacing.getStepX() * d3, 0.0172275 * (double)pSpeed),
            pLevel.random.triangle(0.2, 0.0172275 * (double)pSpeed),
            pLevel.random.triangle((double)pFacing.getStepZ() * d3, 0.0172275 * (double)pSpeed)
        );
        pLevel.addFreshEntity(itementity);
    }

    protected void playSound(BlockSource pBlockSource) {
        playDefaultSound(pBlockSource);
    }

    protected void playAnimation(BlockSource pBlockSource, Direction pDirection) {
        playDefaultAnimation(pBlockSource, pDirection);
    }

    private static void playDefaultSound(BlockSource p_347476_) {
        p_347476_.level().levelEvent(1000, p_347476_.pos(), 0);
    }

    private static void playDefaultAnimation(BlockSource p_347531_, Direction p_347570_) {
        p_347531_.level().levelEvent(2000, p_347531_.pos(), p_347570_.get3DDataValue());
    }

    protected ItemStack consumeWithRemainder(BlockSource p_347658_, ItemStack p_347682_, ItemStack p_347670_) {
        p_347682_.shrink(1);
        if (p_347682_.isEmpty()) {
            return p_347670_;
        } else {
            this.addToInventoryOrDispense(p_347658_, p_347670_);
            return p_347682_;
        }
    }

    private void addToInventoryOrDispense(BlockSource p_347634_, ItemStack p_347604_) {
        ItemStack itemstack = p_347634_.blockEntity().insertItem(p_347604_);
        if (!itemstack.isEmpty()) {
            Direction direction = p_347634_.state().getValue(DispenserBlock.FACING);
            spawnItem(p_347634_.level(), itemstack, 6, direction, DispenserBlock.getDispensePosition(p_347634_));
            playDefaultSound(p_347634_);
            playDefaultAnimation(p_347634_, direction);
        }
    }
}
