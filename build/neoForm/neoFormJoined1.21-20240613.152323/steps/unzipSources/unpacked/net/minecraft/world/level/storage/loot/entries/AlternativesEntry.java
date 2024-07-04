package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A composite loot pool entry container that expands all its children in order until one of them succeeds.
 * This container succeeds if one of its children succeeds.
 */
public class AlternativesEntry extends CompositeEntryBase {
    public static final MapCodec<AlternativesEntry> CODEC = createCodec(AlternativesEntry::new);

    AlternativesEntry(List<LootPoolEntryContainer> p_299062_, List<LootItemCondition> p_298668_) {
        super(p_299062_, p_298668_);
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.ALTERNATIVES;
    }

    @Override
    protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> pChildren) {
        return switch (pChildren.size()) {
            case 0 -> ALWAYS_FALSE;
            case 1 -> (ComposableEntryContainer)pChildren.get(0);
            case 2 -> pChildren.get(0).or(pChildren.get(1));
            default -> (p_298004_, p_298005_) -> {
            for (ComposableEntryContainer composableentrycontainer : pChildren) {
                if (composableentrycontainer.expand(p_298004_, p_298005_)) {
                    return true;
                }
            }

            return false;
        };
        };
    }

    @Override
    public void validate(ValidationContext pValidationContext) {
        super.validate(pValidationContext);

        for (int i = 0; i < this.children.size() - 1; i++) {
            if (this.children.get(i).conditions.isEmpty()) {
                pValidationContext.reportProblem("Unreachable entry!");
            }
        }
    }

    public static AlternativesEntry.Builder alternatives(LootPoolEntryContainer.Builder<?>... pChildren) {
        return new AlternativesEntry.Builder(pChildren);
    }

    public static <E> AlternativesEntry.Builder alternatives(Collection<E> pChildrenSources, Function<E, LootPoolEntryContainer.Builder<?>> pToChildrenFunction) {
        return new AlternativesEntry.Builder(pChildrenSources.stream().map(pToChildrenFunction::apply).toArray(LootPoolEntryContainer.Builder[]::new));
    }

    public static class Builder extends LootPoolEntryContainer.Builder<AlternativesEntry.Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

        public Builder(LootPoolEntryContainer.Builder<?>... pChildren) {
            for (LootPoolEntryContainer.Builder<?> builder : pChildren) {
                this.entries.add(builder.build());
            }
        }

        protected AlternativesEntry.Builder getThis() {
            return this;
        }

        @Override
        public AlternativesEntry.Builder otherwise(LootPoolEntryContainer.Builder<?> pChildBuilder) {
            this.entries.add(pChildBuilder.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new AlternativesEntry(this.entries.build(), this.getConditions());
        }
    }
}
