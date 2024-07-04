package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class WeightedListHeight extends HeightProvider {
    public static final MapCodec<WeightedListHeight> CODEC = RecordCodecBuilder.mapCodec(
        p_191539_ -> p_191539_.group(
                    SimpleWeightedRandomList.wrappedCodec(HeightProvider.CODEC).fieldOf("distribution").forGetter(p_191541_ -> p_191541_.distribution)
                )
                .apply(p_191539_, WeightedListHeight::new)
    );
    private final SimpleWeightedRandomList<HeightProvider> distribution;

    public WeightedListHeight(SimpleWeightedRandomList<HeightProvider> p_191536_) {
        this.distribution = p_191536_;
    }

    @Override
    public int sample(RandomSource pRandom, WorldGenerationContext pContext) {
        return this.distribution.getRandomValue(pRandom).orElseThrow(IllegalStateException::new).sample(pRandom, pContext);
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.WEIGHTED_LIST;
    }
}
