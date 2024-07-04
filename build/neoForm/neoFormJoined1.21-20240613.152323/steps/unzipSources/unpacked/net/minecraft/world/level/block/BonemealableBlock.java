package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface BonemealableBlock {
    boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState);

    boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState);

    void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState);

    default BlockPos getParticlePos(BlockPos pPos) {
        return switch (this.getType()) {
            case NEIGHBOR_SPREADER -> pPos.above();
            case GROWER -> pPos;
        };
    }

    default BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.GROWER;
    }

    public static enum Type {
        NEIGHBOR_SPREADER,
        GROWER;
    }
}
