package net.minecraft.resources;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.util.ExtraCodecs;

public class RegistryOps<T> extends DelegatingOps<T> {
    private final RegistryOps.RegistryInfoLookup lookupProvider;

    public static <T> RegistryOps<T> create(DynamicOps<T> pDelegate, HolderLookup.Provider pRegistries) {
        return create(pDelegate, new RegistryOps.HolderLookupAdapter(pRegistries));
    }

    public static <T> RegistryOps<T> create(DynamicOps<T> pDelegate, RegistryOps.RegistryInfoLookup pLookupProvider) {
        return new RegistryOps<>(pDelegate, pLookupProvider);
    }

    public static <T> Dynamic<T> injectRegistryContext(Dynamic<T> pDynamic, HolderLookup.Provider pRegistries) {
        return new Dynamic<>(pRegistries.createSerializationContext(pDynamic.getOps()), pDynamic.getValue());
    }

    protected RegistryOps(DynamicOps<T> pDelegate, RegistryOps.RegistryInfoLookup pLookupProvider) {
        super(pDelegate);
        this.lookupProvider = pLookupProvider;
    }

    protected RegistryOps(RegistryOps<T> other) {
        super(other);
        this.lookupProvider = other.lookupProvider;
    }

    public <U> RegistryOps<U> withParent(DynamicOps<U> pOps) {
        return (RegistryOps<U>)(pOps == this.delegate ? this : new RegistryOps<>(pOps, this.lookupProvider));
    }

    public <E> Optional<HolderOwner<E>> owner(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
        return this.lookupProvider.lookup(pRegistryKey).map(RegistryOps.RegistryInfo::owner);
    }

    public <E> Optional<HolderGetter<E>> getter(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
        return this.lookupProvider.lookup(pRegistryKey).map(RegistryOps.RegistryInfo::getter);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (pOther != null && this.getClass() == pOther.getClass()) {
            RegistryOps<?> registryops = (RegistryOps<?>)pOther;
            return this.delegate.equals(registryops.delegate) && this.lookupProvider.equals(registryops.lookupProvider);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode() * 31 + this.lookupProvider.hashCode();
    }

    public static <E, O> RecordCodecBuilder<O, HolderGetter<E>> retrieveGetter(ResourceKey<? extends Registry<? extends E>> pRegistryOps) {
        return ExtraCodecs.retrieveContext(
                p_274811_ -> p_274811_ instanceof RegistryOps<?> registryops
                        ? registryops.lookupProvider
                            .lookup(pRegistryOps)
                            .map(p_255527_ -> DataResult.success(p_255527_.getter(), p_255527_.elementsLifecycle()))
                            .orElseGet(() -> DataResult.error(() -> "Unknown registry: " + pRegistryOps))
                        : DataResult.error(() -> "Not a registry ops")
            )
            .forGetter(p_255526_ -> null);
    }

    public static <E> com.mojang.serialization.MapCodec<HolderLookup.RegistryLookup<E>> retrieveRegistryLookup(ResourceKey<? extends Registry<? extends E>> resourceKey) {
        return ExtraCodecs.retrieveContext(ops -> {
            if (!(ops instanceof RegistryOps<?> registryOps))
                return DataResult.error(() -> "Not a registry ops");

            return registryOps.lookupProvider.lookup(resourceKey).map(registryInfo -> {
                if (!(registryInfo.owner() instanceof HolderLookup.RegistryLookup<E> registryLookup))
                    return DataResult.<HolderLookup.RegistryLookup<E>>error(() -> "Found holder getter but was not a registry lookup for " + resourceKey);

                return DataResult.success(registryLookup, registryInfo.elementsLifecycle());
            }).orElseGet(() -> DataResult.error(() -> "Unknown registry: " + resourceKey));
        });
    }

    public static <E, O> RecordCodecBuilder<O, Holder.Reference<E>> retrieveElement(ResourceKey<E> pKey) {
        ResourceKey<? extends Registry<E>> resourcekey = ResourceKey.createRegistryKey(pKey.registry());
        return ExtraCodecs.retrieveContext(
                p_274808_ -> p_274808_ instanceof RegistryOps<?> registryops
                        ? registryops.lookupProvider
                            .lookup(resourcekey)
                            .flatMap(p_255518_ -> p_255518_.getter().get(pKey))
                            .map(DataResult::success)
                            .orElseGet(() -> DataResult.error(() -> "Can't find value: " + pKey))
                        : DataResult.error(() -> "Not a registry ops")
            )
            .forGetter(p_255524_ -> null);
    }

    static final class HolderLookupAdapter implements RegistryOps.RegistryInfoLookup {
        private final HolderLookup.Provider lookupProvider;
        private final Map<ResourceKey<? extends Registry<?>>, Optional<? extends RegistryOps.RegistryInfo<?>>> lookups = new ConcurrentHashMap<>();

        public HolderLookupAdapter(HolderLookup.Provider pLookupProvider) {
            this.lookupProvider = pLookupProvider;
        }

        @Override
        public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
            return (Optional<RegistryOps.RegistryInfo<E>>)this.lookups.computeIfAbsent(pRegistryKey, this::createLookup);
        }

        private Optional<RegistryOps.RegistryInfo<Object>> createLookup(ResourceKey<? extends Registry<?>> p_341910_) {
            return this.lookupProvider.lookup(p_341910_).map(RegistryOps.RegistryInfo::fromRegistryLookup);
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else {
                if (pOther instanceof RegistryOps.HolderLookupAdapter registryops$holderlookupadapter
                    && this.lookupProvider.equals(registryops$holderlookupadapter.lookupProvider)) {
                    return true;
                }

                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.lookupProvider.hashCode();
        }
    }

    public static record RegistryInfo<T>(HolderOwner<T> owner, HolderGetter<T> getter, Lifecycle elementsLifecycle) {
        public static <T> RegistryOps.RegistryInfo<T> fromRegistryLookup(HolderLookup.RegistryLookup<T> pRegistryLookup) {
            return new RegistryOps.RegistryInfo<>(pRegistryLookup, pRegistryLookup, pRegistryLookup.registryLifecycle());
        }
    }

    public interface RegistryInfoLookup {
        <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> pRegistryKey);
    }
}
