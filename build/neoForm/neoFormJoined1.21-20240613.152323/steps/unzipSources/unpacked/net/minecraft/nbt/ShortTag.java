package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ShortTag extends NumericTag {
    private static final int SELF_SIZE_IN_BYTES = 10;
    public static final TagType<ShortTag> TYPE = new TagType.StaticSize<ShortTag>() {
        public ShortTag load(DataInput p_129282_, NbtAccounter p_129284_) throws IOException {
            return ShortTag.valueOf(readAccounted(p_129282_, p_129284_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197517_, StreamTagVisitor p_197518_, NbtAccounter p_302385_) throws IOException {
            return p_197518_.visit(readAccounted(p_197517_, p_302385_));
        }

        private static short readAccounted(DataInput p_302332_, NbtAccounter p_302335_) throws IOException {
            p_302335_.accountBytes(10L);
            return p_302332_.readShort();
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public String getName() {
            return "SHORT";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Short";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final short data;

    ShortTag(short pData) {
        this.data = pData;
    }

    public static ShortTag valueOf(short pData) {
        return pData >= -128 && pData <= 1024 ? ShortTag.Cache.cache[pData - -128] : new ShortTag(pData);
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        pOutput.writeShort(this.data);
    }

    @Override
    public int sizeInBytes() {
        return 10;
    }

    @Override
    public byte getId() {
        return 2;
    }

    @Override
    public TagType<ShortTag> getType() {
        return TYPE;
    }

    public ShortTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof ShortTag && this.data == ((ShortTag)pOther).data;
    }

    @Override
    public int hashCode() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor pVisitor) {
        pVisitor.visitShort(this);
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
        return this.data;
    }

    @Override
    public byte getAsByte() {
        return (byte)(this.data & 255);
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
        static final ShortTag[] cache = new ShortTag[1153];

        private Cache() {
        }

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new ShortTag((short)(-128 + i));
            }
        }
    }
}
