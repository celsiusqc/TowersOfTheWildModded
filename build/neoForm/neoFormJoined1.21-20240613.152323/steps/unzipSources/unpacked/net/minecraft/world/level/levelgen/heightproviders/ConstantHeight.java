package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class ConstantHeight extends HeightProvider {
    public static final ConstantHeight ZERO = new ConstantHeight(VerticalAnchor.absolute(0));
    public static final MapCodec<ConstantHeight> CODEC = VerticalAnchor.CODEC.fieldOf("value").xmap(ConstantHeight::new, ConstantHeight::getValue);
    private final VerticalAnchor value;

    public static ConstantHeight of(VerticalAnchor pValue) {
        return new ConstantHeight(pValue);
    }

    private ConstantHeight(VerticalAnchor p_161950_) {
        this.value = p_161950_;
    }

    public VerticalAnchor getValue() {
        return this.value;
    }

    @Override
    public int sample(RandomSource pRandom, WorldGenerationContext pContext) {
        return this.value.resolveY(pContext);
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.CONSTANT;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }
}
