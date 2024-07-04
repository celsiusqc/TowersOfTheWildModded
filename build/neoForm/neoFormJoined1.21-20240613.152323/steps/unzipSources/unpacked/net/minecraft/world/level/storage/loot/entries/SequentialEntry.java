package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A composite loot pool entry container that expands all its children in order until one of them fails.
 * This container succeeds if all children succeed.
 */
public class SequentialEntry extends CompositeEntryBase {
    public static final MapCodec<SequentialEntry> CODEC = createCodec(SequentialEntry::new);

    SequentialEntry(List<LootPoolEntryContainer> p_299160_, List<LootItemCondition> p_298450_) {
        super(p_299160_, p_298450_);
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.SEQUENCE;
    }

    @Override
    protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> pChildren) {
        return switch (pChildren.size()) {
            case 0 -> ALWAYS_TRUE;
            case 1 -> (ComposableEntryContainer)pChildren.get(0);
            case 2 -> pChildren.get(0).and(pChildren.get(1));
            default -> (p_298031_, p_298032_) -> {
            for (ComposableEntryContainer composableentrycontainer : pChildren) {
                if (!composableentrycontainer.expand(p_298031_, p_298032_)) {
                    return false;
                }
            }

            return true;
        };
        };
    }

    public static SequentialEntry.Builder sequential(LootPoolEntryContainer.Builder<?>... pChildren) {
        return new SequentialEntry.Builder(pChildren);
    }

    public static class Builder extends LootPoolEntryContainer.Builder<SequentialEntry.Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

        public Builder(LootPoolEntryContainer.Builder<?>... pChildren) {
            for (LootPoolEntryContainer.Builder<?> builder : pChildren) {
                this.entries.add(builder.build());
            }
        }

        protected SequentialEntry.Builder getThis() {
            return this;
        }

        @Override
        public SequentialEntry.Builder then(LootPoolEntryContainer.Builder<?> pChildBuilder) {
            this.entries.add(pChildBuilder.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new SequentialEntry(this.entries.build(), this.getConditions());
        }
    }
}
