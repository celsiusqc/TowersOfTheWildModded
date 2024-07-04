package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.io.FileUtils;

public class RecreatingSimpleRegionStorage extends SimpleRegionStorage {
    private final IOWorker writeWorker;
    private final Path writeFolder;

    public RecreatingSimpleRegionStorage(
        RegionStorageInfo pInfo,
        Path pFolder,
        RegionStorageInfo pWriteInfo,
        Path pWriteFolder,
        DataFixer pFixerUpper,
        boolean pSync,
        DataFixTypes pDataFixType
    ) {
        super(pInfo, pFolder, pFixerUpper, pSync, pDataFixType);
        this.writeFolder = pWriteFolder;
        this.writeWorker = new IOWorker(pWriteInfo, pWriteFolder, pSync);
    }

    @Override
    public CompletableFuture<Void> write(ChunkPos pChunkPos, @Nullable CompoundTag pData) {
        return this.writeWorker.store(pChunkPos, pData);
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.writeWorker.close();
        if (this.writeFolder.toFile().exists()) {
            FileUtils.deleteDirectory(this.writeFolder.toFile());
        }
    }
}
