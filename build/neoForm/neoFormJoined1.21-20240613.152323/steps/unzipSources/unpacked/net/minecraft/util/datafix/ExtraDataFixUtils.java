package net.minecraft.util.datafix;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ExtraDataFixUtils {
    public static Dynamic<?> fixBlockPos(Dynamic<?> pData) {
        Optional<Number> optional = pData.get("X").asNumber().result();
        Optional<Number> optional1 = pData.get("Y").asNumber().result();
        Optional<Number> optional2 = pData.get("Z").asNumber().result();
        return !optional.isEmpty() && !optional1.isEmpty() && !optional2.isEmpty()
            ? pData.createIntList(IntStream.of(optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue()))
            : pData;
    }

    public static <T, R> Typed<R> cast(Type<R> pType, Typed<T> pData) {
        return new Typed<>(pType, pData.getOps(), (R)pData.getValue());
    }

    @SafeVarargs
    public static <T> Function<Typed<?>, Typed<?>> chainAllFilters(Function<Typed<?>, Typed<?>>... p_344769_) {
        return p_345927_ -> {
            for (Function<Typed<?>, Typed<?>> function : p_344769_) {
                p_345927_ = function.apply(p_345927_);
            }

            return p_345927_;
        };
    }
}
