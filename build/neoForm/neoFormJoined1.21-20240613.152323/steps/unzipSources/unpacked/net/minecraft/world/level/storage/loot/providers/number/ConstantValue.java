package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * A {@link NumberProvider} that provides a constant value.
 */
public record ConstantValue(float value) implements NumberProvider {
    public static final MapCodec<ConstantValue> CODEC = RecordCodecBuilder.mapCodec(
        p_299242_ -> p_299242_.group(Codec.FLOAT.fieldOf("value").forGetter(ConstantValue::value)).apply(p_299242_, ConstantValue::new)
    );
    public static final Codec<ConstantValue> INLINE_CODEC = Codec.FLOAT.xmap(ConstantValue::new, ConstantValue::value);

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.CONSTANT;
    }

    @Override
    public float getFloat(LootContext pLootContext) {
        return this.value;
    }

    public static ConstantValue exactly(float pValue) {
        return new ConstantValue(pValue);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther != null && this.getClass() == pOther.getClass() ? Float.compare(((ConstantValue)pOther).value, this.value) == 0 : false;
        }
    }

    @Override
    public int hashCode() {
        return this.value != 0.0F ? Float.floatToIntBits(this.value) : 0;
    }
}
