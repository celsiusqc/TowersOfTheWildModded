package net.minecraft.util.valueproviders;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

public class MultipliedFloats implements SampledFloat {
    private final SampledFloat[] values;

    public MultipliedFloats(SampledFloat... pValues) {
        this.values = pValues;
    }

    @Override
    public float sample(RandomSource pRandom) {
        float f = 1.0F;

        for (SampledFloat sampledfloat : this.values) {
            f *= sampledfloat.sample(pRandom);
        }

        return f;
    }

    @Override
    public String toString() {
        return "MultipliedFloats" + Arrays.toString((Object[])this.values);
    }
}
