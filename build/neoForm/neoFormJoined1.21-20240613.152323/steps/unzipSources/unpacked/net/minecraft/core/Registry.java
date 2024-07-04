package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public interface Registry<T> extends Keyable, IdMap<T>, net.neoforged.neoforge.registries.IRegistryExtension<T> {
    ResourceKey<? extends Registry<T>> key();

    default Codec<T> byNameCodec() {
        return this.referenceHolderWithLifecycle()
            .flatComapMap(Holder.Reference::value, p_325515_ -> this.safeCastToReference(this.wrapAsHolder((T)p_325515_)));
    }

    default Codec<Holder<T>> holderByNameCodec() {
        return this.referenceHolderWithLifecycle().flatComapMap(p_325516_ -> (Holder<T>)p_325516_, this::safeCastToReference);
    }

    private Codec<Holder.Reference<T>> referenceHolderWithLifecycle() {
        Codec<Holder.Reference<T>> codec = ResourceLocation.CODEC
            .comapFlatMap(
                p_315852_ -> this.getHolder(p_315852_)
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + p_315852_)),
                p_325513_ -> p_325513_.key().location()
            );
        return ExtraCodecs.overrideLifecycle(
            codec, p_325514_ -> this.registrationInfo(p_325514_.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental())
        );
    }

    private DataResult<Holder.Reference<T>> safeCastToReference(Holder<T> p_326365_) {
        return p_326365_.getDelegate() instanceof Holder.Reference reference
            ? DataResult.success(reference)
            : DataResult.error(() -> "Unregistered holder in " + this.key() + ": " + p_326365_);
    }

    @Override
    default <U> Stream<U> keys(DynamicOps<U> pOps) {
        return this.keySet().stream().map(p_235784_ -> pOps.createString(p_235784_.toString()));
    }

    /**
     * @return the name used to identify the given object within this registry or {@code null} if the object is not within this registry
     */
    @Nullable
    ResourceLocation getKey(T pValue);

    Optional<ResourceKey<T>> getResourceKey(T pValue);

    /**
     * @return the integer ID used to identify the given object
     */
    @Override
    int getId(@Nullable T pValue);

    @Nullable
    T get(@Nullable ResourceKey<T> pKey);

    @Nullable
    T get(@Nullable ResourceLocation pName);

    Optional<RegistrationInfo> registrationInfo(ResourceKey<T> pKey);

    Lifecycle registryLifecycle();

    default Optional<T> getOptional(@Nullable ResourceLocation pName) {
        return Optional.ofNullable(this.get(pName));
    }

    default Optional<T> getOptional(@Nullable ResourceKey<T> pRegistryKey) {
        return Optional.ofNullable(this.get(pRegistryKey));
    }

    Optional<Holder.Reference<T>> getAny();

    default T getOrThrow(ResourceKey<T> pKey) {
        T t = this.get(pKey);
        if (t == null) {
            throw new IllegalStateException("Missing key in " + this.key() + ": " + pKey);
        } else {
            return t;
        }
    }

    Set<ResourceLocation> keySet();

    Set<Entry<ResourceKey<T>, T>> entrySet();

    Set<ResourceKey<T>> registryKeySet();

    Optional<Holder.Reference<T>> getRandom(RandomSource pRandom);

    default Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    boolean containsKey(ResourceLocation pName);

    boolean containsKey(ResourceKey<T> pKey);

    static <T> T register(Registry<? super T> pRegistry, String pName, T pValue) {
        return register(pRegistry, ResourceLocation.parse(pName), pValue);
    }

    static <V, T extends V> T register(Registry<V> pRegistry, ResourceLocation pName, T pValue) {
        return register(pRegistry, ResourceKey.create(pRegistry.key(), pName), pValue);
    }

    static <V, T extends V> T register(Registry<V> pRegistry, ResourceKey<V> pKey, T pValue) {
        ((WritableRegistry)pRegistry).register(pKey, (V)pValue, RegistrationInfo.BUILT_IN);
        return pValue;
    }

    static <T> Holder.Reference<T> registerForHolder(Registry<T> pRegistry, ResourceKey<T> pKey, T pValue) {
        return ((WritableRegistry)pRegistry).register(pKey, pValue, RegistrationInfo.BUILT_IN);
    }

    static <T> Holder.Reference<T> registerForHolder(Registry<T> pRegistry, ResourceLocation pName, T pValue) {
        return registerForHolder(pRegistry, ResourceKey.create(pRegistry.key(), pName), pValue);
    }

    Registry<T> freeze();

    Holder.Reference<T> createIntrusiveHolder(T pValue);

    Optional<Holder.Reference<T>> getHolder(int pId);

    Optional<Holder.Reference<T>> getHolder(ResourceLocation pLocation);

    Optional<Holder.Reference<T>> getHolder(ResourceKey<T> pKey);

    Holder<T> wrapAsHolder(T pValue);

    default Holder.Reference<T> getHolderOrThrow(ResourceKey<T> pKey) {
        return this.getHolder(pKey).orElseThrow(() -> new IllegalStateException("Missing key in " + this.key() + ": " + pKey));
    }

    Stream<Holder.Reference<T>> holders();

    Optional<HolderSet.Named<T>> getTag(TagKey<T> pKey);

    default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> pKey) {
        return DataFixUtils.orElse(this.getTag(pKey), List.of());
    }

    default Optional<Holder<T>> getRandomElementOf(TagKey<T> pKey, RandomSource pRandom) {
        return this.getTag(pKey).flatMap(p_319421_ -> p_319421_.getRandomElement(pRandom));
    }

    HolderSet.Named<T> getOrCreateTag(TagKey<T> pKey);

    Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags();

    Stream<TagKey<T>> getTagNames();

    void resetTags();

    void bindTags(Map<TagKey<T>, List<Holder<T>>> pTagMap);

    default IdMap<Holder<T>> asHolderIdMap() {
        return new IdMap<Holder<T>>() {
            public int getId(Holder<T> p_259992_) {
                return Registry.this.getId(p_259992_.value());
            }

            @Nullable
            public Holder<T> byId(int p_259972_) {
                return (Holder<T>)Registry.this.getHolder(p_259972_).orElse(null);
            }

            @Override
            public int size() {
                return Registry.this.size();
            }

            @Override
            public Iterator<Holder<T>> iterator() {
                return Registry.this.holders().map(p_260061_ -> (Holder<T>)p_260061_).iterator();
            }
        };
    }

    HolderOwner<T> holderOwner();

    HolderLookup.RegistryLookup<T> asLookup();

    default HolderLookup.RegistryLookup<T> asTagAddingLookup() {
        return new HolderLookup.RegistryLookup.Delegate<T>() {
            @Override
            public HolderLookup.RegistryLookup<T> parent() {
                return Registry.this.asLookup();
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> p_259111_) {
                return Optional.of(this.getOrThrow(p_259111_));
            }

            @Override
            public HolderSet.Named<T> getOrThrow(TagKey<T> p_259653_) {
                return Registry.this.getOrCreateTag(p_259653_);
            }
        };
    }
}
