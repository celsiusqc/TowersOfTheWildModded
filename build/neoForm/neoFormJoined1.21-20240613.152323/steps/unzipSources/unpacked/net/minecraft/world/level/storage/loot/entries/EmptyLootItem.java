package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A loot pool entry that does not generate any items.
 */
public class EmptyLootItem extends LootPoolSingletonContainer {
    public static final MapCodec<EmptyLootItem> CODEC = RecordCodecBuilder.mapCodec(
        p_299288_ -> singletonFields(p_299288_).apply(p_299288_, EmptyLootItem::new)
    );

    private EmptyLootItem(int p_79519_, int p_79520_, List<LootItemCondition> p_298942_, List<LootItemFunction> p_298310_) {
        super(p_79519_, p_79520_, p_298942_, p_298310_);
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.EMPTY;
    }

    /**
     * Generate the loot stacks of this entry.
     * Contrary to the method name this method does not always generate one stack, it can also generate zero or multiple stacks.
     */
    @Override
    public void createItemStack(Consumer<ItemStack> pStackConsumer, LootContext pLootContext) {
    }

    public static LootPoolSingletonContainer.Builder<?> emptyItem() {
        return simpleBuilder(EmptyLootItem::new);
    }
}
