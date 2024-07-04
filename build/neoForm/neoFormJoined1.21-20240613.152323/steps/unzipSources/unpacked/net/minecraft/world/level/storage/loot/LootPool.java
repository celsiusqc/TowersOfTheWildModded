package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.apache.commons.lang3.mutable.MutableInt;

public class LootPool {
    public static final Codec<LootPool> CODEC = RecordCodecBuilder.create(
        p_344669_ -> p_344669_.group(
                    LootPoolEntries.CODEC.listOf().fieldOf("entries").forGetter(p_297995_ -> p_297995_.entries),
                    LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(p_297992_ -> p_297992_.conditions),
                    LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("functions", List.of()).forGetter(p_297994_ -> p_297994_.functions),
                    NumberProviders.CODEC.fieldOf("rolls").forGetter(p_297993_ -> p_297993_.rolls),
                    NumberProviders.CODEC.fieldOf("bonus_rolls").orElse(ConstantValue.exactly(0.0F)).forGetter(p_297997_ -> p_297997_.bonusRolls),
                    Codec.STRING.optionalFieldOf("name").forGetter(pool -> java.util.Optional.ofNullable(pool.name).filter(name -> !name.startsWith("custom#")))
                )
                .apply(p_344669_, LootPool::new)
    );
    private final List<LootPoolEntryContainer> entries;
    private final List<LootItemCondition> conditions;
    private final Predicate<LootContext> compositeCondition;
    private final List<LootItemFunction> functions;
    private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;
    private NumberProvider rolls;
    private NumberProvider bonusRolls;

    LootPool(
        List<LootPoolEntryContainer> p_298764_,
        List<LootItemCondition> p_299316_,
        List<LootItemFunction> p_298954_,
        NumberProvider p_165131_,
        NumberProvider p_165132_,
        java.util.Optional<String> name
    ) {
        this.entries = p_298764_;
        this.conditions = p_299316_;
        this.compositeCondition = Util.allOf(p_299316_);
        this.functions = p_298954_;
        this.compositeFunction = LootItemFunctions.compose(p_298954_);
        this.rolls = p_165131_;
        this.bonusRolls = p_165132_;
        this.name = name.orElse(null);
    }

    private void addRandomItem(Consumer<ItemStack> pStackConsumer, LootContext pContext) {
        RandomSource randomsource = pContext.getRandom();
        List<LootPoolEntry> list = Lists.newArrayList();
        MutableInt mutableint = new MutableInt();

        for (LootPoolEntryContainer lootpoolentrycontainer : this.entries) {
            lootpoolentrycontainer.expand(pContext, p_79048_ -> {
                int k = p_79048_.getWeight(pContext.getLuck());
                if (k > 0) {
                    list.add(p_79048_);
                    mutableint.add(k);
                }
            });
        }

        int i = list.size();
        if (mutableint.intValue() != 0 && i != 0) {
            if (i == 1) {
                list.get(0).createItemStack(pStackConsumer, pContext);
            } else {
                int j = randomsource.nextInt(mutableint.intValue());

                for (LootPoolEntry lootpoolentry : list) {
                    j -= lootpoolentry.getWeight(pContext.getLuck());
                    if (j < 0) {
                        lootpoolentry.createItemStack(pStackConsumer, pContext);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Generate the random items from this LootPool to the given {@code stackConsumer}.
     * This first checks this pool's conditions, generating nothing if they do not match.
     * Then the random items are generated based on the {@link LootPoolEntry LootPoolEntries} in this pool according to the rolls and bonusRolls, applying any loot functions.
     */
    public void addRandomItems(Consumer<ItemStack> pStackConsumer, LootContext pLootContext) {
        if (this.compositeCondition.test(pLootContext)) {
            Consumer<ItemStack> consumer = LootItemFunction.decorate(this.compositeFunction, pStackConsumer, pLootContext);
            int i = this.rolls.getInt(pLootContext) + Mth.floor(this.bonusRolls.getFloat(pLootContext) * pLootContext.getLuck());

            for (int j = 0; j < i; j++) {
                this.addRandomItem(consumer, pLootContext);
            }
        }
    }

    /**
     * Validate this LootPool according to the given context.
     */
    public void validate(ValidationContext pContext) {
        for (int i = 0; i < this.conditions.size(); i++) {
            this.conditions.get(i).validate(pContext.forChild(".condition[" + i + "]"));
        }

        for (int j = 0; j < this.functions.size(); j++) {
            this.functions.get(j).validate(pContext.forChild(".functions[" + j + "]"));
        }

        for (int k = 0; k < this.entries.size(); k++) {
            this.entries.get(k).validate(pContext.forChild(".entries[" + k + "]"));
        }

        this.rolls.validate(pContext.forChild(".rolls"));
        this.bonusRolls.validate(pContext.forChild(".bonusRolls"));
    }
    //======================== FORGE START =============================================
    private boolean isFrozen = false;
    public void freeze() { this.isFrozen = true; }
    public boolean isFrozen(){ return this.isFrozen; }
    private void checkFrozen() {
        if (this.isFrozen())
            throw new RuntimeException("Attempted to modify LootPool after being frozen!");
    }
    @org.jetbrains.annotations.Nullable
    private String name;
    @org.jetbrains.annotations.Nullable
    public String getName() { return this.name; }
    void setName(final String name) {
        if (this.name != null) {
            throw new UnsupportedOperationException("Cannot change the name of a pool when it has a name set!");
        }
        this.name = name;
    }
    public NumberProvider getRolls()        { return this.rolls; }
    public NumberProvider getBonusRolls() { return this.bonusRolls; }
    public void setRolls      (NumberProvider v){ checkFrozen(); this.rolls = v; }
    public void setBonusRolls(NumberProvider v){ checkFrozen(); this.bonusRolls = v; }
    //======================== FORGE END ===============================================

    public static LootPool.Builder lootPool() {
        return new LootPool.Builder();
    }

    public static class Builder implements FunctionUserBuilder<LootPool.Builder>, ConditionUserBuilder<LootPool.Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();
        private final ImmutableList.Builder<LootItemCondition> conditions = ImmutableList.builder();
        private final ImmutableList.Builder<LootItemFunction> functions = ImmutableList.builder();
        private NumberProvider rolls = ConstantValue.exactly(1.0F);
        private NumberProvider bonusRolls = ConstantValue.exactly(0.0F);
        @org.jetbrains.annotations.Nullable
        private String name;

        public LootPool.Builder setRolls(NumberProvider pRolls) {
            this.rolls = pRolls;
            return this;
        }

        public LootPool.Builder unwrap() {
            return this;
        }

        public LootPool.Builder setBonusRolls(NumberProvider pBonusRolls) {
            this.bonusRolls = pBonusRolls;
            return this;
        }

        public LootPool.Builder add(LootPoolEntryContainer.Builder<?> pEntriesBuilder) {
            this.entries.add(pEntriesBuilder.build());
            return this;
        }

        public LootPool.Builder when(LootItemCondition.Builder pConditionBuilder) {
            this.conditions.add(pConditionBuilder.build());
            return this;
        }

        public LootPool.Builder apply(LootItemFunction.Builder pFunctionBuilder) {
            this.functions.add(pFunctionBuilder.build());
            return this;
        }

        public LootPool.Builder name(String name) {
            this.name = name;
            return this;
        }

        public LootPool build() {
            return new LootPool(this.entries.build(), this.conditions.build(), this.functions.build(), this.rolls, this.bonusRolls, java.util.Optional.ofNullable(this.name));
        }
    }
}
