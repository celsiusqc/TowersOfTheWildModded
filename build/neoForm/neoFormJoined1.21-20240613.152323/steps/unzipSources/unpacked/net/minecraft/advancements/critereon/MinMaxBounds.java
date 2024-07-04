package net.minecraft.advancements.critereon;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public interface MinMaxBounds<T extends Number> {
    SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(Component.translatable("argument.range.empty"));
    SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType(Component.translatable("argument.range.swapped"));

    Optional<T> min();

    Optional<T> max();

    default boolean isAny() {
        return this.min().isEmpty() && this.max().isEmpty();
    }

    default Optional<T> unwrapPoint() {
        Optional<T> optional = this.min();
        Optional<T> optional1 = this.max();
        return optional.equals(optional1) ? optional : Optional.empty();
    }

    static <T extends Number, R extends MinMaxBounds<T>> Codec<R> createCodec(Codec<T> pCodec, MinMaxBounds.BoundsFactory<T, R> pBoundsFactory) {
        Codec<R> codec = RecordCodecBuilder.create(
            p_337383_ -> p_337383_.group(
                        pCodec.optionalFieldOf("min").forGetter(MinMaxBounds::min), pCodec.optionalFieldOf("max").forGetter(MinMaxBounds::max)
                    )
                    .apply(p_337383_, pBoundsFactory::create)
        );
        return Codec.either(codec, pCodec)
            .xmap(
                p_298558_ -> p_298558_.map(p_299210_ -> (R)p_299210_, p_298935_ -> pBoundsFactory.create(Optional.of((T)p_298935_), Optional.of((T)p_298935_))),
                p_298447_ -> {
                    Optional<T> optional = p_298447_.unwrapPoint();
                    return optional.isPresent() ? Either.right(optional.get()) : Either.left((R)p_298447_);
                }
            );
    }

    static <T extends Number, R extends MinMaxBounds<T>> R fromReader(
        StringReader pReader,
        MinMaxBounds.BoundsFromReaderFactory<T, R> pBoundedFactory,
        Function<String, T> pValueFactory,
        Supplier<DynamicCommandExceptionType> pCommandExceptionSupplier,
        Function<T, T> pFormatter
    ) throws CommandSyntaxException {
        if (!pReader.canRead()) {
            throw ERROR_EMPTY.createWithContext(pReader);
        } else {
            int i = pReader.getCursor();

            try {
                Optional<T> optional = readNumber(pReader, pValueFactory, pCommandExceptionSupplier).map(pFormatter);
                Optional<T> optional1;
                if (pReader.canRead(2) && pReader.peek() == '.' && pReader.peek(1) == '.') {
                    pReader.skip();
                    pReader.skip();
                    optional1 = readNumber(pReader, pValueFactory, pCommandExceptionSupplier).map(pFormatter);
                    if (optional.isEmpty() && optional1.isEmpty()) {
                        throw ERROR_EMPTY.createWithContext(pReader);
                    }
                } else {
                    optional1 = optional;
                }

                if (optional.isEmpty() && optional1.isEmpty()) {
                    throw ERROR_EMPTY.createWithContext(pReader);
                } else {
                    return pBoundedFactory.create(pReader, optional, optional1);
                }
            } catch (CommandSyntaxException commandsyntaxexception) {
                pReader.setCursor(i);
                throw new CommandSyntaxException(commandsyntaxexception.getType(), commandsyntaxexception.getRawMessage(), commandsyntaxexception.getInput(), i);
            }
        }
    }

    private static <T extends Number> Optional<T> readNumber(
        StringReader pReader, Function<String, T> pStringToValueFunction, Supplier<DynamicCommandExceptionType> pCommandExceptionSupplier
    ) throws CommandSyntaxException {
        int i = pReader.getCursor();

        while (pReader.canRead() && isAllowedInputChat(pReader)) {
            pReader.skip();
        }

        String s = pReader.getString().substring(i, pReader.getCursor());
        if (s.isEmpty()) {
            return Optional.empty();
        } else {
            try {
                return Optional.of(pStringToValueFunction.apply(s));
            } catch (NumberFormatException numberformatexception) {
                throw pCommandExceptionSupplier.get().createWithContext(pReader, s);
            }
        }
    }

    private static boolean isAllowedInputChat(StringReader pReader) {
        char c0 = pReader.peek();
        if ((c0 < '0' || c0 > '9') && c0 != '-') {
            return c0 != '.' ? false : !pReader.canRead(2) || pReader.peek(1) != '.';
        } else {
            return true;
        }
    }

    @FunctionalInterface
    public interface BoundsFactory<T extends Number, R extends MinMaxBounds<T>> {
        R create(Optional<T> pMin, Optional<T> pMax);
    }

    @FunctionalInterface
    public interface BoundsFromReaderFactory<T extends Number, R extends MinMaxBounds<T>> {
        R create(StringReader pReader, Optional<T> pMin, Optional<T> pMax) throws CommandSyntaxException;
    }

    public static record Doubles(Optional<Double> min, Optional<Double> max, Optional<Double> minSq, Optional<Double> maxSq) implements MinMaxBounds<Double> {
        public static final MinMaxBounds.Doubles ANY = new MinMaxBounds.Doubles(Optional.empty(), Optional.empty());
        public static final Codec<MinMaxBounds.Doubles> CODEC = MinMaxBounds.<Double, MinMaxBounds.Doubles>createCodec(Codec.DOUBLE, MinMaxBounds.Doubles::new);

        private Doubles(Optional<Double> p_298243_, Optional<Double> p_299159_) {
            this(p_298243_, p_299159_, squareOpt(p_298243_), squareOpt(p_299159_));
        }

        private static MinMaxBounds.Doubles create(StringReader p_154796_, Optional<Double> p_298478_, Optional<Double> p_298476_) throws CommandSyntaxException {
            if (p_298478_.isPresent() && p_298476_.isPresent() && p_298478_.get() > p_298476_.get()) {
                throw ERROR_SWAPPED.createWithContext(p_154796_);
            } else {
                return new MinMaxBounds.Doubles(p_298478_, p_298476_);
            }
        }

        private static Optional<Double> squareOpt(Optional<Double> pValue) {
            return pValue.map(p_297908_ -> p_297908_ * p_297908_);
        }

        public static MinMaxBounds.Doubles exactly(double pValue) {
            return new MinMaxBounds.Doubles(Optional.of(pValue), Optional.of(pValue));
        }

        public static MinMaxBounds.Doubles between(double pMin, double pMax) {
            return new MinMaxBounds.Doubles(Optional.of(pMin), Optional.of(pMax));
        }

        public static MinMaxBounds.Doubles atLeast(double pMin) {
            return new MinMaxBounds.Doubles(Optional.of(pMin), Optional.empty());
        }

        public static MinMaxBounds.Doubles atMost(double pMax) {
            return new MinMaxBounds.Doubles(Optional.empty(), Optional.of(pMax));
        }

        public boolean matches(double pValue) {
            return this.min.isPresent() && this.min.get() > pValue ? false : this.max.isEmpty() || !(this.max.get() < pValue);
        }

        public boolean matchesSqr(double pValue) {
            return this.minSq.isPresent() && this.minSq.get() > pValue ? false : this.maxSq.isEmpty() || !(this.maxSq.get() < pValue);
        }

        public static MinMaxBounds.Doubles fromReader(StringReader pReader) throws CommandSyntaxException {
            return fromReader(pReader, p_154807_ -> p_154807_);
        }

        public static MinMaxBounds.Doubles fromReader(StringReader pReader, Function<Double, Double> pFormatter) throws CommandSyntaxException {
            return MinMaxBounds.fromReader(
                pReader, MinMaxBounds.Doubles::create, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble, pFormatter
            );
        }
    }

    public static record Ints(Optional<Integer> min, Optional<Integer> max, Optional<Long> minSq, Optional<Long> maxSq) implements MinMaxBounds<Integer> {
        public static final MinMaxBounds.Ints ANY = new MinMaxBounds.Ints(Optional.empty(), Optional.empty());
        public static final Codec<MinMaxBounds.Ints> CODEC = MinMaxBounds.<Integer, MinMaxBounds.Ints>createCodec(Codec.INT, MinMaxBounds.Ints::new);

        private Ints(Optional<Integer> p_298275_, Optional<Integer> p_298272_) {
            this(p_298275_, p_298272_, p_298275_.map(p_297910_ -> p_297910_.longValue() * p_297910_.longValue()), squareOpt(p_298272_));
        }

        private static MinMaxBounds.Ints create(StringReader p_55378_, Optional<Integer> p_298250_, Optional<Integer> p_298579_) throws CommandSyntaxException {
            if (p_298250_.isPresent() && p_298579_.isPresent() && p_298250_.get() > p_298579_.get()) {
                throw ERROR_SWAPPED.createWithContext(p_55378_);
            } else {
                return new MinMaxBounds.Ints(p_298250_, p_298579_);
            }
        }

        private static Optional<Long> squareOpt(Optional<Integer> pValue) {
            return pValue.map(p_297909_ -> p_297909_.longValue() * p_297909_.longValue());
        }

        public static MinMaxBounds.Ints exactly(int pValue) {
            return new MinMaxBounds.Ints(Optional.of(pValue), Optional.of(pValue));
        }

        public static MinMaxBounds.Ints between(int pMin, int pMax) {
            return new MinMaxBounds.Ints(Optional.of(pMin), Optional.of(pMax));
        }

        public static MinMaxBounds.Ints atLeast(int pMin) {
            return new MinMaxBounds.Ints(Optional.of(pMin), Optional.empty());
        }

        public static MinMaxBounds.Ints atMost(int pMax) {
            return new MinMaxBounds.Ints(Optional.empty(), Optional.of(pMax));
        }

        public boolean matches(int pValue) {
            return this.min.isPresent() && this.min.get() > pValue ? false : this.max.isEmpty() || this.max.get() >= pValue;
        }

        public boolean matchesSqr(long pValue) {
            return this.minSq.isPresent() && this.minSq.get() > pValue ? false : this.maxSq.isEmpty() || this.maxSq.get() >= pValue;
        }

        public static MinMaxBounds.Ints fromReader(StringReader pReader) throws CommandSyntaxException {
            return fromReader(pReader, p_55389_ -> p_55389_);
        }

        public static MinMaxBounds.Ints fromReader(StringReader pReader, Function<Integer, Integer> pValueFunction) throws CommandSyntaxException {
            return MinMaxBounds.fromReader(
                pReader, MinMaxBounds.Ints::create, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt, pValueFunction
            );
        }
    }
}
