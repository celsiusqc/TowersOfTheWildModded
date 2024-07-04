package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class IntTag extends NumericTag {
    private static final int SELF_SIZE_IN_BYTES = 12;
    public static final TagType<IntTag> TYPE = new TagType.StaticSize<IntTag>() {
        public IntTag load(DataInput p_128708_, NbtAccounter p_128710_) throws IOException {
            return IntTag.valueOf(readAccounted(p_128708_, p_128710_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197483_, StreamTagVisitor p_197484_, NbtAccounter p_302339_) throws IOException {
            return p_197484_.visit(readAccounted(p_197483_, p_302339_));
        }

        private static int readAccounted(DataInput p_302357_, NbtAccounter p_302392_) throws IOException {
            p_302392_.accountBytes(12L);
            return p_302357_.readInt();
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public String getName() {
            return "INT";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Int";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final int data;

    IntTag(int pData) {
        this.data = pData;
    }

    public static IntTag valueOf(int pData) {
        return pData >= -128 && pData <= 1024 ? IntTag.Cache.cache[pData - -128] : new IntTag(pData);
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        pOutput.writeInt(this.data);
    }

    @Override
    public int sizeInBytes() {
        return 12;
    }

    @Override
    public byte getId() {
        return 3;
    }

    @Override
    public TagType<IntTag> getType() {
        return TYPE;
    }

    public IntTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof IntTag && this.data == ((IntTag)pOther).data;
    }

    @Override
    public int hashCode() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor pVisitor) {
        pVisitor.visitInt(this);
    }

    @Override
    public long getAsLong() {
        return (long)this.data;
    }

    @Override
    public int getAsInt() {
        return this.data;
    }

    @Override
    public short getAsShort() {
        return (short)(this.data & 65535);
    }

    @Override
    public byte getAsByte() {
        return (byte)(this.data & 0xFF);
    }

    @Override
    public double getAsDouble() {
        return (double)this.data;
    }

    @Override
    public float getAsFloat() {
        return (float)this.data;
    }

    @Override
    public Number getAsNumber() {
        return this.data;
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
        return pVisitor.visit(this.data);
    }

    static class Cache {
        private static final int HIGH = 1024;
        private static final int LOW = -128;
        static final IntTag[] cache = new IntTag[1153];

        private Cache() {
        }

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new IntTag(-128 + i);
            }
        }
    }
}
