package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class DefaultedMappedRegistry<T> extends MappedRegistry<T> implements DefaultedRegistry<T> {
    private final ResourceLocation defaultKey;
    private Holder.Reference<T> defaultValue;

    public DefaultedMappedRegistry(String pDefaultKey, ResourceKey<? extends Registry<T>> pKey, Lifecycle pRegistryLifecycle, boolean pHasIntrusiveHolders) {
        super(pKey, pRegistryLifecycle, pHasIntrusiveHolders);
        this.defaultKey = ResourceLocation.parse(pDefaultKey);
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> pKey, T pValue, RegistrationInfo pRegistrationInfo) {
        Holder.Reference<T> reference = super.register(pKey, pValue, pRegistrationInfo);
        if (this.defaultKey.equals(pKey.location())) {
            this.defaultValue = reference;
        }

        return reference;
    }

    /**
     * @return the integer ID used to identify the given object
     */
    @Override
    public int getId(@Nullable T pValue) {
        int i = super.getId(pValue);
        return i == -1 ? super.getId(this.defaultValue.value()) : i;
    }

    /**
     * @return the name used to identify the given object within this registry or {@code null} if the object is not within this registry
     */
    @Nonnull
    @Override
    public ResourceLocation getKey(T pValue) {
        ResourceLocation resourcelocation = super.getKey(pValue);
        return resourcelocation == null ? this.defaultKey : resourcelocation;
    }

    @Nonnull
    @Override
    public T get(@Nullable ResourceLocation pName) {
        T t = super.get(pName);
        return t == null ? this.defaultValue.value() : t;
    }

    @Override
    public Optional<T> getOptional(@Nullable ResourceLocation pName) {
        return Optional.ofNullable(super.get(pName));
    }

    @Override
    public Optional<Holder.Reference<T>> getAny() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Nonnull
    @Override
    public T byId(int pId) {
        T t = super.byId(pId);
        return t == null ? this.defaultValue.value() : t;
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource pRandom) {
        return super.getRandom(pRandom).or(() -> Optional.of(this.defaultValue));
    }

    @Override
    public ResourceLocation getDefaultKey() {
        return this.defaultKey;
    }
}
