package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerPotBlock extends Block {
    public static final MapCodec<FlowerPotBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_344655_ -> p_344655_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("potted").forGetter(p_304928_ -> p_304928_.potted), propertiesCodec())
                .apply(p_344655_, FlowerPotBlock::new)
    );
    private static final Map<Block, Block> POTTED_BY_CONTENT = Maps.newHashMap();
    public static final float AABB_SIZE = 3.0F;
    protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0);
    /** Neo: Field accesses are redirected to {@link #getPotted()} with a coremod. */
    private final Block potted;

    @Override
    public MapCodec<FlowerPotBlock> codec() {
        return CODEC;
    }

    @Deprecated // Mods should use the constructor below
    public FlowerPotBlock(Block p_53528_, BlockBehaviour.Properties p_53529_) {
        this(Blocks.FLOWER_POT == null ? null : () -> (FlowerPotBlock) Blocks.FLOWER_POT, () -> p_53528_, p_53529_);
        if (Blocks.FLOWER_POT != null) {
            ((FlowerPotBlock)Blocks.FLOWER_POT).addPlant(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(p_53528_), () -> this);
        }
    }

    /**
     * For mod use, eliminates the need to extend this class, and prevents modded
     * flower pots from altering vanilla behavior.
     *
     * @param emptyPot The empty pot for this pot, or null for self.
     */
    public FlowerPotBlock(@org.jetbrains.annotations.Nullable java.util.function.Supplier<FlowerPotBlock> emptyPot, java.util.function.Supplier<? extends Block> p_53528_, BlockBehaviour.Properties properties) {
        super(properties);
        this.potted = null; // Unused, redirected by coremod
        this.flowerDelegate = p_53528_;
        if (emptyPot == null) {
            this.fullPots = Maps.newHashMap();
            this.emptyPot = null;
        } else {
            this.fullPots = java.util.Collections.emptyMap();
            this.emptyPot = emptyPot;
        }
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
    ) {
        BlockState blockstate = (pStack.getItem() instanceof BlockItem blockitem
                ? getEmptyPot().fullPots.getOrDefault(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockitem.getBlock()), () -> Blocks.AIR).get()
                : Blocks.AIR)
            .defaultBlockState();
        if (blockstate.isAir()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        } else if (!this.isEmpty()) {
            return ItemInteractionResult.CONSUME;
        } else {
            pLevel.setBlock(pPos, blockstate, 3);
            pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos);
            pPlayer.awardStat(Stats.POT_FLOWER);
            pStack.consume(1, pPlayer);
            return ItemInteractionResult.sidedSuccess(pLevel.isClientSide);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        if (this.isEmpty()) {
            return InteractionResult.CONSUME;
        } else {
            ItemStack itemstack = new ItemStack(this.potted);
            if (!pPlayer.addItem(itemstack)) {
                pPlayer.drop(itemstack, false);
            }

            pLevel.setBlock(pPos, getEmptyPot().defaultBlockState(), 3);
            pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos);
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return this.isEmpty() ? super.getCloneItemStack(pLevel, pPos, pState) : new ItemStack(this.potted);
    }

    private boolean isEmpty() {
        return this.potted == Blocks.AIR;
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        return pFacing == Direction.DOWN && !pState.canSurvive(pLevel, pCurrentPos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    public Block getPotted() {
        return flowerDelegate.get();
    }

    @Override
    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        return false;
    }

    // Neo: Maps flower blocks to the filled flower pot equivalent
    private final Map<net.minecraft.resources.ResourceLocation, java.util.function.Supplier<? extends Block>> fullPots;

    @org.jetbrains.annotations.Nullable
    private final java.util.function.Supplier<FlowerPotBlock> emptyPot;

    private final java.util.function.Supplier<? extends Block> flowerDelegate;

    public FlowerPotBlock getEmptyPot() {
         return emptyPot == null ? this : emptyPot.get();
    }

    /**
     * Maps the given flower to the filled pot it is for.
     * Call this on the empty pot block. Attempting to call this on a filled pot will throw an exception.
     *
     * @param flower The ResourceLocation of the flower block. Not flower item
     * @param fullPot The filled flower pot to map the flower block to
     */
    public void addPlant(net.minecraft.resources.ResourceLocation flower, java.util.function.Supplier<? extends Block> fullPot) {
         if (getEmptyPot() != this) {
              throw new IllegalArgumentException("Cannot add plant to non-empty pot: " + this + " (Please call addPlant on the empty pot instead)");
         }
         fullPots.put(flower, fullPot);
    }

    /**
     * Returns all the filled pots that can be spawned from filling this pot. (If this pot is filled, returned map will be empty)
     */
    public Map<net.minecraft.resources.ResourceLocation, java.util.function.Supplier<? extends Block>> getFullPotsView() {
        return java.util.Collections.unmodifiableMap(fullPots);
    }
}
