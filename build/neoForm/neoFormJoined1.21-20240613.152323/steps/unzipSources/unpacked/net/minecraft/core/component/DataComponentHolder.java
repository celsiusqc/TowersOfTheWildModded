package net.minecraft.core.component;

import javax.annotation.Nullable;

public interface DataComponentHolder extends net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension {
    DataComponentMap getComponents();

    @Nullable
    default <T> T get(DataComponentType<? extends T> pComponent) {
        return this.getComponents().get(pComponent);
    }

    default <T> T getOrDefault(DataComponentType<? extends T> pComponent, T pDefaultValue) {
        return this.getComponents().getOrDefault(pComponent, pDefaultValue);
    }

    default boolean has(DataComponentType<?> pComponent) {
        return this.getComponents().has(pComponent);
    }
}
