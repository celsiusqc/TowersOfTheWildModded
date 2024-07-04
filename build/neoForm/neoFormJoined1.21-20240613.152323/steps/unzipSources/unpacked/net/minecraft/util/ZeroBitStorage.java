package net.minecraft.util;

import java.util.Arrays;
import java.util.function.IntConsumer;
import org.apache.commons.lang3.Validate;

public class ZeroBitStorage implements BitStorage {
    public static final long[] RAW = new long[0];
    private final int size;

    public ZeroBitStorage(int pSize) {
        this.size = pSize;
    }

    @Override
    public int getAndSet(int pIndex, int pValue) {
        Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)pIndex);
        Validate.inclusiveBetween(0L, 0L, (long)pValue);
        return 0;
    }

    /**
     * Sets the entry at the given location to the given value
     */
    @Override
    public void set(int pIndex, int pValue) {
        Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)pIndex);
        Validate.inclusiveBetween(0L, 0L, (long)pValue);
    }

    /**
     * Gets the entry at the given index
     */
    @Override
    public int get(int pIndex) {
        Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)pIndex);
        return 0;
    }

    @Override
    public long[] getRaw() {
        return RAW;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public int getBits() {
        return 0;
    }

    @Override
    public void getAll(IntConsumer pConsumer) {
        for (int i = 0; i < this.size; i++) {
            pConsumer.accept(0);
        }
    }

    @Override
    public void unpack(int[] pArray) {
        Arrays.fill(pArray, 0, this.size, 0);
    }

    @Override
    public BitStorage copy() {
        return this;
    }
}
