package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class CubePointRange extends AbstractDoubleList {
    private final int parts;

    public CubePointRange(int pParts) {
        if (pParts <= 0) {
            throw new IllegalArgumentException("Need at least 1 part");
        } else {
            this.parts = pParts;
        }
    }

    @Override
    public double getDouble(int pValue) {
        return (double)pValue / (double)this.parts;
    }

    @Override
    public int size() {
        return this.parts + 1;
    }
}
