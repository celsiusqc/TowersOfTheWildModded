package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record ChunkIdentification(String level, String dimension, int x, int z) {
    public static ChunkIdentification from(RecordedEvent pEvent) {
        return new ChunkIdentification(
            pEvent.getString("level"), pEvent.getString("dimension"), pEvent.getInt("chunkPosX"), pEvent.getInt("chunkPosZ")
        );
    }
}
