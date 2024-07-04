package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.datafixers.Products.P4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseProvider extends NoiseBasedStateProvider {
    public static final MapCodec<NoiseProvider> CODEC = RecordCodecBuilder.mapCodec(
        p_191462_ -> noiseProviderCodec(p_191462_).apply(p_191462_, NoiseProvider::new)
    );
    protected final List<BlockState> states;

    protected static <P extends NoiseProvider> P4<Mu<P>, Long, NormalNoise.NoiseParameters, Float, List<BlockState>> noiseProviderCodec(Instance<P> pInstance) {
        return noiseCodec(pInstance).and(Codec.list(BlockState.CODEC).fieldOf("states").forGetter(p_191448_ -> p_191448_.states));
    }

    public NoiseProvider(long p_191442_, NormalNoise.NoiseParameters p_191443_, float p_191444_, List<BlockState> p_191445_) {
        super(p_191442_, p_191443_, p_191444_);
        this.states = p_191445_;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.NOISE_PROVIDER;
    }

    @Override
    public BlockState getState(RandomSource pRandom, BlockPos pPos) {
        return this.getRandomState(this.states, pPos, (double)this.scale);
    }

    protected BlockState getRandomState(List<BlockState> pPossibleStates, BlockPos pPos, double pDelta) {
        double d0 = this.getNoiseValue(pPos, pDelta);
        return this.getRandomState(pPossibleStates, d0);
    }

    protected BlockState getRandomState(List<BlockState> pPossibleStates, double pDelta) {
        double d0 = Mth.clamp((1.0 + pDelta) / 2.0, 0.0, 0.9999);
        return pPossibleStates.get((int)(d0 * (double)pPossibleStates.size()));
    }
}
