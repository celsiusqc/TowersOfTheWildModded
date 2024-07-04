package net.minecraft.advancements.critereon;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public interface SingleComponentItemPredicate<T> extends ItemSubPredicate {
    @Override
    default boolean matches(ItemStack pStack) {
        T t = pStack.get(this.componentType());
        return t != null && this.matches(pStack, t);
    }

    DataComponentType<T> componentType();

    boolean matches(ItemStack pStack, T pValue);
}
