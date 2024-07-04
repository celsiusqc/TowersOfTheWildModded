package net.minecraft.world.inventory;

public abstract class DataSlot {
    private int prevValue;

    public static DataSlot forContainer(final ContainerData pData, final int pIdx) {
        return new DataSlot() {
            @Override
            public int get() {
                return pData.get(pIdx);
            }

            @Override
            public void set(int p_39416_) {
                pData.set(pIdx, p_39416_);
            }
        };
    }

    public static DataSlot shared(final int[] pData, final int pIdx) {
        return new DataSlot() {
            @Override
            public int get() {
                return pData[pIdx];
            }

            @Override
            public void set(int p_39424_) {
                pData[pIdx] = p_39424_;
            }
        };
    }

    public static DataSlot standalone() {
        return new DataSlot() {
            private int value;

            @Override
            public int get() {
                return this.value;
            }

            @Override
            public void set(int p_39429_) {
                this.value = p_39429_;
            }
        };
    }

    public abstract int get();

    public abstract void set(int pValue);

    public boolean checkAndClearUpdateFlag() {
        int i = this.get();
        boolean flag = i != this.prevValue;
        this.prevValue = i;
        return flag;
    }
}
