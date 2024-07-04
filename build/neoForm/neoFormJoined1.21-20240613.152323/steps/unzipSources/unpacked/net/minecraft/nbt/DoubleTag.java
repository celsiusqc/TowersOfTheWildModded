package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.util.Mth;

public class DoubleTag extends NumericTag {
    private static final int SELF_SIZE_IN_BYTES = 16;
    public static final DoubleTag ZERO = new DoubleTag(0.0);
    public static final TagType<DoubleTag> TYPE = new TagType.StaticSize<DoubleTag>() {
        public DoubleTag load(DataInput p_128524_, NbtAccounter p_128526_) throws IOException {
            return DoubleTag.valueOf(readAccounted(p_128524_, p_128526_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197454_, StreamTagVisitor p_197455_, NbtAccounter p_302353_) throws IOException {
            return p_197455_.visit(readAccounted(p_197454_, p_302353_));
        }

        private static double readAccounted(DataInput p_302363_, NbtAccounter p_302397_) throws IOException {
            p_302397_.accountBytes(16L);
            return p_302363_.readDouble();
        }

        @Override
        public int size() {
            return 8;
        }

        @Override
        public String getName() {
            return "DOUBLE";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Double";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final double data;

    private DoubleTag(double pData) {
        this.data = pData;
    }

    public static DoubleTag valueOf(double pData) {
        return pData == 0.0 ? ZERO : new DoubleTag(pData);
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        pOutput.writeDouble(this.data);
    }

    @Override
    public int sizeInBytes() {
        return 16;
    }

    @Override
    public byte getId() {
        return 6;
    }

    @Override
    public TagType<DoubleTag> getType() {
        return TYPE;
    }

    public DoubleTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof DoubleTag && this.data == ((DoubleTag)pOther).data;
    }

    @Override
    public int hashCode() {
        long i = Double.doubleToLongBits(this.data);
        return (int)(i ^ i >>> 32);
    }

    @Override
    public void accept(TagVisitor pVisitor) {
        pVisitor.visitDouble(this);
    }

    @Override
    public long getAsLong() {
        return (long)Math.floor(this.data);
    }

    @Override
    public int getAsInt() {
        return Mth.floor(this.data);
    }

    @Override
    public short getAsShort() {
        return (short)(Mth.floor(this.data) & 65535);
    }

    @Override
    public byte getAsByte() {
        return (byte)(Mth.floor(this.data) & 0xFF);
    }

    @Override
    public double getAsDouble() {
        return this.data;
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
}
