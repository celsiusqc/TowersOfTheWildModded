package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionCountsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {
    List<CollectionCountsPredicate.Entry<T, P>> unpack();

    static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate<T, P>> codec(Codec<P> pTestCodec) {
        return CollectionCountsPredicate.Entry.<T, P>codec(pTestCodec).listOf().xmap(CollectionCountsPredicate::of, CollectionCountsPredicate::unpack);
    }

    @SafeVarargs
    static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(CollectionCountsPredicate.Entry<T, P>... pEntries) {
        return of(List.of(pEntries));
    }

    static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(List<CollectionCountsPredicate.Entry<T, P>> p_341102_) {
        return (CollectionCountsPredicate<T, P>)(switch (p_341102_.size()) {
            case 0 -> new CollectionCountsPredicate.Zero();
            case 1 -> new CollectionCountsPredicate.Single(p_341102_.getFirst());
            default -> new CollectionCountsPredicate.Multiple(p_341102_);
        });
    }

    public static record Entry<T, P extends Predicate<T>>(P test, MinMaxBounds.Ints count) {
        public static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate.Entry<T, P>> codec(Codec<P> pTestCodec) {
            return RecordCodecBuilder.create(
                p_340986_ -> p_340986_.group(
                            pTestCodec.fieldOf("test").forGetter(CollectionCountsPredicate.Entry::test),
                            MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(CollectionCountsPredicate.Entry::count)
                        )
                        .apply(p_340986_, CollectionCountsPredicate.Entry::new)
            );
        }

        public boolean test(Iterable<T> pCollection) {
            int i = 0;

            for (T t : pCollection) {
                if (this.test.test(t)) {
                    i++;
                }
            }

            return this.count.matches(i);
        }
    }

    public static record Multiple<T, P extends Predicate<T>>(List<CollectionCountsPredicate.Entry<T, P>> entries) implements CollectionCountsPredicate<T, P> {
        public boolean test(Iterable<T> pCollection) {
            for (CollectionCountsPredicate.Entry<T, P> entry : this.entries) {
                if (!entry.test(pCollection)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
            return this.entries;
        }
    }

    public static record Single<T, P extends Predicate<T>>(CollectionCountsPredicate.Entry<T, P> entry) implements CollectionCountsPredicate<T, P> {
        public boolean test(Iterable<T> pCollection) {
            return this.entry.test(pCollection);
        }

        @Override
        public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
            return List.of(this.entry);
        }
    }

    public static class Zero<T, P extends Predicate<T>> implements CollectionCountsPredicate<T, P> {
        public boolean test(Iterable<T> pCollection) {
            return true;
        }

        @Override
        public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
            return List.of();
        }
    }
}
