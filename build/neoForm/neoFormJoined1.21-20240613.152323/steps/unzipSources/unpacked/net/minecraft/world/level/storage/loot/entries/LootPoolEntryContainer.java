package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Base class for loot pool entry containers. This class just stores a list of conditions that are checked before the entry generates loot.
 */
public abstract class LootPoolEntryContainer implements ComposableEntryContainer {
    /**
     * Conditions for the loot entry to be applied.
     */
    protected final List<LootItemCondition> conditions;
    private final Predicate<LootContext> compositeCondition;

    protected LootPoolEntryContainer(List<LootItemCondition> pConditions) {
        this.conditions = pConditions;
        this.compositeCondition = Util.allOf(pConditions);
    }

    protected static <T extends LootPoolEntryContainer> P1<Mu<T>, List<LootItemCondition>> commonFields(Instance<T> pInstance) {
        return pInstance.group(LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(p_298548_ -> p_298548_.conditions));
    }

    public void validate(ValidationContext pValidationContext) {
        for (int i = 0; i < this.conditions.size(); i++) {
            this.conditions.get(i).validate(pValidationContext.forChild(".condition[" + i + "]"));
        }
    }

    protected final boolean canRun(LootContext pLootContext) {
        return this.compositeCondition.test(pLootContext);
    }

    public abstract LootPoolEntryType getType();

    public abstract static class Builder<T extends LootPoolEntryContainer.Builder<T>> implements ConditionUserBuilder<T> {
        private final ImmutableList.Builder<LootItemCondition> conditions = ImmutableList.builder();

        protected abstract T getThis();

        public T when(LootItemCondition.Builder pConditionBuilder) {
            this.conditions.add(pConditionBuilder.build());
            return this.getThis();
        }

        public final T unwrap() {
            return this.getThis();
        }

        protected List<LootItemCondition> getConditions() {
            return this.conditions.build();
        }

        public AlternativesEntry.Builder otherwise(LootPoolEntryContainer.Builder<?> pChildBuilder) {
            return new AlternativesEntry.Builder(this, pChildBuilder);
        }

        public EntryGroup.Builder append(LootPoolEntryContainer.Builder<?> pChildBuilder) {
            return new EntryGroup.Builder(this, pChildBuilder);
        }

        public SequentialEntry.Builder then(LootPoolEntryContainer.Builder<?> pChildBuilder) {
            return new SequentialEntry.Builder(this, pChildBuilder);
        }

        public abstract LootPoolEntryContainer build();
    }
}
