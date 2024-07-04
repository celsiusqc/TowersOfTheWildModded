package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeLootItemCondition implements LootItemCondition {
    protected final List<LootItemCondition> terms;
    private final Predicate<LootContext> composedPredicate;

    protected CompositeLootItemCondition(List<LootItemCondition> pTerms, Predicate<LootContext> pComposedPredicate) {
        this.terms = pTerms;
        this.composedPredicate = pComposedPredicate;
    }

    protected static <T extends CompositeLootItemCondition> MapCodec<T> createCodec(Function<List<LootItemCondition>, T> pFactory) {
        return RecordCodecBuilder.mapCodec(
            p_344714_ -> p_344714_.group(LootItemCondition.DIRECT_CODEC.listOf().fieldOf("terms").forGetter(p_298337_ -> p_298337_.terms))
                    .apply(p_344714_, pFactory)
        );
    }

    protected static <T extends CompositeLootItemCondition> Codec<T> createInlineCodec(Function<List<LootItemCondition>, T> pFactory) {
        return LootItemCondition.DIRECT_CODEC.listOf().xmap(pFactory, p_298237_ -> p_298237_.terms);
    }

    public final boolean test(LootContext pContext) {
        return this.composedPredicate.test(pContext);
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        LootItemCondition.super.validate(pContext);

        for (int i = 0; i < this.terms.size(); i++) {
            this.terms.get(i).validate(pContext.forChild(".term[" + i + "]"));
        }
    }

    public abstract static class Builder implements LootItemCondition.Builder {
        private final ImmutableList.Builder<LootItemCondition> terms = ImmutableList.builder();

        protected Builder(LootItemCondition.Builder... pConditions) {
            for (LootItemCondition.Builder lootitemcondition$builder : pConditions) {
                this.terms.add(lootitemcondition$builder.build());
            }
        }

        public void addTerm(LootItemCondition.Builder pCondition) {
            this.terms.add(pCondition.build());
        }

        @Override
        public LootItemCondition build() {
            return this.create(this.terms.build());
        }

        protected abstract LootItemCondition create(List<LootItemCondition> pConditions);
    }
}
