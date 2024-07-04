package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

public interface SpawnPlacementType {
    boolean isSpawnPositionOk(LevelReader pLevel, BlockPos pPos, @Nullable EntityType<?> pEntityType);

    default BlockPos adjustSpawnPosition(LevelReader pLevel, BlockPos pPos) {
        return pPos;
    }
}
