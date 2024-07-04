package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/**
 * LootItemCondition that checks if a number provided by a {@link NumberProvider} is within an {@link IntRange}.
 */
public record ValueCheckCondition(NumberProvider provider, IntRange range) implements LootItemCondition {
    public static final MapCodec<ValueCheckCondition> CODEC = RecordCodecBuilder.mapCodec(
        p_298196_ -> p_298196_.group(
                    NumberProviders.CODEC.fieldOf("value").forGetter(ValueCheckCondition::provider),
                    IntRange.CODEC.fieldOf("range").forGetter(ValueCheckCondition::range)
                )
                .apply(p_298196_, ValueCheckCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.VALUE_CHECK;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Sets.union(this.provider.getReferencedContextParams(), this.range.getReferencedContextParams());
    }

    public boolean test(LootContext pContext) {
        return this.range.test(pContext, this.provider.getInt(pContext));
    }

    public static LootItemCondition.Builder hasValue(NumberProvider pProvider, IntRange pRange) {
        return () -> new ValueCheckCondition(pProvider, pRange);
    }
}
