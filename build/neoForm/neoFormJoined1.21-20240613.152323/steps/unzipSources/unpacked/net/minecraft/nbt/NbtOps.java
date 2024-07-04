package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class NbtOps implements DynamicOps<Tag> {
    public static final NbtOps INSTANCE = new NbtOps();
    private static final String WRAPPER_MARKER = "";

    protected NbtOps() {
    }

    public Tag empty() {
        return EndTag.INSTANCE;
    }

    public <U> U convertTo(DynamicOps<U> pOps, Tag pTag) {
        return (U)(switch (pTag.getId()) {
            case 0 -> (Object)pOps.empty();
            case 1 -> (Object)pOps.createByte(((NumericTag)pTag).getAsByte());
            case 2 -> (Object)pOps.createShort(((NumericTag)pTag).getAsShort());
            case 3 -> (Object)pOps.createInt(((NumericTag)pTag).getAsInt());
            case 4 -> (Object)pOps.createLong(((NumericTag)pTag).getAsLong());
            case 5 -> (Object)pOps.createFloat(((NumericTag)pTag).getAsFloat());
            case 6 -> (Object)pOps.createDouble(((NumericTag)pTag).getAsDouble());
            case 7 -> (Object)pOps.createByteList(ByteBuffer.wrap(((ByteArrayTag)pTag).getAsByteArray()));
            case 8 -> (Object)pOps.createString(pTag.getAsString());
            case 9 -> (Object)this.convertList(pOps, pTag);
            case 10 -> (Object)this.convertMap(pOps, pTag);
            case 11 -> (Object)pOps.createIntList(Arrays.stream(((IntArrayTag)pTag).getAsIntArray()));
            case 12 -> (Object)pOps.createLongList(Arrays.stream(((LongArrayTag)pTag).getAsLongArray()));
            default -> throw new IllegalStateException("Unknown tag type: " + pTag);
        });
    }

    public DataResult<Number> getNumberValue(Tag pTag) {
        return pTag instanceof NumericTag numerictag ? DataResult.success(numerictag.getAsNumber()) : DataResult.error(() -> "Not a number");
    }

    public Tag createNumeric(Number pData) {
        return DoubleTag.valueOf(pData.doubleValue());
    }

    public Tag createByte(byte pData) {
        return ByteTag.valueOf(pData);
    }

    public Tag createShort(short pData) {
        return ShortTag.valueOf(pData);
    }

    public Tag createInt(int pData) {
        return IntTag.valueOf(pData);
    }

    public Tag createLong(long pData) {
        return LongTag.valueOf(pData);
    }

    public Tag createFloat(float pData) {
        return FloatTag.valueOf(pData);
    }

    public Tag createDouble(double pData) {
        return DoubleTag.valueOf(pData);
    }

    public Tag createBoolean(boolean pData) {
        return ByteTag.valueOf(pData);
    }

    public DataResult<String> getStringValue(Tag pTag) {
        return pTag instanceof StringTag stringtag ? DataResult.success(stringtag.getAsString()) : DataResult.error(() -> "Not a string");
    }

    public Tag createString(String pData) {
        return StringTag.valueOf(pData);
    }

    public DataResult<Tag> mergeToList(Tag pList, Tag pTag) {
        return createCollector(pList)
            .map(p_248053_ -> DataResult.success(p_248053_.accept(pTag).result()))
            .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + pList, pList));
    }

    public DataResult<Tag> mergeToList(Tag pList, List<Tag> pTags) {
        return createCollector(pList)
            .map(p_248048_ -> DataResult.success(p_248048_.acceptAll(pTags).result()))
            .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + pList, pList));
    }

    public DataResult<Tag> mergeToMap(Tag pMap, Tag pKey, Tag pValue) {
        if (!(pMap instanceof CompoundTag) && !(pMap instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + pMap, pMap);
        } else if (!(pKey instanceof StringTag)) {
            return DataResult.error(() -> "key is not a string: " + pKey, pMap);
        } else {
            CompoundTag compoundtag = pMap instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            compoundtag.put(pKey.getAsString(), pValue);
            return DataResult.success(compoundtag);
        }
    }

    public DataResult<Tag> mergeToMap(Tag pMap, MapLike<Tag> pOtherMap) {
        if (!(pMap instanceof CompoundTag) && !(pMap instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + pMap, pMap);
        } else {
            CompoundTag compoundtag = pMap instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            List<Tag> list = new ArrayList<>();
            pOtherMap.entries().forEach(p_128994_ -> {
                Tag tag = p_128994_.getFirst();
                if (!(tag instanceof StringTag)) {
                    list.add(tag);
                } else {
                    compoundtag.put(tag.getAsString(), p_128994_.getSecond());
                }
            });
            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundtag) : DataResult.success(compoundtag);
        }
    }

    public DataResult<Tag> mergeToMap(Tag p_341945_, Map<Tag, Tag> p_341920_) {
        if (!(p_341945_ instanceof CompoundTag) && !(p_341945_ instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + p_341945_, p_341945_);
        } else {
            CompoundTag compoundtag = p_341945_ instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
            List<Tag> list = new ArrayList<>();

            for (Entry<Tag, Tag> entry : p_341920_.entrySet()) {
                Tag tag = entry.getKey();
                if (tag instanceof StringTag) {
                    compoundtag.put(tag.getAsString(), entry.getValue());
                } else {
                    list.add(tag);
                }
            }

            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundtag) : DataResult.success(compoundtag);
        }
    }

    public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag pMap) {
        return pMap instanceof CompoundTag compoundtag
            ? DataResult.success(compoundtag.entrySet().stream().map(p_341872_ -> (Pair<Tag, Tag>)Pair.of(this.createString(p_341872_.getKey()), p_341872_.getValue())))
            : DataResult.error(() -> "Not a map: " + pMap);
    }

    public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag pMap) {
        return pMap instanceof CompoundTag compoundtag ? DataResult.success(p_341867_ -> {
            for (Entry<String, Tag> entry : compoundtag.entrySet()) {
                p_341867_.accept(this.createString(entry.getKey()), entry.getValue());
            }
        }) : DataResult.error(() -> "Not a map: " + pMap);
    }

    public DataResult<MapLike<Tag>> getMap(Tag pMap) {
        return pMap instanceof CompoundTag compoundtag ? DataResult.success(new MapLike<Tag>() {
            @Nullable
            public Tag get(Tag p_129174_) {
                return compoundtag.get(p_129174_.getAsString());
            }

            @Nullable
            public Tag get(String p_129169_) {
                return compoundtag.get(p_129169_);
            }

            @Override
            public Stream<Pair<Tag, Tag>> entries() {
                return compoundtag.entrySet().stream().map(p_341873_ -> Pair.of(NbtOps.this.createString(p_341873_.getKey()), p_341873_.getValue()));
            }

            @Override
            public String toString() {
                return "MapLike[" + compoundtag + "]";
            }
        }) : DataResult.error(() -> "Not a map: " + pMap);
    }

    public Tag createMap(Stream<Pair<Tag, Tag>> pData) {
        CompoundTag compoundtag = new CompoundTag();
        pData.forEach(p_129018_ -> compoundtag.put(p_129018_.getFirst().getAsString(), p_129018_.getSecond()));
        return compoundtag;
    }

    private static Tag tryUnwrap(CompoundTag pTag) {
        if (pTag.size() == 1) {
            Tag tag = pTag.get("");
            if (tag != null) {
                return tag;
            }
        }

        return pTag;
    }

    public DataResult<Stream<Tag>> getStream(Tag pTag) {
        if (pTag instanceof ListTag listtag) {
            return listtag.getElementType() == 10
                ? DataResult.success(listtag.stream().map(p_248049_ -> tryUnwrap((CompoundTag)p_248049_)))
                : DataResult.success(listtag.stream());
        } else {
            return pTag instanceof CollectionTag<?> collectiontag
                ? DataResult.success(collectiontag.stream().map(p_129158_ -> p_129158_))
                : DataResult.error(() -> "Not a list");
        }
    }

    public DataResult<Consumer<Consumer<Tag>>> getList(Tag pTag) {
        if (pTag instanceof ListTag listtag) {
            return listtag.getElementType() == 10 ? DataResult.success(p_341869_ -> {
                for (Tag tag : listtag) {
                    p_341869_.accept(tryUnwrap((CompoundTag)tag));
                }
            }) : DataResult.success(listtag::forEach);
        } else {
            return pTag instanceof CollectionTag<?> collectiontag
                ? DataResult.success(collectiontag::forEach)
                : DataResult.error(() -> "Not a list: " + pTag);
        }
    }

    public DataResult<ByteBuffer> getByteBuffer(Tag pTag) {
        return pTag instanceof ByteArrayTag bytearraytag
            ? DataResult.success(ByteBuffer.wrap(bytearraytag.getAsByteArray()))
            : DynamicOps.super.getByteBuffer(pTag);
    }

    public Tag createByteList(ByteBuffer pData) {
        ByteBuffer bytebuffer = pData.duplicate().clear();
        byte[] abyte = new byte[pData.capacity()];
        bytebuffer.get(0, abyte, 0, abyte.length);
        return new ByteArrayTag(abyte);
    }

    public DataResult<IntStream> getIntStream(Tag pTag) {
        return pTag instanceof IntArrayTag intarraytag
            ? DataResult.success(Arrays.stream(intarraytag.getAsIntArray()))
            : DynamicOps.super.getIntStream(pTag);
    }

    public Tag createIntList(IntStream pData) {
        return new IntArrayTag(pData.toArray());
    }

    public DataResult<LongStream> getLongStream(Tag pTag) {
        return pTag instanceof LongArrayTag longarraytag
            ? DataResult.success(Arrays.stream(longarraytag.getAsLongArray()))
            : DynamicOps.super.getLongStream(pTag);
    }

    public Tag createLongList(LongStream pData) {
        return new LongArrayTag(pData.toArray());
    }

    public Tag createList(Stream<Tag> pData) {
        return NbtOps.InitialListCollector.INSTANCE.acceptAll(pData).result();
    }

    public Tag remove(Tag pMap, String pRemoveKey) {
        if (pMap instanceof CompoundTag compoundtag) {
            CompoundTag compoundtag1 = compoundtag.shallowCopy();
            compoundtag1.remove(pRemoveKey);
            return compoundtag1;
        } else {
            return pMap;
        }
    }

    @Override
    public String toString() {
        return "NBT";
    }

    @Override
    public RecordBuilder<Tag> mapBuilder() {
        return new NbtOps.NbtRecordBuilder();
    }

    private static Optional<NbtOps.ListCollector> createCollector(Tag pTag) {
        if (pTag instanceof EndTag) {
            return Optional.of(NbtOps.InitialListCollector.INSTANCE);
        } else {
            if (pTag instanceof CollectionTag<?> collectiontag) {
                if (collectiontag.isEmpty()) {
                    return Optional.of(NbtOps.InitialListCollector.INSTANCE);
                }

                if (collectiontag instanceof ListTag listtag) {
                    return switch (listtag.getElementType()) {
                        case 0 -> Optional.of(NbtOps.InitialListCollector.INSTANCE);
                        case 10 -> Optional.of(new NbtOps.HeterogenousListCollector(listtag));
                        default -> Optional.of(new NbtOps.HomogenousListCollector(listtag));
                    };
                }

                if (collectiontag instanceof ByteArrayTag bytearraytag) {
                    return Optional.of(new NbtOps.ByteListCollector(bytearraytag.getAsByteArray()));
                }

                if (collectiontag instanceof IntArrayTag intarraytag) {
                    return Optional.of(new NbtOps.IntListCollector(intarraytag.getAsIntArray()));
                }

                if (collectiontag instanceof LongArrayTag longarraytag) {
                    return Optional.of(new NbtOps.LongListCollector(longarraytag.getAsLongArray()));
                }
            }

            return Optional.empty();
        }
    }

    static class ByteListCollector implements NbtOps.ListCollector {
        private final ByteArrayList values = new ByteArrayList();

        public ByteListCollector(byte pValue) {
            this.values.add(pValue);
        }

        public ByteListCollector(byte[] pValues) {
            this.values.addElements(0, pValues);
        }

        @Override
        public NbtOps.ListCollector accept(Tag pTag) {
            if (pTag instanceof ByteTag bytetag) {
                this.values.add(bytetag.getAsByte());
                return this;
            } else {
                return new NbtOps.HeterogenousListCollector(this.values).accept(pTag);
            }
        }

        @Override
        public Tag result() {
            return new ByteArrayTag(this.values.toByteArray());
        }
    }

    static class HeterogenousListCollector implements NbtOps.ListCollector {
        private final ListTag result = new ListTag();

        public HeterogenousListCollector() {
        }

        public HeterogenousListCollector(Collection<Tag> pTags) {
            this.result.addAll(pTags);
        }

        public HeterogenousListCollector(IntArrayList pData) {
            pData.forEach(p_249166_ -> this.result.add(wrapElement(IntTag.valueOf(p_249166_))));
        }

        public HeterogenousListCollector(ByteArrayList pData) {
            pData.forEach(p_249160_ -> this.result.add(wrapElement(ByteTag.valueOf(p_249160_))));
        }

        public HeterogenousListCollector(LongArrayList pData) {
            pData.forEach(p_249754_ -> this.result.add(wrapElement(LongTag.valueOf(p_249754_))));
        }

        private static boolean isWrapper(CompoundTag pTag) {
            return pTag.size() == 1 && pTag.contains("");
        }

        private static Tag wrapIfNeeded(Tag pTag) {
            if (pTag instanceof CompoundTag compoundtag && !isWrapper(compoundtag)) {
                return compoundtag;
            }

            return wrapElement(pTag);
        }

        private static CompoundTag wrapElement(Tag pTag) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.put("", pTag);
            return compoundtag;
        }

        @Override
        public NbtOps.ListCollector accept(Tag pTag) {
            this.result.add(wrapIfNeeded(pTag));
            return this;
        }

        @Override
        public Tag result() {
            return this.result;
        }
    }

    static class HomogenousListCollector implements NbtOps.ListCollector {
        private final ListTag result = new ListTag();

        HomogenousListCollector(Tag pValue) {
            this.result.add(pValue);
        }

        HomogenousListCollector(ListTag pValues) {
            this.result.addAll(pValues);
        }

        @Override
        public NbtOps.ListCollector accept(Tag pTag) {
            if (pTag.getId() != this.result.getElementType()) {
                return new NbtOps.HeterogenousListCollector().acceptAll(this.result).accept(pTag);
            } else {
                this.result.add(pTag);
                return this;
            }
        }

        @Override
        public Tag result() {
            return this.result;
        }
    }

    static class InitialListCollector implements NbtOps.ListCollector {
        public static final NbtOps.InitialListCollector INSTANCE = new NbtOps.InitialListCollector();

        private InitialListCollector() {
        }

        @Override
        public NbtOps.ListCollector accept(Tag p_251635_) {
            if (p_251635_ instanceof CompoundTag compoundtag) {
                return new NbtOps.HeterogenousListCollector().accept(compoundtag);
            } else if (p_251635_ instanceof ByteTag bytetag) {
                return new NbtOps.ByteListCollector(bytetag.getAsByte());
            } else if (p_251635_ instanceof IntTag inttag) {
                return new NbtOps.IntListCollector(inttag.getAsInt());
            } else {
                return (NbtOps.ListCollector)(p_251635_ instanceof LongTag longtag
                    ? new NbtOps.LongListCollector(longtag.getAsLong())
                    : new NbtOps.HomogenousListCollector(p_251635_));
            }
        }

        @Override
        public Tag result() {
            return new ListTag();
        }
    }

    static class IntListCollector implements NbtOps.ListCollector {
        private final IntArrayList values = new IntArrayList();

        public IntListCollector(int pValue) {
            this.values.add(pValue);
        }

        public IntListCollector(int[] pValues) {
            this.values.addElements(0, pValues);
        }

        @Override
        public NbtOps.ListCollector accept(Tag pTag) {
            if (pTag instanceof IntTag inttag) {
                this.values.add(inttag.getAsInt());
                return this;
            } else {
                return new NbtOps.HeterogenousListCollector(this.values).accept(pTag);
            }
        }

        @Override
        public Tag result() {
            return new IntArrayTag(this.values.toIntArray());
        }
    }

    interface ListCollector {
        NbtOps.ListCollector accept(Tag pTag);

        default NbtOps.ListCollector acceptAll(Iterable<Tag> pTags) {
            NbtOps.ListCollector nbtops$listcollector = this;

            for (Tag tag : pTags) {
                nbtops$listcollector = nbtops$listcollector.accept(tag);
            }

            return nbtops$listcollector;
        }

        default NbtOps.ListCollector acceptAll(Stream<Tag> pTags) {
            return this.acceptAll(pTags::iterator);
        }

        Tag result();
    }

    static class LongListCollector implements NbtOps.ListCollector {
        private final LongArrayList values = new LongArrayList();

        public LongListCollector(long pValue) {
            this.values.add(pValue);
        }

        public LongListCollector(long[] pValues) {
            this.values.addElements(0, pValues);
        }

        @Override
        public NbtOps.ListCollector accept(Tag pTag) {
            if (pTag instanceof LongTag longtag) {
                this.values.add(longtag.getAsLong());
                return this;
            } else {
                return new NbtOps.HeterogenousListCollector(this.values).accept(pTag);
            }
        }

        @Override
        public Tag result() {
            return new LongArrayTag(this.values.toLongArray());
        }
    }

    class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
        protected NbtRecordBuilder() {
            super(NbtOps.this);
        }

        protected CompoundTag initBuilder() {
            return new CompoundTag();
        }

        protected CompoundTag append(String pKey, Tag pValue, CompoundTag pTag) {
            pTag.put(pKey, pValue);
            return pTag;
        }

        protected DataResult<Tag> build(CompoundTag p_129190_, Tag p_129191_) {
            if (p_129191_ == null || p_129191_ == EndTag.INSTANCE) {
                return DataResult.success(p_129190_);
            } else if (!(p_129191_ instanceof CompoundTag compoundtag)) {
                return DataResult.error(() -> "mergeToMap called with not a map: " + p_129191_, p_129191_);
            } else {
                CompoundTag compoundtag1 = compoundtag.shallowCopy();

                for (Entry<String, Tag> entry : p_129190_.entrySet()) {
                    compoundtag1.put(entry.getKey(), entry.getValue());
                }

                return DataResult.success(compoundtag1);
            }
        }
    }
}
