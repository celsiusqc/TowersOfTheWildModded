package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.phys.BlockHitResult;

public class StructureBlock extends BaseEntityBlock implements GameMasterBlock {
    public static final MapCodec<StructureBlock> CODEC = simpleCodec(StructureBlock::new);
    public static final EnumProperty<StructureMode> MODE = BlockStateProperties.STRUCTUREBLOCK_MODE;

    @Override
    public MapCodec<StructureBlock> codec() {
        return CODEC;
    }

    public StructureBlock(BlockBehaviour.Properties p_57113_) {
        super(p_57113_);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, StructureMode.LOAD));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new StructureBlockEntity(pPos, pState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof StructureBlockEntity) {
            return ((StructureBlockEntity)blockentity).usedBy(pPlayer) ? InteractionResult.sidedSuccess(pLevel.isClientSide) : InteractionResult.PASS;
        } else {
            return InteractionResult.PASS;
        }
    }

    /**
     * Called by BlockItem after this block has been placed.
     */
    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        if (!pLevel.isClientSide) {
            if (pPlacer != null) {
                BlockEntity blockentity = pLevel.getBlockEntity(pPos);
                if (blockentity instanceof StructureBlockEntity) {
                    ((StructureBlockEntity)blockentity).createdBy(pPlacer);
                }
            }
        }
    }

    /**
     * The type of render function called. MODEL for mixed tesr and static model, MODELBLOCK_ANIMATED for TESR-only, LIQUID for vanilla liquids, INVISIBLE to skip all rendering
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getRenderShape} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(MODE);
    }

    @Override
    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if (pLevel instanceof ServerLevel) {
            if (pLevel.getBlockEntity(pPos) instanceof StructureBlockEntity structureblockentity) {
                boolean flag = pLevel.hasNeighborSignal(pPos);
                boolean flag1 = structureblockentity.isPowered();
                if (flag && !flag1) {
                    structureblockentity.setPowered(true);
                    this.trigger((ServerLevel)pLevel, structureblockentity);
                } else if (!flag && flag1) {
                    structureblockentity.setPowered(false);
                }
            }
        }
    }

    private void trigger(ServerLevel pLevel, StructureBlockEntity pBlockEntity) {
        switch (pBlockEntity.getMode()) {
            case SAVE:
                pBlockEntity.saveStructure(false);
                break;
            case LOAD:
                pBlockEntity.placeStructure(pLevel);
                break;
            case CORNER:
                pBlockEntity.unloadStructure();
            case DATA:
        }
    }
}
