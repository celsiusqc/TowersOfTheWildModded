package net.minecraft.util.debugchart;

public interface SampleStorage {
    int capacity();

    int size();

    long get(int pIndex);

    long get(int pIndex, int pDimension);

    void reset();
}
