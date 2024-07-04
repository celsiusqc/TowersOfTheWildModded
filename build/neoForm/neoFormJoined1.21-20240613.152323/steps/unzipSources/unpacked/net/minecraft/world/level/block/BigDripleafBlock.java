package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BigDripleafBlock extends HorizontalDirectionalBlock implements BonemealableBlock, SimpleWaterloggedBlock {
    public static final MapCodec<BigDripleafBlock> CODEC = simpleCodec(BigDripleafBlock::new);
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final EnumProperty<Tilt> TILT = BlockStateProperties.TILT;
    private static final int NO_TICK = -1;
    private static final Object2IntMap<Tilt> DELAY_UNTIL_NEXT_TILT_STATE = Util.make(new Object2IntArrayMap<>(), p_152305_ -> {
        p_152305_.defaultReturnValue(-1);
        p_152305_.put(Tilt.UNSTABLE, 10);
        p_152305_.put(Tilt.PARTIAL, 10);
        p_152305_.put(Tilt.FULL, 100);
    });
    private static final int MAX_GEN_HEIGHT = 5;
    private static final int STEM_WIDTH = 6;
    private static final int ENTITY_DETECTION_MIN_Y = 11;
    private static final int LOWEST_LEAF_TOP = 13;
    private static final Map<Tilt, VoxelShape> LEAF_SHAPES = ImmutableMap.of(
        Tilt.NONE,
        Block.box(0.0, 11.0, 0.0, 16.0, 15.0, 16.0),
        Tilt.UNSTABLE,
        Block.box(0.0, 11.0, 0.0, 16.0, 15.0, 16.0),
        Tilt.PARTIAL,
        Block.box(0.0, 11.0, 0.0, 16.0, 13.0, 16.0),
        Tilt.FULL,
        Shapes.empty()
    );
    private static final VoxelShape STEM_SLICER = Block.box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0);
    private static final Map<Direction, VoxelShape> STEM_SHAPES = ImmutableMap.of(
        Direction.NORTH,
        Shapes.joinUnoptimized(BigDripleafStemBlock.NORTH_SHAPE, STEM_SLICER, BooleanOp.ONLY_FIRST),
        Direction.SOUTH,
        Shapes.joinUnoptimized(BigDripleafStemBlock.SOUTH_SHAPE, STEM_SLICER, BooleanOp.ONLY_FIRST),
        Direction.EAST,
        Shapes.joinUnoptimized(BigDripleafStemBlock.EAST_SHAPE, STEM_SLICER, BooleanOp.ONLY_FIRST),
        Direction.WEST,
        Shapes.joinUnoptimized(BigDripleafStemBlock.WEST_SHAPE, STEM_SLICER, BooleanOp.ONLY_FIRST)
    );
    private final Map<BlockState, VoxelShape> shapesCache;

    @Override
    public MapCodec<BigDripleafBlock> codec() {
        return CODEC;
    }

    public BigDripleafBlock(BlockBehaviour.Properties p_152214_) {
        super(p_152214_);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(WATERLOGGED, Boolean.valueOf(false)).setValue(FACING, Direction.NORTH).setValue(TILT, Tilt.NONE)
        );
        this.shapesCache = this.getShapeForEachState(BigDripleafBlock::calculateShape);
    }

    private static VoxelShape calculateShape(BlockState p_152318_) {
        return Shapes.or(LEAF_SHAPES.get(p_152318_.getValue(TILT)), STEM_SHAPES.get(p_152318_.getValue(FACING)));
    }

    public static void placeWithRandomHeight(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, Direction pDirection) {
        int i = Mth.nextInt(pRandom, 2, 5);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();
        int j = 0;

        while (j < i && canPlaceAt(pLevel, blockpos$mutableblockpos, pLevel.getBlockState(blockpos$mutableblockpos))) {
            j++;
            blockpos$mutableblockpos.move(Direction.UP);
        }

        int k = pPos.getY() + j - 1;
        blockpos$mutableblockpos.setY(pPos.getY());

        while (blockpos$mutableblockpos.getY() < k) {
            BigDripleafStemBlock.place(pLevel, blockpos$mutableblockpos, pLevel.getFluidState(blockpos$mutableblockpos), pDirection);
            blockpos$mutableblockpos.move(Direction.UP);
        }

        place(pLevel, blockpos$mutableblockpos, pLevel.getFluidState(blockpos$mutableblockpos), pDirection);
    }

    private static boolean canReplace(BlockState pState) {
        return pState.isAir() || pState.is(Blocks.WATER) || pState.is(Blocks.SMALL_DRIPLEAF);
    }

    protected static boolean canPlaceAt(LevelHeightAccessor pLevel, BlockPos pPos, BlockState pState) {
        return !pLevel.isOutsideBuildHeight(pPos) && canReplace(pState);
    }

    protected static boolean place(LevelAccessor pLevel, BlockPos pPos, FluidState pFluidState, Direction pDirection) {
        BlockState blockstate = Blocks.BIG_DRIPLEAF
            .defaultBlockState()
            .setValue(WATERLOGGED, Boolean.valueOf(pFluidState.isSourceOfType(Fluids.WATER)))
            .setValue(FACING, pDirection);
        return pLevel.setBlock(pPos, blockstate, 3);
    }

    @Override
    protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        this.setTiltAndScheduleTick(pState, pLevel, pHit.getBlockPos(), Tilt.FULL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return blockstate.is(this) || blockstate.is(Blocks.BIG_DRIPLEAF_STEM) || blockstate.is(BlockTags.BIG_DRIPLEAF_PLACEABLE);
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(
        BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos
    ) {
        if (pDirection == Direction.DOWN && !pState.canSurvive(pLevel, pPos)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if (pState.getValue(WATERLOGGED)) {
                pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
            }

            return pDirection == Direction.UP && pNeighborState.is(this)
                ? Blocks.BIG_DRIPLEAF_STEM.withPropertiesOf(pState)
                : super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        BlockState blockstate = pLevel.getBlockState(pPos.above());
        return canReplace(blockstate);
    }

    @Override
    public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        BlockPos blockpos = pPos.above();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (canPlaceAt(pLevel, blockpos, blockstate)) {
            Direction direction = pState.getValue(FACING);
            BigDripleafStemBlock.place(pLevel, pPos, pState.getFluidState(), direction);
            place(pLevel, blockpos, blockstate.getFluidState(), direction);
        }
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (!pLevel.isClientSide) {
            if (pState.getValue(TILT) == Tilt.NONE && canEntityTilt(pPos, pEntity) && !pLevel.hasNeighborSignal(pPos)) {
                this.setTiltAndScheduleTick(pState, pLevel, pPos, Tilt.UNSTABLE, null);
            }
        }
    }

    @Override
    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pLevel.hasNeighborSignal(pPos)) {
            resetTilt(pState, pLevel, pPos);
        } else {
            Tilt tilt = pState.getValue(TILT);
            if (tilt == Tilt.UNSTABLE) {
                this.setTiltAndScheduleTick(pState, pLevel, pPos, Tilt.PARTIAL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
            } else if (tilt == Tilt.PARTIAL) {
                this.setTiltAndScheduleTick(pState, pLevel, pPos, Tilt.FULL, SoundEvents.BIG_DRIPLEAF_TILT_DOWN);
            } else if (tilt == Tilt.FULL) {
                resetTilt(pState, pLevel, pPos);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
        if (pLevel.hasNeighborSignal(pPos)) {
            resetTilt(pState, pLevel, pPos);
        }
    }

    private static void playTiltSound(Level pLevel, BlockPos pPos, SoundEvent pSound) {
        float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
        pLevel.playSound(null, pPos, pSound, SoundSource.BLOCKS, 1.0F, f);
    }

    private static boolean canEntityTilt(BlockPos pPos, Entity pEntity) {
        return pEntity.onGround() && pEntity.position().y > (double)((float)pPos.getY() + 0.6875F);
    }

    private void setTiltAndScheduleTick(BlockState pState, Level pLevel, BlockPos pPos, Tilt pTilt, @Nullable SoundEvent pSound) {
        setTilt(pState, pLevel, pPos, pTilt);
        if (pSound != null) {
            playTiltSound(pLevel, pPos, pSound);
        }

        int i = DELAY_UNTIL_NEXT_TILT_STATE.getInt(pTilt);
        if (i != -1) {
            pLevel.scheduleTick(pPos, this, i);
        }
    }

    private static void resetTilt(BlockState pState, Level pLevel, BlockPos pPos) {
        setTilt(pState, pLevel, pPos, Tilt.NONE);
        if (pState.getValue(TILT) != Tilt.NONE) {
            playTiltSound(pLevel, pPos, SoundEvents.BIG_DRIPLEAF_TILT_UP);
        }
    }

    private static void setTilt(BlockState pState, Level pLevel, BlockPos pPos, Tilt pTilt) {
        Tilt tilt = pState.getValue(TILT);
        pLevel.setBlock(pPos, pState.setValue(TILT, pTilt), 2);
        if (pTilt.causesVibration() && pTilt != tilt) {
            pLevel.gameEvent(null, GameEvent.BLOCK_CHANGE, pPos);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return LEAF_SHAPES.get(pState.getValue(TILT));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.shapesCache.get(pState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos().below());
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        boolean flag = blockstate.is(Blocks.BIG_DRIPLEAF) || blockstate.is(Blocks.BIG_DRIPLEAF_STEM);
        return this.defaultBlockState()
            .setValue(WATERLOGGED, Boolean.valueOf(fluidstate.isSourceOfType(Fluids.WATER)))
            .setValue(FACING, flag ? blockstate.getValue(FACING) : pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WATERLOGGED, FACING, TILT);
    }
}
