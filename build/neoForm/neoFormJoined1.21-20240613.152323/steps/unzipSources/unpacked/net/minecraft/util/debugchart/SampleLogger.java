package net.minecraft.util.debugchart;

public interface SampleLogger {
    void logFullSample(long[] pSample);

    void logSample(long pValue);

    void logPartialSample(long pValue, int pIndex);
}
