package net.minecraft.world.entity;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface SlotAccess {
    SlotAccess NULL = new SlotAccess() {
        @Override
        public ItemStack get() {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean set(ItemStack p_147314_) {
            return false;
        }
    };

    static SlotAccess of(final Supplier<ItemStack> pGetter, final Consumer<ItemStack> pSetter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return pGetter.get();
            }

            @Override
            public boolean set(ItemStack p_147324_) {
                pSetter.accept(p_147324_);
                return true;
            }
        };
    }

    static SlotAccess forContainer(final Container pInventory, final int pSlot, final Predicate<ItemStack> pStackFilter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return pInventory.getItem(pSlot);
            }

            @Override
            public boolean set(ItemStack p_147334_) {
                if (!pStackFilter.test(p_147334_)) {
                    return false;
                } else {
                    pInventory.setItem(pSlot, p_147334_);
                    return true;
                }
            }
        };
    }

    static SlotAccess forContainer(Container pInventory, int pSlot) {
        return forContainer(pInventory, pSlot, p_147310_ -> true);
    }

    static SlotAccess forEquipmentSlot(final LivingEntity pEntity, final EquipmentSlot pSlot, final Predicate<ItemStack> pStackFilter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return pEntity.getItemBySlot(pSlot);
            }

            @Override
            public boolean set(ItemStack p_341038_) {
                if (!pStackFilter.test(p_341038_)) {
                    return false;
                } else {
                    pEntity.setItemSlot(pSlot, p_341038_);
                    return true;
                }
            }
        };
    }

    static SlotAccess forEquipmentSlot(LivingEntity pEntity, EquipmentSlot pSlot) {
        return forEquipmentSlot(pEntity, pSlot, p_147308_ -> true);
    }

    ItemStack get();

    boolean set(ItemStack pCarried);
}
