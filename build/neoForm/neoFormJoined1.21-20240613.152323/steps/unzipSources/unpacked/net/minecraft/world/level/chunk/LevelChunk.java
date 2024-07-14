package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public class LevelChunk extends ChunkAccess implements net.neoforged.neoforge.attachment.IAttachmentHolder {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
        @Override
        public void tick() {
        }

        @Override
        public boolean isRemoved() {
            return true;
        }

        @Override
        public BlockPos getPos() {
            return BlockPos.ZERO;
        }

        @Override
        public String getType() {
            return "<null>";
        }
    };
    private final Map<BlockPos, LevelChunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
    private boolean loaded;
    final Level level;
    @Nullable
    private Supplier<FullChunkStatus> fullStatus;
    @Nullable
    private LevelChunk.PostLoadProcessor postLoad;
    private final Int2ObjectMap<GameEventListenerRegistry> gameEventListenerRegistrySections;
    private final LevelChunkTicks<Block> blockTicks;
    private final LevelChunkTicks<Fluid> fluidTicks;

    public LevelChunk(Level pLevel, ChunkPos pPos) {
        this(pLevel, pPos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, null, null, null);
    }

    public LevelChunk(
        Level pLevel,
        ChunkPos pPos,
        UpgradeData pData,
        LevelChunkTicks<Block> pBlockTicks,
        LevelChunkTicks<Fluid> pFluidTicks,
        long pInhabitedTime,
        @Nullable LevelChunkSection[] pSections,
        @Nullable LevelChunk.PostLoadProcessor pPostLoad,
        @Nullable BlendingData pBlendingData
    ) {
        super(pPos, pData, pLevel, pLevel.registryAccess().registryOrThrow(Registries.BIOME), pInhabitedTime, pSections, pBlendingData);
        this.level = pLevel;
        this.gameEventListenerRegistrySections = new Int2ObjectOpenHashMap<>();

        for (Heightmap.Types heightmap$types : Heightmap.Types.values()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(heightmap$types)) {
                this.heightmaps.put(heightmap$types, new Heightmap(this, heightmap$types));
            }
        }

        this.postLoad = pPostLoad;
        this.blockTicks = pBlockTicks;
        this.fluidTicks = pFluidTicks;
    }

    public LevelChunk(ServerLevel pLevel, ProtoChunk pChunk, @Nullable LevelChunk.PostLoadProcessor pPostLoad) {
        this(
            pLevel,
            pChunk.getPos(),
            pChunk.getUpgradeData(),
            pChunk.unpackBlockTicks(),
            pChunk.unpackFluidTicks(),
            pChunk.getInhabitedTime(),
            pChunk.getSections(),
            pPostLoad,
            pChunk.getBlendingData()
        );

        for (BlockEntity blockentity : pChunk.getBlockEntities().values()) {
            this.setBlockEntity(blockentity);
        }

        this.pendingBlockEntities.putAll(pChunk.getBlockEntityNbts());

        for (int i = 0; i < pChunk.getPostProcessing().length; i++) {
            this.postProcessing[i] = pChunk.getPostProcessing()[i];
        }

        this.setAllStarts(pChunk.getAllStarts());
        this.setAllReferences(pChunk.getAllReferences());

        net.neoforged.neoforge.attachment.AttachmentInternals.copyChunkAttachmentsOnPromotion(pLevel.registryAccess(), pChunk.getAttachmentHolder(), this.getAttachmentHolder());
        for (Entry<Heightmap.Types, Heightmap> entry : pChunk.getHeightmaps()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
                this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
            }
        }

        this.skyLightSources = pChunk.skyLightSources;
        this.setLightCorrect(pChunk.isLightCorrect());
        this.unsaved = true;
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess.TicksToSave getTicksForSerialization() {
        return new ChunkAccess.TicksToSave(this.blockTicks, this.fluidTicks);
    }

    @Override
    public GameEventListenerRegistry getListenerRegistry(int pSectionY) {
        return this.level instanceof ServerLevel serverlevel
            ? this.gameEventListenerRegistrySections
                .computeIfAbsent(pSectionY, p_281221_ -> new EuclideanGameEventListenerRegistry(serverlevel, pSectionY, this::removeGameEventListenerRegistry))
            : super.getListenerRegistry(pSectionY);
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();
        if (this.level.isDebug()) {
            BlockState blockstate = null;
            if (j == 60) {
                blockstate = Blocks.BARRIER.defaultBlockState();
            }

            if (j == 70) {
                blockstate = DebugLevelSource.getBlockStateFor(i, k);
            }

            return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
        } else {
            try {
                int l = this.getSectionIndex(j);
                if (l >= 0 && l < this.sections.length) {
                    LevelChunkSection levelchunksection = this.sections[l];
                    if (!levelchunksection.hasOnlyAir()) {
                        return levelchunksection.getBlockState(i & 15, j & 15, k & 15);
                    }
                }

                return Blocks.AIR.defaultBlockState();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting block state");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
                crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, i, j, k));
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        return this.getFluidState(pPos.getX(), pPos.getY(), pPos.getZ());
    }

    public FluidState getFluidState(int pX, int pY, int pZ) {
        try {
            int i = this.getSectionIndex(pY);
            if (i >= 0 && i < this.sections.length) {
                LevelChunkSection levelchunksection = this.sections[i];
                if (!levelchunksection.hasOnlyAir()) {
                    return levelchunksection.getFluidState(pX & 15, pY & 15, pZ & 15);
                }
            }

            return Fluids.EMPTY.defaultFluidState();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting fluid state");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, pX, pY, pZ));
            throw new ReportedException(crashreport);
        }
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pPos, BlockState pState, boolean pIsMoving) {
        int i = pPos.getY();
        LevelChunkSection levelchunksection = this.getSection(this.getSectionIndex(i));
        boolean flag = levelchunksection.hasOnlyAir();
        if (flag && pState.isAir()) {
            return null;
        } else {
            int j = pPos.getX() & 15;
            int k = i & 15;
            int l = pPos.getZ() & 15;
            BlockState blockstate = levelchunksection.setBlockState(j, k, l, pState);
            if (blockstate == pState) {
                return null;
            } else {
                Block block = pState.getBlock();
                this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(j, i, l, pState);
                this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(j, i, l, pState);
                this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(j, i, l, pState);
                this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(j, i, l, pState);
                boolean flag1 = levelchunksection.hasOnlyAir();
                if (flag != flag1) {
                    this.level.getChunkSource().getLightEngine().updateSectionStatus(pPos, flag1);
                }

                if (LightEngine.hasDifferentLightProperties(this, pPos, blockstate, pState)) {
                    ProfilerFiller profilerfiller = this.level.getProfiler();
                    profilerfiller.push("updateSkyLightSources");
                    this.skyLightSources.update(this, j, i, l);
                    profilerfiller.popPush("queueCheckLight");
                    this.level.getChunkSource().getLightEngine().checkBlock(pPos);
                    profilerfiller.pop();
                }

                boolean flag2 = blockstate.hasBlockEntity();
                if (!this.level.isClientSide) {
                    blockstate.onRemove(this.level, pPos, pState, pIsMoving);
                } else if ((!blockstate.is(block) || !pState.hasBlockEntity()) && flag2) {
                    this.removeBlockEntity(pPos);
                }

                if (!levelchunksection.getBlockState(j, k, l).is(block)) {
                    return null;
                } else {
                    if (!this.level.isClientSide && !this.level.captureBlockSnapshots) {
                        pState.onPlace(this.level, pPos, blockstate, pIsMoving);
                    }

                    if (pState.hasBlockEntity()) {
                        BlockEntity blockentity = this.getBlockEntity(pPos, LevelChunk.EntityCreationType.CHECK);
                        if (blockentity == null) {
                            blockentity = ((EntityBlock)block).newBlockEntity(pPos, pState);
                            if (blockentity != null) {
                                this.addAndRegisterBlockEntity(blockentity);
                            }
                        } else {
                            blockentity.setBlockState(pState);
                            this.updateBlockEntityTicker(blockentity);
                        }
                    }

                    this.unsaved = true;
                    return blockstate;
                }
            }
        }
    }

    @Deprecated
    @Override
    public void addEntity(Entity pEntity) {
    }

    @Nullable
    private BlockEntity createBlockEntity(BlockPos pPos) {
        BlockState blockstate = this.getBlockState(pPos);
        return !blockstate.hasBlockEntity() ? null : ((EntityBlock)blockstate.getBlock()).newBlockEntity(pPos, blockstate);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos) {
        return this.getBlockEntity(pPos, LevelChunk.EntityCreationType.CHECK);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pPos, LevelChunk.EntityCreationType pCreationType) {
        BlockEntity blockentity = this.blockEntities.get(pPos);
        if (blockentity != null && blockentity.isRemoved()) {
            blockEntities.remove(pPos);
            blockentity = null;
        }
        if (blockentity == null) {
            CompoundTag compoundtag = this.pendingBlockEntities.remove(pPos);
            if (compoundtag != null) {
                BlockEntity blockentity1 = this.promotePendingBlockEntity(pPos, compoundtag);
                if (blockentity1 != null) {
                    return blockentity1;
                }
            }
        }

        if (blockentity == null) {
            if (pCreationType == LevelChunk.EntityCreationType.IMMEDIATE) {
                blockentity = this.createBlockEntity(pPos);
                if (blockentity != null) {
                    this.addAndRegisterBlockEntity(blockentity);
                }
            }
        }

        return blockentity;
    }

    public void addAndRegisterBlockEntity(BlockEntity pBlockEntity) {
        this.setBlockEntity(pBlockEntity);
        if (this.isInLevel()) {
            if (this.level instanceof ServerLevel serverlevel) {
                this.addGameEventListener(pBlockEntity, serverlevel);
            }

            this.updateBlockEntityTicker(pBlockEntity);
            this.level.addFreshBlockEntities(java.util.List.of(pBlockEntity));
        }
    }

    private boolean isInLevel() {
        return this.loaded || this.level.isClientSide();
    }

    boolean isTicking(BlockPos pPos) {
        if (!this.level.getWorldBorder().isWithinBounds(pPos)) {
            return false;
        } else {
            return !(this.level instanceof ServerLevel serverlevel)
                ? true
                : this.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) && serverlevel.areEntitiesLoaded(ChunkPos.asLong(pPos));
        }
    }

    @Override
    public void setBlockEntity(BlockEntity pBlockEntity) {
        BlockPos blockpos = pBlockEntity.getBlockPos();
        BlockState blockstate = this.getBlockState(blockpos);
        if (!blockstate.hasBlockEntity()) {
            LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", pBlockEntity, blockpos, blockstate);
        } else {
            BlockState blockstate1 = pBlockEntity.getBlockState();
            if (blockstate != blockstate1) {
                if (!pBlockEntity.getType().isValid(blockstate)) {
                    LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", pBlockEntity, blockpos, blockstate);
                    return;
                }

                if (blockstate.getBlock() != blockstate1.getBlock()) {
                    LOGGER.warn("Block state mismatch on block entity {} in position {}, {} != {}, updating", pBlockEntity, blockpos, blockstate, blockstate1);
                }

                pBlockEntity.setBlockState(blockstate);
            }

            pBlockEntity.setLevel(this.level);
            pBlockEntity.clearRemoved();
            BlockEntity blockentity = this.blockEntities.put(blockpos.immutable(), pBlockEntity);
            if (blockentity != null && blockentity != pBlockEntity) {
                blockentity.setRemoved();
                auxLightManager.removeLightAt(blockpos);
            }
        }
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbtForSaving(BlockPos pPos, HolderLookup.Provider pRegistries) {
        BlockEntity blockentity = this.getBlockEntity(pPos);
        if (blockentity != null && !blockentity.isRemoved()) {
            try {
            CompoundTag compoundtag1 = blockentity.saveWithFullMetadata(this.level.registryAccess());
            compoundtag1.putBoolean("keepPacked", false);
            return compoundtag1;
            } catch (Exception e) {
                LOGGER.error("A BlockEntity type {} has thrown an exception trying to write state. It will not persist, Report this to the mod author", blockentity.getClass().getName(), e);
                return null;
            }
        } else {
            CompoundTag compoundtag = this.pendingBlockEntities.get(pPos);
            if (compoundtag != null) {
                compoundtag = compoundtag.copy();
                compoundtag.putBoolean("keepPacked", true);
            }

            return compoundtag;
        }
    }

    @Override
    public void removeBlockEntity(BlockPos pPos) {
        if (this.isInLevel()) {
            BlockEntity blockentity = this.blockEntities.remove(pPos);
            if (blockentity != null) {
                if (this.level instanceof ServerLevel serverlevel) {
                    this.removeGameEventListener(blockentity, serverlevel);
                }

                blockentity.setRemoved();
                auxLightManager.removeLightAt(pPos);
            }
        }

        this.removeBlockEntityTicker(pPos);
    }

    private <T extends BlockEntity> void removeGameEventListener(T pBlockEntity, ServerLevel pLevel) {
        Block block = pBlockEntity.getBlockState().getBlock();
        if (block instanceof EntityBlock) {
            GameEventListener gameeventlistener = ((EntityBlock)block).getListener(pLevel, pBlockEntity);
            if (gameeventlistener != null) {
                int i = SectionPos.blockToSectionCoord(pBlockEntity.getBlockPos().getY());
                GameEventListenerRegistry gameeventlistenerregistry = this.getListenerRegistry(i);
                gameeventlistenerregistry.unregister(gameeventlistener);
            }
        }
    }

    private void removeGameEventListenerRegistry(int p_283355_) {
        this.gameEventListenerRegistrySections.remove(p_283355_);
    }

    private void removeBlockEntityTicker(BlockPos pPos) {
        LevelChunk.RebindableTickingBlockEntityWrapper levelchunk$rebindabletickingblockentitywrapper = this.tickersInLevel.remove(pPos);
        if (levelchunk$rebindabletickingblockentitywrapper != null) {
            levelchunk$rebindabletickingblockentitywrapper.rebind(NULL_TICKER);
        }
    }

    public void runPostLoad() {
        if (this.postLoad != null) {
            this.postLoad.run(this);
            this.postLoad = null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public void replaceWithPacketData(
        FriendlyByteBuf pBuffer, CompoundTag pTag, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> pOutputTagConsumer
    ) {
        this.clearAllBlockEntities();

        for (LevelChunkSection levelchunksection : this.sections) {
            levelchunksection.read(pBuffer);
        }

        for (Heightmap.Types heightmap$types : Heightmap.Types.values()) {
            String s = heightmap$types.getSerializationKey();
            if (pTag.contains(s, 12)) {
                this.setHeightmap(heightmap$types, pTag.getLongArray(s));
            }
        }

        this.initializeLightSources();
        pOutputTagConsumer.accept((p_338077_, p_338078_, p_338079_) -> {
            BlockEntity blockentity = this.getBlockEntity(p_338077_, LevelChunk.EntityCreationType.IMMEDIATE);
            if (blockentity != null && p_338079_ != null && blockentity.getType() == p_338078_) {
                blockentity.handleUpdateTag(p_338079_, this.level.registryAccess());
            }
        });
    }

    public void replaceBiomes(FriendlyByteBuf pBuffer) {
        for (LevelChunkSection levelchunksection : this.sections) {
            levelchunksection.readBiomes(pBuffer);
        }
    }

    public void setLoaded(boolean pLoaded) {
        this.loaded = pLoaded;
    }

    public Level getLevel() {
        return this.level;
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void postProcessGeneration() {
        ChunkPos chunkpos = this.getPos();

        for (int i = 0; i < this.postProcessing.length; i++) {
            if (this.postProcessing[i] != null) {
                for (Short oshort : this.postProcessing[i]) {
                    BlockPos blockpos = ProtoChunk.unpackOffsetCoordinates(oshort, this.getSectionYFromSectionIndex(i), chunkpos);
                    BlockState blockstate = this.getBlockState(blockpos);
                    FluidState fluidstate = blockstate.getFluidState();
                    if (!fluidstate.isEmpty()) {
                        fluidstate.tick(this.level, blockpos);
                    }

                    if (!(blockstate.getBlock() instanceof LiquidBlock)) {
                        BlockState blockstate1 = Block.updateFromNeighbourShapes(blockstate, this.level, blockpos);
                        this.level.setBlock(blockpos, blockstate1, 20);
                    }
                }

                this.postProcessing[i].clear();
            }
        }

        for (BlockPos blockpos1 : ImmutableList.copyOf(this.pendingBlockEntities.keySet())) {
            this.getBlockEntity(blockpos1);
        }

        this.pendingBlockEntities.clear();
        this.upgradeData.upgrade(this);
    }

    @Nullable
    private BlockEntity promotePendingBlockEntity(BlockPos pPos, CompoundTag pTag) {
        BlockState blockstate = this.getBlockState(pPos);
        BlockEntity blockentity;
        if ("DUMMY".equals(pTag.getString("id"))) {
            if (blockstate.hasBlockEntity()) {
                blockentity = ((EntityBlock)blockstate.getBlock()).newBlockEntity(pPos, blockstate);
            } else {
                blockentity = null;
                LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pPos, blockstate);
            }
        } else {
            blockentity = BlockEntity.loadStatic(pPos, blockstate, pTag, this.level.registryAccess());
        }

        if (blockentity != null) {
            blockentity.setLevel(this.level);
            this.addAndRegisterBlockEntity(blockentity);
        } else {
            LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockstate, pPos);
        }

        return blockentity;
    }

    public void unpackTicks(long pPos) {
        this.blockTicks.unpack(pPos);
        this.fluidTicks.unpack(pPos);
    }

    public void registerTickContainerInLevel(ServerLevel pLevel) {
        pLevel.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
        pLevel.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
    }

    public void unregisterTickContainerFromLevel(ServerLevel pLevel) {
        pLevel.getBlockTicks().removeContainer(this.chunkPos);
        pLevel.getFluidTicks().removeContainer(this.chunkPos);
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return ChunkStatus.FULL;
    }

    public FullChunkStatus getFullStatus() {
        return this.fullStatus == null ? FullChunkStatus.FULL : this.fullStatus.get();
    }

    public void setFullStatus(Supplier<FullChunkStatus> pFullStatus) {
        this.fullStatus = pFullStatus;
    }

    public void clearAllBlockEntities() {
        this.blockEntities.values().forEach(BlockEntity::onChunkUnloaded);
        this.blockEntities.values().forEach(BlockEntity::setRemoved);
        this.blockEntities.clear();
        this.tickersInLevel.values().forEach(p_187966_ -> p_187966_.rebind(NULL_TICKER));
        this.tickersInLevel.clear();
    }

    public void registerAllBlockEntitiesAfterLevelLoad() {
        this.level.addFreshBlockEntities(this.blockEntities.values());
        this.blockEntities.values().forEach(p_187988_ -> {
            if (this.level instanceof ServerLevel serverlevel) {
                this.addGameEventListener(p_187988_, serverlevel);
            }

            this.updateBlockEntityTicker(p_187988_);
        });
    }

    private <T extends BlockEntity> void addGameEventListener(T pBlockEntity, ServerLevel pLevel) {
        Block block = pBlockEntity.getBlockState().getBlock();
        if (block instanceof EntityBlock) {
            GameEventListener gameeventlistener = ((EntityBlock)block).getListener(pLevel, pBlockEntity);
            if (gameeventlistener != null) {
                this.getListenerRegistry(SectionPos.blockToSectionCoord(pBlockEntity.getBlockPos().getY())).register(gameeventlistener);
            }
        }
    }

    private <T extends BlockEntity> void updateBlockEntityTicker(T pBlockEntity) {
        BlockState blockstate = pBlockEntity.getBlockState();
        BlockEntityTicker<T> blockentityticker = blockstate.getTicker(this.level, (BlockEntityType<T>)pBlockEntity.getType());
        if (blockentityticker == null) {
            this.removeBlockEntityTicker(pBlockEntity.getBlockPos());
        } else {
            this.tickersInLevel
                .compute(
                    pBlockEntity.getBlockPos(),
                    (p_187963_, p_187964_) -> {
                        TickingBlockEntity tickingblockentity = this.createTicker(pBlockEntity, blockentityticker);
                        if (p_187964_ != null) {
                            p_187964_.rebind(tickingblockentity);
                            return (LevelChunk.RebindableTickingBlockEntityWrapper)p_187964_;
                        } else if (this.isInLevel()) {
                            LevelChunk.RebindableTickingBlockEntityWrapper levelchunk$rebindabletickingblockentitywrapper = new LevelChunk.RebindableTickingBlockEntityWrapper(
                                tickingblockentity
                            );
                            this.level.addBlockEntityTicker(levelchunk$rebindabletickingblockentitywrapper);
                            return levelchunk$rebindabletickingblockentitywrapper;
                        } else {
                            return null;
                        }
                    }
                );
        }
    }

    private <T extends BlockEntity> TickingBlockEntity createTicker(T pBlockEntity, BlockEntityTicker<T> pTicker) {
        return new LevelChunk.BoundTickingBlockEntity<>(pBlockEntity, pTicker);
    }

    // Neo: Threadsafe lighting system for BlockEntities that change lighting based on dynamic data changing.
    private final net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager auxLightManager = new net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager(this);

    @Override
    public net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return auxLightManager;
    }

    class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
        private final T blockEntity;
        private final BlockEntityTicker<T> ticker;
        private boolean loggedInvalidBlockState;

        BoundTickingBlockEntity(T pBlockEntity, BlockEntityTicker<T> pTicker) {
            this.blockEntity = pBlockEntity;
            this.ticker = pTicker;
        }

        @Override
        public void tick() {
            if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
                BlockPos blockpos = this.blockEntity.getBlockPos();
                if (LevelChunk.this.isTicking(blockpos)) {
                    try {
                        ProfilerFiller profilerfiller = LevelChunk.this.level.getProfiler();
                        net.neoforged.neoforge.server.timings.TimeTracker.BLOCK_ENTITY_UPDATE.trackStart(blockEntity);
                        profilerfiller.push(this::getType);
                        BlockState blockstate = LevelChunk.this.getBlockState(blockpos);
                        if (this.blockEntity.getType().isValid(blockstate)) {
                            this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), blockstate, this.blockEntity);
                            this.loggedInvalidBlockState = false;
                        } else if (!this.loggedInvalidBlockState) {
                            this.loggedInvalidBlockState = true;
                            LevelChunk.LOGGER
                                .warn(
                                    "Block entity {} @ {} state {} invalid for ticking:",
                                    LogUtils.defer(this::getType),
                                    LogUtils.defer(this::getPos),
                                    blockstate
                                );
                        }

                        profilerfiller.pop();
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking block entity");
                        CrashReportCategory crashreportcategory = crashreport.addCategory("Block entity being ticked");
                        this.blockEntity.fillCrashReportCategory(crashreportcategory);

                        if (net.neoforged.neoforge.common.NeoForgeConfig.SERVER.removeErroringBlockEntities.get()) {
                            LOGGER.error("{}", crashreport.getFriendlyReport(net.minecraft.ReportType.CRASH));
                            blockEntity.setRemoved();
                            LevelChunk.this.removeBlockEntity(blockEntity.getBlockPos());
                        } else
                        throw new ReportedException(crashreport);
                    } finally {
                        net.neoforged.neoforge.server.timings.TimeTracker.BLOCK_ENTITY_UPDATE.trackEnd(blockEntity);
                    }
                }
            }
        }

        @Override
        public boolean isRemoved() {
            return this.blockEntity.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.blockEntity.getBlockPos();
        }

        @Override
        public String getType() {
            return BlockEntityType.getKey(this.blockEntity.getType()).toString();
        }

        @Override
        public String toString() {
            return "Level ticker for " + this.getType() + "@" + this.getPos();
        }
    }

    public static enum EntityCreationType {
        IMMEDIATE,
        QUEUED,
        CHECK;
    }

    @FunctionalInterface
    public interface PostLoadProcessor {
        void run(LevelChunk pChunk);
    }

    class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
        private TickingBlockEntity ticker;

        RebindableTickingBlockEntityWrapper(TickingBlockEntity pTicker) {
            this.ticker = pTicker;
        }

        void rebind(TickingBlockEntity pTicker) {
            this.ticker = pTicker;
        }

        @Override
        public void tick() {
            this.ticker.tick();
        }

        @Override
        public boolean isRemoved() {
            return this.ticker.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.ticker.getPos();
        }

        @Override
        public String getType() {
            return this.ticker.getType();
        }

        @Override
        public String toString() {
            return this.ticker + " <wrapped>";
        }
    }
}
