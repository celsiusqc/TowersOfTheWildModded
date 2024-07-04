package net.minecraft.util.random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface WeightedEntry {
    Weight getWeight();

    static <T> WeightedEntry.Wrapper<T> wrap(T pData, int pWeight) {
        return new WeightedEntry.Wrapper<>(pData, Weight.of(pWeight));
    }

    public static class IntrusiveBase implements WeightedEntry {
        private final Weight weight;

        public IntrusiveBase(int pWeight) {
            this.weight = Weight.of(pWeight);
        }

        public IntrusiveBase(Weight pWeight) {
            this.weight = pWeight;
        }

        @Override
        public Weight getWeight() {
            return this.weight;
        }
    }

    public static record Wrapper<T>(T data, Weight weight) implements WeightedEntry {
        @Override
        public Weight getWeight() {
            return this.weight;
        }

        public static <E> Codec<WeightedEntry.Wrapper<E>> codec(Codec<E> pElementCodec) {
            return RecordCodecBuilder.create(
                p_146309_ -> p_146309_.group(
                            pElementCodec.fieldOf("data").forGetter((Function<WeightedEntry.Wrapper<E>, E>)(WeightedEntry.Wrapper::data)),
                            Weight.CODEC.fieldOf("weight").forGetter(WeightedEntry.Wrapper::weight)
                        )
                        .apply(p_146309_, (BiFunction<E, Weight, WeightedEntry.Wrapper<E>>)(WeightedEntry.Wrapper::new))
            );
        }
    }
}
