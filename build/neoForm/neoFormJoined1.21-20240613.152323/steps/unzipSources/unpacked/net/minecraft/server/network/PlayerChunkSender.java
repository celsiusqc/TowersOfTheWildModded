package net.minecraft.server.network;

import com.google.common.collect.Comparators;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

public class PlayerChunkSender {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float MIN_CHUNKS_PER_TICK = 0.01F;
    public static final float MAX_CHUNKS_PER_TICK = 64.0F;
    private static final float START_CHUNKS_PER_TICK = 9.0F;
    private static final int MAX_UNACKNOWLEDGED_BATCHES = 10;
    private final LongSet pendingChunks = new LongOpenHashSet();
    private final boolean memoryConnection;
    private float desiredChunksPerTick = 9.0F;
    private float batchQuota;
    private int unacknowledgedBatches;
    private int maxUnacknowledgedBatches = 1;

    public PlayerChunkSender(boolean pMemoryConnection) {
        this.memoryConnection = pMemoryConnection;
    }

    public void markChunkPendingToSend(LevelChunk pChunk) {
        this.pendingChunks.add(pChunk.getPos().toLong());
    }

    public void dropChunk(ServerPlayer pPlayer, ChunkPos pChunkPos) {
        if (!this.pendingChunks.remove(pChunkPos.toLong()) && pPlayer.isAlive()) {
            pPlayer.connection.send(new ClientboundForgetLevelChunkPacket(pChunkPos));
        }
    }

    public void sendNextChunks(ServerPlayer pPlayer) {
        if (this.unacknowledgedBatches < this.maxUnacknowledgedBatches) {
            float f = Math.max(1.0F, this.desiredChunksPerTick);
            this.batchQuota = Math.min(this.batchQuota + this.desiredChunksPerTick, f);
            if (!(this.batchQuota < 1.0F)) {
                if (!this.pendingChunks.isEmpty()) {
                    ServerLevel serverlevel = pPlayer.serverLevel();
                    ChunkMap chunkmap = serverlevel.getChunkSource().chunkMap;
                    List<LevelChunk> list = this.collectChunksToSend(chunkmap, pPlayer.chunkPosition());
                    if (!list.isEmpty()) {
                        ServerGamePacketListenerImpl servergamepacketlistenerimpl = pPlayer.connection;
                        this.unacknowledgedBatches++;
                        servergamepacketlistenerimpl.send(ClientboundChunkBatchStartPacket.INSTANCE);

                        for (LevelChunk levelchunk : list) {
                            sendChunk(servergamepacketlistenerimpl, serverlevel, levelchunk);
                        }

                        servergamepacketlistenerimpl.send(new ClientboundChunkBatchFinishedPacket(list.size()));
                        this.batchQuota = this.batchQuota - (float)list.size();
                    }
                }
            }
        }
    }

    private static void sendChunk(ServerGamePacketListenerImpl pPacketListener, ServerLevel pLevel, LevelChunk pChunk) {
        pPacketListener.send(pChunk.getAuxLightManager(pChunk.getPos()).sendLightDataTo(
                new ClientboundLevelChunkWithLightPacket(pChunk, pLevel.getLightEngine(), null, null)
        ));
        ChunkPos chunkpos = pChunk.getPos();
        DebugPackets.sendPoiPacketsForChunk(pLevel, chunkpos);
        net.neoforged.neoforge.event.EventHooks.fireChunkSent(pPacketListener.player, pChunk, pLevel);
    }

    private List<LevelChunk> collectChunksToSend(ChunkMap pChunkMap, ChunkPos pChunkPos) {
        int i = Mth.floor(this.batchQuota);
        List<LevelChunk> list;
        if (!this.memoryConnection && this.pendingChunks.size() > i) {
            list = this.pendingChunks
                .stream()
                .collect(Comparators.least(i, Comparator.comparingInt(pChunkPos::distanceSquared)))
                .stream()
                .mapToLong(Long::longValue)
                .mapToObj(pChunkMap::getChunkToSend)
                .filter(Objects::nonNull)
                .toList();
        } else {
            list = this.pendingChunks
                .longStream()
                .mapToObj(pChunkMap::getChunkToSend)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(p_294268_ -> pChunkPos.distanceSquared(p_294268_.getPos())))
                .toList();
        }

        for (LevelChunk levelchunk : list) {
            this.pendingChunks.remove(levelchunk.getPos().toLong());
        }

        return list;
    }

    public void onChunkBatchReceivedByClient(float pDesiredBatchSize) {
        this.unacknowledgedBatches--;
        this.desiredChunksPerTick = Double.isNaN((double)pDesiredBatchSize) ? 0.01F : Mth.clamp(pDesiredBatchSize, 0.01F, 64.0F);
        if (this.unacknowledgedBatches == 0) {
            this.batchQuota = 1.0F;
        }

        this.maxUnacknowledgedBatches = 10;
    }

    public boolean isPending(long pChunkPos) {
        return this.pendingChunks.contains(pChunkPos);
    }
}
