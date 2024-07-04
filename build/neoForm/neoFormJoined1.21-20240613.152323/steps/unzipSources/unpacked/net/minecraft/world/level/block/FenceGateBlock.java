package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FenceGateBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FenceGateBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308823_ -> p_308823_.group(WoodType.CODEC.optionalFieldOf("wood_type").forGetter(p_304842_ -> java.util.Optional.ofNullable(p_304842_.type)), propertiesCodec(),
                        net.minecraft.sounds.SoundEvent.DIRECT_CODEC.optionalFieldOf("open_sound").forGetter(fence -> java.util.Optional.of(fence.openSound).filter(s -> fence.type == null || s != fence.type.fenceGateOpen())),
                        net.minecraft.sounds.SoundEvent.DIRECT_CODEC.optionalFieldOf("close_sound").forGetter(fence -> java.util.Optional.of(fence.closeSound).filter(s -> fence.type == null || s != fence.type.fenceGateClose())))
                .apply(p_308823_, FenceGateBlock::new)
    );
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty IN_WALL = BlockStateProperties.IN_WALL;
    protected static final VoxelShape Z_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    protected static final VoxelShape X_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    protected static final VoxelShape Z_SHAPE_LOW = Block.box(0.0, 0.0, 6.0, 16.0, 13.0, 10.0);
    protected static final VoxelShape X_SHAPE_LOW = Block.box(6.0, 0.0, 0.0, 10.0, 13.0, 16.0);
    protected static final VoxelShape Z_COLLISION_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 24.0, 10.0);
    protected static final VoxelShape X_COLLISION_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 24.0, 16.0);
    protected static final VoxelShape Z_SUPPORT_SHAPE = Block.box(0.0, 5.0, 6.0, 16.0, 24.0, 10.0);
    protected static final VoxelShape X_SUPPORT_SHAPE = Block.box(6.0, 5.0, 0.0, 10.0, 24.0, 16.0);
    protected static final VoxelShape Z_OCCLUSION_SHAPE = Shapes.or(Block.box(0.0, 5.0, 7.0, 2.0, 16.0, 9.0), Block.box(14.0, 5.0, 7.0, 16.0, 16.0, 9.0));
    protected static final VoxelShape X_OCCLUSION_SHAPE = Shapes.or(Block.box(7.0, 5.0, 0.0, 9.0, 16.0, 2.0), Block.box(7.0, 5.0, 14.0, 9.0, 16.0, 16.0));
    protected static final VoxelShape Z_OCCLUSION_SHAPE_LOW = Shapes.or(Block.box(0.0, 2.0, 7.0, 2.0, 13.0, 9.0), Block.box(14.0, 2.0, 7.0, 16.0, 13.0, 9.0));
    protected static final VoxelShape X_OCCLUSION_SHAPE_LOW = Shapes.or(Block.box(7.0, 2.0, 0.0, 9.0, 13.0, 2.0), Block.box(7.0, 2.0, 14.0, 9.0, 13.0, 16.0));
    public final net.minecraft.sounds.SoundEvent openSound, closeSound;
    @org.jetbrains.annotations.Nullable
    private final WoodType type;

    @Override
    public MapCodec<FenceGateBlock> codec() {
        return CODEC;
    }

    public FenceGateBlock(WoodType p_273340_, BlockBehaviour.Properties p_273352_) {
        this(java.util.Optional.of(p_273340_), p_273352_.sound(p_273340_.soundType()), java.util.Optional.of(p_273340_.fenceGateOpen()), java.util.Optional.of(p_273340_.fenceGateClose()));
    }
    public FenceGateBlock(BlockBehaviour.Properties p_273352_, net.minecraft.sounds.SoundEvent openSound, net.minecraft.sounds.SoundEvent closeSound) {
        this(java.util.Optional.empty(), p_273352_, java.util.Optional.of(openSound), java.util.Optional.of(closeSound));
    }
    public FenceGateBlock(java.util.Optional<WoodType> p_273340_, BlockBehaviour.Properties p_273352_, java.util.Optional<net.minecraft.sounds.SoundEvent> openSound, java.util.Optional<net.minecraft.sounds.SoundEvent> closeSound) {
        super(p_273352_);
        com.google.common.base.Preconditions.checkArgument(p_273340_.isPresent() || (openSound.isPresent() && closeSound.isPresent()), "Fence gates must have sounds set");
        this.type = p_273340_.orElse(null);
        this.openSound = openSound.orElseGet(() -> type.fenceGateOpen()); // Type may be null, so we cannot do a method ref
        this.closeSound = closeSound.orElseGet(() -> type.fenceGateClose());
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(OPEN, Boolean.valueOf(false))
                .setValue(POWERED, Boolean.valueOf(false))
                .setValue(IN_WALL, Boolean.valueOf(false))
        );
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (pState.getValue(IN_WALL)) {
            return pState.getValue(FACING).getAxis() == Direction.Axis.X ? X_SHAPE_LOW : Z_SHAPE_LOW;
        } else {
            return pState.getValue(FACING).getAxis() == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
        }
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        Direction.Axis direction$axis = pFacing.getAxis();
        if (pState.getValue(FACING).getClockWise().getAxis() != direction$axis) {
            return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
        } else {
            boolean flag = this.isWall(pFacingState) || this.isWall(pLevel.getBlockState(pCurrentPos.relative(pFacing.getOpposite())));
            return pState.setValue(IN_WALL, Boolean.valueOf(flag));
        }
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        if (pState.getValue(OPEN)) {
            return Shapes.empty();
        } else {
            return pState.getValue(FACING).getAxis() == Direction.Axis.Z ? Z_SUPPORT_SHAPE : X_SUPPORT_SHAPE;
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (pState.getValue(OPEN)) {
            return Shapes.empty();
        } else {
            return pState.getValue(FACING).getAxis() == Direction.Axis.Z ? Z_COLLISION_SHAPE : X_COLLISION_SHAPE;
        }
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        if (pState.getValue(IN_WALL)) {
            return pState.getValue(FACING).getAxis() == Direction.Axis.X ? X_OCCLUSION_SHAPE_LOW : Z_OCCLUSION_SHAPE_LOW;
        } else {
            return pState.getValue(FACING).getAxis() == Direction.Axis.X ? X_OCCLUSION_SHAPE : Z_OCCLUSION_SHAPE;
        }
    }

    @Override
    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        switch (pPathComputationType) {
            case LAND:
                return pState.getValue(OPEN);
            case WATER:
                return false;
            case AIR:
                return pState.getValue(OPEN);
            default:
                return false;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        boolean flag = level.hasNeighborSignal(blockpos);
        Direction direction = pContext.getHorizontalDirection();
        Direction.Axis direction$axis = direction.getAxis();
        boolean flag1 = direction$axis == Direction.Axis.Z
                && (this.isWall(level.getBlockState(blockpos.west())) || this.isWall(level.getBlockState(blockpos.east())))
            || direction$axis == Direction.Axis.X && (this.isWall(level.getBlockState(blockpos.north())) || this.isWall(level.getBlockState(blockpos.south())));
        return this.defaultBlockState()
            .setValue(FACING, direction)
            .setValue(OPEN, Boolean.valueOf(flag))
            .setValue(POWERED, Boolean.valueOf(flag))
            .setValue(IN_WALL, Boolean.valueOf(flag1));
    }

    private boolean isWall(BlockState pState) {
        return pState.is(BlockTags.WALLS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        if (pState.getValue(OPEN)) {
            pState = pState.setValue(OPEN, Boolean.valueOf(false));
            pLevel.setBlock(pPos, pState, 10);
        } else {
            Direction direction = pPlayer.getDirection();
            if (pState.getValue(FACING) == direction.getOpposite()) {
                pState = pState.setValue(FACING, direction);
            }

            pState = pState.setValue(OPEN, Boolean.valueOf(true));
            pLevel.setBlock(pPos, pState, 10);
        }

        boolean flag = pState.getValue(OPEN);
        pLevel.playSound(
            pPlayer,
            pPos,
            flag ? openSound : closeSound,
            SoundSource.BLOCKS,
            1.0F,
            pLevel.getRandom().nextFloat() * 0.1F + 0.9F
        );
        pLevel.gameEvent(pPlayer, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
        return InteractionResult.sidedSuccess(pLevel.isClientSide);
    }

    @Override
    protected void onExplosionHit(BlockState pState, Level pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
        if (pExplosion.canTriggerBlocks() && !pState.getValue(POWERED)) {
            boolean flag = pState.getValue(OPEN);
            pLevel.setBlockAndUpdate(pPos, pState.setValue(OPEN, Boolean.valueOf(!flag)));
            pLevel.playSound(
                null,
                pPos,
                flag ? closeSound : openSound,
                SoundSource.BLOCKS,
                1.0F,
                pLevel.getRandom().nextFloat() * 0.1F + 0.9F
            );
            pLevel.gameEvent(flag ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pPos, GameEvent.Context.of(pState));
        }

        super.onExplosionHit(pState, pLevel, pPos, pExplosion, pDropConsumer);
    }

    @Override
    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if (!pLevel.isClientSide) {
            boolean flag = pLevel.hasNeighborSignal(pPos);
            if (pState.getValue(POWERED) != flag) {
                pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(flag)).setValue(OPEN, Boolean.valueOf(flag)), 2);
                if (pState.getValue(OPEN) != flag) {
                    pLevel.playSound(
                        null,
                        pPos,
                        flag ? openSound : closeSound,
                        SoundSource.BLOCKS,
                        1.0F,
                        pLevel.getRandom().nextFloat() * 0.1F + 0.9F
                    );
                    pLevel.gameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, OPEN, POWERED, IN_WALL);
    }

    public static boolean connectsToDirection(BlockState pState, Direction pDirection) {
        return pState.getValue(FACING).getAxis() == pDirection.getClockWise().getAxis();
    }
}
