package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class XoroshiroRandomSource implements RandomSource {
    private static final float FLOAT_UNIT = 5.9604645E-8F;
    private static final double DOUBLE_UNIT = 1.110223E-16F;
    public static final Codec<XoroshiroRandomSource> CODEC = Xoroshiro128PlusPlus.CODEC
        .xmap(p_287645_ -> new XoroshiroRandomSource(p_287645_), p_287690_ -> p_287690_.randomNumberGenerator);
    private Xoroshiro128PlusPlus randomNumberGenerator;
    private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

    public XoroshiroRandomSource(long pSeed) {
        this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(pSeed));
    }

    public XoroshiroRandomSource(RandomSupport.Seed128bit pSeed) {
        this.randomNumberGenerator = new Xoroshiro128PlusPlus(pSeed);
    }

    public XoroshiroRandomSource(long pSeedLo, long pSeedHi) {
        this.randomNumberGenerator = new Xoroshiro128PlusPlus(pSeedLo, pSeedHi);
    }

    private XoroshiroRandomSource(Xoroshiro128PlusPlus pRandomNumberGenerator) {
        this.randomNumberGenerator = pRandomNumberGenerator;
    }

    @Override
    public RandomSource fork() {
        return new XoroshiroRandomSource(this.randomNumberGenerator.nextLong(), this.randomNumberGenerator.nextLong());
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return new XoroshiroRandomSource.XoroshiroPositionalRandomFactory(this.randomNumberGenerator.nextLong(), this.randomNumberGenerator.nextLong());
    }

    @Override
    public void setSeed(long pSeed) {
        this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(pSeed));
        this.gaussianSource.reset();
    }

    @Override
    public int nextInt() {
        return (int)this.randomNumberGenerator.nextLong();
    }

    @Override
    public int nextInt(int pBound) {
        if (pBound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        } else {
            long i = Integer.toUnsignedLong(this.nextInt());
            long j = i * (long)pBound;
            long k = j & 4294967295L;
            if (k < (long)pBound) {
                for (int l = Integer.remainderUnsigned(~pBound + 1, pBound); k < (long)l; k = j & 4294967295L) {
                    i = Integer.toUnsignedLong(this.nextInt());
                    j = i * (long)pBound;
                }
            }

            long i1 = j >> 32;
            return (int)i1;
        }
    }

    @Override
    public long nextLong() {
        return this.randomNumberGenerator.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return (this.randomNumberGenerator.nextLong() & 1L) != 0L;
    }

    @Override
    public float nextFloat() {
        return (float)this.nextBits(24) * 5.9604645E-8F;
    }

    @Override
    public double nextDouble() {
        return (double)this.nextBits(53) * 1.110223E-16F;
    }

    @Override
    public double nextGaussian() {
        return this.gaussianSource.nextGaussian();
    }

    @Override
    public void consumeCount(int pCount) {
        for (int i = 0; i < pCount; i++) {
            this.randomNumberGenerator.nextLong();
        }
    }

    private long nextBits(int pBits) {
        return this.randomNumberGenerator.nextLong() >>> 64 - pBits;
    }

    public static class XoroshiroPositionalRandomFactory implements PositionalRandomFactory {
        private final long seedLo;
        private final long seedHi;

        public XoroshiroPositionalRandomFactory(long pSeedLo, long pSeedHi) {
            this.seedLo = pSeedLo;
            this.seedHi = pSeedHi;
        }

        @Override
        public RandomSource at(int pX, int pY, int pZ) {
            long i = Mth.getSeed(pX, pY, pZ);
            long j = i ^ this.seedLo;
            return new XoroshiroRandomSource(j, this.seedHi);
        }

        @Override
        public RandomSource fromHashOf(String pName) {
            RandomSupport.Seed128bit randomsupport$seed128bit = RandomSupport.seedFromHashOf(pName);
            return new XoroshiroRandomSource(randomsupport$seed128bit.xor(this.seedLo, this.seedHi));
        }

        @Override
        public RandomSource fromSeed(long p_348665_) {
            return new XoroshiroRandomSource(p_348665_ ^ this.seedLo, p_348665_ ^ this.seedHi);
        }

        @VisibleForTesting
        @Override
        public void parityConfigString(StringBuilder pBuilder) {
            pBuilder.append("seedLo: ").append(this.seedLo).append(", seedHi: ").append(this.seedHi);
        }
    }
}
