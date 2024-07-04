package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public class SequenceFunction implements LootItemFunction {
    public static final MapCodec<SequenceFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_335342_ -> p_335342_.group(LootItemFunctions.TYPED_CODEC.listOf().fieldOf("functions").forGetter(p_298431_ -> p_298431_.functions))
                .apply(p_335342_, SequenceFunction::new)
    );
    public static final Codec<SequenceFunction> INLINE_CODEC = LootItemFunctions.TYPED_CODEC
        .listOf()
        .xmap(SequenceFunction::new, p_298862_ -> p_298862_.functions);
    private final List<LootItemFunction> functions;
    private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

    private SequenceFunction(List<LootItemFunction> p_299323_) {
        this.functions = p_299323_;
        this.compositeFunction = LootItemFunctions.compose(p_299323_);
    }

    public static SequenceFunction of(List<LootItemFunction> pFunctions) {
        return new SequenceFunction(List.copyOf(pFunctions));
    }

    public ItemStack apply(ItemStack pStack, LootContext pContext) {
        return this.compositeFunction.apply(pStack, pContext);
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        LootItemFunction.super.validate(pContext);

        for (int i = 0; i < this.functions.size(); i++) {
            this.functions.get(i).validate(pContext.forChild(".function[" + i + "]"));
        }
    }

    @Override
    public LootItemFunctionType<SequenceFunction> getType() {
        return LootItemFunctions.SEQUENCE;
    }
}
