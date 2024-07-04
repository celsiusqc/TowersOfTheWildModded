package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;

public interface StringRepresentable {
    int PRE_BUILT_MAP_THRESHOLD = 16;

    String getSerializedName();

    static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> pElementsSupplier) {
        return fromEnumWithMapping(pElementsSupplier, p_304817_ -> p_304817_);
    }

    static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(
        Supplier<E[]> pEnumValues, Function<String, String> pKeyFunction
    ) {
        E[] ae = (E[])pEnumValues.get();
        Function<String, E> function = createNameLookup(ae, pKeyFunction);
        return new StringRepresentable.EnumCodec<>(ae, function);
    }

    static <T extends StringRepresentable> Codec<T> fromValues(Supplier<T[]> pValuesSupplier) {
        T[] at = (T[])pValuesSupplier.get();
        Function<String, T> function = createNameLookup(at, p_304333_ -> p_304333_);
        ToIntFunction<T> tointfunction = Util.createIndexLookup(Arrays.asList(at));
        return new StringRepresentable.StringRepresentableCodec<>(at, function, tointfunction);
    }

    static <T extends StringRepresentable> Function<String, T> createNameLookup(T[] pValues, Function<String, String> pKeyFunction) {
        if (pValues.length > 16) {
            Map<String, T> map = Arrays.<StringRepresentable>stream(pValues)
                .collect(Collectors.toMap(p_304335_ -> pKeyFunction.apply(p_304335_.getSerializedName()), p_304719_ -> (T)p_304719_));
            return p_304332_ -> p_304332_ == null ? null : map.get(p_304332_);
        } else {
            return p_304338_ -> {
                for (T t : pValues) {
                    if (pKeyFunction.apply(t.getSerializedName()).equals(p_304338_)) {
                        return t;
                    }
                }

                return null;
            };
        }
    }

    static Keyable keys(final StringRepresentable[] pSerializables) {
        return new Keyable() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> p_184758_) {
                return Arrays.stream(pSerializables).map(StringRepresentable::getSerializedName).map(p_184758_::createString);
            }
        };
    }

    @Deprecated
    public static class EnumCodec<E extends Enum<E> & StringRepresentable> extends StringRepresentable.StringRepresentableCodec<E> {
        private final Function<String, E> resolver;

        public EnumCodec(E[] pValues, Function<String, E> pResolver) {
            super(pValues, pResolver, p_216454_ -> p_216454_.ordinal());
            this.resolver = pResolver;
        }

        @Nullable
        public E byName(@Nullable String pName) {
            return this.resolver.apply(pName);
        }

        public E byName(@Nullable String pName, E pDefaultValue) {
            return Objects.requireNonNullElse(this.byName(pName), pDefaultValue);
        }
    }

    public static class StringRepresentableCodec<S extends StringRepresentable> implements Codec<S> {
        private final Codec<S> codec;

        public StringRepresentableCodec(S[] pValues, Function<String, S> pNameLookup, ToIntFunction<S> pIndexLookup) {
            this.codec = ExtraCodecs.orCompressed(
                Codec.stringResolver(StringRepresentable::getSerializedName, pNameLookup),
                ExtraCodecs.idResolverCodec(pIndexLookup, p_304986_ -> p_304986_ >= 0 && p_304986_ < pValues.length ? pValues[p_304986_] : null, -1)
            );
        }

        @Override
        public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> pOps, T pValue) {
            return this.codec.decode(pOps, pValue);
        }

        public <T> DataResult<T> encode(S pInput, DynamicOps<T> pOps, T pPrefix) {
            return this.codec.encode(pInput, pOps, pPrefix);
        }
    }
}
