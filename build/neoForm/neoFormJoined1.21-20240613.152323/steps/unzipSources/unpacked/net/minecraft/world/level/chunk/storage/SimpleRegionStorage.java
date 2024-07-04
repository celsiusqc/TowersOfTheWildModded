package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;

public class SimpleRegionStorage implements AutoCloseable {
    private final IOWorker worker;
    private final DataFixer fixerUpper;
    private final DataFixTypes dataFixType;

    public SimpleRegionStorage(RegionStorageInfo pInfo, Path pFolder, DataFixer pFixerUpper, boolean pSync, DataFixTypes pDataFixType) {
        this.fixerUpper = pFixerUpper;
        this.dataFixType = pDataFixType;
        this.worker = new IOWorker(pInfo, pFolder, pSync);
    }

    public CompletableFuture<Optional<CompoundTag>> read(ChunkPos pChunkPos) {
        return this.worker.loadAsync(pChunkPos);
    }

    public CompletableFuture<Void> write(ChunkPos pChunkPos, @Nullable CompoundTag pData) {
        return this.worker.store(pChunkPos, pData);
    }

    public CompoundTag upgradeChunkTag(CompoundTag pTag, int pVersion) {
        int i = NbtUtils.getDataVersion(pTag, pVersion);
        return this.dataFixType.updateToCurrentVersion(this.fixerUpper, pTag, i);
    }

    public Dynamic<Tag> upgradeChunkTag(Dynamic<Tag> pTag, int pVersion) {
        return this.dataFixType.updateToCurrentVersion(this.fixerUpper, pTag, pVersion);
    }

    public CompletableFuture<Void> synchronize(boolean pFlushStorage) {
        return this.worker.synchronize(pFlushStorage);
    }

    @Override
    public void close() throws IOException {
        this.worker.close();
    }

    public RegionStorageInfo storageInfo() {
        return this.worker.storageInfo();
    }
}
