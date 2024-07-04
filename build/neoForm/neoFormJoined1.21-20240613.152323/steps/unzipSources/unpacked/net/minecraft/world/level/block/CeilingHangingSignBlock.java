package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CeilingHangingSignBlock extends SignBlock {
    public static final MapCodec<CeilingHangingSignBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308810_ -> p_308810_.group(WoodType.CODEC.fieldOf("wood_type").forGetter(SignBlock::type), propertiesCodec())
                .apply(p_308810_, CeilingHangingSignBlock::new)
    );
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    protected static final float AABB_OFFSET = 5.0F;
    protected static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final Map<Integer, VoxelShape> AABBS = Maps.newHashMap(
        ImmutableMap.of(
            0,
            Block.box(1.0, 0.0, 7.0, 15.0, 10.0, 9.0),
            4,
            Block.box(7.0, 0.0, 1.0, 9.0, 10.0, 15.0),
            8,
            Block.box(1.0, 0.0, 7.0, 15.0, 10.0, 9.0),
            12,
            Block.box(7.0, 0.0, 1.0, 9.0, 10.0, 15.0)
        )
    );

    @Override
    public MapCodec<CeilingHangingSignBlock> codec() {
        return CODEC;
    }

    public CeilingHangingSignBlock(WoodType p_248716_, BlockBehaviour.Properties p_250481_) {
        super(p_248716_, p_250481_.sound(p_248716_.hangingSignSoundType()));
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(ROTATION, Integer.valueOf(0))
                .setValue(ATTACHED, Boolean.valueOf(false))
                .setValue(WATERLOGGED, Boolean.valueOf(false))
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
    ) {
        if (pLevel.getBlockEntity(pPos) instanceof SignBlockEntity signblockentity
            && this.shouldTryToChainAnotherHangingSign(pPlayer, pHitResult, signblockentity, pStack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        return super.useItemOn(pStack, pState, pLevel, pPos, pPlayer, pHand, pHitResult);
    }

    private boolean shouldTryToChainAnotherHangingSign(Player pPlayer, BlockHitResult pHitResult, SignBlockEntity pSign, ItemStack pStack) {
        return !pSign.canExecuteClickCommands(pSign.isFacingFrontText(pPlayer), pPlayer)
            && pStack.getItem() instanceof HangingSignItem
            && pHitResult.getDirection().equals(Direction.DOWN);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos.above()).isFaceSturdy(pLevel, pPos.above(), Direction.DOWN, SupportType.CENTER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Level level = pContext.getLevel();
        FluidState fluidstate = level.getFluidState(pContext.getClickedPos());
        BlockPos blockpos = pContext.getClickedPos().above();
        BlockState blockstate = level.getBlockState(blockpos);
        boolean flag = blockstate.is(BlockTags.ALL_HANGING_SIGNS);
        Direction direction = Direction.fromYRot((double)pContext.getRotation());
        boolean flag1 = !Block.isFaceFull(blockstate.getCollisionShape(level, blockpos), Direction.DOWN) || pContext.isSecondaryUseActive();
        if (flag && !pContext.isSecondaryUseActive()) {
            if (blockstate.hasProperty(WallHangingSignBlock.FACING)) {
                Direction direction1 = blockstate.getValue(WallHangingSignBlock.FACING);
                if (direction1.getAxis().test(direction)) {
                    flag1 = false;
                }
            } else if (blockstate.hasProperty(ROTATION)) {
                Optional<Direction> optional = RotationSegment.convertToDirection(blockstate.getValue(ROTATION));
                if (optional.isPresent() && optional.get().getAxis().test(direction)) {
                    flag1 = false;
                }
            }
        }

        int i = !flag1 ? RotationSegment.convertToSegment(direction.getOpposite()) : RotationSegment.convertToSegment(pContext.getRotation() + 180.0F);
        return this.defaultBlockState()
            .setValue(ATTACHED, Boolean.valueOf(flag1))
            .setValue(ROTATION, Integer.valueOf(i))
            .setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        VoxelShape voxelshape = AABBS.get(pState.getValue(ROTATION));
        return voxelshape == null ? SHAPE : voxelshape;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return this.getShape(pState, pLevel, pPos, CollisionContext.empty());
    }

    @Override
    protected BlockState updateShape(
        BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
    ) {
        return pFacing == Direction.UP && !this.canSurvive(pState, pLevel, pCurrentPos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    public float getYRotationDegrees(BlockState pState) {
        return RotationSegment.convertToDegrees(pState.getValue(ROTATION));
    }

    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#rotate} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(ROTATION, Integer.valueOf(pRotation.rotate(pState.getValue(ROTATION), 16)));
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#mirror} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.setValue(ROTATION, Integer.valueOf(pMirror.mirror(pState.getValue(ROTATION), 16)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(ROTATION, ATTACHED, WATERLOGGED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new HangingSignBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, BlockEntityType.HANGING_SIGN, SignBlockEntity::tick);
    }
}
