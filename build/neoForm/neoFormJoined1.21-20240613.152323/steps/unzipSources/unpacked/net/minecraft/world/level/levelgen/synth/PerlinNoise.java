package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/**
 * This class generates multiple octaves of perlin noise. Each individual octave is an instance of {@link net.minecraft.world.level.levelgen.synth.ImprovedNoise}.
 * Mojang uses the term 'Perlin' to describe octaves or fBm (Fractal Brownian Motion) noise.
 */
public class PerlinNoise {
    private static final int ROUND_OFF = 33554432;
    private final ImprovedNoise[] noiseLevels;
    private final int firstOctave;
    private final DoubleList amplitudes;
    private final double lowestFreqValueFactor;
    private final double lowestFreqInputFactor;
    private final double maxValue;

    @Deprecated
    public static PerlinNoise createLegacyForBlendedNoise(RandomSource pRandom, IntStream pOctaves) {
        return new PerlinNoise(pRandom, makeAmplitudes(new IntRBTreeSet(pOctaves.boxed().collect(ImmutableList.toImmutableList()))), false);
    }

    @Deprecated
    public static PerlinNoise createLegacyForLegacyNetherBiome(RandomSource pRandom, int pFirstOctave, DoubleList pAmplitudes) {
        return new PerlinNoise(pRandom, Pair.of(pFirstOctave, pAmplitudes), false);
    }

    public static PerlinNoise create(RandomSource pRandom, IntStream pOctaves) {
        return create(pRandom, pOctaves.boxed().collect(ImmutableList.toImmutableList()));
    }

    public static PerlinNoise create(RandomSource pRandom, List<Integer> pOctaves) {
        return new PerlinNoise(pRandom, makeAmplitudes(new IntRBTreeSet(pOctaves)), true);
    }

    public static PerlinNoise create(RandomSource pRandom, int pFirstOctave, double pFirstAmplitude, double... pAmplitudes) {
        DoubleArrayList doublearraylist = new DoubleArrayList(pAmplitudes);
        doublearraylist.add(0, pFirstAmplitude);
        return new PerlinNoise(pRandom, Pair.of(pFirstOctave, doublearraylist), true);
    }

    public static PerlinNoise create(RandomSource pRandom, int pFirstOctave, DoubleList pAmplitudes) {
        return new PerlinNoise(pRandom, Pair.of(pFirstOctave, pAmplitudes), true);
    }

    private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet pOctaves) {
        if (pOctaves.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        } else {
            int i = -pOctaves.firstInt();
            int j = pOctaves.lastInt();
            int k = i + j + 1;
            if (k < 1) {
                throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
            } else {
                DoubleList doublelist = new DoubleArrayList(new double[k]);
                IntBidirectionalIterator intbidirectionaliterator = pOctaves.iterator();

                while (intbidirectionaliterator.hasNext()) {
                    int l = intbidirectionaliterator.nextInt();
                    doublelist.set(l + i, 1.0);
                }

                return Pair.of(-i, doublelist);
            }
        }
    }

    protected PerlinNoise(RandomSource pRandom, Pair<Integer, DoubleList> pOctavesAndAmplitudes, boolean pUseNewFactory) {
        this.firstOctave = pOctavesAndAmplitudes.getFirst();
        this.amplitudes = pOctavesAndAmplitudes.getSecond();
        int i = this.amplitudes.size();
        int j = -this.firstOctave;
        this.noiseLevels = new ImprovedNoise[i];
        if (pUseNewFactory) {
            PositionalRandomFactory positionalrandomfactory = pRandom.forkPositional();

            for (int k = 0; k < i; k++) {
                if (this.amplitudes.getDouble(k) != 0.0) {
                    int l = this.firstOctave + k;
                    this.noiseLevels[k] = new ImprovedNoise(positionalrandomfactory.fromHashOf("octave_" + l));
                }
            }
        } else {
            ImprovedNoise improvednoise = new ImprovedNoise(pRandom);
            if (j >= 0 && j < i) {
                double d0 = this.amplitudes.getDouble(j);
                if (d0 != 0.0) {
                    this.noiseLevels[j] = improvednoise;
                }
            }

            for (int i1 = j - 1; i1 >= 0; i1--) {
                if (i1 < i) {
                    double d1 = this.amplitudes.getDouble(i1);
                    if (d1 != 0.0) {
                        this.noiseLevels[i1] = new ImprovedNoise(pRandom);
                    } else {
                        skipOctave(pRandom);
                    }
                } else {
                    skipOctave(pRandom);
                }
            }

            if (Arrays.stream(this.noiseLevels).filter(Objects::nonNull).count() != this.amplitudes.stream().filter(p_192897_ -> p_192897_ != 0.0).count()) {
                throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
            }

            if (j < i - 1) {
                throw new IllegalArgumentException("Positive octaves are temporarily disabled");
            }
        }

        this.lowestFreqInputFactor = Math.pow(2.0, (double)(-j));
        this.lowestFreqValueFactor = Math.pow(2.0, (double)(i - 1)) / (Math.pow(2.0, (double)i) - 1.0);
        this.maxValue = this.edgeValue(2.0);
    }

    protected double maxValue() {
        return this.maxValue;
    }

    private static void skipOctave(RandomSource pRandom) {
        pRandom.consumeCount(262);
    }

    public double getValue(double pX, double pY, double pZ) {
        return this.getValue(pX, pY, pZ, 0.0, 0.0, false);
    }

    @Deprecated
    public double getValue(double pX, double pY, double pZ, double pYScale, double pYMax, boolean pUseFixedY) {
        double d0 = 0.0;
        double d1 = this.lowestFreqInputFactor;
        double d2 = this.lowestFreqValueFactor;

        for (int i = 0; i < this.noiseLevels.length; i++) {
            ImprovedNoise improvednoise = this.noiseLevels[i];
            if (improvednoise != null) {
                double d3 = improvednoise.noise(
                    wrap(pX * d1), pUseFixedY ? -improvednoise.yo : wrap(pY * d1), wrap(pZ * d1), pYScale * d1, pYMax * d1
                );
                d0 += this.amplitudes.getDouble(i) * d3 * d2;
            }

            d1 *= 2.0;
            d2 /= 2.0;
        }

        return d0;
    }

    public double maxBrokenValue(double pYMultiplier) {
        return this.edgeValue(pYMultiplier + 2.0);
    }

    private double edgeValue(double pMultiplier) {
        double d0 = 0.0;
        double d1 = this.lowestFreqValueFactor;

        for (int i = 0; i < this.noiseLevels.length; i++) {
            ImprovedNoise improvednoise = this.noiseLevels[i];
            if (improvednoise != null) {
                d0 += this.amplitudes.getDouble(i) * pMultiplier * d1;
            }

            d1 /= 2.0;
        }

        return d0;
    }

    /**
     * @return A single octave of Perlin noise.
     */
    @Nullable
    public ImprovedNoise getOctaveNoise(int pOctave) {
        return this.noiseLevels[this.noiseLevels.length - 1 - pOctave];
    }

    public static double wrap(double pValue) {
        return pValue - (double)Mth.lfloor(pValue / 3.3554432E7 + 0.5) * 3.3554432E7;
    }

    protected int firstOctave() {
        return this.firstOctave;
    }

    protected DoubleList amplitudes() {
        return this.amplitudes;
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder pBuilder) {
        pBuilder.append("PerlinNoise{");
        List<String> list = this.amplitudes.stream().map(p_192889_ -> String.format(Locale.ROOT, "%.2f", p_192889_)).toList();
        pBuilder.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append(list).append(", noise levels: [");

        for (int i = 0; i < this.noiseLevels.length; i++) {
            pBuilder.append(i).append(": ");
            ImprovedNoise improvednoise = this.noiseLevels[i];
            if (improvednoise == null) {
                pBuilder.append("null");
            } else {
                improvednoise.parityConfigString(pBuilder);
            }

            pBuilder.append(", ");
        }

        pBuilder.append("]");
        pBuilder.append("}");
    }
}
