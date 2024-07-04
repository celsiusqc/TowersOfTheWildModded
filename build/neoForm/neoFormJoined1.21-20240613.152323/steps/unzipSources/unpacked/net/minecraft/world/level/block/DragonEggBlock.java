package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DragonEggBlock extends FallingBlock {
    public static final MapCodec<DragonEggBlock> CODEC = simpleCodec(DragonEggBlock::new);
    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    @Override
    public MapCodec<DragonEggBlock> codec() {
        return CODEC;
    }

    public DragonEggBlock(BlockBehaviour.Properties p_52911_) {
        super(p_52911_);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        this.teleport(pState, pLevel, pPos);
        return InteractionResult.sidedSuccess(pLevel.isClientSide);
    }

    @Override
    protected void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        this.teleport(pState, pLevel, pPos);
    }

    private void teleport(BlockState pState, Level pLevel, BlockPos pPos) {
        WorldBorder worldborder = pLevel.getWorldBorder();

        for (int i = 0; i < 1000; i++) {
            BlockPos blockpos = pPos.offset(
                pLevel.random.nextInt(16) - pLevel.random.nextInt(16),
                pLevel.random.nextInt(8) - pLevel.random.nextInt(8),
                pLevel.random.nextInt(16) - pLevel.random.nextInt(16)
            );
            if (pLevel.getBlockState(blockpos).isAir() && worldborder.isWithinBounds(blockpos)) {
                if (pLevel.isClientSide) {
                    for (int j = 0; j < 128; j++) {
                        double d0 = pLevel.random.nextDouble();
                        float f = (pLevel.random.nextFloat() - 0.5F) * 0.2F;
                        float f1 = (pLevel.random.nextFloat() - 0.5F) * 0.2F;
                        float f2 = (pLevel.random.nextFloat() - 0.5F) * 0.2F;
                        double d1 = Mth.lerp(d0, (double)blockpos.getX(), (double)pPos.getX()) + (pLevel.random.nextDouble() - 0.5) + 0.5;
                        double d2 = Mth.lerp(d0, (double)blockpos.getY(), (double)pPos.getY()) + pLevel.random.nextDouble() - 0.5;
                        double d3 = Mth.lerp(d0, (double)blockpos.getZ(), (double)pPos.getZ()) + (pLevel.random.nextDouble() - 0.5) + 0.5;
                        pLevel.addParticle(ParticleTypes.PORTAL, d1, d2, d3, (double)f, (double)f1, (double)f2);
                    }
                } else {
                    pLevel.setBlock(blockpos, pState, 2);
                    pLevel.removeBlock(pPos, false);
                }

                return;
            }
        }
    }

    @Override
    protected int getDelayAfterPlace() {
        return 5;
    }

    @Override
    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        return false;
    }
}
