package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class FilteredFunction extends LootItemConditionalFunction {
    public static final MapCodec<FilteredFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_340904_ -> commonFields(p_340904_)
                .and(
                    p_340904_.group(
                        ItemPredicate.CODEC.fieldOf("item_filter").forGetter(p_341101_ -> p_341101_.filter),
                        LootItemFunctions.ROOT_CODEC.fieldOf("modifier").forGetter(p_340853_ -> p_340853_.modifier)
                    )
                )
                .apply(p_340904_, FilteredFunction::new)
    );
    private final ItemPredicate filter;
    private final LootItemFunction modifier;

    private FilteredFunction(List<LootItemCondition> p_341370_, ItemPredicate p_341210_, LootItemFunction p_340816_) {
        super(p_341370_);
        this.filter = p_341210_;
        this.modifier = p_340816_;
    }

    @Override
    public LootItemFunctionType<FilteredFunction> getType() {
        return LootItemFunctions.FILTERED;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        return this.filter.test(pStack) ? this.modifier.apply(pStack, pContext) : pStack;
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        super.validate(pContext);
        this.modifier.validate(pContext.forChild(".modifier"));
    }
}
