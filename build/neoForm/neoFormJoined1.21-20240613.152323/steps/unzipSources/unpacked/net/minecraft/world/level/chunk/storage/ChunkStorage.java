package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class ChunkStorage implements AutoCloseable {
    public static final int LAST_MONOLYTH_STRUCTURE_DATA_VERSION = 1493;
    private final IOWorker worker;
    protected final DataFixer fixerUpper;
    @Nullable
    private volatile LegacyStructureDataHandler legacyStructureHandler;

    public ChunkStorage(RegionStorageInfo pInfo, Path pFolder, DataFixer pFixerUpper, boolean pSync) {
        this.fixerUpper = pFixerUpper;
        this.worker = new IOWorker(pInfo, pFolder, pSync);
    }

    public boolean isOldChunkAround(ChunkPos pPos, int pRadius) {
        return this.worker.isOldChunkAround(pPos, pRadius);
    }

    public CompoundTag upgradeChunkTag(
        ResourceKey<Level> pLevelKey,
        Supplier<DimensionDataStorage> pStorage,
        CompoundTag pChunkData,
        Optional<ResourceKey<MapCodec<? extends ChunkGenerator>>> pChunkGeneratorKey
    ) {
        int i = getVersion(pChunkData);
        if (i == SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
            return pChunkData;
        } else {
            try {
                if (i < 1493) {
                    pChunkData = DataFixTypes.CHUNK.update(this.fixerUpper, pChunkData, i, 1493);
                    if (pChunkData.getCompound("Level").getBoolean("hasLegacyStructureData")) {
                        LegacyStructureDataHandler legacystructuredatahandler = this.getLegacyStructureHandler(pLevelKey, pStorage);
                        pChunkData = legacystructuredatahandler.updateFromLegacy(pChunkData);
                    }
                }

                injectDatafixingContext(pChunkData, pLevelKey, pChunkGeneratorKey);
                pChunkData = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, pChunkData, Math.max(1493, i));
                removeDatafixingContext(pChunkData);
                NbtUtils.addCurrentDataVersion(pChunkData);
                return pChunkData;
            } catch (Exception exception) {
                CrashReport crashreport = CrashReport.forThrowable(exception, "Updated chunk");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Updated chunk details");
                crashreportcategory.setDetail("Data version", i);
                throw new ReportedException(crashreport);
            }
        }
    }

    private LegacyStructureDataHandler getLegacyStructureHandler(ResourceKey<Level> pLevel, Supplier<DimensionDataStorage> pStorage) {
        LegacyStructureDataHandler legacystructuredatahandler = this.legacyStructureHandler;
        if (legacystructuredatahandler == null) {
            synchronized (this) {
                legacystructuredatahandler = this.legacyStructureHandler;
                if (legacystructuredatahandler == null) {
                    this.legacyStructureHandler = legacystructuredatahandler = LegacyStructureDataHandler.getLegacyStructureHandler(pLevel, pStorage.get());
                }
            }
        }

        return legacystructuredatahandler;
    }

    public static void injectDatafixingContext(
        CompoundTag pChunkData, ResourceKey<Level> pLevelKey, Optional<ResourceKey<MapCodec<? extends ChunkGenerator>>> pChunkGeneratorKey
    ) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("dimension", pLevelKey.location().toString());
        pChunkGeneratorKey.ifPresent(p_196917_ -> compoundtag.putString("generator", p_196917_.location().toString()));
        pChunkData.put("__context", compoundtag);
    }

    private static void removeDatafixingContext(CompoundTag p_348632_) {
        p_348632_.remove("__context");
    }

    public static int getVersion(CompoundTag pChunkData) {
        return NbtUtils.getDataVersion(pChunkData, -1);
    }

    public CompletableFuture<Optional<CompoundTag>> read(ChunkPos pChunkPos) {
        return this.worker.loadAsync(pChunkPos);
    }

    public CompletableFuture<Void> write(ChunkPos pChunkPos, CompoundTag pData) {
        this.handleLegacyStructureIndex(pChunkPos);
        return this.worker.store(pChunkPos, pData);
    }

    protected void handleLegacyStructureIndex(ChunkPos pChunkPos) {
        if (this.legacyStructureHandler != null) {
            this.legacyStructureHandler.removeIndex(pChunkPos.toLong());
        }
    }

    public void flushWorker() {
        this.worker.synchronize(true).join();
    }

    @Override
    public void close() throws IOException {
        this.worker.close();
    }

    public ChunkScanAccess chunkScanner() {
        return this.worker;
    }

    protected RegionStorageInfo storageInfo() {
        return this.worker.storageInfo();
    }
}
