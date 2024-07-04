package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;

public class CompoundTag implements Tag {
    public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH
        .comapFlatMap(
            p_311527_ -> {
                Tag tag = p_311527_.convert(NbtOps.INSTANCE).getValue();
                return tag instanceof CompoundTag compoundtag
                    ? DataResult.success(compoundtag == p_311527_.getValue() ? compoundtag.copy() : compoundtag)
                    : DataResult.error(() -> "Not a compound tag: " + tag);
            },
            p_311526_ -> new Dynamic<>(NbtOps.INSTANCE, p_311526_.copy())
        );
    private static final int SELF_SIZE_IN_BYTES = 48;
    private static final int MAP_ENTRY_SIZE_IN_BYTES = 32;
    public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>() {
        public CompoundTag load(DataInput p_128485_, NbtAccounter p_128487_) throws IOException {
            p_128487_.pushDepth();

            CompoundTag compoundtag;
            try {
                compoundtag = loadCompound(p_128485_, p_128487_);
            } finally {
                p_128487_.popDepth();
            }

            return compoundtag;
        }

        private static byte readNamedTagType(DataInput p_302338_, NbtAccounter p_302362_) throws IOException {
            p_302362_.accountBytes(2);
            return p_302338_.readByte();
        }

        private static CompoundTag loadCompound(DataInput p_302338_, NbtAccounter p_302362_) throws IOException {
            p_302362_.accountBytes(48L);
            Map<String, Tag> map = Maps.newHashMap();

            byte b0;
            while((b0 = readNamedTagType(p_302338_, p_302362_)) != 0) {
                String s = p_302362_.readUTF(p_302338_.readUTF());
                p_302362_.accountBytes(4); //Forge: 4 extra bytes for the object allocation.
                Tag tag = CompoundTag.readNamedTagData(TagTypes.getType(b0), s, p_302338_, p_302362_);
                if (map.put(s, tag) == null) {
                    p_302362_.accountBytes(36L);
                }
            }

            return new CompoundTag(map);
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197446_, StreamTagVisitor p_197447_, NbtAccounter p_302322_) throws IOException {
            p_302322_.pushDepth();

            StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
            try {
                streamtagvisitor$valueresult = parseCompound(p_197446_, p_197447_, p_302322_);
            } finally {
                p_302322_.popDepth();
            }

            return streamtagvisitor$valueresult;
        }

        private static StreamTagVisitor.ValueResult parseCompound(DataInput p_302325_, StreamTagVisitor p_302352_, NbtAccounter p_302355_) throws IOException {
            p_302355_.accountBytes(48L);

            byte b0;
            label35:
            while ((b0 = p_302325_.readByte()) != 0) {
                TagType<?> tagtype = TagTypes.getType(b0);
                switch (p_302352_.visitEntry(tagtype)) {
                    case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                    case BREAK:
                        StringTag.skipString(p_302325_);
                        tagtype.skip(p_302325_, p_302355_);
                        break label35;
                    case SKIP:
                        StringTag.skipString(p_302325_);
                        tagtype.skip(p_302325_, p_302355_);
                        break;
                    default:
                        String s = readString(p_302325_, p_302355_);
                        switch (p_302352_.visitEntry(tagtype, s)) {
                            case HALT:
                                return StreamTagVisitor.ValueResult.HALT;
                            case BREAK:
                                tagtype.skip(p_302325_, p_302355_);
                                break label35;
                            case SKIP:
                                tagtype.skip(p_302325_, p_302355_);
                                break;
                            default:
                                p_302355_.accountBytes(36L);
                                switch (tagtype.parse(p_302325_, p_302352_, p_302355_)) {
                                    case HALT:
                                        return StreamTagVisitor.ValueResult.HALT;
                                    case BREAK:
                                }
                        }
                }
            }

            if (b0 != 0) {
                while ((b0 = p_302325_.readByte()) != 0) {
                    StringTag.skipString(p_302325_);
                    TagTypes.getType(b0).skip(p_302325_, p_302355_);
                }
            }

            return p_302352_.visitContainerEnd();
        }

        private static String readString(DataInput p_302484_, NbtAccounter p_302494_) throws IOException {
            String s = p_302484_.readUTF();
            p_302494_.accountBytes(28L);
            p_302494_.accountBytes(2L, (long)s.length());
            return s;
        }

        @Override
        public void skip(DataInput p_197444_, NbtAccounter p_302358_) throws IOException {
            p_302358_.pushDepth();

            byte b0;
            try {
                while ((b0 = p_197444_.readByte()) != 0) {
                    StringTag.skipString(p_197444_);
                    TagTypes.getType(b0).skip(p_197444_, p_302358_);
                }
            } finally {
                p_302358_.popDepth();
            }
        }

        @Override
        public String getName() {
            return "COMPOUND";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Compound";
        }
    };
    private final Map<String, Tag> tags;

    protected CompoundTag(Map<String, Tag> pTags) {
        this.tags = pTags;
    }

    public CompoundTag() {
        this(Maps.newHashMap());
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        for (String s : this.tags.keySet()) {
            Tag tag = this.tags.get(s);
            writeNamedTag(s, tag, pOutput);
        }

        pOutput.writeByte(0);
    }

    @Override
    public int sizeInBytes() {
        int i = 48;

        for (Entry<String, Tag> entry : this.tags.entrySet()) {
            i += 28 + 2 * entry.getKey().length();
            i += 36;
            i += entry.getValue().sizeInBytes();
        }

        return i;
    }

    public Set<String> getAllKeys() {
        return this.tags.keySet();
    }

    @Override
    public byte getId() {
        return 10;
    }

    @Override
    public TagType<CompoundTag> getType() {
        return TYPE;
    }

    public int size() {
        return this.tags.size();
    }

    @Nullable
    public Tag put(String pKey, Tag pValue) {
        if (pValue == null) throw new IllegalArgumentException("Invalid null NBT value with key " + pKey);
        return this.tags.put(pKey, pValue);
    }

    public void putByte(String pKey, byte pValue) {
        this.tags.put(pKey, ByteTag.valueOf(pValue));
    }

    public void putShort(String pKey, short pValue) {
        this.tags.put(pKey, ShortTag.valueOf(pValue));
    }

    public void putInt(String pKey, int pValue) {
        this.tags.put(pKey, IntTag.valueOf(pValue));
    }

    public void putLong(String pKey, long pValue) {
        this.tags.put(pKey, LongTag.valueOf(pValue));
    }

    public void putUUID(String pKey, UUID pValue) {
        this.tags.put(pKey, NbtUtils.createUUID(pValue));
    }

    public UUID getUUID(String pKey) {
        return NbtUtils.loadUUID(this.get(pKey));
    }

    public boolean hasUUID(String pKey) {
        Tag tag = this.get(pKey);
        return tag != null && tag.getType() == IntArrayTag.TYPE && ((IntArrayTag)tag).getAsIntArray().length == 4;
    }

    public void putFloat(String pKey, float pValue) {
        this.tags.put(pKey, FloatTag.valueOf(pValue));
    }

    public void putDouble(String pKey, double pValue) {
        this.tags.put(pKey, DoubleTag.valueOf(pValue));
    }

    public void putString(String pKey, String pValue) {
        this.tags.put(pKey, StringTag.valueOf(pValue));
    }

    public void putByteArray(String pKey, byte[] pValue) {
        this.tags.put(pKey, new ByteArrayTag(pValue));
    }

    public void putByteArray(String pKey, List<Byte> pValue) {
        this.tags.put(pKey, new ByteArrayTag(pValue));
    }

    public void putIntArray(String pKey, int[] pValue) {
        this.tags.put(pKey, new IntArrayTag(pValue));
    }

    public void putIntArray(String pKey, List<Integer> pValue) {
        this.tags.put(pKey, new IntArrayTag(pValue));
    }

    public void putLongArray(String pKey, long[] pValue) {
        this.tags.put(pKey, new LongArrayTag(pValue));
    }

    public void putLongArray(String pKey, List<Long> pValue) {
        this.tags.put(pKey, new LongArrayTag(pValue));
    }

    public void putBoolean(String pKey, boolean pValue) {
        this.tags.put(pKey, ByteTag.valueOf(pValue));
    }

    @Nullable
    public Tag get(String pKey) {
        return this.tags.get(pKey);
    }

    /**
     * Gets the byte identifier of the tag of the specified {@code key}, or {@code 0} if no tag exists for the {@code key}.
     */
    public byte getTagType(String pKey) {
        Tag tag = this.tags.get(pKey);
        return tag == null ? 0 : tag.getId();
    }

    public boolean contains(String pKey) {
        return this.tags.containsKey(pKey);
    }

    /**
     * Returns whether the tag of the specified {@code key} is a particular {@code tagType}. If the {@code tagType} is {@code 99}, all numeric tags will be checked against the type of the stored tag.
     */
    public boolean contains(String pKey, int pTagType) {
        int i = this.getTagType(pKey);
        if (i == pTagType) {
            return true;
        } else {
            return pTagType != 99 ? false : i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 6;
        }
    }

    public byte getByte(String pKey) {
        try {
            if (this.contains(pKey, 99)) {
                return ((NumericTag)this.tags.get(pKey)).getAsByte();
            }
        } catch (ClassCastException classcastexception) {
        }

        return 0;
    }

    public short getShort(String pKey) {
        try {
            if (this.contains(pKey, 99)) {
                return ((NumericTag)this.tags.get(pKey)).getAsShort();
            }
        } catch (ClassCastException classcastexception) {
        }

        return 0;
    }

    public int getInt(String pKey) {
        try {
            if (this.contains(pKey, 99)) {
                return ((NumericTag)this.tags.get(pKey)).getAsInt();
            }
        } catch (ClassCastException classcastexception) {
        }

        return 0;
    }

    public long getLong(String pKey) {
        try {
            if (this.contains(pKey, 99)) {
                return ((NumericTag)this.tags.get(pKey)).getAsLong();
            }
        } catch (ClassCastException classcastexception) {
        }

        return 0L;
    }

    public float getFloat(String pKey) {
        try {
            if (this.contains(pKey, 99)) {
                return ((NumericTag)this.tags.get(pKey)).getAsFloat();
            }
        } catch (ClassCastException classcastexception) {
        }

        return 0.0F;
    }

    public double getDouble(String pKey) {
        try {
            if (this.contains(pKey, 99)) {
                return ((NumericTag)this.tags.get(pKey)).getAsDouble();
            }
        } catch (ClassCastException classcastexception) {
        }

        return 0.0;
    }

    public String getString(String pKey) {
        try {
            if (this.contains(pKey, 8)) {
                return this.tags.get(pKey).getAsString();
            }
        } catch (ClassCastException classcastexception) {
        }

        return "";
    }

    public byte[] getByteArray(String pKey) {
        try {
            if (this.contains(pKey, 7)) {
                return ((ByteArrayTag)this.tags.get(pKey)).getAsByteArray();
            }
        } catch (ClassCastException classcastexception) {
            throw new ReportedException(this.createReport(pKey, ByteArrayTag.TYPE, classcastexception));
        }

        return new byte[0];
    }

    public int[] getIntArray(String pKey) {
        try {
            if (this.contains(pKey, 11)) {
                return ((IntArrayTag)this.tags.get(pKey)).getAsIntArray();
            }
        } catch (ClassCastException classcastexception) {
            throw new ReportedException(this.createReport(pKey, IntArrayTag.TYPE, classcastexception));
        }

        return new int[0];
    }

    public long[] getLongArray(String pKey) {
        try {
            if (this.contains(pKey, 12)) {
                return ((LongArrayTag)this.tags.get(pKey)).getAsLongArray();
            }
        } catch (ClassCastException classcastexception) {
            throw new ReportedException(this.createReport(pKey, LongArrayTag.TYPE, classcastexception));
        }

        return new long[0];
    }

    public CompoundTag getCompound(String pKey) {
        try {
            if (this.contains(pKey, 10)) {
                return (CompoundTag)this.tags.get(pKey);
            }
        } catch (ClassCastException classcastexception) {
            throw new ReportedException(this.createReport(pKey, TYPE, classcastexception));
        }

        return new CompoundTag();
    }

    public ListTag getList(String pKey, int pTagType) {
        try {
            if (this.getTagType(pKey) == 9) {
                ListTag listtag = (ListTag)this.tags.get(pKey);
                if (!listtag.isEmpty() && listtag.getElementType() != pTagType) {
                    return new ListTag();
                }

                return listtag;
            }
        } catch (ClassCastException classcastexception) {
            throw new ReportedException(this.createReport(pKey, ListTag.TYPE, classcastexception));
        }

        return new ListTag();
    }

    public boolean getBoolean(String pKey) {
        return this.getByte(pKey) != 0;
    }

    public void remove(String pKey) {
        this.tags.remove(pKey);
    }

    @Override
    public String toString() {
        return this.getAsString();
    }

    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    private CrashReport createReport(String pTagName, TagType<?> pType, ClassCastException pException) {
        CrashReport crashreport = CrashReport.forThrowable(pException, "Reading NBT data");
        CrashReportCategory crashreportcategory = crashreport.addCategory("Corrupt NBT tag", 1);
        crashreportcategory.setDetail("Tag type found", () -> this.tags.get(pTagName).getType().getName());
        crashreportcategory.setDetail("Tag type expected", pType::getName);
        crashreportcategory.setDetail("Tag name", pTagName);
        return crashreport;
    }

    protected CompoundTag shallowCopy() {
        return new CompoundTag(new HashMap<>(this.tags));
    }

    public CompoundTag copy() {
        Map<String, Tag> map = Maps.newHashMap(Maps.transformValues(this.tags, Tag::copy));
        return new CompoundTag(map);
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther ? true : pOther instanceof CompoundTag && Objects.equals(this.tags, ((CompoundTag)pOther).tags);
    }

    @Override
    public int hashCode() {
        return this.tags.hashCode();
    }

    private static void writeNamedTag(String pName, Tag pTag, DataOutput pOutput) throws IOException {
        pOutput.writeByte(pTag.getId());
        if (pTag.getId() != 0) {
            pOutput.writeUTF(pName);
            pTag.write(pOutput);
        }
    }

    static Tag readNamedTagData(TagType<?> pType, String pName, DataInput pInput, NbtAccounter pAccounter) {
        try {
            return pType.load(pInput, pAccounter);
        } catch (IOException ioexception) {
            CrashReport crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data");
            CrashReportCategory crashreportcategory = crashreport.addCategory("NBT Tag");
            crashreportcategory.setDetail("Tag name", pName);
            crashreportcategory.setDetail("Tag type", pType.getName());
            throw new ReportedNbtException(crashreport);
        }
    }

    /**
     * Copies all the tags of {@code other} into this tag, then returns itself.
     * @see #copy()
     */
    public CompoundTag merge(CompoundTag pOther) {
        for (String s : pOther.tags.keySet()) {
            Tag tag = pOther.tags.get(s);
            if (tag.getId() == 10) {
                if (this.contains(s, 10)) {
                    CompoundTag compoundtag = this.getCompound(s);
                    compoundtag.merge((CompoundTag)tag);
                } else {
                    this.put(s, tag.copy());
                }
            } else {
                this.put(s, tag.copy());
            }
        }

        return this;
    }

    @Override
    public void accept(TagVisitor pVisitor) {
        pVisitor.visitCompound(this);
    }

    protected Set<Entry<String, Tag>> entrySet() {
        return this.tags.entrySet();
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
        for (Entry<String, Tag> entry : this.tags.entrySet()) {
            Tag tag = entry.getValue();
            TagType<?> tagtype = tag.getType();
            StreamTagVisitor.EntryResult streamtagvisitor$entryresult = pVisitor.visitEntry(tagtype);
            switch (streamtagvisitor$entryresult) {
                case HALT:
                    return StreamTagVisitor.ValueResult.HALT;
                case BREAK:
                    return pVisitor.visitContainerEnd();
                case SKIP:
                    break;
                default:
                    streamtagvisitor$entryresult = pVisitor.visitEntry(tagtype, entry.getKey());
                    switch (streamtagvisitor$entryresult) {
                        case HALT:
                            return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                            return pVisitor.visitContainerEnd();
                        case SKIP:
                            break;
                        default:
                            StreamTagVisitor.ValueResult streamtagvisitor$valueresult = tag.accept(pVisitor);
                            switch (streamtagvisitor$valueresult) {
                                case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                case BREAK:
                                    return pVisitor.visitContainerEnd();
                            }
                    }
            }
        }

        return pVisitor.visitContainerEnd();
    }
}
