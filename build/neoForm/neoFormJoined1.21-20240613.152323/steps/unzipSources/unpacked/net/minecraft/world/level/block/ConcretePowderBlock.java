package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
    public static final MapCodec<ConcretePowderBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_344654_ -> p_344654_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("concrete").forGetter(p_304618_ -> p_304618_.concrete), propertiesCodec())
                .apply(p_344654_, ConcretePowderBlock::new)
    );
    private final Block concrete;

    @Override
    public MapCodec<ConcretePowderBlock> codec() {
        return CODEC;
    }

    public ConcretePowderBlock(Block p_52060_, BlockBehaviour.Properties p_52061_) {
        super(p_52061_);
        this.concrete = p_52060_;
    }

    @Override
    public void onLand(Level pLevel, BlockPos pPos, BlockState pState, BlockState pReplaceableState, FallingBlockEntity pFallingBlock) {
        if (shouldSolidify(pLevel, pPos, pState, pReplaceableState.getFluidState())) { // Forge: Use block of falling entity instead of block at replaced position, and check if shouldSolidify with the FluidState of the replaced block
            pLevel.setBlock(pPos, this.concrete.defaultBlockState(), 3);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockGetter blockgetter = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = blockgetter.getBlockState(blockpos);
        return shouldSolidify(blockgetter, blockpos, blockstate) ? this.concrete.defaultBlockState() : super.getStateForPlacement(pContext);
    }

    private static boolean shouldSolidify(BlockGetter pLevel, BlockPos pPos, BlockState pState, net.minecraft.world.level.material.FluidState fluidState) {
        return pState.canBeHydrated(pLevel, pPos, fluidState, pPos) || touchesLiquid(pLevel, pPos, pState);
    }

    private static boolean shouldSolidify(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return shouldSolidify(pLevel, pPos, pState, pLevel.getFluidState(pPos));
    }

    private static boolean touchesLiquid(BlockGetter pLevel, BlockPos pPos, BlockState state) {
        boolean flag = false;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

        for (Direction direction : Direction.values()) {
            BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
            if (direction != Direction.DOWN || state.canBeHydrated(pLevel, pPos, blockstate.getFluidState(), blockpos$mutableblockpos)) {
                blockpos$mutableblockpos.setWithOffset(pPos, direction);
                blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
                if (state.canBeHydrated(pLevel, pPos, blockstate.getFluidState(), blockpos$mutableblockpos) && !blockstate.isFaceSturdy(pLevel, pPos, direction.getOpposite())) {
                    flag = true;
                    break;
                }
            }
        }

        return flag;
    }

    private static boolean canSolidify(BlockState pState) {
        return pState.getFluidState().is(FluidTags.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        return touchesLiquid(pLevel, pCurrentPos, pState)
            ? this.concrete.defaultBlockState()
            : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    public int getDustColor(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return pState.getMapColor(pReader, pPos).col;
    }
}
