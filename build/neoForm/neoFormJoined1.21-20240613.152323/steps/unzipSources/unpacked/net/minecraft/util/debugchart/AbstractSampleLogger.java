package net.minecraft.util.debugchart;

public abstract class AbstractSampleLogger implements SampleLogger {
    protected final long[] defaults;
    protected final long[] sample;

    protected AbstractSampleLogger(int pSize, long[] pDefaults) {
        if (pDefaults.length != pSize) {
            throw new IllegalArgumentException("defaults have incorrect length of " + pDefaults.length);
        } else {
            this.sample = new long[pSize];
            this.defaults = pDefaults;
        }
    }

    @Override
    public void logFullSample(long[] pSample) {
        System.arraycopy(pSample, 0, this.sample, 0, pSample.length);
        this.useSample();
        this.resetSample();
    }

    @Override
    public void logSample(long pValue) {
        this.sample[0] = pValue;
        this.useSample();
        this.resetSample();
    }

    @Override
    public void logPartialSample(long pValue, int pIndex) {
        if (pIndex >= 1 && pIndex < this.sample.length) {
            this.sample[pIndex] = pValue;
        } else {
            throw new IndexOutOfBoundsException(pIndex + " out of bounds for dimensions " + this.sample.length);
        }
    }

    protected abstract void useSample();

    protected void resetSample() {
        System.arraycopy(this.defaults, 0, this.sample, 0, this.defaults.length);
    }
}
