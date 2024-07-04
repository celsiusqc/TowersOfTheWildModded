package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.ListBuilder.Builder;
import com.mojang.serialization.RecordBuilder.MapBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A {@link DynamicOps} that delegates all functionality to an internal delegate. Comments and parameters here are copied from {@link DynamicOps} in DataFixerUpper.
 */
public abstract class DelegatingOps<T> implements DynamicOps<T> {
    protected final DynamicOps<T> delegate;

    protected DelegatingOps(DynamicOps<T> pDelegate) {
        this.delegate = pDelegate;
    }

    @Override
    public T empty() {
        return this.delegate.empty();
    }

    @Override
    public T emptyMap() {
        return this.delegate.emptyMap();
    }

    @Override
    public T emptyList() {
        return this.delegate.emptyList();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> pOutOps, T pInput) {
        return this.delegate.convertTo(pOutOps, pInput);
    }

    @Override
    public DataResult<Number> getNumberValue(T pInput) {
        return this.delegate.getNumberValue(pInput);
    }

    @Override
    public T createNumeric(Number pI) {
        return this.delegate.createNumeric(pI);
    }

    @Override
    public T createByte(byte pValue) {
        return this.delegate.createByte(pValue);
    }

    @Override
    public T createShort(short pValue) {
        return this.delegate.createShort(pValue);
    }

    @Override
    public T createInt(int pValue) {
        return this.delegate.createInt(pValue);
    }

    @Override
    public T createLong(long pValue) {
        return this.delegate.createLong(pValue);
    }

    @Override
    public T createFloat(float pValue) {
        return this.delegate.createFloat(pValue);
    }

    @Override
    public T createDouble(double pValue) {
        return this.delegate.createDouble(pValue);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(T pInput) {
        return this.delegate.getBooleanValue(pInput);
    }

    @Override
    public T createBoolean(boolean pValue) {
        return this.delegate.createBoolean(pValue);
    }

    @Override
    public DataResult<String> getStringValue(T pInput) {
        return this.delegate.getStringValue(pInput);
    }

    @Override
    public T createString(String pValue) {
        return this.delegate.createString(pValue);
    }

    /**
     * Only successful if first argument is a list/array or empty.
     */
    @Override
    public DataResult<T> mergeToList(T pList, T pValue) {
        return this.delegate.mergeToList(pList, pValue);
    }

    @Override
    public DataResult<T> mergeToList(T pList, List<T> pValues) {
        return this.delegate.mergeToList(pList, pValues);
    }

    /**
     * Only successful if first argument is a map or empty.
     */
    @Override
    public DataResult<T> mergeToMap(T pMap, T pKey, T pValue) {
        return this.delegate.mergeToMap(pMap, pKey, pValue);
    }

    @Override
    public DataResult<T> mergeToMap(T pMap, MapLike<T> pValues) {
        return this.delegate.mergeToMap(pMap, pValues);
    }

    @Override
    public DataResult<T> mergeToMap(T pMap, Map<T, T> pValues) {
        return this.delegate.mergeToMap(pMap, pValues);
    }

    @Override
    public DataResult<T> mergeToPrimitive(T pPrefix, T pValue) {
        return this.delegate.mergeToPrimitive(pPrefix, pValue);
    }

    @Override
    public DataResult<Stream<Pair<T, T>>> getMapValues(T pInput) {
        return this.delegate.getMapValues(pInput);
    }

    @Override
    public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T pInput) {
        return this.delegate.getMapEntries(pInput);
    }

    @Override
    public T createMap(Map<T, T> pMap) {
        return this.delegate.createMap(pMap);
    }

    @Override
    public T createMap(Stream<Pair<T, T>> pMap) {
        return this.delegate.createMap(pMap);
    }

    @Override
    public DataResult<MapLike<T>> getMap(T pInput) {
        return this.delegate.getMap(pInput);
    }

    @Override
    public DataResult<Stream<T>> getStream(T pInput) {
        return this.delegate.getStream(pInput);
    }

    @Override
    public DataResult<Consumer<Consumer<T>>> getList(T pInput) {
        return this.delegate.getList(pInput);
    }

    @Override
    public T createList(Stream<T> pInput) {
        return this.delegate.createList(pInput);
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(T pInput) {
        return this.delegate.getByteBuffer(pInput);
    }

    @Override
    public T createByteList(ByteBuffer pInput) {
        return this.delegate.createByteList(pInput);
    }

    @Override
    public DataResult<IntStream> getIntStream(T pInput) {
        return this.delegate.getIntStream(pInput);
    }

    @Override
    public T createIntList(IntStream pInput) {
        return this.delegate.createIntList(pInput);
    }

    @Override
    public DataResult<LongStream> getLongStream(T pInput) {
        return this.delegate.getLongStream(pInput);
    }

    @Override
    public T createLongList(LongStream pInput) {
        return this.delegate.createLongList(pInput);
    }

    @Override
    public T remove(T pInput, String pKey) {
        return this.delegate.remove(pInput, pKey);
    }

    @Override
    public boolean compressMaps() {
        return this.delegate.compressMaps();
    }

    @Override
    public ListBuilder<T> listBuilder() {
        return new Builder<>(this);
    }

    @Override
    public RecordBuilder<T> mapBuilder() {
        return new MapBuilder<>(this);
    }
}
