package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public record LightPredicate(MinMaxBounds.Ints composite) {
    public static final Codec<LightPredicate> CODEC = RecordCodecBuilder.create(
        p_337376_ -> p_337376_.group(MinMaxBounds.Ints.CODEC.optionalFieldOf("light", MinMaxBounds.Ints.ANY).forGetter(LightPredicate::composite))
                .apply(p_337376_, LightPredicate::new)
    );

    public boolean matches(ServerLevel pLevel, BlockPos pPos) {
        return !pLevel.isLoaded(pPos) ? false : this.composite.matches(pLevel.getMaxLocalRawBrightness(pPos));
    }

    public static class Builder {
        private MinMaxBounds.Ints composite = MinMaxBounds.Ints.ANY;

        public static LightPredicate.Builder light() {
            return new LightPredicate.Builder();
        }

        public LightPredicate.Builder setComposite(MinMaxBounds.Ints pComposite) {
            this.composite = pComposite;
            return this;
        }

        public LightPredicate build() {
            return new LightPredicate(this.composite);
        }
    }
}
