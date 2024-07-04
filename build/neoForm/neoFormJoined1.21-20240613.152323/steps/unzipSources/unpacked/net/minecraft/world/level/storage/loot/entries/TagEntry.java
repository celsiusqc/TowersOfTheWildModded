package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A loot pool entry container that generates based on an item tag.
 * If {@code expand} is set to true, it will expand into separate LootPoolEntries for every item in the tag, otherwise it will simply generate all items in the tag.
 */
public class TagEntry extends LootPoolSingletonContainer {
    public static final MapCodec<TagEntry> CODEC = RecordCodecBuilder.mapCodec(
        p_298033_ -> p_298033_.group(
                    TagKey.codec(Registries.ITEM).fieldOf("name").forGetter(p_298040_ -> p_298040_.tag),
                    Codec.BOOL.fieldOf("expand").forGetter(p_298039_ -> p_298039_.expand)
                )
                .and(singletonFields(p_298033_))
                .apply(p_298033_, TagEntry::new)
    );
    private final TagKey<Item> tag;
    private final boolean expand;

    private TagEntry(
        TagKey<Item> p_205078_, boolean p_205079_, int p_205080_, int p_205081_, List<LootItemCondition> p_298985_, List<LootItemFunction> p_299088_
    ) {
        super(p_205080_, p_205081_, p_298985_, p_299088_);
        this.tag = p_205078_;
        this.expand = p_205079_;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.TAG;
    }

    /**
     * Generate the loot stacks of this entry.
     * Contrary to the method name this method does not always generate one stack, it can also generate zero or multiple stacks.
     */
    @Override
    public void createItemStack(Consumer<ItemStack> pStackConsumer, LootContext pLootContext) {
        BuiltInRegistries.ITEM.getTagOrEmpty(this.tag).forEach(p_205094_ -> pStackConsumer.accept(new ItemStack((Holder<Item>)p_205094_)));
    }

    private boolean expandTag(LootContext pContext, Consumer<LootPoolEntry> pGeneratorConsumer) {
        if (!this.canRun(pContext)) {
            return false;
        } else {
            for (final Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
                pGeneratorConsumer.accept(new LootPoolSingletonContainer.EntryBase() {
                    @Override
                    public void createItemStack(Consumer<ItemStack> p_79869_, LootContext p_79870_) {
                        p_79869_.accept(new ItemStack(holder));
                    }
                });
            }

            return true;
        }
    }

    /**
     * Expand this loot pool entry container by calling {@code entryConsumer} with any applicable entries
     *
     * @return whether this loot pool entry container successfully expanded or not
     */
    @Override
    public boolean expand(LootContext pLootContext, Consumer<LootPoolEntry> pEntryConsumer) {
        return this.expand ? this.expandTag(pLootContext, pEntryConsumer) : super.expand(pLootContext, pEntryConsumer);
    }

    public static LootPoolSingletonContainer.Builder<?> tagContents(TagKey<Item> pTag) {
        return simpleBuilder((p_298035_, p_298036_, p_298037_, p_298038_) -> new TagEntry(pTag, false, p_298035_, p_298036_, p_298037_, p_298038_));
    }

    public static LootPoolSingletonContainer.Builder<?> expandTag(TagKey<Item> pTag) {
        return simpleBuilder((p_298042_, p_298043_, p_298044_, p_298045_) -> new TagEntry(pTag, true, p_298042_, p_298043_, p_298044_, p_298045_));
    }
}
