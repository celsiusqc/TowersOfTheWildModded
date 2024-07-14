package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CactusBlock extends Block {
    public static final MapCodec<CactusBlock> CODEC = simpleCodec(CactusBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final int MAX_AGE = 15;
    protected static final int AABB_OFFSET = 1;
    protected static final VoxelShape COLLISION_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);
    protected static final VoxelShape OUTLINE_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    @Override
    public MapCodec<CactusBlock> codec() {
        return CODEC;
    }

    public CactusBlock(BlockBehaviour.Properties p_51136_) {
        super(p_51136_);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, Integer.valueOf(0)));
    }

    @Override
    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!pLevel.isAreaLoaded(pPos, 1)) return; // Forge: prevent growing cactus from loading unloaded chunks with block update
        if (!pState.canSurvive(pLevel, pPos)) {
            pLevel.destroyBlock(pPos, true);
        }
    }

    /**
     * Performs a random tick on a block.
     */
    @Override
    protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockPos blockpos = pPos.above();
        if (pLevel.isEmptyBlock(blockpos)) {
            int i = 1;

            while (pLevel.getBlockState(pPos.below(i)).is(this)) {
                i++;
            }

            if (i < 3) {
                int j = pState.getValue(AGE);
                if(net.neoforged.neoforge.common.CommonHooks.canCropGrow(pLevel, blockpos, pState, true)) {
                if (j == 15) {
                    pLevel.setBlockAndUpdate(blockpos, this.defaultBlockState());
                    BlockState blockstate = pState.setValue(AGE, Integer.valueOf(0));
                    pLevel.setBlock(pPos, blockstate, 4);
                    pLevel.neighborChanged(blockstate, blockpos, this, pPos, false);
                } else {
                    pLevel.setBlock(pPos, pState.setValue(AGE, Integer.valueOf(j + 1)), 4);
                }
                net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(pLevel, pPos, pState);
                }
            }
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return COLLISION_SHAPE;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return OUTLINE_SHAPE;
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (!pState.canSurvive(pLevel, pCurrentPos)) {
            pLevel.scheduleTick(pCurrentPos, this, 1);
        }

        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState blockstate = pLevel.getBlockState(pPos.relative(direction));
            if (blockstate.isSolid() || pLevel.getFluidState(pPos.relative(direction)).is(FluidTags.LAVA)) {
                return false;
            }
        }

        BlockState blockstate1 = pLevel.getBlockState(pPos.below());
        net.neoforged.neoforge.common.util.TriState soilDecision = blockstate1.canSustainPlant(pLevel, pPos.below(), Direction.UP, pState);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        return (blockstate1.is(Blocks.CACTUS) || blockstate1.is(BlockTags.SAND)) && !pLevel.getBlockState(pPos.above()).liquid();
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        pEntity.hurt(pLevel.damageSources().cactus(), 1.0F);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
    }

    @Override
    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        return false;
    }
}
