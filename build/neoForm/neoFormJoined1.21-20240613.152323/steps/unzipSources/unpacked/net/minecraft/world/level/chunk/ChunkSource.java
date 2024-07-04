package net.minecraft.world.level.chunk;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

public abstract class ChunkSource implements LightChunkGetter, AutoCloseable {
    @Nullable
    public LevelChunk getChunk(int pChunkX, int pChunkZ, boolean pLoad) {
        return (LevelChunk)this.getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, pLoad);
    }

    @Nullable
    public LevelChunk getChunkNow(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, false);
    }

    @Nullable
    @Override
    public LightChunk getChunkForLighting(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, ChunkStatus.EMPTY, false);
    }

    /**
     * @return {@code true} if a chunk is loaded at the provided position, without forcing a chunk load.
     */
    public boolean hasChunk(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, false) != null;
    }

    @Nullable
    public abstract ChunkAccess getChunk(int pX, int pZ, ChunkStatus pChunkStatus, boolean pRequireChunk);

    public abstract void tick(BooleanSupplier pHasTimeLeft, boolean pTickChunks);

    public abstract String gatherStats();

    public abstract int getLoadedChunksCount();

    @Override
    public void close() throws IOException {
    }

    public abstract LevelLightEngine getLightEngine();

    public void setSpawnSettings(boolean pHostile, boolean pPeaceful) {
    }

    public void updateChunkForced(ChunkPos pPos, boolean pAdd) {
    }
}
