package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A LootItemFunction that only modifies the stacks if a list of {@linkplain LootItemCondition predicates} passes.
 */
public abstract class LootItemConditionalFunction implements LootItemFunction {
    protected final List<LootItemCondition> predicates;
    private final Predicate<LootContext> compositePredicates;

    protected LootItemConditionalFunction(List<LootItemCondition> pPredicates) {
        this.predicates = pPredicates;
        this.compositePredicates = Util.allOf(pPredicates);
    }

    @Override
    public abstract LootItemFunctionType<? extends LootItemConditionalFunction> getType();

    protected static <T extends LootItemConditionalFunction> P1<Mu<T>, List<LootItemCondition>> commonFields(Instance<T> pInstance) {
        return pInstance.group(LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(p_299114_ -> p_299114_.predicates));
    }

    public final ItemStack apply(ItemStack pStack, LootContext pContext) {
        return this.compositePredicates.test(pContext) ? this.run(pStack, pContext) : pStack;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    protected abstract ItemStack run(ItemStack pStack, LootContext pContext);

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        LootItemFunction.super.validate(pContext);

        for (int i = 0; i < this.predicates.size(); i++) {
            this.predicates.get(i).validate(pContext.forChild(".conditions[" + i + "]"));
        }
    }

    protected static LootItemConditionalFunction.Builder<?> simpleBuilder(Function<List<LootItemCondition>, LootItemFunction> pConstructor) {
        return new LootItemConditionalFunction.DummyBuilder(pConstructor);
    }

    public abstract static class Builder<T extends LootItemConditionalFunction.Builder<T>> implements LootItemFunction.Builder, ConditionUserBuilder<T> {
        private final ImmutableList.Builder<LootItemCondition> conditions = ImmutableList.builder();

        public T when(LootItemCondition.Builder p_80694_) {
            this.conditions.add(p_80694_.build());
            return this.getThis();
        }

        public final T unwrap() {
            return this.getThis();
        }

        protected abstract T getThis();

        protected List<LootItemCondition> getConditions() {
            return this.conditions.build();
        }
    }

    static final class DummyBuilder extends LootItemConditionalFunction.Builder<LootItemConditionalFunction.DummyBuilder> {
        private final Function<List<LootItemCondition>, LootItemFunction> constructor;

        public DummyBuilder(Function<List<LootItemCondition>, LootItemFunction> pConstructor) {
            this.constructor = pConstructor;
        }

        protected LootItemConditionalFunction.DummyBuilder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return this.constructor.apply(this.getConditions());
        }
    }
}
