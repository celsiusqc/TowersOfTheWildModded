package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.util.Mth;

public class FloatTag extends NumericTag {
    private static final int SELF_SIZE_IN_BYTES = 12;
    public static final FloatTag ZERO = new FloatTag(0.0F);
    public static final TagType<FloatTag> TYPE = new TagType.StaticSize<FloatTag>() {
        public FloatTag load(DataInput p_128590_, NbtAccounter p_128592_) throws IOException {
            return FloatTag.valueOf(readAccounted(p_128590_, p_128592_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197470_, StreamTagVisitor p_197471_, NbtAccounter p_302329_) throws IOException {
            return p_197471_.visit(readAccounted(p_197470_, p_302329_));
        }

        private static float readAccounted(DataInput p_302328_, NbtAccounter p_302359_) throws IOException {
            p_302359_.accountBytes(12L);
            return p_302328_.readFloat();
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public String getName() {
            return "FLOAT";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Float";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    private final float data;

    private FloatTag(float pData) {
        this.data = pData;
    }

    public static FloatTag valueOf(float pData) {
        return pData == 0.0F ? ZERO : new FloatTag(pData);
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        pOutput.writeFloat(this.data);
    }

    @Override
    public int sizeInBytes() {
        return 12;
    }

    @Override
    public byte getId() {
        return 5;
    }

    @Override
    public TagType<FloatTag> getType() {
        return TYPE;
    }

    public FloatTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof FloatTag && this.data == ((FloatTag)pOther).data;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(this.data);
    }

    @Override
    public void accept(TagVisitor pVisitor) {
        pVisitor.visitFloat(this);
    }

    @Override
    public long getAsLong() {
        return (long)this.data;
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
        return (double)this.data;
    }

    @Override
    public float getAsFloat() {
        return this.data;
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
