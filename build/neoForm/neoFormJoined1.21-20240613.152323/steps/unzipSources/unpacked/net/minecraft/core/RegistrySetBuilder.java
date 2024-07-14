package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;

public class RegistrySetBuilder {
    private final List<RegistrySetBuilder.RegistryStub<?>> entries = new ArrayList<>();

    static <T> HolderGetter<T> wrapContextLookup(final HolderLookup.RegistryLookup<T> pOwner) {
        return new RegistrySetBuilder.EmptyTagLookup<T>(pOwner) {
            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> p_255765_) {
                return pOwner.get(p_255765_);
            }
        };
    }

    static <T> HolderLookup.RegistryLookup<T> lookupFromMap(
        final ResourceKey<? extends Registry<? extends T>> pRegistryKey,
        final Lifecycle pRegistryLifecycle,
        HolderOwner<T> pOwner,
        final Map<ResourceKey<T>, Holder.Reference<T>> pElements
    ) {
        return new RegistrySetBuilder.EmptyTagRegistryLookup<T>(pOwner) {
            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return pRegistryKey;
            }

            @Override
            public Lifecycle registryLifecycle() {
                return pRegistryLifecycle;
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> p_312659_) {
                return Optional.ofNullable(pElements.get(p_312659_));
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return pElements.values().stream();
            }
        };
    }

    public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> pKey, Lifecycle pLifecycle, RegistrySetBuilder.RegistryBootstrap<T> pBootstrap) {
        this.entries.add(new RegistrySetBuilder.RegistryStub<>(pKey, pLifecycle, pBootstrap));
        return this;
    }

    public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> pKey, RegistrySetBuilder.RegistryBootstrap<T> pBootstrap) {
        return this.add(pKey, Lifecycle.stable(), pBootstrap);
    }

    public List<? extends ResourceKey<? extends Registry<?>>> getEntryKeys() {
        return this.entries.stream().map(RegistrySetBuilder.RegistryStub::key).toList();
    }

    private RegistrySetBuilder.BuildState createState(RegistryAccess pRegistryAccess) {
        RegistrySetBuilder.BuildState registrysetbuilder$buildstate = RegistrySetBuilder.BuildState.create(
            pRegistryAccess, this.entries.stream().map(RegistrySetBuilder.RegistryStub::key)
        );
        this.entries.forEach(p_255629_ -> p_255629_.apply(registrysetbuilder$buildstate));
        return registrysetbuilder$buildstate;
    }

    private static HolderLookup.Provider buildProviderWithContext(
        RegistrySetBuilder.UniversalOwner pOwner, RegistryAccess pRegistryAccess, Stream<HolderLookup.RegistryLookup<?>> pLookups
    ) {
        record Entry<T>(HolderLookup.RegistryLookup<T> lookup, RegistryOps.RegistryInfo<T> opsInfo) {
            public static <T> Entry<T> createForContextRegistry(HolderLookup.RegistryLookup<T> p_326874_) {
                return new Entry<>(new RegistrySetBuilder.EmptyTagLookupWrapper<>(p_326874_, p_326874_), RegistryOps.RegistryInfo.fromRegistryLookup(p_326874_));
            }

            public static <T> Entry<T> createForNewRegistry(RegistrySetBuilder.UniversalOwner p_326836_, HolderLookup.RegistryLookup<T> p_326940_) {
                return new Entry<>(
                    new RegistrySetBuilder.EmptyTagLookupWrapper<>(p_326836_.cast(), p_326940_),
                    new RegistryOps.RegistryInfo<>(p_326836_.cast(), p_326940_, p_326940_.registryLifecycle())
                );
            }
        }

        final Map<ResourceKey<? extends Registry<?>>, Entry<?>> map = new HashMap<>();
        pRegistryAccess.registries().forEach(p_326730_ -> map.put(p_326730_.key(), Entry.createForContextRegistry(p_326730_.value().asLookup())));
        pLookups.forEach(p_326724_ -> map.put(p_326724_.key(), Entry.createForNewRegistry(pOwner, p_326724_)));
        return new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return map.keySet().stream();
            }

            <T> Optional<Entry<T>> getEntry(ResourceKey<? extends Registry<? extends T>> p_326910_) {
                return Optional.ofNullable((Entry<T>)map.get(p_326910_));
            }

            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_326942_) {
                return this.getEntry(p_326942_).map(Entry::lookup);
            }

            @Override
            public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> p_326947_) {
                return RegistryOps.create(p_326947_, new RegistryOps.RegistryInfoLookup() {
                    @Override
                    public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_326905_) {
                        return getEntry(p_326905_).map(Entry::opsInfo);
                    }
                });
            }
        };
    }

    public HolderLookup.Provider build(RegistryAccess pRegistryAccess) {
        RegistrySetBuilder.BuildState registrysetbuilder$buildstate = this.createState(pRegistryAccess);
        Stream<HolderLookup.RegistryLookup<?>> stream = this.entries
            .stream()
            .map(p_326726_ -> p_326726_.collectRegisteredValues(registrysetbuilder$buildstate).buildAsLookup(registrysetbuilder$buildstate.owner));
        HolderLookup.Provider holderlookup$provider = buildProviderWithContext(registrysetbuilder$buildstate.owner, pRegistryAccess, stream);
        registrysetbuilder$buildstate.reportNotCollectedHolders();
        registrysetbuilder$buildstate.reportUnclaimedRegisteredValues();
        registrysetbuilder$buildstate.throwOnError();
        return holderlookup$provider;
    }

    private HolderLookup.Provider createLazyFullPatchedRegistries(
        RegistryAccess pRegistry,
        HolderLookup.Provider pLookupProvider,
        Cloner.Factory pClonerFactory,
        Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> pRegistries,
        HolderLookup.Provider pRegistryLookupProvider
    ) {
        RegistrySetBuilder.UniversalOwner registrysetbuilder$universalowner = new RegistrySetBuilder.UniversalOwner();
        MutableObject<HolderLookup.Provider> mutableobject = new MutableObject<>();
        List<HolderLookup.RegistryLookup<?>> list = pRegistries.keySet()
            .stream()
            .map(
                p_311471_ -> this.createLazyFullPatchedRegistries(
                        registrysetbuilder$universalowner,
                        pClonerFactory,
                        (ResourceKey<? extends Registry<? extends Object>>)p_311471_,
                        pRegistryLookupProvider,
                        pLookupProvider,
                        mutableobject
                    )
            )
            .collect(Collectors.toUnmodifiableList());
        HolderLookup.Provider holderlookup$provider = buildProviderWithContext(registrysetbuilder$universalowner, pRegistry, list.stream());
        mutableobject.setValue(holderlookup$provider);
        return holderlookup$provider;
    }

    private <T> HolderLookup.RegistryLookup<T> createLazyFullPatchedRegistries(
        HolderOwner<T> pOwner,
        Cloner.Factory pClonerFactory,
        ResourceKey<? extends Registry<? extends T>> pRegistryKey,
        HolderLookup.Provider pRegistryLookupProvider,
        HolderLookup.Provider pLookupProvider,
        MutableObject<HolderLookup.Provider> pObject
    ) {
        Cloner<T> cloner = pClonerFactory.cloner(pRegistryKey);
        if (cloner == null) {
            throw new NullPointerException("No cloner for " + pRegistryKey.location());
        } else {
            Map<ResourceKey<T>, Holder.Reference<T>> map = new HashMap<>();
            HolderLookup.RegistryLookup<T> registrylookup = pRegistryLookupProvider.lookupOrThrow(pRegistryKey);
            registrylookup.listElements().forEach(p_311483_ -> {
                ResourceKey<T> resourcekey = p_311483_.key();
                RegistrySetBuilder.LazyHolder<T> lazyholder = new RegistrySetBuilder.LazyHolder<>(pOwner, resourcekey);
                lazyholder.supplier = () -> cloner.clone((T)p_311483_.value(), pRegistryLookupProvider, pObject.getValue());
                map.put(resourcekey, lazyholder);
            });
            Optional<HolderLookup.RegistryLookup<T>> lookup = pLookupProvider.lookup(pRegistryKey);
            Lifecycle lifecycle;
            if (lookup.isPresent()) {
                HolderLookup.RegistryLookup<T> registrylookup1 = lookup.get();
                registrylookup1.listElements().forEach(p_311506_ -> {
                    ResourceKey<T> resourcekey = p_311506_.key();
                    map.computeIfAbsent(resourcekey, p_311494_ -> {
                        RegistrySetBuilder.LazyHolder<T> lazyholder = new RegistrySetBuilder.LazyHolder<>(pOwner, resourcekey);
                        lazyholder.supplier = () -> cloner.clone((T) p_311506_.value(), pLookupProvider, pObject.getValue());
                        return lazyholder;
                    });
                });
                lifecycle = registrylookup.registryLifecycle().add(registrylookup1.registryLifecycle());
            } else {
                lifecycle = registrylookup.registryLifecycle();
            }
            return lookupFromMap(pRegistryKey, lifecycle, pOwner, map);
        }
    }

    public RegistrySetBuilder.PatchedRegistries buildPatch(RegistryAccess pRegistry, HolderLookup.Provider pLookupProvider, Cloner.Factory pClonerFactory) {
        RegistrySetBuilder.BuildState registrysetbuilder$buildstate = this.createState(pRegistry);
        Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> map = new HashMap<>();
        this.entries
            .stream()
            .map(p_311508_ -> p_311508_.collectRegisteredValues(registrysetbuilder$buildstate))
            .forEach(p_272339_ -> map.put(p_272339_.key, (RegistrySetBuilder.RegistryContents<?>)p_272339_));
        Set<ResourceKey<? extends Registry<?>>> set = pRegistry.listRegistries().collect(Collectors.toUnmodifiableSet());
        pLookupProvider.listRegistries()
            .filter(p_311485_ -> !set.contains(p_311485_))
            .forEach(
                p_311473_ -> map.putIfAbsent(
                        (ResourceKey<? extends Registry<?>>)p_311473_,
                        new RegistrySetBuilder.RegistryContents<>((ResourceKey<? extends Registry<?>>)p_311473_, Lifecycle.stable(), Map.of())
                    )
            );
        Stream<HolderLookup.RegistryLookup<?>> stream = map.values().stream().map(p_326728_ -> p_326728_.buildAsLookup(registrysetbuilder$buildstate.owner));
        HolderLookup.Provider holderlookup$provider = buildProviderWithContext(registrysetbuilder$buildstate.owner, pRegistry, stream);
        registrysetbuilder$buildstate.reportUnclaimedRegisteredValues();
        registrysetbuilder$buildstate.throwOnError();
        HolderLookup.Provider holderlookup$provider1 = this.createLazyFullPatchedRegistries(pRegistry, pLookupProvider, pClonerFactory, map, holderlookup$provider);
        return new RegistrySetBuilder.PatchedRegistries(holderlookup$provider1, holderlookup$provider);
    }

    static record BuildState(
        RegistrySetBuilder.UniversalOwner owner,
        RegistrySetBuilder.UniversalLookup lookup,
        Map<ResourceLocation, HolderGetter<?>> registries,
        Map<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>> registeredValues,
        List<RuntimeException> errors
    ) {
        public static RegistrySetBuilder.BuildState create(RegistryAccess pRegistryAccess, Stream<ResourceKey<? extends Registry<?>>> pRegistries) {
            RegistrySetBuilder.UniversalOwner registrysetbuilder$universalowner = new RegistrySetBuilder.UniversalOwner();
            List<RuntimeException> list = new ArrayList<>();
            RegistrySetBuilder.UniversalLookup registrysetbuilder$universallookup = new RegistrySetBuilder.UniversalLookup(registrysetbuilder$universalowner);
            Builder<ResourceLocation, HolderGetter<?>> builder = ImmutableMap.builder();
            pRegistryAccess.registries()
                .forEach(p_258197_ -> builder.put(p_258197_.key().location(), net.neoforged.neoforge.common.CommonHooks.wrapRegistryLookup(p_258197_.value().asLookup())));
            pRegistries.forEach(p_256603_ -> builder.put(p_256603_.location(), registrysetbuilder$universallookup));
            return new RegistrySetBuilder.BuildState(
                registrysetbuilder$universalowner, registrysetbuilder$universallookup, builder.build(), new HashMap<>(), list
            );
        }

        public <T> BootstrapContext<T> bootstrapContext() {
            return new BootstrapContext<T>() {
                @Override
                public Holder.Reference<T> register(ResourceKey<T> p_256176_, T p_256422_, Lifecycle p_255924_) {
                    RegistrySetBuilder.RegisteredValue<?> registeredvalue = BuildState.this.registeredValues
                        .put(p_256176_, new RegistrySetBuilder.RegisteredValue(p_256422_, p_255924_));
                    if (registeredvalue != null) {
                        BuildState.this.errors
                            .add(new IllegalStateException("Duplicate registration for " + p_256176_ + ", new=" + p_256422_ + ", old=" + registeredvalue.value));
                    }

                    return BuildState.this.lookup.getOrCreate(p_256176_);
                }

                @Override
                public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> p_255961_) {
                    return (HolderGetter<S>)BuildState.this.registries.getOrDefault(p_255961_.location(), BuildState.this.lookup);
                }

                @Override
                public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
                    return Optional.ofNullable((HolderLookup.RegistryLookup<S>) BuildState.this.registries.get(registry.location()));
                }
            };
        }

        public void reportUnclaimedRegisteredValues() {
            this.registeredValues
                .forEach((p_339338_, p_339339_) -> this.errors.add(new IllegalStateException("Orpaned value " + p_339339_.value + " for key " + p_339338_)));
        }

        public void reportNotCollectedHolders() {
            for (ResourceKey<Object> resourcekey : this.lookup.holders.keySet()) {
                this.errors.add(new IllegalStateException("Unreferenced key: " + resourcekey));
            }
        }

        public void throwOnError() {
            if (!this.errors.isEmpty()) {
                IllegalStateException illegalstateexception = new IllegalStateException("Errors during registry creation");

                for (RuntimeException runtimeexception : this.errors) {
                    illegalstateexception.addSuppressed(runtimeexception);
                }

                throw illegalstateexception;
            }
        }
    }

    abstract static class EmptyTagLookup<T> implements HolderGetter<T> {
        protected final HolderOwner<T> owner;

        protected EmptyTagLookup(HolderOwner<T> pOwner) {
            this.owner = pOwner;
        }

        @Override
        public Optional<HolderSet.Named<T>> get(TagKey<T> pTagKey) {
            return Optional.of(HolderSet.emptyNamed(this.owner, pTagKey));
        }
    }

    static class EmptyTagLookupWrapper<T> extends RegistrySetBuilder.EmptyTagRegistryLookup<T> implements HolderLookup.RegistryLookup.Delegate<T> {
        private final HolderLookup.RegistryLookup<T> parent;

        EmptyTagLookupWrapper(HolderOwner<T> pOwner, HolderLookup.RegistryLookup<T> pParent) {
            super(pOwner);
            this.parent = pParent;
        }

        @Override
        public HolderLookup.RegistryLookup<T> parent() {
            return this.parent;
        }
    }

    abstract static class EmptyTagRegistryLookup<T> extends RegistrySetBuilder.EmptyTagLookup<T> implements HolderLookup.RegistryLookup<T> {
        protected EmptyTagRegistryLookup(HolderOwner<T> p_326933_) {
            super(p_326933_);
        }

        @Override
        public Stream<HolderSet.Named<T>> listTags() {
            throw new UnsupportedOperationException("Tags are not available in datagen");
        }
    }

    static class LazyHolder<T> extends Holder.Reference<T> {
        @Nullable
        Supplier<T> supplier;

        protected LazyHolder(HolderOwner<T> pOwner, @Nullable ResourceKey<T> pKey) {
            super(Holder.Reference.Type.STAND_ALONE, pOwner, pKey, null);
        }

        @Override
        protected void bindValue(T pValue) {
            super.bindValue(pValue);
            this.supplier = null;
        }

        @Override
        public T value() {
            if (this.supplier != null) {
                this.bindValue(this.supplier.get());
            }

            return super.value();
        }
    }

    public static record PatchedRegistries(HolderLookup.Provider full, HolderLookup.Provider patches) {
    }

    static record RegisteredValue<T>(T value, Lifecycle lifecycle) {
    }

    @FunctionalInterface
    public interface RegistryBootstrap<T> {
        void run(BootstrapContext<T> pContext);
    }

    static record RegistryContents<T>(
        ResourceKey<? extends Registry<? extends T>> key, Lifecycle lifecycle, Map<ResourceKey<T>, RegistrySetBuilder.ValueAndHolder<T>> values
    ) {
        public HolderLookup.RegistryLookup<T> buildAsLookup(RegistrySetBuilder.UniversalOwner pOwner) {
            Map<ResourceKey<T>, Holder.Reference<T>> map = this.values
                .entrySet()
                .stream()
                .collect(
                    Collectors.toUnmodifiableMap(
                        java.util.Map.Entry::getKey,
                        p_311927_ -> {
                            RegistrySetBuilder.ValueAndHolder<T> valueandholder = p_311927_.getValue();
                            Holder.Reference<T> reference = valueandholder.holder()
                                .orElseGet(() -> Holder.Reference.createStandAlone(pOwner.cast(), p_311927_.getKey()));
                            reference.bindValue(valueandholder.value().value());
                            return reference;
                        }
                    )
                );
            return RegistrySetBuilder.lookupFromMap(this.key, this.lifecycle, pOwner.cast(), map);
        }
    }

    static record RegistryStub<T>(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, RegistrySetBuilder.RegistryBootstrap<T> bootstrap) {
        void apply(RegistrySetBuilder.BuildState pState) {
            this.bootstrap.run(pState.bootstrapContext());
        }

        public RegistrySetBuilder.RegistryContents<T> collectRegisteredValues(RegistrySetBuilder.BuildState pBuildState) {
            Map<ResourceKey<T>, RegistrySetBuilder.ValueAndHolder<T>> map = new HashMap<>();
            Iterator<java.util.Map.Entry<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>>> iterator = pBuildState.registeredValues.entrySet().iterator();

            while (iterator.hasNext()) {
                java.util.Map.Entry<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>> entry = iterator.next();
                ResourceKey<?> resourcekey = entry.getKey();
                if (resourcekey.isFor(this.key)) {
                    RegistrySetBuilder.RegisteredValue<T> registeredvalue = (RegistrySetBuilder.RegisteredValue<T>)entry.getValue();
                    Holder.Reference<T> reference = (Holder.Reference<T>)pBuildState.lookup.holders.remove(resourcekey);
                    map.put((ResourceKey<T>)resourcekey, new RegistrySetBuilder.ValueAndHolder<>(registeredvalue, Optional.ofNullable(reference)));
                    iterator.remove();
                }
            }

            return new RegistrySetBuilder.RegistryContents<>(this.key, this.lifecycle, map);
        }
    }

    static class UniversalLookup extends RegistrySetBuilder.EmptyTagLookup<Object> {
        final Map<ResourceKey<Object>, Holder.Reference<Object>> holders = new HashMap<>();

        public UniversalLookup(HolderOwner<Object> pOwner) {
            super(pOwner);
        }

        @Override
        public Optional<Holder.Reference<Object>> get(ResourceKey<Object> pResourceKey) {
            return Optional.of(this.getOrCreate(pResourceKey));
        }

        <T> Holder.Reference<T> getOrCreate(ResourceKey<T> pKey) {
            return (Holder.Reference<T>)this.holders
                .computeIfAbsent((ResourceKey<Object>)pKey, p_256154_ -> Holder.Reference.createStandAlone(this.owner, (ResourceKey<Object>)p_256154_));
        }
    }

    static class UniversalOwner implements HolderOwner<Object> {
        public <T> HolderOwner<T> cast() {
            return (HolderOwner<T>)this;
        }
    }

    static record ValueAndHolder<T>(RegistrySetBuilder.RegisteredValue<T> value, Optional<Holder.Reference<T>> holder) {
    }
}
