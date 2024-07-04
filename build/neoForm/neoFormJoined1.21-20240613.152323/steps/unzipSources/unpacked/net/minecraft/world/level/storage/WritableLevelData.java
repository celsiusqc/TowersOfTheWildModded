package net.minecraft.world.level.storage;

import net.minecraft.core.BlockPos;

public interface WritableLevelData extends LevelData {
    void setSpawn(BlockPos pSpawnPoint, float pSpawnAngle);
}
