package net.minecraft.world.level.levelgen;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.RandomSource;

@Deprecated
public class ThreadSafeLegacyRandomSource implements BitRandomSource {
    private static final int MODULUS_BITS = 48;
    private static final long MODULUS_MASK = 281474976710655L;
    private static final long MULTIPLIER = 25214903917L;
    private static final long INCREMENT = 11L;
    private final AtomicLong seed = new AtomicLong();
    private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

    public ThreadSafeLegacyRandomSource(long pSeed) {
        this.setSeed(pSeed);
    }

    @Override
    public RandomSource fork() {
        return new ThreadSafeLegacyRandomSource(this.nextLong());
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return new LegacyRandomSource.LegacyPositionalRandomFactory(this.nextLong());
    }

    @Override
    public void setSeed(long pSeed) {
        this.seed.set((pSeed ^ 25214903917L) & 281474976710655L);
    }

    @Override
    public int next(int pSize) {
        long i;
        long j;
        do {
            i = this.seed.get();
            j = i * 25214903917L + 11L & 281474976710655L;
        } while (!this.seed.compareAndSet(i, j));

        return (int)(j >>> 48 - pSize);
    }

    @Override
    public double nextGaussian() {
        return this.gaussianSource.nextGaussian();
    }
}
