package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ComposterBlock extends Block implements WorldlyContainerHolder {
    public static final MapCodec<ComposterBlock> CODEC = simpleCodec(ComposterBlock::new);
    public static final int READY = 8;
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 7;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;
    /** @deprecated Neo: Use the {@link net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps#COMPOSTABLES compostable} data map instead */
    @Deprecated
    public static final Object2FloatMap<ItemLike> COMPOSTABLES = new Object2FloatOpenHashMap<>();
    private static final int AABB_SIDE_THICKNESS = 2;
    private static final VoxelShape OUTER_SHAPE = Shapes.block();
    private static final VoxelShape[] SHAPES = Util.make(new VoxelShape[9], p_51967_ -> {
        for (int i = 0; i < 8; i++) {
            p_51967_[i] = Shapes.join(OUTER_SHAPE, Block.box(2.0, (double)Math.max(2, 1 + i * 2), 2.0, 14.0, 16.0, 14.0), BooleanOp.ONLY_FIRST);
        }

        p_51967_[8] = p_51967_[7];
    });

    @Override
    public MapCodec<ComposterBlock> codec() {
        return CODEC;
    }

    public static void bootStrap() {
        COMPOSTABLES.defaultReturnValue(-1.0F);
        float f = 0.3F;
        float f1 = 0.5F;
        float f2 = 0.65F;
        float f3 = 0.85F;
        float f4 = 1.0F;
        add(0.3F, Items.JUNGLE_LEAVES);
        add(0.3F, Items.OAK_LEAVES);
        add(0.3F, Items.SPRUCE_LEAVES);
        add(0.3F, Items.DARK_OAK_LEAVES);
        add(0.3F, Items.ACACIA_LEAVES);
        add(0.3F, Items.CHERRY_LEAVES);
        add(0.3F, Items.BIRCH_LEAVES);
        add(0.3F, Items.AZALEA_LEAVES);
        add(0.3F, Items.MANGROVE_LEAVES);
        add(0.3F, Items.OAK_SAPLING);
        add(0.3F, Items.SPRUCE_SAPLING);
        add(0.3F, Items.BIRCH_SAPLING);
        add(0.3F, Items.JUNGLE_SAPLING);
        add(0.3F, Items.ACACIA_SAPLING);
        add(0.3F, Items.CHERRY_SAPLING);
        add(0.3F, Items.DARK_OAK_SAPLING);
        add(0.3F, Items.MANGROVE_PROPAGULE);
        add(0.3F, Items.BEETROOT_SEEDS);
        add(0.3F, Items.DRIED_KELP);
        add(0.3F, Items.SHORT_GRASS);
        add(0.3F, Items.KELP);
        add(0.3F, Items.MELON_SEEDS);
        add(0.3F, Items.PUMPKIN_SEEDS);
        add(0.3F, Items.SEAGRASS);
        add(0.3F, Items.SWEET_BERRIES);
        add(0.3F, Items.GLOW_BERRIES);
        add(0.3F, Items.WHEAT_SEEDS);
        add(0.3F, Items.MOSS_CARPET);
        add(0.3F, Items.PINK_PETALS);
        add(0.3F, Items.SMALL_DRIPLEAF);
        add(0.3F, Items.HANGING_ROOTS);
        add(0.3F, Items.MANGROVE_ROOTS);
        add(0.3F, Items.TORCHFLOWER_SEEDS);
        add(0.3F, Items.PITCHER_POD);
        add(0.5F, Items.DRIED_KELP_BLOCK);
        add(0.5F, Items.TALL_GRASS);
        add(0.5F, Items.FLOWERING_AZALEA_LEAVES);
        add(0.5F, Items.CACTUS);
        add(0.5F, Items.SUGAR_CANE);
        add(0.5F, Items.VINE);
        add(0.5F, Items.NETHER_SPROUTS);
        add(0.5F, Items.WEEPING_VINES);
        add(0.5F, Items.TWISTING_VINES);
        add(0.5F, Items.MELON_SLICE);
        add(0.5F, Items.GLOW_LICHEN);
        add(0.65F, Items.SEA_PICKLE);
        add(0.65F, Items.LILY_PAD);
        add(0.65F, Items.PUMPKIN);
        add(0.65F, Items.CARVED_PUMPKIN);
        add(0.65F, Items.MELON);
        add(0.65F, Items.APPLE);
        add(0.65F, Items.BEETROOT);
        add(0.65F, Items.CARROT);
        add(0.65F, Items.COCOA_BEANS);
        add(0.65F, Items.POTATO);
        add(0.65F, Items.WHEAT);
        add(0.65F, Items.BROWN_MUSHROOM);
        add(0.65F, Items.RED_MUSHROOM);
        add(0.65F, Items.MUSHROOM_STEM);
        add(0.65F, Items.CRIMSON_FUNGUS);
        add(0.65F, Items.WARPED_FUNGUS);
        add(0.65F, Items.NETHER_WART);
        add(0.65F, Items.CRIMSON_ROOTS);
        add(0.65F, Items.WARPED_ROOTS);
        add(0.65F, Items.SHROOMLIGHT);
        add(0.65F, Items.DANDELION);
        add(0.65F, Items.POPPY);
        add(0.65F, Items.BLUE_ORCHID);
        add(0.65F, Items.ALLIUM);
        add(0.65F, Items.AZURE_BLUET);
        add(0.65F, Items.RED_TULIP);
        add(0.65F, Items.ORANGE_TULIP);
        add(0.65F, Items.WHITE_TULIP);
        add(0.65F, Items.PINK_TULIP);
        add(0.65F, Items.OXEYE_DAISY);
        add(0.65F, Items.CORNFLOWER);
        add(0.65F, Items.LILY_OF_THE_VALLEY);
        add(0.65F, Items.WITHER_ROSE);
        add(0.65F, Items.FERN);
        add(0.65F, Items.SUNFLOWER);
        add(0.65F, Items.LILAC);
        add(0.65F, Items.ROSE_BUSH);
        add(0.65F, Items.PEONY);
        add(0.65F, Items.LARGE_FERN);
        add(0.65F, Items.SPORE_BLOSSOM);
        add(0.65F, Items.AZALEA);
        add(0.65F, Items.MOSS_BLOCK);
        add(0.65F, Items.BIG_DRIPLEAF);
        add(0.85F, Items.HAY_BLOCK);
        add(0.85F, Items.BROWN_MUSHROOM_BLOCK);
        add(0.85F, Items.RED_MUSHROOM_BLOCK);
        add(0.85F, Items.NETHER_WART_BLOCK);
        add(0.85F, Items.WARPED_WART_BLOCK);
        add(0.85F, Items.FLOWERING_AZALEA);
        add(0.85F, Items.BREAD);
        add(0.85F, Items.BAKED_POTATO);
        add(0.85F, Items.COOKIE);
        add(0.85F, Items.TORCHFLOWER);
        add(0.85F, Items.PITCHER_PLANT);
        add(1.0F, Items.CAKE);
        add(1.0F, Items.PUMPKIN_PIE);
    }

    private static void add(float pChance, ItemLike pItem) {
        COMPOSTABLES.put(pItem.asItem(), pChance);
    }

    public ComposterBlock(BlockBehaviour.Properties p_51919_) {
        super(p_51919_);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, Integer.valueOf(0)));
    }

    public static void handleFill(Level pLevel, BlockPos pPos, boolean pSuccess) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        pLevel.playLocalSound(pPos, pSuccess ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        double d0 = blockstate.getShape(pLevel, pPos).max(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
        double d1 = 0.13125F;
        double d2 = 0.7375F;
        RandomSource randomsource = pLevel.getRandom();

        for (int i = 0; i < 10; i++) {
            double d3 = randomsource.nextGaussian() * 0.02;
            double d4 = randomsource.nextGaussian() * 0.02;
            double d5 = randomsource.nextGaussian() * 0.02;
            pLevel.addParticle(
                ParticleTypes.COMPOSTER,
                (double)pPos.getX() + 0.13125F + 0.7375F * (double)randomsource.nextFloat(),
                (double)pPos.getY() + d0 + (double)randomsource.nextFloat() * (1.0 - d0),
                (double)pPos.getZ() + 0.13125F + 0.7375F * (double)randomsource.nextFloat(),
                d3,
                d4,
                d5
            );
        }
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES[pState.getValue(LEVEL)];
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return OUTER_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES[0];
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (pState.getValue(LEVEL) == 7) {
            pLevel.scheduleTick(pPos, pState.getBlock(), 20);
        }
        // Neo: Invalidate composter capabilities when a composter is added
        if (!pOldState.is(this)) pLevel.invalidateCapabilities(pPos);
    }

    @Override
    protected void onRemove(BlockState p_60515_, Level p_60516_, BlockPos p_60517_, BlockState p_60518_, boolean p_60519_) {
        super.onRemove(p_60515_, p_60516_, p_60517_, p_60518_, p_60519_);
        // Neo: Invalidate composter capabilities when a composter is removed
        if (!p_60515_.is(p_60518_.getBlock())) p_60516_.invalidateCapabilities(p_60517_);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
    ) {
        int i = pState.getValue(LEVEL);
        if (i < 8 && getValue(pStack) > 0) {
            if (i < 7 && !pLevel.isClientSide) {
                BlockState blockstate = addItem(pPlayer, pState, pLevel, pPos, pStack);
                pLevel.levelEvent(1500, pPos, pState != blockstate ? 1 : 0);
                pPlayer.awardStat(Stats.ITEM_USED.get(pStack.getItem()));
                pStack.consume(1, pPlayer);
            }

            return ItemInteractionResult.sidedSuccess(pLevel.isClientSide);
        } else {
            return super.useItemOn(pStack, pState, pLevel, pPos, pPlayer, pHand, pHitResult);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        int i = pState.getValue(LEVEL);
        if (i == 8) {
            extractProduce(pPlayer, pState, pLevel, pPos);
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    public static BlockState insertItem(Entity pEntity, BlockState pState, ServerLevel pLevel, ItemStack pStack, BlockPos pPos) {
        int i = pState.getValue(LEVEL);
        if (i < 7 && getValue(pStack) > 0) {
            BlockState blockstate = addItem(pEntity, pState, pLevel, pPos, pStack);
            pStack.shrink(1);
            return blockstate;
        } else {
            return pState;
        }
    }

    public static BlockState extractProduce(Entity pEntity, BlockState pState, Level pLevel, BlockPos pPos) {
        if (!pLevel.isClientSide) {
            Vec3 vec3 = Vec3.atLowerCornerWithOffset(pPos, 0.5, 1.01, 0.5).offsetRandom(pLevel.random, 0.7F);
            ItemEntity itementity = new ItemEntity(pLevel, vec3.x(), vec3.y(), vec3.z(), new ItemStack(Items.BONE_MEAL));
            itementity.setDefaultPickUpDelay();
            pLevel.addFreshEntity(itementity);
        }

        BlockState blockstate = empty(pEntity, pState, pLevel, pPos);
        pLevel.playSound(null, pPos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        return blockstate;
    }

    static BlockState empty(@Nullable Entity pEntity, BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
        BlockState blockstate = pState.setValue(LEVEL, Integer.valueOf(0));
        pLevel.setBlock(pPos, blockstate, 3);
        pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
        return blockstate;
    }

    static BlockState addItem(@Nullable Entity pEntity, BlockState pState, LevelAccessor pLevel, BlockPos pPos, ItemStack pStack) {
        int i = pState.getValue(LEVEL);
        float f = getValue(pStack);
        if ((i != 0 || !(f > 0.0F)) && !(pLevel.getRandom().nextDouble() < (double)f)) {
            return pState;
        } else {
            int j = i + 1;
            BlockState blockstate = pState.setValue(LEVEL, Integer.valueOf(j));
            pLevel.setBlock(pPos, blockstate, 3);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
            if (j == 7) {
                pLevel.scheduleTick(pPos, pState.getBlock(), 20);
            }

            return blockstate;
        }
    }

    @Override
    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(LEVEL) == 7) {
            pLevel.setBlock(pPos, pState.cycle(LEVEL), 3);
            pLevel.playSound(null, pPos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
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
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        return pBlockState.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LEVEL);
    }

    @Override
    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        return false;
    }

    @Override
    public WorldlyContainer getContainer(BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
        int i = pState.getValue(LEVEL);
        if (i == 8) {
            return new ComposterBlock.OutputContainer(pState, pLevel, pPos, new ItemStack(Items.BONE_MEAL));
        } else {
            return (WorldlyContainer)(i < 7 ? new ComposterBlock.InputContainer(pState, pLevel, pPos) : new ComposterBlock.EmptyContainer());
        }
    }

    static class EmptyContainer extends SimpleContainer implements WorldlyContainer {
        public EmptyContainer() {
            super(0);
        }

        @Override
        public int[] getSlotsForFace(Direction pSide) {
            return new int[0];
        }

        /**
         * Returns {@code true} if automation can insert the given item in the given slot from the given side.
         */
        @Override
        public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
            return false;
        }

        /**
         * Returns {@code true} if automation can extract the given item in the given slot from the given side.
         */
        @Override
        public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
            return false;
        }
    }

    static class InputContainer extends SimpleContainer implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos pos;
        private boolean changed;

        public InputContainer(BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
            super(1);
            this.state = pState;
            this.level = pLevel;
            this.pos = pPos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction pSide) {
            return pSide == Direction.UP ? new int[]{0} : new int[0];
        }

        /**
         * Returns {@code true} if automation can insert the given item in the given slot from the given side.
         */
        @Override
        public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
            return !this.changed && pDirection == Direction.UP && getValue(pItemStack) > 0f;
        }

        /**
         * Returns {@code true} if automation can extract the given item in the given slot from the given side.
         */
        @Override
        public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
            return false;
        }

        @Override
        public void setChanged() {
            ItemStack itemstack = this.getItem(0);
            if (!itemstack.isEmpty()) {
                this.changed = true;
                BlockState blockstate = ComposterBlock.addItem(null, this.state, this.level, this.pos, itemstack);
                this.level.levelEvent(1500, this.pos, blockstate != this.state ? 1 : 0);
                this.removeItemNoUpdate(0);
            }
        }
    }

    static class OutputContainer extends SimpleContainer implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos pos;
        private boolean changed;

        public OutputContainer(BlockState pState, LevelAccessor pLevel, BlockPos pPos, ItemStack pStack) {
            super(pStack);
            this.state = pState;
            this.level = pLevel;
            this.pos = pPos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction pSide) {
            return pSide == Direction.DOWN ? new int[]{0} : new int[0];
        }

        /**
         * Returns {@code true} if automation can insert the given item in the given slot from the given side.
         */
        @Override
        public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
            return false;
        }

        /**
         * Returns {@code true} if automation can extract the given item in the given slot from the given side.
         */
        @Override
        public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
            return !this.changed && pDirection == Direction.DOWN && pStack.is(Items.BONE_MEAL);
        }

        @Override
        public void setChanged() {
            ComposterBlock.empty(null, this.state, this.level, this.pos);
            this.changed = true;
        }
    }

    public static float getValue(ItemStack item) {
        var value = item.getItemHolder().getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.COMPOSTABLES);
        if (value != null) return value.chance();
        return -1f;
    }
}
