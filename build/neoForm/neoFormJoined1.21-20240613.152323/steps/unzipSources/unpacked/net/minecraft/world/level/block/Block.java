package net.minecraft.world.level.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class Block extends BlockBehaviour implements ItemLike, net.neoforged.neoforge.common.extensions.IBlockExtension {
    public static final MapCodec<Block> CODEC = simpleCodec(Block::new);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Holder.Reference<Block> builtInRegistryHolder = BuiltInRegistries.BLOCK.createIntrusiveHolder(this);
    public static final IdMapper<BlockState> BLOCK_STATE_REGISTRY = net.neoforged.neoforge.registries.GameData.getBlockStateIDMap();
    private static final LoadingCache<VoxelShape, Boolean> SHAPE_FULL_BLOCK_CACHE = CacheBuilder.newBuilder()
        .maximumSize(512L)
        .weakKeys()
        .build(new CacheLoader<VoxelShape, Boolean>() {
            public Boolean load(VoxelShape p_49972_) {
                return !Shapes.joinIsNotEmpty(Shapes.block(), p_49972_, BooleanOp.NOT_SAME);
            }
        });
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_INVISIBLE = 4;
    public static final int UPDATE_IMMEDIATE = 8;
    public static final int UPDATE_KNOWN_SHAPE = 16;
    public static final int UPDATE_SUPPRESS_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int UPDATE_NONE = 4;
    public static final int UPDATE_ALL = 3;
    public static final int UPDATE_ALL_IMMEDIATE = 11;
    public static final float INDESTRUCTIBLE = -1.0F;
    public static final float INSTANT = 0.0F;
    public static final int UPDATE_LIMIT = 512;
    protected final StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultBlockState;
    @Nullable
    private String descriptionId;
    @Nullable
    private Item item;
    private static final int CACHE_SIZE = 2048;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(
        () -> {
            Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2bytelinkedopenhashmap = new Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>(
                2048, 0.25F
            ) {
                @Override
                protected void rehash(int p_49979_) {
                }
            };
            object2bytelinkedopenhashmap.defaultReturnValue((byte)127);
            return object2bytelinkedopenhashmap;
        }
    );

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static int getId(@Nullable BlockState pState) {
        if (pState == null) {
            return 0;
        } else {
            int i = BLOCK_STATE_REGISTRY.getId(pState);
            return i == -1 ? 0 : i;
        }
    }

    public static BlockState stateById(int pId) {
        BlockState blockstate = BLOCK_STATE_REGISTRY.byId(pId);
        return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
    }

    public static Block byItem(@Nullable Item pItem) {
        return pItem instanceof BlockItem ? ((BlockItem)pItem).getBlock() : Blocks.AIR;
    }

    public static BlockState pushEntitiesUp(BlockState pOldState, BlockState pNewState, LevelAccessor pLevel, BlockPos pPos) {
        VoxelShape voxelshape = Shapes.joinUnoptimized(
                pOldState.getCollisionShape(pLevel, pPos), pNewState.getCollisionShape(pLevel, pPos), BooleanOp.ONLY_SECOND
            )
            .move((double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ());
        if (voxelshape.isEmpty()) {
            return pNewState;
        } else {
            for (Entity entity : pLevel.getEntities(null, voxelshape.bounds())) {
                double d0 = Shapes.collide(Direction.Axis.Y, entity.getBoundingBox().move(0.0, 1.0, 0.0), List.of(voxelshape), -1.0);
                entity.teleportRelative(0.0, 1.0 + d0, 0.0);
            }

            return pNewState;
        }
    }

    public static VoxelShape box(double pX1, double pY1, double pZ1, double pX2, double pY2, double pZ2) {
        return Shapes.box(pX1 / 16.0, pY1 / 16.0, pZ1 / 16.0, pX2 / 16.0, pY2 / 16.0, pZ2 / 16.0);
    }

    /**
     * With the provided block state, performs neighbor checks for all neighboring blocks to get an "adjusted" blockstate for placement in the world, if the current state is not valid.
     */
    public static BlockState updateFromNeighbourShapes(BlockState pCurrentState, LevelAccessor pLevel, BlockPos pPos) {
        BlockState blockstate = pCurrentState;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Direction direction : UPDATE_SHAPE_ORDER) {
            blockpos$mutableblockpos.setWithOffset(pPos, direction);
            blockstate = blockstate.updateShape(direction, pLevel.getBlockState(blockpos$mutableblockpos), pLevel, pPos, blockpos$mutableblockpos);
        }

        return blockstate;
    }

    /**
     * Replaces oldState with newState, possibly playing effects and creating drops. Flags are as in {@link net.minecraft.world.level.Level#setBlock(net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, int)}.
     */
    public static void updateOrDestroy(BlockState pOldState, BlockState pNewState, LevelAccessor pLevel, BlockPos pPos, int pFlags) {
        updateOrDestroy(pOldState, pNewState, pLevel, pPos, pFlags, 512);
    }

    public static void updateOrDestroy(BlockState pOldState, BlockState pNewState, LevelAccessor pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
        if (pNewState != pOldState) {
            if (pNewState.isAir()) {
                if (!pLevel.isClientSide()) {
                    pLevel.destroyBlock(pPos, (pFlags & 32) == 0, null, pRecursionLeft);
                }
            } else {
                pLevel.setBlock(pPos, pNewState, pFlags & -33, pRecursionLeft);
            }
        }
    }

    public Block(BlockBehaviour.Properties p_49795_) {
        super(p_49795_);
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(this);
        this.createBlockStateDefinition(builder);
        this.stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
        this.registerDefaultState(this.stateDefinition.any());
        if (SharedConstants.IS_RUNNING_IN_IDE && false) {
            String s = this.getClass().getSimpleName();
            if (!s.endsWith("Block")) {
                LOGGER.error("Block classes should end with Block and {} doesn't.", s);
            }
        }
        initClient();
    }

    public static boolean isExceptionForConnection(BlockState pState) {
        return pState.getBlock() instanceof LeavesBlock
            || pState.is(Blocks.BARRIER)
            || pState.is(Blocks.CARVED_PUMPKIN)
            || pState.is(Blocks.JACK_O_LANTERN)
            || pState.is(Blocks.MELON)
            || pState.is(Blocks.PUMPKIN)
            || pState.is(BlockTags.SHULKER_BOXES);
    }

    public static boolean shouldRenderFace(BlockState pState, BlockGetter pLevel, BlockPos pOffset, Direction pFace, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        if (pState.skipRendering(blockstate, pFace)) {
            return false;
        } else if (blockstate.hidesNeighborFace(pLevel, pPos, pState, pFace.getOpposite()) && pState.supportsExternalFaceHiding()) {
            return false;
        } else if (blockstate.canOcclude()) {
            Block.BlockStatePairKey block$blockstatepairkey = new Block.BlockStatePairKey(pState, blockstate, pFace);
            Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2bytelinkedopenhashmap = OCCLUSION_CACHE.get();
            byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(block$blockstatepairkey);
            if (b0 != 127) {
                return b0 != 0;
            } else {
                VoxelShape voxelshape = pState.getFaceOcclusionShape(pLevel, pOffset, pFace);
                if (voxelshape.isEmpty()) {
                    return true;
                } else {
                    VoxelShape voxelshape1 = blockstate.getFaceOcclusionShape(pLevel, pPos, pFace.getOpposite());
                    boolean flag = Shapes.joinIsNotEmpty(voxelshape, voxelshape1, BooleanOp.ONLY_FIRST);
                    if (object2bytelinkedopenhashmap.size() == 2048) {
                        object2bytelinkedopenhashmap.removeLastByte();
                    }

                    object2bytelinkedopenhashmap.putAndMoveToFirst(block$blockstatepairkey, (byte)(flag ? 1 : 0));
                    return flag;
                }
            }
        } else {
            return true;
        }
    }

    /**
     * @return whether the given position has a rigid top face
     */
    public static boolean canSupportRigidBlock(BlockGetter pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos).isFaceSturdy(pLevel, pPos, Direction.UP, SupportType.RIGID);
    }

    /**
     * @return whether the given position has a solid center in the given direction
     */
    public static boolean canSupportCenter(LevelReader pLevel, BlockPos pPos, Direction pDirection) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        return pDirection == Direction.DOWN && blockstate.is(BlockTags.UNSTABLE_BOTTOM_CENTER)
            ? false
            : blockstate.isFaceSturdy(pLevel, pPos, pDirection, SupportType.CENTER);
    }

    public static boolean isFaceFull(VoxelShape pShape, Direction pFace) {
        VoxelShape voxelshape = pShape.getFaceShape(pFace);
        return isShapeFullBlock(voxelshape);
    }

    /**
     * @return whether the provided {@link net.minecraft.world.phys.shapes.VoxelShape} is a full block (1x1x1)
     */
    public static boolean isShapeFullBlock(VoxelShape pShape) {
        return SHAPE_FULL_BLOCK_CACHE.getUnchecked(pShape);
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
    }

    /**
     * Called after this block has been removed by a player.
     */
    public void destroy(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
    }

    public static List<ItemStack> getDrops(BlockState pState, ServerLevel pLevel, BlockPos pPos, @Nullable BlockEntity pBlockEntity) {
        LootParams.Builder lootparams$builder = new LootParams.Builder(pLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos))
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, pBlockEntity);
        return pState.getDrops(lootparams$builder);
    }

    public static List<ItemStack> getDrops(
        BlockState pState, ServerLevel pLevel, BlockPos pPos, @Nullable BlockEntity pBlockEntity, @Nullable Entity pEntity, ItemStack pTool
    ) {
        LootParams.Builder lootparams$builder = new LootParams.Builder(pLevel)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos))
            .withParameter(LootContextParams.TOOL, pTool)
            .withOptionalParameter(LootContextParams.THIS_ENTITY, pEntity)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, pBlockEntity);
        return pState.getDrops(lootparams$builder);
    }

    public static void dropResources(BlockState pState, Level pLevel, BlockPos pPos) {
        if (pLevel instanceof ServerLevel) {
            beginCapturingDrops();
            getDrops(pState, (ServerLevel)pLevel, pPos, null).forEach(p_152406_ -> popResource(pLevel, pPos, p_152406_));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel) pLevel, pPos, pState, null, captured, null, ItemStack.EMPTY);
        }
    }

    public static void dropResources(BlockState pState, LevelAccessor pLevel, BlockPos pPos, @Nullable BlockEntity pBlockEntity) {
        if (pLevel instanceof ServerLevel) {
            beginCapturingDrops();
            getDrops(pState, (ServerLevel)pLevel, pPos, pBlockEntity).forEach(p_49859_ -> popResource((ServerLevel)pLevel, pPos, p_49859_));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel) pLevel, pPos, pState, pBlockEntity, captured, null, ItemStack.EMPTY);
        }
    }

    public static void dropResources(
            BlockState pState, Level pLevel, BlockPos pPos, @Nullable BlockEntity pBlockEntity, @Nullable Entity pEntity, ItemStack pTool
    ) {
        if (pLevel instanceof ServerLevel) {
            beginCapturingDrops();
            getDrops(pState, (ServerLevel)pLevel, pPos, pBlockEntity, pEntity, pTool).forEach(p_49944_ -> popResource(pLevel, pPos, p_49944_));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel) pLevel, pPos, pState, pBlockEntity, captured, pEntity, pTool);
        }
    }

    /**
     * Spawns the given stack into the Level at the given position, respecting the doTileDrops gamerule
     */
    public static void popResource(Level pLevel, BlockPos pPos, ItemStack pStack) {
        double d0 = (double)EntityType.ITEM.getHeight() / 2.0;
        double d1 = (double)pPos.getX() + 0.5 + Mth.nextDouble(pLevel.random, -0.25, 0.25);
        double d2 = (double)pPos.getY() + 0.5 + Mth.nextDouble(pLevel.random, -0.25, 0.25) - d0;
        double d3 = (double)pPos.getZ() + 0.5 + Mth.nextDouble(pLevel.random, -0.25, 0.25);
        popResource(pLevel, () -> new ItemEntity(pLevel, d1, d2, d3, pStack), pStack);
    }

    public static void popResourceFromFace(Level pLevel, BlockPos pPos, Direction pDirection, ItemStack pStack) {
        int i = pDirection.getStepX();
        int j = pDirection.getStepY();
        int k = pDirection.getStepZ();
        double d0 = (double)EntityType.ITEM.getWidth() / 2.0;
        double d1 = (double)EntityType.ITEM.getHeight() / 2.0;
        double d2 = (double)pPos.getX() + 0.5 + (i == 0 ? Mth.nextDouble(pLevel.random, -0.25, 0.25) : (double)i * (0.5 + d0));
        double d3 = (double)pPos.getY() + 0.5 + (j == 0 ? Mth.nextDouble(pLevel.random, -0.25, 0.25) : (double)j * (0.5 + d1)) - d1;
        double d4 = (double)pPos.getZ() + 0.5 + (k == 0 ? Mth.nextDouble(pLevel.random, -0.25, 0.25) : (double)k * (0.5 + d0));
        double d5 = i == 0 ? Mth.nextDouble(pLevel.random, -0.1, 0.1) : (double)i * 0.1;
        double d6 = j == 0 ? Mth.nextDouble(pLevel.random, 0.0, 0.1) : (double)j * 0.1 + 0.1;
        double d7 = k == 0 ? Mth.nextDouble(pLevel.random, -0.1, 0.1) : (double)k * 0.1;
        popResource(pLevel, () -> new ItemEntity(pLevel, d2, d3, d4, pStack, d5, d6, d7), pStack);
    }

    private static void popResource(Level pLevel, Supplier<ItemEntity> pItemEntitySupplier, ItemStack pStack) {
        if (!pLevel.isClientSide && !pStack.isEmpty() && pLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !pLevel.restoringBlockSnapshots) {
            ItemEntity itementity = pItemEntitySupplier.get();
            itementity.setDefaultPickUpDelay();
            // Neo: Add drops to the captured list if capturing is enabled.
            if (capturedDrops != null) {
                capturedDrops.add(itementity);
            }
            else {
                pLevel.addFreshEntity(itementity);
            }
        }
    }

    /**
     * Spawns the given amount of experience into the Level as experience orb entities.
     */
    public void popExperience(ServerLevel pLevel, BlockPos pPos, int pAmount) {
        if (pLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !pLevel.restoringBlockSnapshots) {
            ExperienceOrb.award(pLevel, Vec3.atCenterOf(pPos), pAmount);
        }
    }

    @Deprecated //Forge: Use more sensitive version
    public float getExplosionResistance() {
        return this.explosionResistance;
    }

    /**
     * Called when this Block is destroyed by an Explosion
     */
    public void wasExploded(Level pLevel, BlockPos pPos, Explosion pExplosion) {
    }

    public void stepOn(Level pLevel, BlockPos pPos, BlockState pState, Entity pEntity) {
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState();
    }

    /**
     * Called after a player has successfully harvested this block. This method will only be called if the player has used the correct tool and drops should be spawned.
     */
    public void playerDestroy(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState, @Nullable BlockEntity pBlockEntity, ItemStack pTool) {
        pPlayer.awardStat(Stats.BLOCK_MINED.get(this));
        pPlayer.causeFoodExhaustion(0.005F);
        dropResources(pState, pLevel, pPos, pBlockEntity, pPlayer, pTool);
    }

    /**
     * Called by BlockItem after this block has been placed.
     */
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
    }

    public boolean isPossibleToRespawnInThis(BlockState pState) {
        return !pState.isSolid() && !pState.liquid();
    }

    public MutableComponent getName() {
        return Component.translatable(this.getDescriptionId());
    }

    public String getDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("block", BuiltInRegistries.BLOCK.getKey(this));
        }

        return this.descriptionId;
    }

    public void fallOn(Level pLevel, BlockState pState, BlockPos pPos, Entity pEntity, float pFallDistance) {
        pEntity.causeFallDamage(pFallDistance, 1.0F, pEntity.damageSources().fall());
    }

    /**
     * Called when an Entity lands on this Block.
     * This method is responsible for doing any modification on the motion of the entity that should result from the landing.
     */
    public void updateEntityAfterFallOn(BlockGetter pLevel, Entity pEntity) {
        pEntity.setDeltaMovement(pEntity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        pEntity.setDeltaMovement(pEntity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
    }

    @Deprecated //Forge: Use more sensitive version
    public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return new ItemStack(this);
    }

    public float getFriction() {
        return this.friction;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public float getJumpFactor() {
        return this.jumpFactor;
    }

    protected void spawnDestroyParticles(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState) {
        pLevel.levelEvent(pPlayer, 2001, pPos, getId(pState));
    }

    public BlockState playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        this.spawnDestroyParticles(pLevel, pPlayer, pPos, pState);
        if (pState.is(BlockTags.GUARDED_BY_PIGLINS)) {
            PiglinAi.angerNearbyPiglins(pPlayer, false);
        }

        pLevel.gameEvent(GameEvent.BLOCK_DESTROY, pPos, GameEvent.Context.of(pPlayer, pState));
        return pState;
    }

    public void handlePrecipitation(BlockState pState, Level pLevel, BlockPos pPos, Biome.Precipitation pPrecipitation) {
    }

    /**
     * @return whether this block should drop its drops when destroyed by the given explosion
     */
    @Deprecated //Forge: Use more sensitive version
    public boolean dropFromExplosion(Explosion pExplosion) {
        return true;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
    }

    public StateDefinition<Block, BlockState> getStateDefinition() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(BlockState pState) {
        this.defaultBlockState = pState;
    }

    public final BlockState defaultBlockState() {
        return this.defaultBlockState;
    }

    public final BlockState withPropertiesOf(BlockState pState) {
        BlockState blockstate = this.defaultBlockState();

        for (Property<?> property : pState.getBlock().getStateDefinition().getProperties()) {
            if (blockstate.hasProperty(property)) {
                blockstate = copyProperty(pState, blockstate, property);
            }
        }

        return blockstate;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState pSourceState, BlockState pTargetState, Property<T> pProperty) {
        return pTargetState.setValue(pProperty, pSourceState.getValue(pProperty));
    }

    @Override
    public Item asItem() {
        if (this.item == null) {
            this.item = Item.byBlock(this);
        }

        return this.item;
    }

    public boolean hasDynamicShape() {
        return this.dynamicShape;
    }

    @Override
    public String toString() {
        return "Block{" + BuiltInRegistries.BLOCK.wrapAsHolder(this).getRegisteredName() + "}";
    }

    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTootipComponents, TooltipFlag pTooltipFlag) {
    }

    @Override
    protected Block asBlock() {
        return this;
    }

    protected ImmutableMap<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> pShapeGetter) {
        return this.stateDefinition.getPossibleStates().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), pShapeGetter));
    }

    /**
     * Neo: Short-lived holder of dropped item entities. Used mainly for Neo hooks and event logic.
     * <p>
     * When not null, records all item entities from {@link #popResource(Level, Supplier, ItemStack)} instead of adding them to the world.
     */
    @Nullable
    private static List<ItemEntity> capturedDrops = null;

    /**
     * Initializes {@link #capturedDrops}, starting the drop capture process.
     * <p>
     * Must only be called on the server thread.
     */
    private static void beginCapturingDrops() {
        capturedDrops = new java.util.ArrayList<>();
    }

    /**
     * Ends the drop capture process by setting {@link #capturedDrops} to null and returning the old list.
     * <p>
     * Must only be called on the server thread.
     */
    private static List<ItemEntity> stopCapturingDrops() {
        List<ItemEntity> drops = capturedDrops;
        capturedDrops = null;
        return drops;
    }

    // Neo: Client rendering for Blocks
    private Object renderProperties;

    /**
     * Neo: DO NOT CALL, IT WILL DISAPPEAR IN THE FUTURE
     * TODO: Replace this with a better solution
     * Call {@link net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions#of(Block)}  instead
     */
    public Object getRenderPropertiesInternal() {
        return renderProperties;
    }

    // Neo: Minecraft instance isn't available in datagen, so don't call initializeClient if in datagen
    private void initClient() {
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT && !net.neoforged.neoforge.data.loading.DatagenModLoader.isRunningDataGen()) {
            initializeClient(properties -> {
                if (properties == this)
                    throw new IllegalStateException("Don't extend IBlockRenderProperties in your block, use an anonymous class instead.");
                this.renderProperties = properties;
            });
        }
    }

    // Neo: Allowing mods to define client behavior for their Blocks
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions> consumer) {
    }

    /** @deprecated */
    @Deprecated
    public Holder.Reference<Block> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    protected void tryDropExperience(ServerLevel pLevel, BlockPos pPos, ItemStack pHeldItem, IntProvider pAmount) {
        int i = EnchantmentHelper.processBlockExperience(pLevel, pHeldItem, pAmount.sample(pLevel.getRandom()));
        if (i > 0) {
            this.popExperience(pLevel, pPos, i);
        }
    }

    public static final class BlockStatePairKey {
        private final BlockState first;
        private final BlockState second;
        private final Direction direction;

        public BlockStatePairKey(BlockState pFirst, BlockState pSecond, Direction pDirection) {
            this.first = pFirst;
            this.second = pSecond;
            this.direction = pDirection;
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else {
                return !(pOther instanceof Block.BlockStatePairKey block$blockstatepairkey)
                    ? false
                    : this.first == block$blockstatepairkey.first
                        && this.second == block$blockstatepairkey.second
                        && this.direction == block$blockstatepairkey.direction;
            }
        }

        @Override
        public int hashCode() {
            int i = this.first.hashCode();
            i = 31 * i + this.second.hashCode();
            return 31 * i + this.direction.hashCode();
        }
    }
}
