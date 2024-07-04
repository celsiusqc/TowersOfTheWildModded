package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

public class PathTypeCache {
    private static final int SIZE = 4096;
    private static final int MASK = 4095;
    private final long[] positions = new long[4096];
    private final PathType[] pathTypes = new PathType[4096];

    public PathType getOrCompute(BlockGetter pLevel, BlockPos pPos) {
        long i = pPos.asLong();
        int j = index(i);
        PathType pathtype = this.get(j, i);
        return pathtype != null ? pathtype : this.compute(pLevel, pPos, j, i);
    }

    @Nullable
    private PathType get(int pIndex, long pPos) {
        return this.positions[pIndex] == pPos ? this.pathTypes[pIndex] : null;
    }

    private PathType compute(BlockGetter pLevel, BlockPos pPos, int pIndex, long pPackedPos) {
        PathType pathtype = WalkNodeEvaluator.getPathTypeFromState(pLevel, pPos);
        this.positions[pIndex] = pPackedPos;
        this.pathTypes[pIndex] = pathtype;
        return pathtype;
    }

    public void invalidate(BlockPos pPos) {
        long i = pPos.asLong();
        int j = index(i);
        if (this.positions[j] == i) {
            this.pathTypes[j] = null;
        }
    }

    private static int index(long pPos) {
        return (int)HashCommon.mix(pPos) & 4095;
    }
}
