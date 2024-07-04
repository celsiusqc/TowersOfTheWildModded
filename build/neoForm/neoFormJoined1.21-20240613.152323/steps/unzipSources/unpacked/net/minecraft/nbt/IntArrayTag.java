package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class IntArrayTag extends CollectionTag<IntTag> {
    private static final int SELF_SIZE_IN_BYTES = 24;
    public static final TagType<IntArrayTag> TYPE = new TagType.VariableSize<IntArrayTag>() {
        public IntArrayTag load(DataInput p_128662_, NbtAccounter p_128664_) throws IOException {
            return new IntArrayTag(readAccounted(p_128662_, p_128664_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197478_, StreamTagVisitor p_197479_, NbtAccounter p_302360_) throws IOException {
            return p_197479_.visit(readAccounted(p_197478_, p_302360_));
        }

        private static int[] readAccounted(DataInput p_302381_, NbtAccounter p_302330_) throws IOException {
            p_302330_.accountBytes(24L);
            int i = p_302381_.readInt();
            p_302330_.accountBytes(4L, (long)i);
            int[] aint = new int[i];

            for (int j = 0; j < i; j++) {
                aint[j] = p_302381_.readInt();
            }

            return aint;
        }

        @Override
        public void skip(DataInput p_197476_, NbtAccounter p_302380_) throws IOException {
            p_197476_.skipBytes(p_197476_.readInt() * 4);
        }

        @Override
        public String getName() {
            return "INT[]";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Int_Array";
        }
    };
    private int[] data;

    public IntArrayTag(int[] pData) {
        this.data = pData;
    }

    public IntArrayTag(List<Integer> pDataList) {
        this(toArray(pDataList));
    }

    private static int[] toArray(List<Integer> pDataList) {
        int[] aint = new int[pDataList.size()];

        for (int i = 0; i < pDataList.size(); i++) {
            Integer integer = pDataList.get(i);
            aint[i] = integer == null ? 0 : integer;
        }

        return aint;
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        pOutput.writeInt(this.data.length);

        for (int i : this.data) {
            pOutput.writeInt(i);
        }
    }

    @Override
    public int sizeInBytes() {
        return 24 + 4 * this.data.length;
    }

    @Override
    public byte getId() {
        return 11;
    }

    @Override
    public TagType<IntArrayTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.getAsString();
    }

    public IntArrayTag copy() {
        int[] aint = new int[this.data.length];
        System.arraycopy(this.data, 0, aint, 0, this.data.length);
        return new IntArrayTag(aint);
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof IntArrayTag && Arrays.equals(this.data, ((IntArrayTag)pOther).data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    public int[] getAsIntArray() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor pVisitor) {
        pVisitor.visitIntArray(this);
    }

    @Override
    public int size() {
        return this.data.length;
    }

    public IntTag get(int pIndex) {
        return IntTag.valueOf(this.data[pIndex]);
    }

    public IntTag set(int pIndex, IntTag pTag) {
        int i = this.data[pIndex];
        this.data[pIndex] = pTag.getAsInt();
        return IntTag.valueOf(i);
    }

    public void add(int pIndex, IntTag pTag) {
        this.data = ArrayUtils.add(this.data, pIndex, pTag.getAsInt());
    }

    @Override
    public boolean setTag(int pIndex, Tag pNbt) {
        if (pNbt instanceof NumericTag) {
            this.data[pIndex] = ((NumericTag)pNbt).getAsInt();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int pIndex, Tag pNbt) {
        if (pNbt instanceof NumericTag) {
            this.data = ArrayUtils.add(this.data, pIndex, ((NumericTag)pNbt).getAsInt());
            return true;
        } else {
            return false;
        }
    }

    public IntTag remove(int pIndex) {
        int i = this.data[pIndex];
        this.data = ArrayUtils.remove(this.data, pIndex);
        return IntTag.valueOf(i);
    }

    @Override
    public byte getElementType() {
        return 3;
    }

    @Override
    public void clear() {
        this.data = new int[0];
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
        return pVisitor.visit(this.data);
    }
}
