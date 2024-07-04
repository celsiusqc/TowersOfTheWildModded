package net.minecraft.server.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Filterable<T>(T raw, Optional<T> filtered) {
    public static <T> Codec<Filterable<T>> codec(Codec<T> pCodec) {
        Codec<Filterable<T>> codec = RecordCodecBuilder.create(
            p_337552_ -> p_337552_.group(
                        pCodec.fieldOf("raw").forGetter(Filterable::raw), pCodec.optionalFieldOf("filtered").forGetter(Filterable::filtered)
                    )
                    .apply(p_337552_, Filterable::new)
        );
        Codec<Filterable<T>> codec1 = pCodec.xmap(Filterable::passThrough, Filterable::raw);
        return Codec.withAlternative(codec, codec1);
    }

    public static <B extends ByteBuf, T> StreamCodec<B, Filterable<T>> streamCodec(StreamCodec<B, T> pCodec) {
        return StreamCodec.composite(pCodec, Filterable::raw, pCodec.apply(ByteBufCodecs::optional), Filterable::filtered, Filterable::new);
    }

    public static <T> Filterable<T> passThrough(T p_330890_) {
        return new Filterable<>(p_330890_, Optional.empty());
    }

    public static Filterable<String> from(FilteredText pFilteredText) {
        return new Filterable<>(pFilteredText.raw(), pFilteredText.isFiltered() ? Optional.of(pFilteredText.filteredOrEmpty()) : Optional.empty());
    }

    public T get(boolean pFiltered) {
        return pFiltered ? this.filtered.orElse(this.raw) : this.raw;
    }

    public <U> Filterable<U> map(Function<T, U> pMappingFunction) {
        return new Filterable<>(pMappingFunction.apply(this.raw), this.filtered.map(pMappingFunction));
    }

    public <U> Optional<Filterable<U>> resolve(Function<T, Optional<U>> pResolver) {
        Optional<U> optional = pResolver.apply(this.raw);
        if (optional.isEmpty()) {
            return Optional.empty();
        } else if (this.filtered.isPresent()) {
            Optional<U> optional1 = pResolver.apply(this.filtered.get());
            return optional1.isEmpty() ? Optional.empty() : Optional.of(new Filterable<>(optional.get(), optional1));
        } else {
            return Optional.of(new Filterable<>(optional.get(), Optional.empty()));
        }
    }
}
