package net.minecraft.server.level.progress;

import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public interface ChunkProgressListener {
    void updateSpawnPos(ChunkPos pCenter);

    void onStatusChange(ChunkPos pChunkPos, @Nullable ChunkStatus pChunkStatus);

    void start();

    void stop();

    static int calculateDiameter(int pRadius) {
        return 2 * pRadius + 1;
    }
}
