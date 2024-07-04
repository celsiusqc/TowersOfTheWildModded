package net.minecraft.world.entity.ai.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.util.VisibleForDebug;

public class ExpirableValue<T> {
    private final T value;
    private long timeToLive;

    public ExpirableValue(T pValue, long pTimeToLive) {
        this.value = pValue;
        this.timeToLive = pTimeToLive;
    }

    public void tick() {
        if (this.canExpire()) {
            this.timeToLive--;
        }
    }

    public static <T> ExpirableValue<T> of(T pValue) {
        return new ExpirableValue<>(pValue, Long.MAX_VALUE);
    }

    public static <T> ExpirableValue<T> of(T pValue, long pTimeToLive) {
        return new ExpirableValue<>(pValue, pTimeToLive);
    }

    public long getTimeToLive() {
        return this.timeToLive;
    }

    public T getValue() {
        return this.value;
    }

    public boolean hasExpired() {
        return this.timeToLive <= 0L;
    }

    @Override
    public String toString() {
        return this.value + (this.canExpire() ? " (ttl: " + this.timeToLive + ")" : "");
    }

    @VisibleForDebug
    public boolean canExpire() {
        return this.timeToLive != Long.MAX_VALUE;
    }

    public static <T> Codec<ExpirableValue<T>> codec(Codec<T> pValueCodec) {
        return RecordCodecBuilder.create(
            p_337791_ -> p_337791_.group(
                        pValueCodec.fieldOf("value").forGetter(p_148193_ -> p_148193_.value),
                        Codec.LONG
                            .lenientOptionalFieldOf("ttl")
                            .forGetter(p_148187_ -> p_148187_.canExpire() ? Optional.of(p_148187_.timeToLive) : Optional.empty())
                    )
                    .apply(p_337791_, (p_148189_, p_148190_) -> new ExpirableValue<>(p_148189_, p_148190_.orElse(Long.MAX_VALUE)))
        );
    }
}
