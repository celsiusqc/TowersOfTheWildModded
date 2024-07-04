package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.state.BlockState;

public class WeightedStateProvider extends BlockStateProvider {
    public static final MapCodec<WeightedStateProvider> CODEC = SimpleWeightedRandomList.wrappedCodec(BlockState.CODEC)
        .comapFlatMap(WeightedStateProvider::create, p_161600_ -> p_161600_.weightedList)
        .fieldOf("entries");
    private final SimpleWeightedRandomList<BlockState> weightedList;

    private static DataResult<WeightedStateProvider> create(SimpleWeightedRandomList<BlockState> p_161598_) {
        return p_161598_.isEmpty() ? DataResult.error(() -> "WeightedStateProvider with no states") : DataResult.success(new WeightedStateProvider(p_161598_));
    }

    public WeightedStateProvider(SimpleWeightedRandomList<BlockState> pWeightedList) {
        this.weightedList = pWeightedList;
    }

    public WeightedStateProvider(SimpleWeightedRandomList.Builder<BlockState> pBuilder) {
        this(pBuilder.build());
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.WEIGHTED_STATE_PROVIDER;
    }

    @Override
    public BlockState getState(RandomSource pRandom, BlockPos pPos) {
        return this.weightedList.getRandomValue(pRandom).orElseThrow(IllegalStateException::new);
    }
}
