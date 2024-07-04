package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractUniversalBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class NullOps implements DynamicOps<Unit> {
    public static final NullOps INSTANCE = new NullOps();

    private NullOps() {
    }

    public <U> U convertTo(DynamicOps<U> pOps, Unit pUnit) {
        return pOps.empty();
    }

    public Unit empty() {
        return Unit.INSTANCE;
    }

    public Unit emptyMap() {
        return Unit.INSTANCE;
    }

    public Unit emptyList() {
        return Unit.INSTANCE;
    }

    public Unit createNumeric(Number pValue) {
        return Unit.INSTANCE;
    }

    public Unit createByte(byte pValue) {
        return Unit.INSTANCE;
    }

    public Unit createShort(short pValue) {
        return Unit.INSTANCE;
    }

    public Unit createInt(int pValue) {
        return Unit.INSTANCE;
    }

    public Unit createLong(long pValue) {
        return Unit.INSTANCE;
    }

    public Unit createFloat(float pValue) {
        return Unit.INSTANCE;
    }

    public Unit createDouble(double pValue) {
        return Unit.INSTANCE;
    }

    public Unit createBoolean(boolean pValue) {
        return Unit.INSTANCE;
    }

    public Unit createString(String pValue) {
        return Unit.INSTANCE;
    }

    public DataResult<Number> getNumberValue(Unit pInput) {
        return DataResult.error(() -> "Not a number");
    }

    public DataResult<Boolean> getBooleanValue(Unit pInput) {
        return DataResult.error(() -> "Not a boolean");
    }

    public DataResult<String> getStringValue(Unit pInput) {
        return DataResult.error(() -> "Not a string");
    }

    public DataResult<Unit> mergeToList(Unit pList, Unit pValue) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Unit> mergeToList(Unit pList, List<Unit> pValues) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Unit> mergeToMap(Unit pMap, Unit pKey, Unit pValue) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Unit> mergeToMap(Unit pMap, Map<Unit, Unit> pValues) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Unit> mergeToMap(Unit pMap, MapLike<Unit> pValues) {
        return DataResult.success(Unit.INSTANCE);
    }

    public DataResult<Stream<Pair<Unit, Unit>>> getMapValues(Unit pInput) {
        return DataResult.error(() -> "Not a map");
    }

    public DataResult<Consumer<BiConsumer<Unit, Unit>>> getMapEntries(Unit pInput) {
        return DataResult.error(() -> "Not a map");
    }

    public DataResult<MapLike<Unit>> getMap(Unit pInput) {
        return DataResult.error(() -> "Not a map");
    }

    public DataResult<Stream<Unit>> getStream(Unit pInput) {
        return DataResult.error(() -> "Not a list");
    }

    public DataResult<Consumer<Consumer<Unit>>> getList(Unit pInput) {
        return DataResult.error(() -> "Not a list");
    }

    public DataResult<ByteBuffer> getByteBuffer(Unit pInput) {
        return DataResult.error(() -> "Not a byte list");
    }

    public DataResult<IntStream> getIntStream(Unit pInput) {
        return DataResult.error(() -> "Not an int list");
    }

    public DataResult<LongStream> getLongStream(Unit pInput) {
        return DataResult.error(() -> "Not a long list");
    }

    public Unit createMap(Stream<Pair<Unit, Unit>> pMap) {
        return Unit.INSTANCE;
    }

    public Unit createMap(Map<Unit, Unit> pMap) {
        return Unit.INSTANCE;
    }

    public Unit createList(Stream<Unit> pInput) {
        return Unit.INSTANCE;
    }

    public Unit createByteList(ByteBuffer pInput) {
        return Unit.INSTANCE;
    }

    public Unit createIntList(IntStream pInput) {
        return Unit.INSTANCE;
    }

    public Unit createLongList(LongStream pInput) {
        return Unit.INSTANCE;
    }

    public Unit remove(Unit pInput, String pKey) {
        return pInput;
    }

    @Override
    public RecordBuilder<Unit> mapBuilder() {
        return new NullOps.NullMapBuilder(this);
    }

    @Override
    public String toString() {
        return "Null";
    }

    static final class NullMapBuilder extends AbstractUniversalBuilder<Unit, Unit> {
        public NullMapBuilder(DynamicOps<Unit> pOps) {
            super(pOps);
        }

        protected Unit initBuilder() {
            return Unit.INSTANCE;
        }

        protected Unit append(Unit p_341121_, Unit p_341327_, Unit p_341036_) {
            return p_341036_;
        }

        protected DataResult<Unit> build(Unit p_341068_, Unit p_341207_) {
            return DataResult.success(p_341207_);
        }
    }
}
