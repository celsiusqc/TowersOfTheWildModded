package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public class BlockPredicateFilter extends PlacementFilter {
    public static final MapCodec<BlockPredicateFilter> CODEC = RecordCodecBuilder.mapCodec(
        p_191575_ -> p_191575_.group(BlockPredicate.CODEC.fieldOf("predicate").forGetter(p_191579_ -> p_191579_.predicate))
                .apply(p_191575_, BlockPredicateFilter::new)
    );
    private final BlockPredicate predicate;

    private BlockPredicateFilter(BlockPredicate p_191573_) {
        this.predicate = p_191573_;
    }

    public static BlockPredicateFilter forPredicate(BlockPredicate pPredicate) {
        return new BlockPredicateFilter(pPredicate);
    }

    @Override
    protected boolean shouldPlace(PlacementContext pContext, RandomSource pRandom, BlockPos pPos) {
        return this.predicate.test(pContext.getLevel(), pPos);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.BLOCK_PREDICATE_FILTER;
    }
}
