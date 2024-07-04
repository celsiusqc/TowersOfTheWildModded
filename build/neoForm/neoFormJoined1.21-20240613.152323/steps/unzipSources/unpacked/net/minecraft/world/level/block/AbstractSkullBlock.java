package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;

public abstract class AbstractSkullBlock extends BaseEntityBlock implements Equipable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final SkullBlock.Type type;

    public AbstractSkullBlock(SkullBlock.Type pType, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.type = pType;
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.valueOf(false)));
    }

    @Override
    protected abstract MapCodec<? extends AbstractSkullBlock> codec();

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new SkullBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide) {
            boolean flag = pState.is(Blocks.DRAGON_HEAD)
                || pState.is(Blocks.DRAGON_WALL_HEAD)
                || pState.is(Blocks.PIGLIN_HEAD)
                || pState.is(Blocks.PIGLIN_WALL_HEAD);
            if (flag) {
                return createTickerHelper(pBlockEntityType, BlockEntityType.SKULL, SkullBlockEntity::animation);
            }
        }

        return null;
    }

    public SkullBlock.Type getType() {
        return this.type;
    }

    @Override
    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        return false;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(POWERED, Boolean.valueOf(pContext.getLevel().hasNeighborSignal(pContext.getClickedPos())));
    }

    @Override
    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
        if (!pLevel.isClientSide) {
            boolean flag = pLevel.hasNeighborSignal(pPos);
            if (flag != pState.getValue(POWERED)) {
                pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(flag)), 2);
            }
        }
    }
}
