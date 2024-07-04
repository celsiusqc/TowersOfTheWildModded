package net.minecraft.util;

import java.util.function.IntConsumer;

public interface BitStorage {
    int getAndSet(int pIndex, int pValue);

    /**
     * Sets the entry at the given location to the given value
     */
    void set(int pIndex, int pValue);

    /**
     * Gets the entry at the given index
     */
    int get(int pIndex);

    long[] getRaw();

    int getSize();

    int getBits();

    void getAll(IntConsumer pConsumer);

    void unpack(int[] pArray);

    BitStorage copy();
}
