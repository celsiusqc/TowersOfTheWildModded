package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

/**
 * Generates a random number which is uniformly distributed between a minimum and a maximum.
 * Minimum and maximum are themselves NumberProviders.
 */
public record UniformGenerator(NumberProvider min, NumberProvider max) implements NumberProvider {
    public static final MapCodec<UniformGenerator> CODEC = RecordCodecBuilder.mapCodec(
        p_298748_ -> p_298748_.group(
                    NumberProviders.CODEC.fieldOf("min").forGetter(UniformGenerator::min),
                    NumberProviders.CODEC.fieldOf("max").forGetter(UniformGenerator::max)
                )
                .apply(p_298748_, UniformGenerator::new)
    );

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.UNIFORM;
    }

    public static UniformGenerator between(float pMin, float pMax) {
        return new UniformGenerator(ConstantValue.exactly(pMin), ConstantValue.exactly(pMax));
    }

    @Override
    public int getInt(LootContext pLootContext) {
        return Mth.nextInt(pLootContext.getRandom(), this.min.getInt(pLootContext), this.max.getInt(pLootContext));
    }

    @Override
    public float getFloat(LootContext pLootContext) {
        return Mth.nextFloat(pLootContext.getRandom(), this.min.getFloat(pLootContext), this.max.getFloat(pLootContext));
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Sets.union(this.min.getReferencedContextParams(), this.max.getReferencedContextParams());
    }
}
