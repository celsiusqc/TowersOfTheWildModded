package net.minecraft.core.component;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

public interface DataComponentMap extends Iterable<TypedDataComponent<?>> {
    DataComponentMap EMPTY = new DataComponentMap() {
        @Nullable
        @Override
        public <T> T get(DataComponentType<? extends T> p_331168_) {
            return null;
        }

        @Override
        public Set<DataComponentType<?>> keySet() {
            return Set.of();
        }

        @Override
        public Iterator<TypedDataComponent<?>> iterator() {
            return Collections.emptyIterator();
        }
    };
    Codec<DataComponentMap> CODEC = makeCodecFromMap(DataComponentType.VALUE_MAP_CODEC);

    static Codec<DataComponentMap> makeCodec(Codec<DataComponentType<?>> p_345883_) {
        return makeCodecFromMap(Codec.dispatchedMap(p_345883_, DataComponentType::codecOrThrow));
    }

    static Codec<DataComponentMap> makeCodecFromMap(Codec<Map<DataComponentType<?>, Object>> p_346302_) {
        return p_346302_.flatComapMap(DataComponentMap.Builder::buildFromMapTrusted, p_337448_ -> {
            int i = p_337448_.size();
            if (i == 0) {
                return DataResult.success(Reference2ObjectMaps.emptyMap());
            } else {
                Reference2ObjectMap<DataComponentType<?>, Object> reference2objectmap = new Reference2ObjectArrayMap<>(i);

                for (TypedDataComponent<?> typeddatacomponent : p_337448_) {
                    if (!typeddatacomponent.type().isTransient()) {
                        reference2objectmap.put(typeddatacomponent.type(), typeddatacomponent.value());
                    }
                }

                return DataResult.success(reference2objectmap);
            }
        });
    }

    static DataComponentMap composite(final DataComponentMap pMap1, final DataComponentMap pMap2) {
        return new DataComponentMap() {
            @Nullable
            @Override
            public <T> T get(DataComponentType<? extends T> p_330291_) {
                T t = pMap2.get(p_330291_);
                return t != null ? t : pMap1.get(p_330291_);
            }

            @Override
            public Set<DataComponentType<?>> keySet() {
                return Sets.union(pMap1.keySet(), pMap2.keySet());
            }
        };
    }

    static DataComponentMap.Builder builder() {
        return new DataComponentMap.Builder();
    }

    @Nullable
    <T> T get(DataComponentType<? extends T> pComponent);

    Set<DataComponentType<?>> keySet();

    default boolean has(DataComponentType<?> pComponent) {
        return this.get(pComponent) != null;
    }

    default <T> T getOrDefault(DataComponentType<? extends T> pComponent, T pDefaultValue) {
        T t = this.get(pComponent);
        return t != null ? t : pDefaultValue;
    }

    @Nullable
    default <T> TypedDataComponent<T> getTyped(DataComponentType<T> pComponent) {
        T t = this.get(pComponent);
        return t != null ? new TypedDataComponent<>(pComponent, t) : null;
    }

    @Override
    default Iterator<TypedDataComponent<?>> iterator() {
        return Iterators.transform(this.keySet().iterator(), p_330954_ -> Objects.requireNonNull(this.getTyped((DataComponentType<?>)p_330954_)));
    }

    default Stream<TypedDataComponent<?>> stream() {
        return StreamSupport.stream(Spliterators.spliterator(this.iterator(), (long)this.size(), 1345), false);
    }

    default int size() {
        return this.keySet().size();
    }

    default boolean isEmpty() {
        return this.size() == 0;
    }

    default DataComponentMap filter(final Predicate<DataComponentType<?>> pPredicate) {
        return new DataComponentMap() {
            @Nullable
            @Override
            public <T> T get(DataComponentType<? extends T> p_341052_) {
                return pPredicate.test(p_341052_) ? DataComponentMap.this.get(p_341052_) : null;
            }

            @Override
            public Set<DataComponentType<?>> keySet() {
                return Sets.filter(DataComponentMap.this.keySet(), pPredicate::test);
            }
        };
    }

    public static class Builder implements net.neoforged.neoforge.common.extensions.IDataComponentMapBuilderExtensions {
        private final Reference2ObjectMap<DataComponentType<?>, Object> map = new Reference2ObjectArrayMap<>();

        Builder() {
        }

        public <T> DataComponentMap.Builder set(DataComponentType<T> pComponent, @Nullable T pValue) {
            this.setUnchecked(pComponent, pValue);
            return this;
        }

        <T> void setUnchecked(DataComponentType<T> pComponent, @Nullable Object pValue) {
            if (pValue != null) {
                this.map.put(pComponent, pValue);
            } else {
                this.map.remove(pComponent);
            }
        }

        public DataComponentMap.Builder addAll(DataComponentMap pComponents) {
            for (TypedDataComponent<?> typeddatacomponent : pComponents) {
                this.map.put(typeddatacomponent.type(), typeddatacomponent.value());
            }

            return this;
        }

        public DataComponentMap build() {
            return buildFromMapTrusted(this.map);
        }

        private static DataComponentMap buildFromMapTrusted(Map<DataComponentType<?>, Object> pMap) {
            if (pMap.isEmpty()) {
                return DataComponentMap.EMPTY;
            } else {
                return pMap.size() < 8
                    ? new DataComponentMap.Builder.SimpleMap(new Reference2ObjectArrayMap<>(pMap))
                    : new DataComponentMap.Builder.SimpleMap(new Reference2ObjectOpenHashMap<>(pMap));
            }
        }

        static record SimpleMap(Reference2ObjectMap<DataComponentType<?>, Object> map) implements DataComponentMap {
            @Nullable
            @Override
            public <T> T get(DataComponentType<? extends T> p_331063_) {
                return (T)this.map.get(p_331063_);
            }

            @Override
            public boolean has(DataComponentType<?> p_331343_) {
                return this.map.containsKey(p_331343_);
            }

            @Override
            public Set<DataComponentType<?>> keySet() {
                return this.map.keySet();
            }

            @Override
            public Iterator<TypedDataComponent<?>> iterator() {
                return Iterators.transform(Reference2ObjectMaps.fastIterator(this.map), TypedDataComponent::fromEntryUnchecked);
            }

            @Override
            public int size() {
                return this.map.size();
            }

            @Override
            public String toString() {
                return this.map.toString();
            }
        }
    }
}
