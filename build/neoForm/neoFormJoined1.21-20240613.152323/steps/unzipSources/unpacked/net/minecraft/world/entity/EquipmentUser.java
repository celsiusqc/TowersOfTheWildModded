package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

public interface EquipmentUser {
    void setItemSlot(EquipmentSlot pSlot, ItemStack pStack);

    ItemStack getItemBySlot(EquipmentSlot pSlot);

    void setDropChance(EquipmentSlot pSlot, float pDropChance);

    default void equip(EquipmentTable pEquipmentTable, LootParams pParams) {
        this.equip(pEquipmentTable.lootTable(), pParams, pEquipmentTable.slotDropChances());
    }

    default void equip(ResourceKey<LootTable> pEquipmentLootTable, LootParams pParams, Map<EquipmentSlot, Float> pSlotDropChances) {
        this.equip(pEquipmentLootTable, pParams, 0L, pSlotDropChances);
    }

    default void equip(ResourceKey<LootTable> pEquipmentLootTable, LootParams pParams, long pSeed, Map<EquipmentSlot, Float> pSlotDropChances) {
        if (!pEquipmentLootTable.equals(BuiltInLootTables.EMPTY)) {
            LootTable loottable = pParams.getLevel().getServer().reloadableRegistries().getLootTable(pEquipmentLootTable);
            if (loottable != LootTable.EMPTY) {
                List<ItemStack> list = loottable.getRandomItems(pParams, pSeed);
                List<EquipmentSlot> list1 = new ArrayList<>();

                for (ItemStack itemstack : list) {
                    EquipmentSlot equipmentslot = this.resolveSlot(itemstack, list1);
                    if (equipmentslot != null) {
                        ItemStack itemstack1 = equipmentslot.limit(itemstack);
                        this.setItemSlot(equipmentslot, itemstack1);
                        Float f = pSlotDropChances.get(equipmentslot);
                        if (f != null) {
                            this.setDropChance(equipmentslot, f);
                        }

                        list1.add(equipmentslot);
                    }
                }
            }
        }
    }

    @Nullable
    default EquipmentSlot resolveSlot(ItemStack pStack, List<EquipmentSlot> pExcludedSlots) {
        if (pStack.isEmpty()) {
            return null;
        } else {
            Equipable equipable = Equipable.get(pStack);
            if (equipable != null) {
                EquipmentSlot equipmentslot = equipable.getEquipmentSlot();
                if (!pExcludedSlots.contains(equipmentslot)) {
                    return equipmentslot;
                }
            } else if (!pExcludedSlots.contains(EquipmentSlot.MAINHAND)) {
                return EquipmentSlot.MAINHAND;
            }

            return null;
        }
    }
}
