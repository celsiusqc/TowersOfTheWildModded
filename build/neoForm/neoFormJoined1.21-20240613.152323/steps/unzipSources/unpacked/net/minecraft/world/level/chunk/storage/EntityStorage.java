package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import org.slf4j.Logger;

public class EntityStorage implements EntityPersistentStorage<Entity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENTITIES_TAG = "Entities";
    private static final String POSITION_TAG = "Position";
    private final ServerLevel level;
    private final SimpleRegionStorage simpleRegionStorage;
    private final LongSet emptyChunks = new LongOpenHashSet();
    private final ProcessorMailbox<Runnable> entityDeserializerQueue;

    public EntityStorage(SimpleRegionStorage pSimpleRegionStorage, ServerLevel pLevel, Executor pExecutor) {
        this.simpleRegionStorage = pSimpleRegionStorage;
        this.level = pLevel;
        this.entityDeserializerQueue = ProcessorMailbox.create(pExecutor, "entity-deserializer");
    }

    @Override
    public CompletableFuture<ChunkEntities<Entity>> loadEntities(ChunkPos pPos) {
        if (this.emptyChunks.contains(pPos.toLong())) {
            return CompletableFuture.completedFuture(emptyChunk(pPos));
        } else {
            CompletableFuture<Optional<CompoundTag>> completablefuture = this.simpleRegionStorage.read(pPos);
            this.reportLoadFailureIfPresent(completablefuture, pPos);
            return completablefuture.thenApplyAsync(p_351988_ -> {
                if (p_351988_.isEmpty()) {
                    this.emptyChunks.add(pPos.toLong());
                    return emptyChunk(pPos);
                } else {
                    try {
                        ChunkPos chunkpos = readChunkPos(p_351988_.get());
                        if (!Objects.equals(pPos, chunkpos)) {
                            LOGGER.error("Chunk file at {} is in the wrong location. (Expected {}, got {})", pPos, pPos, chunkpos);
                            this.level.getServer().reportMisplacedChunk(chunkpos, pPos, this.simpleRegionStorage.storageInfo());
                        }
                    } catch (Exception exception) {
                        LOGGER.warn("Failed to parse chunk {} position info", pPos, exception);
                        this.level.getServer().reportChunkLoadFailure(exception, this.simpleRegionStorage.storageInfo(), pPos);
                    }

                    CompoundTag compoundtag = this.simpleRegionStorage.upgradeChunkTag(p_351988_.get(), -1);
                    ListTag listtag = compoundtag.getList("Entities", 10);
                    List<Entity> list = EntityType.loadEntitiesRecursive(listtag, this.level).collect(ImmutableList.toImmutableList());
                    return new ChunkEntities<>(pPos, list);
                }
            }, this.entityDeserializerQueue::tell);
        }
    }

    private static ChunkPos readChunkPos(CompoundTag pTag) {
        int[] aint = pTag.getIntArray("Position");
        return new ChunkPos(aint[0], aint[1]);
    }

    private static void writeChunkPos(CompoundTag pTag, ChunkPos pPos) {
        pTag.put("Position", new IntArrayTag(new int[]{pPos.x, pPos.z}));
    }

    private static ChunkEntities<Entity> emptyChunk(ChunkPos pPos) {
        return new ChunkEntities<>(pPos, ImmutableList.of());
    }

    @Override
    public void storeEntities(ChunkEntities<Entity> pEntities) {
        ChunkPos chunkpos = pEntities.getPos();
        if (pEntities.isEmpty()) {
            if (this.emptyChunks.add(chunkpos.toLong())) {
                this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(chunkpos, null), chunkpos);
            }
        } else {
            ListTag listtag = new ListTag();
            pEntities.getEntities().forEach(p_156567_ -> {
                CompoundTag compoundtag1 = new CompoundTag();
                try {
                if (p_156567_.save(compoundtag1)) {
                    listtag.add(compoundtag1);
                }
                } catch (Exception e) {
                    LOGGER.error("An Entity type {} has thrown an exception trying to write state. It will not persist. Report this to the mod author", p_156567_.getType(), e);
                }
            });
            CompoundTag compoundtag = NbtUtils.addCurrentDataVersion(new CompoundTag());
            compoundtag.put("Entities", listtag);
            writeChunkPos(compoundtag, chunkpos);
            this.reportSaveFailureIfPresent(this.simpleRegionStorage.write(chunkpos, compoundtag), chunkpos);
            this.emptyChunks.remove(chunkpos.toLong());
        }
    }

    private void reportSaveFailureIfPresent(CompletableFuture<?> p_352240_, ChunkPos p_352077_) {
        p_352240_.exceptionally(p_351986_ -> {
            LOGGER.error("Failed to store entity chunk {}", p_352077_, p_351986_);
            this.level.getServer().reportChunkSaveFailure(p_351986_, this.simpleRegionStorage.storageInfo(), p_352077_);
            return null;
        });
    }

    private void reportLoadFailureIfPresent(CompletableFuture<?> p_352123_, ChunkPos p_352328_) {
        p_352123_.exceptionally(p_351990_ -> {
            LOGGER.error("Failed to load entity chunk {}", p_352328_, p_351990_);
            this.level.getServer().reportChunkLoadFailure(p_351990_, this.simpleRegionStorage.storageInfo(), p_352328_);
            return null;
        });
    }

    @Override
    public void flush(boolean pSynchronize) {
        this.simpleRegionStorage.synchronize(pSynchronize).join();
        this.entityDeserializerQueue.runAll();
    }

    @Override
    public void close() throws IOException {
        this.simpleRegionStorage.close();
    }
}
