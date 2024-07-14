package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public abstract class BushBlock extends Block {
    protected BushBlock(BlockBehaviour.Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected abstract MapCodec<? extends BushBlock> codec();

    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(BlockTags.DIRT) || pState.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        return !pState.canSurvive(pLevel, pCurrentPos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState belowBlockState = pLevel.getBlockState(blockpos);
        net.neoforged.neoforge.common.util.TriState soilDecision = belowBlockState.canSustainPlant(pLevel, blockpos, Direction.UP, pState);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        return this.mayPlaceOn(belowBlockState, pLevel, blockpos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return pState.getFluidState().isEmpty();
    }

    @Override
    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        return pPathComputationType == PathComputationType.AIR && !this.hasCollision ? true : super.isPathfindable(pState, pPathComputationType);
    }
}
