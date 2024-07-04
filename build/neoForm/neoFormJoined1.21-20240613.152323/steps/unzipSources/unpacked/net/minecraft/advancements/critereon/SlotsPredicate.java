package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

public record SlotsPredicate(Map<SlotRange, ItemPredicate> slots) {
    public static final Codec<SlotsPredicate> CODEC = Codec.unboundedMap(SlotRanges.CODEC, ItemPredicate.CODEC)
        .xmap(SlotsPredicate::new, SlotsPredicate::slots);

    public boolean matches(Entity pEntity) {
        for (Entry<SlotRange, ItemPredicate> entry : this.slots.entrySet()) {
            if (!matchSlots(pEntity, entry.getValue(), entry.getKey().slots())) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchSlots(Entity pEntity, ItemPredicate pPredicate, IntList pSlots) {
        for (int i = 0; i < pSlots.size(); i++) {
            int j = pSlots.getInt(i);
            SlotAccess slotaccess = pEntity.getSlot(j);
            if (pPredicate.test(slotaccess.get())) {
                return true;
            }
        }

        return false;
    }
}
