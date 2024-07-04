package net.minecraft.server.level;

import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ChunkHolder extends GenerationChunkHolder {
    public static final ChunkResult<LevelChunk> UNLOADED_LEVEL_CHUNK = ChunkResult.error("Unloaded level chunk");
    private static final CompletableFuture<ChunkResult<LevelChunk>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_LEVEL_CHUNK);
    private final LevelHeightAccessor levelHeightAccessor;
    /**
     * A future that returns the chunk if it is a border chunk, {@link net.minecraft.world.server.ChunkHolder.ChunkLoadingFailure#UNLOADED} otherwise.
     */
    private volatile CompletableFuture<ChunkResult<LevelChunk>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    /**
     * A future that returns the chunk if it is a ticking chunk, {@link net.minecraft.world.server.ChunkHolder.ChunkLoadingFailure#UNLOADED} otherwise.
     */
    private volatile CompletableFuture<ChunkResult<LevelChunk>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    /**
     * A future that returns the chunk if it is an entity ticking chunk, {@link net.minecraft.world.server.ChunkHolder.ChunkLoadingFailure#UNLOADED} otherwise.
     */
    private volatile CompletableFuture<ChunkResult<LevelChunk>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private int oldTicketLevel;
    private int ticketLevel;
    private int queueLevel;
    private boolean hasChangedSections;
    private final ShortSet[] changedBlocksPerSection;
    private final BitSet blockChangedLightSectionFilter = new BitSet();
    private final BitSet skyChangedLightSectionFilter = new BitSet();
    private final LevelLightEngine lightEngine;
    private final ChunkHolder.LevelChangeListener onLevelChange;
    private final ChunkHolder.PlayerProvider playerProvider;
    private boolean wasAccessibleSinceLastSave;
    private CompletableFuture<?> pendingFullStateConfirmation = CompletableFuture.completedFuture(null);
    private CompletableFuture<?> sendSync = CompletableFuture.completedFuture(null);
    private CompletableFuture<?> saveSync = CompletableFuture.completedFuture(null);

    public ChunkHolder(
        ChunkPos pPos,
        int pTicketLevel,
        LevelHeightAccessor pLevelHeightAccessor,
        LevelLightEngine pLightEngine,
        ChunkHolder.LevelChangeListener pOnLevelChange,
        ChunkHolder.PlayerProvider pPlayerProvider
    ) {
        super(pPos);
        this.levelHeightAccessor = pLevelHeightAccessor;
        this.lightEngine = pLightEngine;
        this.onLevelChange = pOnLevelChange;
        this.playerProvider = pPlayerProvider;
        this.oldTicketLevel = ChunkLevel.MAX_LEVEL + 1;
        this.ticketLevel = this.oldTicketLevel;
        this.queueLevel = this.oldTicketLevel;
        this.setTicketLevel(pTicketLevel);
        this.changedBlocksPerSection = new ShortSet[pLevelHeightAccessor.getSectionsCount()];
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getTickingChunkFuture() {
        return this.tickingChunkFuture;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getEntityTickingChunkFuture() {
        return this.entityTickingChunkFuture;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture() {
        return this.fullChunkFuture;
    }

    @Nullable
    public LevelChunk getTickingChunk() {
        return this.getTickingChunkFuture().getNow(UNLOADED_LEVEL_CHUNK).orElse(null);
    }

    @Nullable
    public LevelChunk getChunkToSend() {
        return !this.sendSync.isDone() ? null : this.getTickingChunk();
    }

    public CompletableFuture<?> getSendSyncFuture() {
        return this.sendSync;
    }

    public void addSendDependency(CompletableFuture<?> pDependency) {
        if (this.sendSync.isDone()) {
            this.sendSync = pDependency;
        } else {
            this.sendSync = this.sendSync.thenCombine((CompletionStage<? extends Object>)pDependency, (p_347036_, p_347037_) -> null);
        }
    }

    public CompletableFuture<?> getSaveSyncFuture() {
        return this.saveSync;
    }

    public boolean isReadyForSaving() {
        return this.getGenerationRefCount() == 0 && this.saveSync.isDone();
    }

    private void addSaveDependency(CompletableFuture<?> p_200418_) {
        if (this.saveSync.isDone()) {
            this.saveSync = p_200418_;
        } else {
            this.saveSync = this.saveSync.thenCombine((CompletionStage<? extends Object>)p_200418_, (p_300767_, p_300768_) -> null);
        }
    }

    public void blockChanged(BlockPos pPos) {
        LevelChunk levelchunk = this.getTickingChunk();
        if (levelchunk != null) {
            int i = this.levelHeightAccessor.getSectionIndex(pPos.getY());
            if (this.changedBlocksPerSection[i] == null) {
                this.hasChangedSections = true;
                this.changedBlocksPerSection[i] = new ShortOpenHashSet();
            }

            this.changedBlocksPerSection[i].add(SectionPos.sectionRelativePos(pPos));
        }
    }

    public void sectionLightChanged(LightLayer pType, int pSectionY) {
        ChunkAccess chunkaccess = this.getChunkIfPresent(ChunkStatus.INITIALIZE_LIGHT);
        if (chunkaccess != null) {
            chunkaccess.setUnsaved(true);
            LevelChunk levelchunk = this.getTickingChunk();
            if (levelchunk != null) {
                int i = this.lightEngine.getMinLightSection();
                int j = this.lightEngine.getMaxLightSection();
                if (pSectionY >= i && pSectionY <= j) {
                    int k = pSectionY - i;
                    if (pType == LightLayer.SKY) {
                        this.skyChangedLightSectionFilter.set(k);
                    } else {
                        this.blockChangedLightSectionFilter.set(k);
                    }
                }
            }
        }
    }

    public void broadcastChanges(LevelChunk pChunk) {
        if (this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            Level level = pChunk.getLevel();
            if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
                List<ServerPlayer> list = this.playerProvider.getPlayers(this.pos, true);
                if (!list.isEmpty()) {
                    ClientboundLightUpdatePacket clientboundlightupdatepacket = new ClientboundLightUpdatePacket(
                        pChunk.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter
                    );
                    this.broadcast(list, clientboundlightupdatepacket);
                }

                this.skyChangedLightSectionFilter.clear();
                this.blockChangedLightSectionFilter.clear();
            }

            if (this.hasChangedSections) {
                List<ServerPlayer> list1 = this.playerProvider.getPlayers(this.pos, false);

                for (int j = 0; j < this.changedBlocksPerSection.length; j++) {
                    ShortSet shortset = this.changedBlocksPerSection[j];
                    if (shortset != null) {
                        this.changedBlocksPerSection[j] = null;
                        if (!list1.isEmpty()) {
                            int i = this.levelHeightAccessor.getSectionYFromSectionIndex(j);
                            SectionPos sectionpos = SectionPos.of(pChunk.getPos(), i);
                            if (shortset.size() == 1) {
                                BlockPos blockpos = sectionpos.relativeToBlockPos(shortset.iterator().nextShort());
                                BlockState blockstate = level.getBlockState(blockpos);
                                this.broadcast(list1, new ClientboundBlockUpdatePacket(blockpos, blockstate));
                                this.broadcastBlockEntityIfNeeded(list1, level, blockpos, blockstate);
                            } else {
                                LevelChunkSection levelchunksection = pChunk.getSection(j);
                                ClientboundSectionBlocksUpdatePacket clientboundsectionblocksupdatepacket = new ClientboundSectionBlocksUpdatePacket(
                                    sectionpos, shortset, levelchunksection
                                );
                                this.broadcast(list1, clientboundsectionblocksupdatepacket);
                                clientboundsectionblocksupdatepacket.runUpdates(
                                    (p_288761_, p_288762_) -> this.broadcastBlockEntityIfNeeded(list1, level, p_288761_, p_288762_)
                                );
                            }
                        }
                    }
                }

                this.hasChangedSections = false;
            }
        }
    }

    private void broadcastBlockEntityIfNeeded(List<ServerPlayer> pPlayers, Level pLevel, BlockPos pPos, BlockState pState) {
        if (pState.hasBlockEntity()) {
            this.broadcastBlockEntity(pPlayers, pLevel, pPos);
        }
    }

    private void broadcastBlockEntity(List<ServerPlayer> pPlayers, Level pLevel, BlockPos pPos) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity != null) {
            Packet<?> packet = blockentity.getUpdatePacket();
            if (packet != null) {
                this.broadcast(pPlayers, packet);
            }
        }
    }

    private void broadcast(List<ServerPlayer> pPlayers, Packet<?> pPacket) {
        pPlayers.forEach(p_293798_ -> p_293798_.connection.send(pPacket));
    }

    @Override
    public int getTicketLevel() {
        return this.ticketLevel;
    }

    @Override
    public int getQueueLevel() {
        return this.queueLevel;
    }

    private void setQueueLevel(int p_140087_) {
        this.queueLevel = p_140087_;
    }

    public void setTicketLevel(int pLevel) {
        this.ticketLevel = pLevel;
    }

    private void scheduleFullChunkPromotion(
        ChunkMap pChunkMap, CompletableFuture<ChunkResult<LevelChunk>> pFuture, Executor pExecutor, FullChunkStatus pFullChunkStatus
    ) {
        this.pendingFullStateConfirmation.cancel(false);
        CompletableFuture<Void> completablefuture = new CompletableFuture<>();
        completablefuture.thenRunAsync(() -> pChunkMap.onFullChunkStatusChange(this.pos, pFullChunkStatus), pExecutor);
        this.pendingFullStateConfirmation = completablefuture;
        pFuture.thenAccept(p_329909_ -> p_329909_.ifSuccess(p_200424_ -> completablefuture.complete(null)));
    }

    private void demoteFullChunk(ChunkMap pChunkMap, FullChunkStatus pFullChunkStatus) {
        this.pendingFullStateConfirmation.cancel(false);
        pChunkMap.onFullChunkStatusChange(this.pos, pFullChunkStatus);
    }

    protected void updateFutures(ChunkMap pChunkMap, Executor pExecutor) {
        FullChunkStatus fullchunkstatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus fullchunkstatus1 = ChunkLevel.fullStatus(this.ticketLevel);
        boolean flag = fullchunkstatus.isOrAfter(FullChunkStatus.FULL);
        boolean flag1 = fullchunkstatus1.isOrAfter(FullChunkStatus.FULL);
        this.wasAccessibleSinceLastSave |= flag1;
        if (!flag && flag1) {
            this.fullChunkFuture = pChunkMap.prepareAccessibleChunk(this);
            this.scheduleFullChunkPromotion(pChunkMap, this.fullChunkFuture, pExecutor, FullChunkStatus.FULL);
            this.addSaveDependency(this.fullChunkFuture);
        }

        if (flag && !flag1) {
            this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean flag2 = fullchunkstatus.isOrAfter(FullChunkStatus.BLOCK_TICKING);
        boolean flag3 = fullchunkstatus1.isOrAfter(FullChunkStatus.BLOCK_TICKING);
        if (!flag2 && flag3) {
            this.tickingChunkFuture = pChunkMap.prepareTickingChunk(this);
            this.scheduleFullChunkPromotion(pChunkMap, this.tickingChunkFuture, pExecutor, FullChunkStatus.BLOCK_TICKING);
            this.addSaveDependency(this.tickingChunkFuture);
        }

        if (flag2 && !flag3) {
            this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean flag4 = fullchunkstatus.isOrAfter(FullChunkStatus.ENTITY_TICKING);
        boolean flag5 = fullchunkstatus1.isOrAfter(FullChunkStatus.ENTITY_TICKING);
        if (!flag4 && flag5) {
            if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
                throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
            }

            this.entityTickingChunkFuture = pChunkMap.prepareEntityTickingChunk(this);
            this.scheduleFullChunkPromotion(pChunkMap, this.entityTickingChunkFuture, pExecutor, FullChunkStatus.ENTITY_TICKING);
            this.addSaveDependency(this.entityTickingChunkFuture);
        }

        if (flag4 && !flag5) {
            this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        if (!fullchunkstatus1.isOrAfter(fullchunkstatus)) {
            this.demoteFullChunk(pChunkMap, fullchunkstatus1);
        }

        this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
        this.oldTicketLevel = this.ticketLevel;
    }

    public boolean wasAccessibleSinceLastSave() {
        return this.wasAccessibleSinceLastSave;
    }

    public void refreshAccessibility() {
        this.wasAccessibleSinceLastSave = ChunkLevel.fullStatus(this.ticketLevel).isOrAfter(FullChunkStatus.FULL);
    }

    @FunctionalInterface
    public interface LevelChangeListener {
        void onLevelChange(ChunkPos pChunkPos, IntSupplier p_140120_, int p_140121_, IntConsumer p_140122_);
    }

    public interface PlayerProvider {
        /**
         * Returns the players tracking the given chunk.
         */
        List<ServerPlayer> getPlayers(ChunkPos pPos, boolean pBoundaryOnly);
    }
}
