package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A loot pool entry container that will generate the dynamic drops with a given name.
 *
 * @see LootContext.DynamicDrops
 */
public class DynamicLoot extends LootPoolSingletonContainer {
    public static final MapCodec<DynamicLoot> CODEC = RecordCodecBuilder.mapCodec(
        p_298006_ -> p_298006_.group(ResourceLocation.CODEC.fieldOf("name").forGetter(p_298012_ -> p_298012_.name))
                .and(singletonFields(p_298006_))
                .apply(p_298006_, DynamicLoot::new)
    );
    private final ResourceLocation name;

    private DynamicLoot(ResourceLocation p_79465_, int p_79466_, int p_79467_, List<LootItemCondition> p_299033_, List<LootItemFunction> p_298474_) {
        super(p_79466_, p_79467_, p_299033_, p_298474_);
        this.name = p_79465_;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.DYNAMIC;
    }

    /**
     * Generate the loot stacks of this entry.
     * Contrary to the method name this method does not always generate one stack, it can also generate zero or multiple stacks.
     */
    @Override
    public void createItemStack(Consumer<ItemStack> pStackConsumer, LootContext pLootContext) {
        pLootContext.addDynamicDrops(this.name, pStackConsumer);
    }

    public static LootPoolSingletonContainer.Builder<?> dynamicEntry(ResourceLocation pDynamicDropsName) {
        return simpleBuilder((p_298008_, p_298009_, p_298010_, p_298011_) -> new DynamicLoot(pDynamicDropsName, p_298008_, p_298009_, p_298010_, p_298011_));
    }
}
