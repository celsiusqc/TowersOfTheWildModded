package net.minecraft.nbt;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class LongArrayTag extends CollectionTag<LongTag> {
    private static final int SELF_SIZE_IN_BYTES = 24;
    public static final TagType<LongArrayTag> TYPE = new TagType.VariableSize<LongArrayTag>() {
        public LongArrayTag load(DataInput p_128865_, NbtAccounter p_128867_) throws IOException {
            return new LongArrayTag(readAccounted(p_128865_, p_128867_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197501_, StreamTagVisitor p_197502_, NbtAccounter p_302321_) throws IOException {
            return p_197502_.visit(readAccounted(p_197501_, p_302321_));
        }

        private static long[] readAccounted(DataInput p_302379_, NbtAccounter p_302364_) throws IOException {
            p_302364_.accountBytes(24L);
            int i = p_302379_.readInt();
            p_302364_.accountBytes(8L, (long)i);
            long[] along = new long[i];

            for (int j = 0; j < i; j++) {
                along[j] = p_302379_.readLong();
            }

            return along;
        }

        @Override
        public void skip(DataInput p_197499_, NbtAccounter p_302368_) throws IOException {
            p_197499_.skipBytes(p_197499_.readInt() * 8);
        }

        @Override
        public String getName() {
            return "LONG[]";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Long_Array";
        }
    };
    private long[] data;

    public LongArrayTag(long[] pData) {
        this.data = pData;
    }

    public LongArrayTag(LongSet pDataSet) {
        this.data = pDataSet.toLongArray();
    }

    public LongArrayTag(List<Long> pDataList) {
        this(toArray(pDataList));
    }

    private static long[] toArray(List<Long> pDataList) {
        long[] along = new long[pDataList.size()];

        for (int i = 0; i < pDataList.size(); i++) {
            Long olong = pDataList.get(i);
            along[i] = olong == null ? 0L : olong;
        }

        return along;
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        pOutput.writeInt(this.data.length);

        for (long i : this.data) {
            pOutput.writeLong(i);
        }
    }

    @Override
    public int sizeInBytes() {
        return 24 + 8 * this.data.length;
    }

    @Override
    public byte getId() {
        return 12;
    }

    @Override
    public TagType<LongArrayTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.getAsString();
    }

    public LongArrayTag copy() {
        long[] along = new long[this.data.length];
        System.arraycopy(this.data, 0, along, 0, this.data.length);
        return new LongArrayTag(along);
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof LongArrayTag && Arrays.equals(this.data, ((LongArrayTag)pOther).data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    @Override
    public void accept(TagVisitor pVisitor) {
        pVisitor.visitLongArray(this);
    }

    public long[] getAsLongArray() {
        return this.data;
    }

    @Override
    public int size() {
        return this.data.length;
    }

    public LongTag get(int pIndex) {
        return LongTag.valueOf(this.data[pIndex]);
    }

    public LongTag set(int pIndex, LongTag pTag) {
        long i = this.data[pIndex];
        this.data[pIndex] = pTag.getAsLong();
        return LongTag.valueOf(i);
    }

    public void add(int pIndex, LongTag pTag) {
        this.data = ArrayUtils.add(this.data, pIndex, pTag.getAsLong());
    }

    @Override
    public boolean setTag(int pIndex, Tag pNbt) {
        if (pNbt instanceof NumericTag) {
            this.data[pIndex] = ((NumericTag)pNbt).getAsLong();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int pIndex, Tag pNbt) {
        if (pNbt instanceof NumericTag) {
            this.data = ArrayUtils.add(this.data, pIndex, ((NumericTag)pNbt).getAsLong());
            return true;
        } else {
            return false;
        }
    }

    public LongTag remove(int pIndex) {
        long i = this.data[pIndex];
        this.data = ArrayUtils.remove(this.data, pIndex);
        return LongTag.valueOf(i);
    }

    @Override
    public byte getElementType() {
        return 4;
    }

    @Override
    public void clear() {
        this.data = new long[0];
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
        return pVisitor.visit(this.data);
    }
}
