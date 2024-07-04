package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;

public abstract class Level extends net.neoforged.neoforge.attachment.AttachmentHolder implements LevelAccessor, AutoCloseable, net.neoforged.neoforge.common.extensions.ILevelExtension {
    public static final Codec<ResourceKey<Level>> RESOURCE_KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);
    public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    public static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));
    public static final ResourceKey<Level> END = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_end"));
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final int LONG_PARTICLE_CLIP_RANGE = 512;
    public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
    public static final int MAX_BRIGHTNESS = 15;
    public static final int TICKS_PER_DAY = 24000;
    public static final int MAX_ENTITY_SPAWN_Y = 20000000;
    public static final int MIN_ENTITY_SPAWN_Y = -20000000;
    protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
    protected final NeighborUpdater neighborUpdater;
    private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
    private boolean tickingBlockEntities;
    private final Thread thread;
    private final boolean isDebug;
    private int skyDarken;
    /**
     * Contains the current Linear Congruential Generator seed for block updates. Used with an A value of 3 and a C value of 0x3c6ef35f, producing a highly planar series of values ill-suited for choosing random blocks in a 16x128x16 field.
     */
    protected int randValue = RandomSource.create().nextInt();
    protected final int addend = 1013904223;
    public float oRainLevel;
    public float rainLevel;
    public float oThunderLevel;
    public float thunderLevel;
    public final RandomSource random = RandomSource.create();
    @Deprecated
    private final RandomSource threadSafeRandom = RandomSource.createThreadSafe();
    private final Holder<DimensionType> dimensionTypeRegistration;
    protected final WritableLevelData levelData;
    private final Supplier<ProfilerFiller> profiler;
    public final boolean isClientSide;
    private final WorldBorder worldBorder;
    private final BiomeManager biomeManager;
    private final ResourceKey<Level> dimension;
    private final RegistryAccess registryAccess;
    private final DamageSources damageSources;
    private long subTickCount;
    public boolean restoringBlockSnapshots = false;
    public boolean captureBlockSnapshots = false;
    public java.util.ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot> capturedBlockSnapshots = new java.util.ArrayList<>();
    private final java.util.ArrayList<BlockEntity> freshBlockEntities = new java.util.ArrayList<>();
    private final java.util.ArrayList<BlockEntity> pendingFreshBlockEntities = new java.util.ArrayList<>();

    protected Level(
        WritableLevelData pLevelData,
        ResourceKey<Level> pDimension,
        RegistryAccess pRegistryAccess,
        Holder<DimensionType> pDimensionTypeRegistration,
        Supplier<ProfilerFiller> pProfiler,
        boolean pIsClientSide,
        boolean pIsDebug,
        long pBiomeZoomSeed,
        int pMaxChainedNeighborUpdates
    ) {
        this.profiler = pProfiler;
        this.levelData = pLevelData;
        this.dimensionTypeRegistration = pDimensionTypeRegistration;
        final DimensionType dimensiontype = pDimensionTypeRegistration.value();
        this.dimension = pDimension;
        this.isClientSide = pIsClientSide;
        if (dimensiontype.coordinateScale() != 1.0) {
            this.worldBorder = new WorldBorder() {
                @Override
                public double getCenterX() {
                    return super.getCenterX() / dimensiontype.coordinateScale();
                }

                @Override
                public double getCenterZ() {
                    return super.getCenterZ() / dimensiontype.coordinateScale();
                }
            };
        } else {
            this.worldBorder = new WorldBorder();
        }

        this.thread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, pBiomeZoomSeed);
        this.isDebug = pIsDebug;
        this.neighborUpdater = new CollectingNeighborUpdater(this, pMaxChainedNeighborUpdates);
        this.registryAccess = pRegistryAccess;
        this.damageSources = new DamageSources(pRegistryAccess);
    }

    @Override
    public boolean isClientSide() {
        return this.isClientSide;
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return null;
    }

    /**
     * Check if the given BlockPos has valid coordinates
     */
    public boolean isInWorldBounds(BlockPos pPos) {
        return !this.isOutsideBuildHeight(pPos) && isInWorldBoundsHorizontal(pPos);
    }

    public static boolean isInSpawnableBounds(BlockPos pPos) {
        return !isOutsideSpawnableHeight(pPos.getY()) && isInWorldBoundsHorizontal(pPos);
    }

    private static boolean isInWorldBoundsHorizontal(BlockPos pPos) {
        return pPos.getX() >= -30000000 && pPos.getZ() >= -30000000 && pPos.getX() < 30000000 && pPos.getZ() < 30000000;
    }

    private static boolean isOutsideSpawnableHeight(int pY) {
        return pY < -20000000 || pY >= 20000000;
    }

    public LevelChunk getChunkAt(BlockPos pPos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
    }

    public LevelChunk getChunk(int pChunkX, int pChunkZ) {
        return (LevelChunk)this.getChunk(pChunkX, pChunkZ, ChunkStatus.FULL);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int pX, int pZ, ChunkStatus pChunkStatus, boolean pRequireChunk) {
        ChunkAccess chunkaccess = this.getChunkSource().getChunk(pX, pZ, pChunkStatus, pRequireChunk);
        if (chunkaccess == null && pRequireChunk) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        } else {
            return chunkaccess;
        }
    }

    /**
     * Sets a block state into this world.Flags are as follows:
     * 1 will notify neighboring blocks through {@link net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase#neighborChanged neighborChanged} updates.
     * 2 will send the change to clients.
     * 4 will prevent the block from being re-rendered.
     * 8 will force any re-renders to run on the main thread instead
     * 16 will prevent neighbor reactions (e.g. fences connecting, observers pulsing).
     * 32 will prevent neighbor reactions from spawning drops.
     * 64 will signify the block is being moved.
     * Flags can be OR-ed
     */
    @Override
    public boolean setBlock(BlockPos pPos, BlockState pNewState, int pFlags) {
        return this.setBlock(pPos, pNewState, pFlags, 512);
    }

    @Override
    public boolean setBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft) {
        if (this.isOutsideBuildHeight(pPos)) {
            return false;
        } else if (!this.isClientSide && this.isDebug()) {
            return false;
        } else {
            LevelChunk levelchunk = this.getChunkAt(pPos);
            Block block = pState.getBlock();

            pPos = pPos.immutable(); // Forge - prevent mutable BlockPos leaks
            net.neoforged.neoforge.common.util.BlockSnapshot blockSnapshot = null;
            if (this.captureBlockSnapshots && !this.isClientSide) {
                 blockSnapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(this.dimension, this, pPos, pFlags);
                 this.capturedBlockSnapshots.add(blockSnapshot);
            }

            BlockState old = getBlockState(pPos);
            int oldLight = old.getLightEmission(this, pPos);
            int oldOpacity = old.getLightBlock(this, pPos);

            BlockState blockstate = levelchunk.setBlockState(pPos, pState, (pFlags & 64) != 0);
            if (blockstate == null) {
                if (blockSnapshot != null) this.capturedBlockSnapshots.remove(blockSnapshot);
                return false;
            } else {
                BlockState blockstate1 = this.getBlockState(pPos);

                if (blockSnapshot == null) { // Don't notify clients or update physics while capturing blockstates
                    this.markAndNotifyBlock(pPos, levelchunk, blockstate, pState, pFlags, pRecursionLeft);
                }

                return true;
            }
        }
    }

    // Split off from original setBlockState(BlockPos, BlockState, int, int) method in order to directly send client and physic updates
    public void markAndNotifyBlock(BlockPos p_46605_, @Nullable LevelChunk levelchunk, BlockState blockstate, BlockState p_46606_, int p_46607_, int p_46608_) {
        Block block = p_46606_.getBlock();
        BlockState blockstate1 = getBlockState(p_46605_);
        {
            {
                if (blockstate1 == p_46606_) {
                    if (blockstate != blockstate1) {
                        this.setBlocksDirty(p_46605_, blockstate, blockstate1);
                    }

                    if ((p_46607_ & 2) != 0
                        && (!this.isClientSide || (p_46607_ & 4) == 0)
                        && (this.isClientSide || levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
                        this.sendBlockUpdated(p_46605_, blockstate, p_46606_, p_46607_);
                    }

                    if ((p_46607_ & 1) != 0) {
                        this.blockUpdated(p_46605_, blockstate.getBlock());
                        if (!this.isClientSide && p_46606_.hasAnalogOutputSignal()) {
                            this.updateNeighbourForOutputSignal(p_46605_, block);
                        }
                    }

                    if ((p_46607_ & 16) == 0 && p_46608_ > 0) {
                        int i = p_46607_ & -34;
                        blockstate.updateIndirectNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                        p_46606_.updateNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                        p_46606_.updateIndirectNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                    }

                    this.onBlockStateChange(p_46605_, blockstate, blockstate1);
                    p_46606_.onBlockStateChange(this, p_46605_, blockstate);
                }
            }
        }
    }

    public void onBlockStateChange(BlockPos pPos, BlockState pBlockState, BlockState pNewState) {
    }

    @Override
    public boolean removeBlock(BlockPos pPos, boolean pIsMoving) {
        FluidState fluidstate = this.getFluidState(pPos);
        return this.setBlock(pPos, fluidstate.createLegacyBlock(), 3 | (pIsMoving ? 64 : 0));
    }

    @Override
    public boolean destroyBlock(BlockPos pPos, boolean pDropBlock, @Nullable Entity pEntity, int pRecursionLeft) {
        BlockState blockstate = this.getBlockState(pPos);
        if (blockstate.isAir()) {
            return false;
        } else {
            FluidState fluidstate = this.getFluidState(pPos);
            if (!(blockstate.getBlock() instanceof BaseFireBlock)) {
                this.levelEvent(2001, pPos, Block.getId(blockstate));
            }

            if (pDropBlock) {
                BlockEntity blockentity = blockstate.hasBlockEntity() ? this.getBlockEntity(pPos) : null;
                Block.dropResources(blockstate, this, pPos, blockentity, pEntity, ItemStack.EMPTY);
            }

            boolean flag = this.setBlock(pPos, fluidstate.createLegacyBlock(), 3, pRecursionLeft);
            if (flag) {
                this.gameEvent(GameEvent.BLOCK_DESTROY, pPos, GameEvent.Context.of(pEntity, blockstate));
            }

            return flag;
        }
    }

    public void addDestroyBlockEffect(BlockPos pPos, BlockState pState) {
    }

    /**
     * Convenience method to update the block on both the client and server
     */
    public boolean setBlockAndUpdate(BlockPos pPos, BlockState pState) {
        return this.setBlock(pPos, pState, 3);
    }

    /**
     * Flags are as in setBlockState
     */
    public abstract void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags);

    public void setBlocksDirty(BlockPos pBlockPos, BlockState pOldState, BlockState pNewState) {
    }

    public void updateNeighborsAt(BlockPos pPos, Block pBlock) {
        net.neoforged.neoforge.event.EventHooks.onNeighborNotify(this, pPos, this.getBlockState(pPos), java.util.EnumSet.allOf(Direction.class), false).isCanceled();
    }

    public void updateNeighborsAtExceptFromFacing(BlockPos pPos, Block pBlockType, Direction pSkipSide) {
    }

    public void neighborChanged(BlockPos pPos, Block pBlock, BlockPos pFromPos) {
    }

    public void neighborChanged(BlockState pState, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
    }

    /**
     * @param pQueried   The block state of the current block
     * @param pPos       The position of the neighbor block
     * @param pOffsetPos The position of the current block
     */
    @Override
    public void neighborShapeChanged(Direction pDirection, BlockState pQueried, BlockPos pPos, BlockPos pOffsetPos, int pFlags, int pRecursionLevel) {
        this.neighborUpdater.shapeUpdate(pDirection, pQueried, pPos, pOffsetPos, pFlags, pRecursionLevel);
    }

    @Override
    public int getHeight(Heightmap.Types pHeightmapType, int pX, int pZ) {
        int i;
        if (pX >= -30000000 && pZ >= -30000000 && pX < 30000000 && pZ < 30000000) {
            if (this.hasChunk(SectionPos.blockToSectionCoord(pX), SectionPos.blockToSectionCoord(pZ))) {
                i = this.getChunk(SectionPos.blockToSectionCoord(pX), SectionPos.blockToSectionCoord(pZ))
                        .getHeight(pHeightmapType, pX & 15, pZ & 15)
                    + 1;
            } else {
                i = this.getMinBuildHeight();
            }
        } else {
            i = this.getSeaLevel() + 1;
        }

        return i;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.getChunkSource().getLightEngine();
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        if (this.isOutsideBuildHeight(pPos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            LevelChunk levelchunk = this.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
            return levelchunk.getBlockState(pPos);
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        if (this.isOutsideBuildHeight(pPos)) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            LevelChunk levelchunk = this.getChunkAt(pPos);
            return levelchunk.getFluidState(pPos);
        }
    }

    public boolean isDay() {
        return !this.dimensionType().hasFixedTime() && this.skyDarken < 4;
    }

    public boolean isNight() {
        return !this.dimensionType().hasFixedTime() && !this.isDay();
    }

    public void playSound(@Nullable Entity pEntity, BlockPos pPos, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch) {
        this.playSound(pEntity instanceof Player player ? player : null, pPos, pSound, pCategory, pVolume, pPitch);
    }

    /**
     * Plays a sound. On the server, the sound is broadcast to all nearby <em>except</em> the given player. On the client, the sound only plays if the given player is the client player. Thus, this method is intended to be called from code running on both sides. The client plays it locally and the server plays it for everyone else.
     */
    @Override
    public void playSound(@Nullable Player pPlayer, BlockPos pPos, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch) {
        this.playSound(
            pPlayer, (double)pPos.getX() + 0.5, (double)pPos.getY() + 0.5, (double)pPos.getZ() + 0.5, pSound, pCategory, pVolume, pPitch
        );
    }

    public abstract void playSeededSound(
        @Nullable Player pPlayer,
        double pX,
        double pY,
        double pZ,
        Holder<SoundEvent> pSound,
        SoundSource pCategory,
        float pVolume,
        float pPitch,
        long pSeed
    );

    public void playSeededSound(
        @Nullable Player pPlayer,
        double pX,
        double pY,
        double pZ,
        SoundEvent pSound,
        SoundSource pCategory,
        float pVolume,
        float pPitch,
        long pSeed
    ) {
        this.playSeededSound(
            pPlayer, pX, pY, pZ, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(pSound), pCategory, pVolume, pPitch, pSeed
        );
    }

    public abstract void playSeededSound(
        @Nullable Player pPlayer, Entity pEntity, Holder<SoundEvent> pSound, SoundSource pCategory, float pVolume, float pPitch, long pSeed
    );

    public void playSound(@Nullable Player pPlayer, double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory) {
        this.playSound(pPlayer, pX, pY, pZ, pSound, pCategory, 1.0F, 1.0F);
    }

    public void playSound(
        @Nullable Player pPlayer, double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch
    ) {
        this.playSeededSound(pPlayer, pX, pY, pZ, pSound, pCategory, pVolume, pPitch, this.threadSafeRandom.nextLong());
    }

    public void playSound(
        @Nullable Player p_347719_,
        double p_347460_,
        double p_347457_,
        double p_347558_,
        Holder<SoundEvent> p_347499_,
        SoundSource p_347522_,
        float p_347447_,
        float p_347667_
    ) {
        this.playSeededSound(p_347719_, p_347460_, p_347457_, p_347558_, p_347499_, p_347522_, p_347447_, p_347667_, this.threadSafeRandom.nextLong());
    }

    public void playSound(@Nullable Player pPlayer, Entity pEntity, SoundEvent pEvent, SoundSource pCategory, float pVolume, float pPitch) {
        this.playSeededSound(
            pPlayer, pEntity, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(pEvent), pCategory, pVolume, pPitch, this.threadSafeRandom.nextLong()
        );
    }

    public void playLocalSound(BlockPos pPos, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch, boolean pDistanceDelay) {
        this.playLocalSound(
            (double)pPos.getX() + 0.5,
            (double)pPos.getY() + 0.5,
            (double)pPos.getZ() + 0.5,
            pSound,
            pCategory,
            pVolume,
            pPitch,
            pDistanceDelay
        );
    }

    public void playLocalSound(Entity pEntity, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch) {
    }

    public void playLocalSound(
        double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch, boolean pDistanceDelay
    ) {
    }

    @Override
    public void addParticle(ParticleOptions pParticleData, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
    }

    public void addParticle(
        ParticleOptions pParticleData, boolean pForceAlwaysRender, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    ) {
    }

    public void addAlwaysVisibleParticle(
        ParticleOptions pParticleData, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    ) {
    }

    public void addAlwaysVisibleParticle(
        ParticleOptions pParticleData, boolean pIgnoreRange, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    ) {
    }

    /**
     * Return getCelestialAngle()*2*PI
     */
    public float getSunAngle(float pPartialTicks) {
        float f = this.getTimeOfDay(pPartialTicks);
        return f * (float) (Math.PI * 2);
    }

    public void addBlockEntityTicker(TickingBlockEntity pTicker) {
        (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(pTicker);
    }

    public void addFreshBlockEntities(java.util.Collection<BlockEntity> beList) {
        if (this.tickingBlockEntities) {
            this.pendingFreshBlockEntities.addAll(beList);
        } else {
            this.freshBlockEntities.addAll(beList);
        }
    }

    protected void tickBlockEntities() {
        ProfilerFiller profilerfiller = this.getProfiler();
        profilerfiller.push("blockEntities");
        if (!this.pendingFreshBlockEntities.isEmpty()) {
            this.freshBlockEntities.addAll(this.pendingFreshBlockEntities);
            this.pendingFreshBlockEntities.clear();
        }
        this.tickingBlockEntities = true;
        if (!this.freshBlockEntities.isEmpty()) {
            this.freshBlockEntities.forEach(BlockEntity::onLoad);
            this.freshBlockEntities.clear();
        }
        if (!this.pendingBlockEntityTickers.isEmpty()) {
            this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
            this.pendingBlockEntityTickers.clear();
        }

        Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();
        boolean flag = this.tickRateManager().runsNormally();

        while (iterator.hasNext()) {
            TickingBlockEntity tickingblockentity = iterator.next();
            if (tickingblockentity.isRemoved()) {
                iterator.remove();
            } else if (flag && this.shouldTickBlocksAt(tickingblockentity.getPos())) {
                tickingblockentity.tick();
            }
        }

        this.tickingBlockEntities = false;
        profilerfiller.pop();
    }

    public <T extends Entity> void guardEntityTick(Consumer<T> pConsumerEntity, T pEntity) {
        try {
            net.neoforged.neoforge.server.timings.TimeTracker.ENTITY_UPDATE.trackStart(pEntity);
            pConsumerEntity.accept(pEntity);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking entity");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being ticked");
            pEntity.fillCrashReportCategory(crashreportcategory);
            if (net.neoforged.neoforge.common.NeoForgeConfig.SERVER.removeErroringEntities.get()) {
                com.mojang.logging.LogUtils.getLogger().error("{}", crashreport.getFriendlyReport(net.minecraft.ReportType.CRASH));
                pEntity.discard();
            } else
            throw new ReportedException(crashreport);
        } finally {
            net.neoforged.neoforge.server.timings.TimeTracker.ENTITY_UPDATE.trackEnd(pEntity);
        }
    }

    public boolean shouldTickDeath(Entity pEntity) {
        return true;
    }

    public boolean shouldTickBlocksAt(long pChunkPos) {
        return true;
    }

    public boolean shouldTickBlocksAt(BlockPos pPos) {
        return this.shouldTickBlocksAt(ChunkPos.asLong(pPos));
    }

    public Explosion explode(
        @Nullable Entity pSource, double pX, double pY, double pZ, float pRadius, Level.ExplosionInteraction pExplosionInteraction
    ) {
        return this.explode(
            pSource,
            Explosion.getDefaultDamageSource(this, pSource),
            null,
            pX,
            pY,
            pZ,
            pRadius,
            false,
            pExplosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion explode(
        @Nullable Entity pSource,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        boolean pFire,
        Level.ExplosionInteraction pExplosionInteraction
    ) {
        return this.explode(
            pSource,
            Explosion.getDefaultDamageSource(this, pSource),
            null,
            pX,
            pY,
            pZ,
            pRadius,
            pFire,
            pExplosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion explode(
        @Nullable Entity pSource,
        @Nullable DamageSource pDamageSource,
        @Nullable ExplosionDamageCalculator pDamageCalculator,
        Vec3 pPos,
        float pRadius,
        boolean pFire,
        Level.ExplosionInteraction pExplosionInteraction
    ) {
        return this.explode(
            pSource,
            pDamageSource,
            pDamageCalculator,
            pPos.x(),
            pPos.y(),
            pPos.z(),
            pRadius,
            pFire,
            pExplosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion explode(
        @Nullable Entity pSource,
        @Nullable DamageSource pDamageSource,
        @Nullable ExplosionDamageCalculator pDamageCalculator,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        boolean pFire,
        Level.ExplosionInteraction pExplosionInteraction
    ) {
        return this.explode(
            pSource,
            pDamageSource,
            pDamageCalculator,
            pX,
            pY,
            pZ,
            pRadius,
            pFire,
            pExplosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion explode(
        @Nullable Entity pSource,
        @Nullable DamageSource pDamageSource,
        @Nullable ExplosionDamageCalculator pDamageCalculator,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        boolean pFire,
        Level.ExplosionInteraction pExplosionInteraction,
        ParticleOptions pSmallExplosionParticles,
        ParticleOptions pLargeExplosionParticles,
        Holder<SoundEvent> pExplosionSound
    ) {
        return this.explode(
            pSource, pDamageSource, pDamageCalculator, pX, pY, pZ, pRadius, pFire, pExplosionInteraction, true, pSmallExplosionParticles, pLargeExplosionParticles, pExplosionSound
        );
    }

    public Explosion explode(
        @Nullable Entity pSource,
        @Nullable DamageSource pDamageSource,
        @Nullable ExplosionDamageCalculator pDamageCalculator,
        double pX,
        double pY,
        double pZ,
        float pRadius,
        boolean pFire,
        Level.ExplosionInteraction pExplosionInteraction,
        boolean pSpawnParticles,
        ParticleOptions pSmallExplosionParticles,
        ParticleOptions pLargeExplosionParticles,
        Holder<SoundEvent> pExplosionSound
    ) {
        Explosion.BlockInteraction explosion$blockinteraction = switch (pExplosionInteraction) {
            case NONE -> Explosion.BlockInteraction.KEEP;
            case BLOCK -> this.getDestroyType(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY);
            case MOB -> net.neoforged.neoforge.event.EventHooks.canEntityGrief(this, pSource)
            ? this.getDestroyType(GameRules.RULE_MOB_EXPLOSION_DROP_DECAY)
            : Explosion.BlockInteraction.KEEP;
            case TNT -> this.getDestroyType(GameRules.RULE_TNT_EXPLOSION_DROP_DECAY);
            case TRIGGER -> Explosion.BlockInteraction.TRIGGER_BLOCK;
        };
        Explosion explosion = new Explosion(
            this,
            pSource,
            pDamageSource,
            pDamageCalculator,
            pX,
            pY,
            pZ,
            pRadius,
            pFire,
            explosion$blockinteraction,
            pSmallExplosionParticles,
            pLargeExplosionParticles,
            pExplosionSound
        );
        if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(this, explosion)) return explosion;
        explosion.explode();
        explosion.finalizeExplosion(pSpawnParticles);
        return explosion;
    }

    private Explosion.BlockInteraction getDestroyType(GameRules.Key<GameRules.BooleanValue> pGameRule) {
        return this.getGameRules().getBoolean(pGameRule) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
    }

    public abstract String gatherChunkSourceStats();

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos) {
        if (this.isOutsideBuildHeight(pPos)) {
            return null;
        } else {
            return !this.isClientSide && Thread.currentThread() != this.thread
                ? null
                : this.getChunkAt(pPos).getBlockEntity(pPos, LevelChunk.EntityCreationType.IMMEDIATE);
        }
    }

    public void setBlockEntity(BlockEntity pBlockEntity) {
        BlockPos blockpos = pBlockEntity.getBlockPos();
        if (!this.isOutsideBuildHeight(blockpos)) {
            this.getChunkAt(blockpos).addAndRegisterBlockEntity(pBlockEntity);
        }
    }

    public void removeBlockEntity(BlockPos pPos) {
        if (!this.isOutsideBuildHeight(pPos)) {
            this.getChunkAt(pPos).removeBlockEntity(pPos);
        }
        this.updateNeighbourForOutputSignal(pPos, getBlockState(pPos).getBlock()); //Notify neighbors of changes
    }

    public boolean isLoaded(BlockPos pPos) {
        return this.isOutsideBuildHeight(pPos)
            ? false
            : this.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
    }

    public boolean loadedAndEntityCanStandOnFace(BlockPos pPos, Entity pEntity, Direction pDirection) {
        if (this.isOutsideBuildHeight(pPos)) {
            return false;
        } else {
            ChunkAccess chunkaccess = this.getChunk(
                SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()), ChunkStatus.FULL, false
            );
            return chunkaccess == null ? false : chunkaccess.getBlockState(pPos).entityCanStandOnFace(this, pPos, pEntity, pDirection);
        }
    }

    public boolean loadedAndEntityCanStandOn(BlockPos pPos, Entity pEntity) {
        return this.loadedAndEntityCanStandOnFace(pPos, pEntity, Direction.UP);
    }

    public void updateSkyBrightness() {
        double d0 = 1.0 - (double)(this.getRainLevel(1.0F) * 5.0F) / 16.0;
        double d1 = 1.0 - (double)(this.getThunderLevel(1.0F) * 5.0F) / 16.0;
        double d2 = 0.5 + 2.0 * Mth.clamp((double)Mth.cos(this.getTimeOfDay(1.0F) * (float) (Math.PI * 2)), -0.25, 0.25);
        this.skyDarken = (int)((1.0 - d2 * d0 * d1) * 11.0);
    }

    /**
     * First boolean for hostile mobs and second for peaceful mobs
     */
    public void setSpawnSettings(boolean pHostile, boolean pPeaceful) {
        this.getChunkSource().setSpawnSettings(pHostile, pPeaceful);
    }

    public BlockPos getSharedSpawnPos() {
        BlockPos blockpos = this.levelData.getSpawnPos();
        if (!this.getWorldBorder().isWithinBounds(blockpos)) {
            blockpos = this.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(this.getWorldBorder().getCenterX(), 0.0, this.getWorldBorder().getCenterZ())
            );
        }

        return blockpos;
    }

    public float getSharedSpawnAngle() {
        return this.levelData.getSpawnAngle();
    }

    protected void prepareWeather() {
        if (this.levelData.isRaining()) {
            this.rainLevel = 1.0F;
            if (this.levelData.isThundering()) {
                this.thunderLevel = 1.0F;
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.getChunkSource().close();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, false);
    }

    /**
     * Gets all entities within the specified AABB excluding the one passed into it.
     */
    @Override
    public List<Entity> getEntities(@Nullable Entity pEntity, AABB pBoundingBox, Predicate<? super Entity> pPredicate) {
        this.getProfiler().incrementCounter("getEntities");
        List<Entity> list = Lists.newArrayList();
        this.getEntities().get(pBoundingBox, p_151522_ -> {
            if (p_151522_ != pEntity && pPredicate.test(p_151522_)) {
                list.add(p_151522_);
            }

            if (false)
            if (p_151522_ instanceof EnderDragon) {
                for (EnderDragonPart enderdragonpart : ((EnderDragon)p_151522_).getSubEntities()) {
                    if (p_151522_ != pEntity && pPredicate.test(enderdragonpart)) {
                        list.add(enderdragonpart);
                    }
                }
            }
        });
        for (net.neoforged.neoforge.entity.PartEntity<?> p : this.getPartEntities()) {
            if (p != pEntity && p.getBoundingBox().intersects(pBoundingBox) && pPredicate.test(p)) {
                list.add(p);
            }
        }
        return list;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB pBounds, Predicate<? super T> pPredicate) {
        List<T> list = Lists.newArrayList();
        this.getEntities(pEntityTypeTest, pBounds, pPredicate, list);
        return list;
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB pBounds, Predicate<? super T> pPredicate, List<? super T> pOutput) {
        this.getEntities(pEntityTypeTest, pBounds, pPredicate, pOutput, Integer.MAX_VALUE);
    }

    public <T extends Entity> void getEntities(
        EntityTypeTest<Entity, T> pEntityTypeTest, AABB pBounds, Predicate<? super T> pPredicate, List<? super T> pOutput, int pMaxResults
    ) {
        this.getProfiler().incrementCounter("getEntities");
        this.getEntities().get(pEntityTypeTest, pBounds, p_261454_ -> {
            if (pPredicate.test(p_261454_)) {
                pOutput.add(p_261454_);
                if (pOutput.size() >= pMaxResults) {
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }


            if (false)
            if (p_261454_ instanceof EnderDragon enderdragon) {
                for (EnderDragonPart enderdragonpart : enderdragon.getSubEntities()) {
                    T t = pEntityTypeTest.tryCast(enderdragonpart);
                    if (t != null && pPredicate.test(t)) {
                        pOutput.add(t);
                        if (pOutput.size() >= pMaxResults) {
                            return AbortableIterationConsumer.Continuation.ABORT;
                        }
                    }
                }
            }

            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
        for (net.neoforged.neoforge.entity.PartEntity<?> p : this.getPartEntities()) {
            T t = pEntityTypeTest.tryCast(p);
            if (t != null && t.getBoundingBox().intersects(pBounds) && pPredicate.test(t)) {
                pOutput.add(t);
                if (pOutput.size() >= pMaxResults) {
                    break;
                }
            }
        }
    }

    /**
     * Returns the Entity with the given ID, or null if it doesn't exist in this Level.
     */
    @Nullable
    public abstract Entity getEntity(int pId);

    public void blockEntityChanged(BlockPos pPos) {
        if (this.hasChunkAt(pPos)) {
            this.getChunkAt(pPos).setUnsaved(true);
        }
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    public void disconnect() {
    }

    public long getGameTime() {
        return this.levelData.getGameTime();
    }

    public long getDayTime() {
        return this.levelData.getDayTime();
    }

    public boolean mayInteract(Player pPlayer, BlockPos pPos) {
        return true;
    }

    /**
     * Sends a {@link net.minecraft.network.protocol.game.ClientboundEntityEventPacket} to all tracked players of that entity.
     */
    public void broadcastEntityEvent(Entity pEntity, byte pState) {
    }

    public void broadcastDamageEvent(Entity pEntity, DamageSource pDamageSource) {
    }

    public void blockEvent(BlockPos pPos, Block pBlock, int pEventID, int pEventParam) {
        this.getBlockState(pPos).triggerEvent(this, pPos, pEventID, pEventParam);
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    public GameRules getGameRules() {
        return this.levelData.getGameRules();
    }

    public abstract TickRateManager tickRateManager();

    public float getThunderLevel(float pDelta) {
        return Mth.lerp(pDelta, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(pDelta);
    }

    /**
     * Sets the strength of the thunder.
     */
    public void setThunderLevel(float pStrength) {
        float f = Mth.clamp(pStrength, 0.0F, 1.0F);
        this.oThunderLevel = f;
        this.thunderLevel = f;
    }

    /**
     * Returns rain strength.
     */
    public float getRainLevel(float pDelta) {
        return Mth.lerp(pDelta, this.oRainLevel, this.rainLevel);
    }

    /**
     * Sets the strength of the rain.
     */
    public void setRainLevel(float pStrength) {
        float f = Mth.clamp(pStrength, 0.0F, 1.0F);
        this.oRainLevel = f;
        this.rainLevel = f;
    }

    public boolean isThundering() {
        return this.dimensionType().hasSkyLight() && !this.dimensionType().hasCeiling() ? (double)this.getThunderLevel(1.0F) > 0.9 : false;
    }

    public boolean isRaining() {
        return (double)this.getRainLevel(1.0F) > 0.2;
    }

    /**
     * Check if precipitation is currently happening at a position
     */
    public boolean isRainingAt(BlockPos pPos) {
        if (!this.isRaining()) {
            return false;
        } else if (!this.canSeeSky(pPos)) {
            return false;
        } else if (this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pPos).getY() > pPos.getY()) {
            return false;
        } else {
            Biome biome = this.getBiome(pPos).value();
            return biome.getPrecipitationAt(pPos) == Biome.Precipitation.RAIN;
        }
    }

    @Nullable
    public abstract MapItemSavedData getMapData(MapId pMapId);

    public abstract void setMapData(MapId pMapId, MapItemSavedData pMapData);

    public abstract MapId getFreeMapId();

    public void globalLevelEvent(int pId, BlockPos pPos, int pData) {
    }

    /**
     * Adds some basic stats of the world to the given crash report.
     */
    public CrashReportCategory fillReportDetails(CrashReport pReport) {
        CrashReportCategory crashreportcategory = pReport.addCategory("Affected level", 1);
        crashreportcategory.setDetail("All players", () -> this.players().size() + " total; " + this.players());
        crashreportcategory.setDetail("Chunk stats", this.getChunkSource()::gatherStats);
        crashreportcategory.setDetail("Level dimension", () -> this.dimension().location().toString());

        try {
            this.levelData.fillCrashReportCategory(crashreportcategory, this);
        } catch (Throwable throwable) {
            crashreportcategory.setDetailError("Level Data Unobtainable", throwable);
        }

        return crashreportcategory;
    }

    public abstract void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress);

    public void createFireworks(
        double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, List<FireworkExplosion> pExplosions
    ) {
    }

    public abstract Scoreboard getScoreboard();

    public void updateNeighbourForOutputSignal(BlockPos pPos, Block pBlock) {
        for(Direction direction : Direction.values()) {
            BlockPos blockpos = pPos.relative(direction);
            if (this.hasChunkAt(blockpos)) {
                BlockState blockstate = this.getBlockState(blockpos);
                blockstate.onNeighborChange(this, blockpos, pPos);
                if (blockstate.isRedstoneConductor(this, blockpos)) {
                    blockpos = blockpos.relative(direction);
                    blockstate = this.getBlockState(blockpos);
                    if (blockstate.getWeakChanges(this, blockpos)) {
                        this.neighborChanged(blockstate, blockpos, pBlock, pPos, false);
                    }
                }
            }
        }
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pPos) {
        long i = 0L;
        float f = 0.0F;
        if (this.hasChunkAt(pPos)) {
            f = this.getMoonBrightness();
            i = this.getChunkAt(pPos).getInhabitedTime();
        }

        return new DifficultyInstance(this.getDifficulty(), this.getDayTime(), i, f);
    }

    @Override
    public int getSkyDarken() {
        return this.skyDarken;
    }

    public void setSkyFlashTime(int pTimeFlash) {
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    public void sendPacketToServer(Packet<?> pPacket) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionTypeRegistration.value();
    }

    public Holder<DimensionType> dimensionTypeRegistration() {
        return this.dimensionTypeRegistration;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pPos, Predicate<BlockState> pState) {
        return pState.test(this.getBlockState(pPos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pPos, Predicate<FluidState> pPredicate) {
        return pPredicate.test(this.getFluidState(pPos));
    }

    public abstract RecipeManager getRecipeManager();

    public BlockPos getBlockRandomPos(int pX, int pY, int pZ, int pYMask) {
        this.randValue = this.randValue * 3 + 1013904223;
        int i = this.randValue >> 2;
        return new BlockPos(pX + (i & 15), pY + (i >> 16 & pYMask), pZ + (i >> 8 & 15));
    }

    public boolean noSave() {
        return false;
    }

    public ProfilerFiller getProfiler() {
        return this.profiler.get();
    }

    public Supplier<ProfilerFiller> getProfilerSupplier() {
        return this.profiler;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    private double maxEntityRadius = 2.0D;
    @Override
    public double getMaxEntityRadius() {
        return maxEntityRadius;
    }
    @Override
    public double increaseMaxEntityRadius(double value) {
        if (value > maxEntityRadius)
            maxEntityRadius = value;
        return maxEntityRadius;
    }

    public final boolean isDebug() {
        return this.isDebug;
    }

    protected abstract LevelEntityGetter<Entity> getEntities();

    @Override
    public long nextSubTickCount() {
        return this.subTickCount++;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public DamageSources damageSources() {
        return this.damageSources;
    }

    public abstract PotionBrewing potionBrewing();

    public static enum ExplosionInteraction implements StringRepresentable {
        NONE("none"),
        BLOCK("block"),
        MOB("mob"),
        TNT("tnt"),
        TRIGGER("trigger");

        public static final Codec<Level.ExplosionInteraction> CODEC = StringRepresentable.fromEnum(Level.ExplosionInteraction::values);
        private final String id;

        private ExplosionInteraction(String p_346300_) {
            this.id = p_346300_;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }
    }
}
