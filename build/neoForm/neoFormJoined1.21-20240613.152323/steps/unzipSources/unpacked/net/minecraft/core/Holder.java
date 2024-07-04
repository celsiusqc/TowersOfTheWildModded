package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public interface Holder<T> extends net.neoforged.neoforge.common.extensions.IHolderExtension<T> {
    T value();

    boolean isBound();

    boolean is(ResourceLocation pLocation);

    boolean is(ResourceKey<T> pResourceKey);

    boolean is(Predicate<ResourceKey<T>> pPredicate);

    boolean is(TagKey<T> pTagKey);

    @Deprecated
    boolean is(Holder<T> pHolder);

    Stream<TagKey<T>> tags();

    Either<ResourceKey<T>, T> unwrap();

    Optional<ResourceKey<T>> unwrapKey();

    Holder.Kind kind();

    boolean canSerializeIn(HolderOwner<T> pOwner);

    default String getRegisteredName() {
        return this.unwrapKey().map(p_316542_ -> p_316542_.location().toString()).orElse("[unregistered]");
    }

    static <T> Holder<T> direct(T pValue) {
        return new Holder.Direct<>(pValue);
    }

    public static record Direct<T>(T value) implements Holder<T> {
        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean is(ResourceLocation p_205727_) {
            return false;
        }

        @Override
        public boolean is(ResourceKey<T> p_205725_) {
            return false;
        }

        @Override
        public boolean is(TagKey<T> p_205719_) {
            return false;
        }

        @Override
        public boolean is(Holder<T> p_316277_) {
            return this.value.equals(p_316277_.value());
        }

        @Override
        public boolean is(Predicate<ResourceKey<T>> p_205723_) {
            return false;
        }

        @Override
        public Either<ResourceKey<T>, T> unwrap() {
            return Either.right(this.value);
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.empty();
        }

        @Override
        public Holder.Kind kind() {
            return Holder.Kind.DIRECT;
        }

        @Override
        public String toString() {
            return "Direct{" + this.value + "}";
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> p_256328_) {
            return true;
        }

        @Override
        public Stream<TagKey<T>> tags() {
            return Stream.of();
        }
    }

    public static enum Kind {
        REFERENCE,
        DIRECT;
    }

    public static class Reference<T> implements Holder<T> {
        private final HolderOwner<T> owner;
        private Set<TagKey<T>> tags = Set.of();
        private final Holder.Reference.Type type;
        @Nullable
        private ResourceKey<T> key;
        @Nullable
        private T value;

        protected Reference(Holder.Reference.Type pType, HolderOwner<T> pOwner, @Nullable ResourceKey<T> pKey, @Nullable T pValue) {
            this.owner = pOwner;
            this.type = pType;
            this.key = pKey;
            this.value = pValue;
        }

        public static <T> Holder.Reference<T> createStandAlone(HolderOwner<T> pOwner, ResourceKey<T> pKey) {
            return new Holder.Reference<>(Holder.Reference.Type.STAND_ALONE, pOwner, pKey, null);
        }

        @Deprecated
        public static <T> Holder.Reference<T> createIntrusive(HolderOwner<T> pOwner, @Nullable T pValue) {
            return new Holder.Reference<>(Holder.Reference.Type.INTRUSIVE, pOwner, null, pValue);
        }

        public ResourceKey<T> key() {
            if (this.key == null) {
                throw new IllegalStateException("Trying to access unbound value '" + this.value + "' from registry " + this.owner);
            } else {
                return this.key;
            }
        }

        @Override
        public T value() {
            if (this.value == null) {
                throw new IllegalStateException("Trying to access unbound value '" + this.key + "' from registry " + this.owner);
            } else {
                return this.value;
            }
        }

        @Override
        public boolean is(ResourceLocation pLocation) {
            return this.key().location().equals(pLocation);
        }

        @Override
        public boolean is(ResourceKey<T> pResourceKey) {
            return this.key() == pResourceKey;
        }

        @Override
        public boolean is(TagKey<T> pTagKey) {
            return this.tags.contains(pTagKey);
        }

        @Override
        public boolean is(Holder<T> pHolder) {
            return pHolder.is(this.key());
        }

        @Override
        public boolean is(Predicate<ResourceKey<T>> pPredicate) {
            return pPredicate.test(this.key());
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> pOwner) {
            return this.owner.canSerializeIn(pOwner);
        }

        @Override
        public Either<ResourceKey<T>, T> unwrap() {
            return Either.left(this.key());
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.of(this.key());
        }

        @Override
        public Holder.Kind kind() {
            return Holder.Kind.REFERENCE;
        }

        @Override
        public boolean isBound() {
            return this.key != null && this.value != null;
        }

        public void bindKey(ResourceKey<T> pKey) {
            if (this.key != null && pKey != this.key) {
                throw new IllegalStateException("Can't change holder key: existing=" + this.key + ", new=" + pKey);
            } else {
                this.key = pKey;
            }
        }

        public void bindValue(T pValue) {
            if (this.type == Holder.Reference.Type.INTRUSIVE && this.value != pValue) {
                throw new IllegalStateException("Can't change holder " + this.key + " value: existing=" + this.value + ", new=" + pValue);
            } else {
                this.value = pValue;
            }
        }

        @org.jetbrains.annotations.Nullable
        public <A> A getData(net.neoforged.neoforge.registries.datamaps.DataMapType<T, A> type) {
            if (owner instanceof HolderLookup.RegistryLookup<T> lookup) {
                return lookup.getData(type, key());
            }
            return null;
        }

        public void bindTags(Collection<TagKey<T>> pTags) {
            this.tags = Set.copyOf(pTags);
        }

        @Override
        public Stream<TagKey<T>> tags() {
            return this.tags.stream();
        }

        @Override
        public String toString() {
            return "Reference{" + this.key + "=" + this.value + "}";
        }

        // Neo Start

        // Neo: Add DeferredHolder-compatible hashCode() and equals() overrides
        @Override
        public int hashCode() {
            return key().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof Holder<?> h && h.kind() == Kind.REFERENCE && h.unwrapKey().orElseThrow() == this.key();
        }

        @Override
        @org.jetbrains.annotations.Nullable
        public HolderLookup.RegistryLookup<T> unwrapLookup() {
            return this.owner instanceof HolderLookup.RegistryLookup<T> rl ? rl : null;
        }

        // Neo End

        public static enum Type {
            STAND_ALONE,
            INTRUSIVE;
        }
    }
}
