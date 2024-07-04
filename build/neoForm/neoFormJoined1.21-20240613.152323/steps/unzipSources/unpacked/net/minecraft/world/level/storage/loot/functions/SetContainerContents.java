package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulators;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets the contents of a container such as a chest by setting the {@code BlocKEntityTag} of the stacks.
 * The contents are based on a list of loot pools.
 */
public class SetContainerContents extends LootItemConditionalFunction {
    public static final MapCodec<SetContainerContents> CODEC = RecordCodecBuilder.mapCodec(
        p_340801_ -> commonFields(p_340801_)
                .and(
                    p_340801_.group(
                        ContainerComponentManipulators.CODEC.fieldOf("component").forGetter(p_340802_ -> p_340802_.component),
                        LootPoolEntries.CODEC.listOf().fieldOf("entries").forGetter(p_298103_ -> p_298103_.entries)
                    )
                )
                .apply(p_340801_, SetContainerContents::new)
    );
    private final ContainerComponentManipulator<?> component;
    private final List<LootPoolEntryContainer> entries;

    SetContainerContents(List<LootItemCondition> p_193035_, ContainerComponentManipulator<?> p_340836_, List<LootPoolEntryContainer> p_298300_) {
        super(p_193035_);
        this.component = p_340836_;
        this.entries = List.copyOf(p_298300_);
    }

    @Override
    public LootItemFunctionType<SetContainerContents> getType() {
        return LootItemFunctions.SET_CONTENTS;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (pStack.isEmpty()) {
            return pStack;
        } else {
            Stream.Builder<ItemStack> builder = Stream.builder();
            this.entries
                .forEach(
                    p_80916_ -> p_80916_.expand(
                            pContext, p_287573_ -> p_287573_.createItemStack(LootTable.createStackSplitter(pContext.getLevel(), builder::add), pContext)
                        )
                );
            this.component.setContents(pStack, builder.build());
            return pStack;
        }
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        super.validate(pContext);

        for (int i = 0; i < this.entries.size(); i++) {
            this.entries.get(i).validate(pContext.forChild(".entry[" + i + "]"));
        }
    }

    public static SetContainerContents.Builder setContents(ContainerComponentManipulator<?> pComponent) {
        return new SetContainerContents.Builder(pComponent);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetContainerContents.Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();
        private final ContainerComponentManipulator<?> component;

        public Builder(ContainerComponentManipulator<?> pComponent) {
            this.component = pComponent;
        }

        protected SetContainerContents.Builder getThis() {
            return this;
        }

        public SetContainerContents.Builder withEntry(LootPoolEntryContainer.Builder<?> pLootEntryBuilder) {
            this.entries.add(pLootEntryBuilder.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetContainerContents(this.getConditions(), this.component, this.entries.build());
        }
    }
}
