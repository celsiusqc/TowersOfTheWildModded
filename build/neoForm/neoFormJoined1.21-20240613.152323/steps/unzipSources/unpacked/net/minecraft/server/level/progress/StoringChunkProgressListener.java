package net.minecraft.server.level.progress;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class StoringChunkProgressListener implements ChunkProgressListener {
    private final LoggerChunkProgressListener delegate;
    private final Long2ObjectOpenHashMap<ChunkStatus> statuses = new Long2ObjectOpenHashMap<>();
    private ChunkPos spawnPos = new ChunkPos(0, 0);
    private final int fullDiameter;
    private final int radius;
    private final int diameter;
    private boolean started;

    private StoringChunkProgressListener(LoggerChunkProgressListener pDelegate, int pFullDiameter, int pRadius, int pDiameter) {
        this.delegate = pDelegate;
        this.fullDiameter = pFullDiameter;
        this.radius = pRadius;
        this.diameter = pDiameter;
    }

    public static StoringChunkProgressListener createFromGameruleRadius(int pRadius) {
        return pRadius > 0 ? create(pRadius + 1) : createCompleted();
    }

    public static StoringChunkProgressListener create(int pRadius) {
        LoggerChunkProgressListener loggerchunkprogresslistener = LoggerChunkProgressListener.create(pRadius);
        int i = ChunkProgressListener.calculateDiameter(pRadius);
        int j = pRadius + ChunkLevel.RADIUS_AROUND_FULL_CHUNK;
        int k = ChunkProgressListener.calculateDiameter(j);
        return new StoringChunkProgressListener(loggerchunkprogresslistener, i, j, k);
    }

    public static StoringChunkProgressListener createCompleted() {
        return new StoringChunkProgressListener(LoggerChunkProgressListener.createCompleted(), 0, 0, 0);
    }

    @Override
    public void updateSpawnPos(ChunkPos pCenter) {
        if (this.started) {
            this.delegate.updateSpawnPos(pCenter);
            this.spawnPos = pCenter;
        }
    }

    @Override
    public void onStatusChange(ChunkPos pChunkPos, @Nullable ChunkStatus pChunkStatus) {
        if (this.started) {
            this.delegate.onStatusChange(pChunkPos, pChunkStatus);
            if (pChunkStatus == null) {
                this.statuses.remove(pChunkPos.toLong());
            } else {
                this.statuses.put(pChunkPos.toLong(), pChunkStatus);
            }
        }
    }

    @Override
    public void start() {
        this.started = true;
        this.statuses.clear();
        this.delegate.start();
    }

    @Override
    public void stop() {
        this.started = false;
        this.delegate.stop();
    }

    public int getFullDiameter() {
        return this.fullDiameter;
    }

    public int getDiameter() {
        return this.diameter;
    }

    public int getProgress() {
        return this.delegate.getProgress();
    }

    @Nullable
    public ChunkStatus getStatus(int pX, int pZ) {
        return this.statuses.get(ChunkPos.asLong(pX + this.spawnPos.x - this.radius, pZ + this.spawnPos.z - this.radius));
    }
}
