package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public interface ChangeOverTimeBlock<T extends Enum<T>> {
    int SCAN_DISTANCE = 4;

    Optional<BlockState> getNext(BlockState pState);

    float getChanceModifier();

    default void changeOverTime(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        float f = 0.05688889F;
        if (pRandom.nextFloat() < 0.05688889F) {
            this.getNextState(pState, pLevel, pPos, pRandom).ifPresent(p_153039_ -> pLevel.setBlockAndUpdate(pPos, p_153039_));
        }
    }

    T getAge();

    default Optional<BlockState> getNextState(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        int i = this.getAge().ordinal();
        int j = 0;
        int k = 0;

        for (BlockPos blockpos : BlockPos.withinManhattan(pPos, 4, 4, 4)) {
            int l = blockpos.distManhattan(pPos);
            if (l > 4) {
                break;
            }

            if (!blockpos.equals(pPos) && pLevel.getBlockState(blockpos).getBlock() instanceof ChangeOverTimeBlock<?> changeovertimeblock) {
                Enum<?> oenum = changeovertimeblock.getAge();
                if (this.getAge().getClass() == oenum.getClass()) {
                    int i1 = oenum.ordinal();
                    if (i1 < i) {
                        return Optional.empty();
                    }

                    if (i1 > i) {
                        k++;
                    } else {
                        j++;
                    }
                }
            }
        }

        float f = (float)(k + 1) / (float)(k + j + 1);
        float f1 = f * f * this.getChanceModifier();
        return pRandom.nextFloat() < f1 ? this.getNext(pState) : Optional.empty();
    }
}
