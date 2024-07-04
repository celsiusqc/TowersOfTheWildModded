package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class FireChargeItem extends Item implements ProjectileItem {
    public FireChargeItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        boolean flag = false;
        if (!CampfireBlock.canLight(blockstate) && !CandleBlock.canLight(blockstate) && !CandleCakeBlock.canLight(blockstate)) {
            blockpos = blockpos.relative(pContext.getClickedFace());
            if (BaseFireBlock.canBePlacedAt(level, blockpos, pContext.getHorizontalDirection())) {
                this.playSound(level, blockpos);
                level.setBlockAndUpdate(blockpos, BaseFireBlock.getState(level, blockpos));
                level.gameEvent(pContext.getPlayer(), GameEvent.BLOCK_PLACE, blockpos);
                flag = true;
            }
        } else {
            this.playSound(level, blockpos);
            level.setBlockAndUpdate(blockpos, blockstate.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)));
            level.gameEvent(pContext.getPlayer(), GameEvent.BLOCK_CHANGE, blockpos);
            flag = true;
        }

        if (flag) {
            pContext.getItemInHand().shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.FAIL;
        }
    }

    private void playSound(Level pLevel, BlockPos pPos) {
        RandomSource randomsource = pLevel.getRandom();
        pLevel.playSound(
            null, pPos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F
        );
    }

    @Override
    public Projectile asProjectile(Level pLevel, Position pPos, ItemStack pStack, Direction pDirection) {
        RandomSource randomsource = pLevel.getRandom();
        double d0 = randomsource.triangle((double)pDirection.getStepX(), 0.11485000000000001);
        double d1 = randomsource.triangle((double)pDirection.getStepY(), 0.11485000000000001);
        double d2 = randomsource.triangle((double)pDirection.getStepZ(), 0.11485000000000001);
        Vec3 vec3 = new Vec3(d0, d1, d2);
        SmallFireball smallfireball = new SmallFireball(pLevel, pPos.x(), pPos.y(), pPos.z(), vec3.normalize());
        smallfireball.setItem(pStack);
        return smallfireball;
    }

    @Override
    public void shoot(Projectile pProjectile, double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
    }

    @Override
    public ProjectileItem.DispenseConfig createDispenseConfig() {
        return ProjectileItem.DispenseConfig.builder()
            .positionFunction((p_338834_, p_338717_) -> DispenserBlock.getDispensePosition(p_338834_, 1.0, Vec3.ZERO))
            .uncertainty(6.6666665F)
            .power(1.0F)
            .overrideDispenseEvent(1018)
            .build();
    }
}
