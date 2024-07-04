package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class LeaveVineDecorator extends TreeDecorator {
    public static final MapCodec<LeaveVineDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
        .fieldOf("probability")
        .xmap(LeaveVineDecorator::new, p_226037_ -> p_226037_.probability);
    private final float probability;

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.LEAVE_VINE;
    }

    public LeaveVineDecorator(float p_226031_) {
        this.probability = p_226031_;
    }

    @Override
    public void place(TreeDecorator.Context pContext) {
        RandomSource randomsource = pContext.random();
        pContext.leaves().forEach(p_226035_ -> {
            if (randomsource.nextFloat() < this.probability) {
                BlockPos blockpos = p_226035_.west();
                if (pContext.isAir(blockpos)) {
                    addHangingVine(blockpos, VineBlock.EAST, pContext);
                }
            }

            if (randomsource.nextFloat() < this.probability) {
                BlockPos blockpos1 = p_226035_.east();
                if (pContext.isAir(blockpos1)) {
                    addHangingVine(blockpos1, VineBlock.WEST, pContext);
                }
            }

            if (randomsource.nextFloat() < this.probability) {
                BlockPos blockpos2 = p_226035_.north();
                if (pContext.isAir(blockpos2)) {
                    addHangingVine(blockpos2, VineBlock.SOUTH, pContext);
                }
            }

            if (randomsource.nextFloat() < this.probability) {
                BlockPos blockpos3 = p_226035_.south();
                if (pContext.isAir(blockpos3)) {
                    addHangingVine(blockpos3, VineBlock.NORTH, pContext);
                }
            }
        });
    }

    private static void addHangingVine(BlockPos pPos, BooleanProperty pSideProperty, TreeDecorator.Context pContext) {
        pContext.placeVine(pPos, pSideProperty);
        int i = 4;

        for (BlockPos blockpos = pPos.below(); pContext.isAir(blockpos) && i > 0; i--) {
            pContext.placeVine(blockpos, pSideProperty);
            blockpos = blockpos.below();
        }
    }
}
