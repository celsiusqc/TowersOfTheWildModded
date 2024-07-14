package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public record Brightness(int block, int sky) {
    public static final Codec<Integer> LIGHT_VALUE_CODEC = ExtraCodecs.intRange(0, 15);
    public static final Codec<Brightness> CODEC = RecordCodecBuilder.create(
        p_270774_ -> p_270774_.group(
                    LIGHT_VALUE_CODEC.fieldOf("block").forGetter(Brightness::block), LIGHT_VALUE_CODEC.fieldOf("sky").forGetter(Brightness::sky)
                )
                .apply(p_270774_, Brightness::new)
    );
    public static Brightness FULL_BRIGHT = new Brightness(15, 15);

    public int pack() {
        return this.block << 4 | this.sky << 20;
    }

    public static Brightness unpack(int pPackedBrightness) {
        int i = pPackedBrightness >> 4 & 65535;
        int j = pPackedBrightness >> 20 & 65535;
        return new Brightness(i, j);
    }
}