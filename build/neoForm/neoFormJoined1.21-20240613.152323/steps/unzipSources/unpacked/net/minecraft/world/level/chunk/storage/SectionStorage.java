package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.slf4j.Logger;

public class SectionStorage<R> implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECTIONS_TAG = "Sections";
    private final SimpleRegionStorage simpleRegionStorage;
    private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap<>();
    private final LongLinkedOpenHashSet dirty = new LongLinkedOpenHashSet();
    private final Function<Runnable, Codec<R>> codec;
    private final Function<Runnable, R> factory;
    private final RegistryAccess registryAccess;
    private final ChunkIOErrorReporter errorReporter;
    protected final LevelHeightAccessor levelHeightAccessor;

    public SectionStorage(
        SimpleRegionStorage p_321814_,
        Function<Runnable, Codec<R>> p_223510_,
        Function<Runnable, R> p_223511_,
        RegistryAccess p_223515_,
        ChunkIOErrorReporter p_352357_,
        LevelHeightAccessor p_223516_
    ) {
        this.simpleRegionStorage = p_321814_;
        this.codec = p_223510_;
        this.factory = p_223511_;
        this.registryAccess = p_223515_;
        this.errorReporter = p_352357_;
        this.levelHeightAccessor = p_223516_;
    }

    protected void tick(BooleanSupplier pAheadOfTime) {
        while (this.hasWork() && pAheadOfTime.getAsBoolean()) {
            ChunkPos chunkpos = SectionPos.of(this.dirty.firstLong()).chunk();
            this.writeColumn(chunkpos);
        }
    }

    public boolean hasWork() {
        return !this.dirty.isEmpty();
    }

    @Nullable
    protected Optional<R> get(long pSectionKey) {
        return this.storage.get(pSectionKey);
    }

    protected Optional<R> getOrLoad(long pSectionKey) {
        if (this.outsideStoredRange(pSectionKey)) {
            return Optional.empty();
        } else {
            Optional<R> optional = this.get(pSectionKey);
            if (optional != null) {
                return optional;
            } else {
                this.readColumn(SectionPos.of(pSectionKey).chunk());
                optional = this.get(pSectionKey);
                if (optional == null) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
                } else {
                    return optional;
                }
            }
        }
    }

    protected boolean outsideStoredRange(long pSectionKey) {
        int i = SectionPos.sectionToBlockCoord(SectionPos.y(pSectionKey));
        return this.levelHeightAccessor.isOutsideBuildHeight(i);
    }

    protected R getOrCreate(long pSectionKey) {
        if (this.outsideStoredRange(pSectionKey)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("sectionPos out of bounds"));
        } else {
            Optional<R> optional = this.getOrLoad(pSectionKey);
            if (optional.isPresent()) {
                return optional.get();
            } else {
                R r = this.factory.apply(() -> this.setDirty(pSectionKey));
                this.storage.put(pSectionKey, Optional.of(r));
                return r;
            }
        }
    }

    private void readColumn(ChunkPos pChunkPos) {
        Optional<CompoundTag> optional = this.tryRead(pChunkPos).join();
        RegistryOps<Tag> registryops = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
        this.readColumn(pChunkPos, registryops, optional.orElse(null));
    }

    private CompletableFuture<Optional<CompoundTag>> tryRead(ChunkPos pChunkPos) {
        return this.simpleRegionStorage.read(pChunkPos).exceptionally(p_351994_ -> {
            if (p_351994_ instanceof IOException ioexception) {
                LOGGER.error("Error reading chunk {} data from disk", pChunkPos, ioexception);
                this.errorReporter.reportChunkLoadFailure(ioexception, this.simpleRegionStorage.storageInfo(), pChunkPos);
                return Optional.empty();
            } else {
                throw new CompletionException(p_351994_);
            }
        });
    }

    private void readColumn(ChunkPos pChunkPos, RegistryOps<Tag> pOps, @Nullable CompoundTag pTag) {
        if (pTag == null) {
            for (int i = this.levelHeightAccessor.getMinSection(); i < this.levelHeightAccessor.getMaxSection(); i++) {
                this.storage.put(getKey(pChunkPos, i), Optional.empty());
            }
        } else {
            Dynamic<Tag> dynamic1 = new Dynamic<>(pOps, pTag);
            int j = getVersion(dynamic1);
            int k = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
            boolean flag = j != k;
            Dynamic<Tag> dynamic = this.simpleRegionStorage.upgradeChunkTag(dynamic1, j);
            OptionalDynamic<Tag> optionaldynamic = dynamic.get("Sections");

            for (int l = this.levelHeightAccessor.getMinSection(); l < this.levelHeightAccessor.getMaxSection(); l++) {
                long i1 = getKey(pChunkPos, l);
                Optional<R> optional = optionaldynamic.get(Integer.toString(l))
                    .result()
                    .flatMap(p_338087_ -> this.codec.apply(() -> this.setDirty(i1)).parse((Dynamic<Tag>)p_338087_).resultOrPartial(LOGGER::error));
                this.storage.put(i1, optional);
                optional.ifPresent(p_223523_ -> {
                    this.onSectionLoad(i1);
                    if (flag) {
                        this.setDirty(i1);
                    }
                });
            }
        }
    }

    private void writeColumn(ChunkPos pChunkPos) {
        RegistryOps<Tag> registryops = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
        Dynamic<Tag> dynamic = this.writeColumn(pChunkPos, registryops);
        Tag tag = dynamic.getValue();
        if (tag instanceof CompoundTag) {
            this.simpleRegionStorage.write(pChunkPos, (CompoundTag)tag).exceptionally(p_351992_ -> {
                this.errorReporter.reportChunkSaveFailure(p_351992_, this.simpleRegionStorage.storageInfo(), pChunkPos);
                return null;
            });
        } else {
            LOGGER.error("Expected compound tag, got {}", tag);
        }
    }

    private <T> Dynamic<T> writeColumn(ChunkPos pChunkPos, DynamicOps<T> pOps) {
        Map<T, T> map = Maps.newHashMap();

        for (int i = this.levelHeightAccessor.getMinSection(); i < this.levelHeightAccessor.getMaxSection(); i++) {
            long j = getKey(pChunkPos, i);
            this.dirty.remove(j);
            Optional<R> optional = this.storage.get(j);
            if (optional != null && !optional.isEmpty()) {
                DataResult<T> dataresult = this.codec.apply(() -> this.setDirty(j)).encodeStart(pOps, optional.get());
                String s = Integer.toString(i);
                dataresult.resultOrPartial(LOGGER::error).ifPresent(p_223531_ -> map.put(pOps.createString(s), (T)p_223531_));
            }
        }

        return new Dynamic<>(
            pOps,
            pOps.createMap(
                ImmutableMap.of(
                    pOps.createString("Sections"),
                    pOps.createMap(map),
                    pOps.createString("DataVersion"),
                    pOps.createInt(SharedConstants.getCurrentVersion().getDataVersion().getVersion())
                )
            )
        );
    }

    private static long getKey(ChunkPos pChunkPos, int pSectionY) {
        return SectionPos.asLong(pChunkPos.x, pSectionY, pChunkPos.z);
    }

    protected void onSectionLoad(long pSectionKey) {
    }

    protected void setDirty(long pSectionPos) {
        Optional<R> optional = this.storage.get(pSectionPos);
        if (optional != null && !optional.isEmpty()) {
            this.dirty.add(pSectionPos);
        } else {
            LOGGER.warn("No data for position: {}", SectionPos.of(pSectionPos));
        }
    }

    private static int getVersion(Dynamic<?> pColumnData) {
        return pColumnData.get("DataVersion").asInt(1945);
    }

    public void flush(ChunkPos pChunkPos) {
        if (this.hasWork()) {
            for (int i = this.levelHeightAccessor.getMinSection(); i < this.levelHeightAccessor.getMaxSection(); i++) {
                long j = getKey(pChunkPos, i);
                if (this.dirty.contains(j)) {
                    this.writeColumn(pChunkPos);
                    return;
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.simpleRegionStorage.close();
    }

    /**
     * Neo: Removes the data for the given chunk position.
     * See PR #937
     */
    public void remove(long sectionPosAsLong) {
        this.storage.remove(sectionPosAsLong);
    }
}
