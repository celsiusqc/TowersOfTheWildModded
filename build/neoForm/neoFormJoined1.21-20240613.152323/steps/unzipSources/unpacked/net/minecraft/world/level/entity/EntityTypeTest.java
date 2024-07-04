package net.minecraft.world.level.entity;

import javax.annotation.Nullable;

public interface EntityTypeTest<B, T extends B> {
    static <B, T extends B> EntityTypeTest<B, T> forClass(final Class<T> pClazz) {
        return new EntityTypeTest<B, T>() {
            @Nullable
            @Override
            public T tryCast(B p_156924_) {
                return (T)(pClazz.isInstance(p_156924_) ? p_156924_ : null);
            }

            @Override
            public Class<? extends B> getBaseClass() {
                return pClazz;
            }
        };
    }

    static <B, T extends B> EntityTypeTest<B, T> forExactClass(final Class<T> pClazz) {
        return new EntityTypeTest<B, T>() {
            @Nullable
            @Override
            public T tryCast(B p_313860_) {
                return (T)(pClazz.equals(p_313860_.getClass()) ? p_313860_ : null);
            }

            @Override
            public Class<? extends B> getBaseClass() {
                return pClazz;
            }
        };
    }

    @Nullable
    T tryCast(B pEntity);

    Class<? extends B> getBaseClass();
}
