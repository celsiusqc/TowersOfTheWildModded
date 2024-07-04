package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A composite loot pool entry container that expands all its children in order.
 * This container always succeeds.
 */
public class EntryGroup extends CompositeEntryBase {
    public static final MapCodec<EntryGroup> CODEC = createCodec(EntryGroup::new);

    EntryGroup(List<LootPoolEntryContainer> p_298565_, List<LootItemCondition> p_298406_) {
        super(p_298565_, p_298406_);
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.GROUP;
    }

    @Override
    protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> pChildren) {
        return switch (pChildren.size()) {
            case 0 -> ALWAYS_TRUE;
            case 1 -> (ComposableEntryContainer)pChildren.get(0);
            case 2 -> {
                ComposableEntryContainer composableentrycontainer = pChildren.get(0);
                ComposableEntryContainer composableentrycontainer1 = pChildren.get(1);
                yield (p_79556_, p_79557_) -> {
                    composableentrycontainer.expand(p_79556_, p_79557_);
                    composableentrycontainer1.expand(p_79556_, p_79557_);
                    return true;
                };
            }
            default -> (p_298014_, p_298015_) -> {
            for (ComposableEntryContainer composableentrycontainer2 : pChildren) {
                composableentrycontainer2.expand(p_298014_, p_298015_);
            }

            return true;
        };
        };
    }

    public static EntryGroup.Builder list(LootPoolEntryContainer.Builder<?>... pChildren) {
        return new EntryGroup.Builder(pChildren);
    }

    public static class Builder extends LootPoolEntryContainer.Builder<EntryGroup.Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

        public Builder(LootPoolEntryContainer.Builder<?>... pChildren) {
            for (LootPoolEntryContainer.Builder<?> builder : pChildren) {
                this.entries.add(builder.build());
            }
        }

        protected EntryGroup.Builder getThis() {
            return this;
        }

        @Override
        public EntryGroup.Builder append(LootPoolEntryContainer.Builder<?> pChildBuilder) {
            this.entries.add(pChildBuilder.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new EntryGroup(this.entries.build(), this.getConditions());
        }
    }
}
