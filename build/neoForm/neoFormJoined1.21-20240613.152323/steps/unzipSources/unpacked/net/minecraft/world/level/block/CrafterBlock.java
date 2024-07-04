package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CrafterBlock extends BaseEntityBlock {
    public static final MapCodec<CrafterBlock> CODEC = simpleCodec(CrafterBlock::new);
    public static final BooleanProperty CRAFTING = BlockStateProperties.CRAFTING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    private static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
    private static final int MAX_CRAFTING_TICKS = 6;
    private static final int CRAFTING_TICK_DELAY = 4;
    private static final RecipeCache RECIPE_CACHE = new RecipeCache(10);
    private static final int CRAFTER_ADVANCEMENT_DIAMETER = 17;

    public CrafterBlock(BlockBehaviour.Properties p_307674_) {
        super(p_307674_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(ORIENTATION, FrontAndTop.NORTH_UP)
                .setValue(TRIGGERED, Boolean.valueOf(false))
                .setValue(CRAFTING, Boolean.valueOf(false))
        );
    }

    @Override
    protected MapCodec<CrafterBlock> codec() {
        return CODEC;
    }

    /**
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#hasAnalogOutputSignal} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    /**
     * Returns the analog signal this block emits. This is the signal a comparator can read from it.
     *
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getAnalogOutputSignal} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        return pLevel.getBlockEntity(pPos) instanceof CrafterBlockEntity crafterblockentity ? crafterblockentity.getRedstoneSignal() : 0;
    }

    @Override
    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
        boolean flag = pLevel.hasNeighborSignal(pPos);
        boolean flag1 = pState.getValue(TRIGGERED);
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (flag && !flag1) {
            pLevel.scheduleTick(pPos, this, 4);
            pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(true)), 2);
            this.setBlockEntityTriggered(blockentity, true);
        } else if (!flag && flag1) {
            pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(false)).setValue(CRAFTING, Boolean.valueOf(false)), 2);
            this.setBlockEntityTriggered(blockentity, false);
        }
    }

    @Override
    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        this.dispenseFrom(pState, pLevel, pPos);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, BlockEntityType.CRAFTER, CrafterBlockEntity::serverTick);
    }

    private void setBlockEntityTriggered(@Nullable BlockEntity pBlockEntity, boolean pTriggered) {
        if (pBlockEntity instanceof CrafterBlockEntity crafterblockentity) {
            crafterblockentity.setTriggered(pTriggered);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        CrafterBlockEntity crafterblockentity = new CrafterBlockEntity(pPos, pState);
        crafterblockentity.setTriggered(pState.hasProperty(TRIGGERED) && pState.getValue(TRIGGERED));
        return crafterblockentity;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction direction = pContext.getNearestLookingDirection().getOpposite();

        Direction direction1 = switch (direction) {
            case DOWN -> pContext.getHorizontalDirection().getOpposite();
            case UP -> pContext.getHorizontalDirection();
            case NORTH, SOUTH, WEST, EAST -> Direction.UP;
        };
        return this.defaultBlockState()
            .setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(direction, direction1))
            .setValue(TRIGGERED, Boolean.valueOf(pContext.getLevel().hasNeighborSignal(pContext.getClickedPos())));
    }

    /**
     * Called by BlockItem after this block has been placed.
     */
    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        if (pState.getValue(TRIGGERED)) {
            pLevel.scheduleTick(pPos, this, 4);
        }
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        Containers.dropContentsOnDestroy(pState, pNewState, pLevel, pPos);
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof CrafterBlockEntity) {
                pPlayer.openMenu((CrafterBlockEntity)blockentity);
            }

            return InteractionResult.CONSUME;
        }
    }

    protected void dispenseFrom(BlockState pState, ServerLevel pLevel, BlockPos pPos) {
        if (pLevel.getBlockEntity(pPos) instanceof CrafterBlockEntity crafterblockentity) {
            CraftingInput craftinginput = crafterblockentity.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> optional = getPotentialResults(pLevel, craftinginput);
            if (optional.isEmpty()) {
                pLevel.levelEvent(1050, pPos, 0);
            } else {
                RecipeHolder<CraftingRecipe> recipeholder = optional.get();
                ItemStack itemstack = recipeholder.value().assemble(craftinginput, pLevel.registryAccess());
                if (itemstack.isEmpty()) {
                    pLevel.levelEvent(1050, pPos, 0);
                } else {
                    crafterblockentity.setCraftingTicksRemaining(6);
                    pLevel.setBlock(pPos, pState.setValue(CRAFTING, Boolean.valueOf(true)), 2);
                    itemstack.onCraftedBySystem(pLevel);
                    this.dispenseItem(pLevel, pPos, crafterblockentity, itemstack, pState, recipeholder);

                    for (ItemStack itemstack1 : recipeholder.value().getRemainingItems(craftinginput)) {
                        if (!itemstack1.isEmpty()) {
                            this.dispenseItem(pLevel, pPos, crafterblockentity, itemstack1, pState, recipeholder);
                        }
                    }

                    crafterblockentity.getItems().forEach(p_307295_ -> {
                        if (!p_307295_.isEmpty()) {
                            p_307295_.shrink(1);
                        }
                    });
                    crafterblockentity.setChanged();
                }
            }
        }
    }

    public static Optional<RecipeHolder<CraftingRecipe>> getPotentialResults(Level p_307625_, CraftingInput p_345096_) {
        return RECIPE_CACHE.get(p_307625_, p_345096_);
    }

    private void dispenseItem(
        ServerLevel pLevel,
        BlockPos pPos,
        CrafterBlockEntity pCrafter,
        ItemStack pStack,
        BlockState pState,
        RecipeHolder<CraftingRecipe> pRecipe
    ) {
        Direction direction = pState.getValue(ORIENTATION).front();
        Container container = HopperBlockEntity.getContainerAt(pLevel, pPos.relative(direction));
        ItemStack itemstack = pStack.copy();
        if (container != null && (container instanceof CrafterBlockEntity || pStack.getCount() > container.getMaxStackSize(pStack))) {
            while (!itemstack.isEmpty()) {
                ItemStack itemstack2 = itemstack.copyWithCount(1);
                ItemStack itemstack1 = HopperBlockEntity.addItem(pCrafter, container, itemstack2, direction.getOpposite());
                if (!itemstack1.isEmpty()) {
                    break;
                }

                itemstack.shrink(1);
            }
        } else if (container != null) {
            while (!itemstack.isEmpty()) {
                int i = itemstack.getCount();
                itemstack = HopperBlockEntity.addItem(pCrafter, container, itemstack, direction.getOpposite());
                if (i == itemstack.getCount()) {
                    break;
                }
            }
        }

        if (!itemstack.isEmpty()) {
            Vec3 vec3 = Vec3.atCenterOf(pPos);
            Vec3 vec31 = vec3.relative(direction, 0.7);
            DefaultDispenseItemBehavior.spawnItem(pLevel, itemstack, 6, direction, vec31);

            for (ServerPlayer serverplayer : pLevel.getEntitiesOfClass(ServerPlayer.class, AABB.ofSize(vec3, 17.0, 17.0, 17.0))) {
                CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.trigger(serverplayer, pRecipe.id(), pCrafter.getItems());
            }

            pLevel.levelEvent(1049, pPos, 0);
            pLevel.levelEvent(2010, pPos, direction.get3DDataValue());
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

    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#rotate} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(ORIENTATION, pRotation.rotation().rotate(pState.getValue(ORIENTATION)));
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#mirror} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.setValue(ORIENTATION, pMirror.rotation().rotate(pState.getValue(ORIENTATION)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(ORIENTATION, TRIGGERED, CRAFTING);
    }
}
