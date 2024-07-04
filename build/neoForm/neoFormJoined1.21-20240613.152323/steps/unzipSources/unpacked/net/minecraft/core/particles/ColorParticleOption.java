package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FastColor;

public class ColorParticleOption implements ParticleOptions {
    private final ParticleType<ColorParticleOption> type;
    private final int color;

    public static MapCodec<ColorParticleOption> codec(ParticleType<ColorParticleOption> pParticleType) {
        return ExtraCodecs.ARGB_COLOR_CODEC.xmap(p_333828_ -> new ColorParticleOption(pParticleType, p_333828_), p_333908_ -> p_333908_.color).fieldOf("color");
    }

    public static StreamCodec<? super ByteBuf, ColorParticleOption> streamCodec(ParticleType<ColorParticleOption> pType) {
        return ByteBufCodecs.INT.map(p_333912_ -> new ColorParticleOption(pType, p_333912_), p_334072_ -> p_334072_.color);
    }

    private ColorParticleOption(ParticleType<ColorParticleOption> pType, int pColor) {
        this.type = pType;
        this.color = pColor;
    }

    @Override
    public ParticleType<ColorParticleOption> getType() {
        return this.type;
    }

    public float getRed() {
        return (float)FastColor.ARGB32.red(this.color) / 255.0F;
    }

    public float getGreen() {
        return (float)FastColor.ARGB32.green(this.color) / 255.0F;
    }

    public float getBlue() {
        return (float)FastColor.ARGB32.blue(this.color) / 255.0F;
    }

    public float getAlpha() {
        return (float)FastColor.ARGB32.alpha(this.color) / 255.0F;
    }

    public static ColorParticleOption create(ParticleType<ColorParticleOption> pType, int pColor) {
        return new ColorParticleOption(pType, pColor);
    }

    public static ColorParticleOption create(ParticleType<ColorParticleOption> pType, float pRed, float pGreen, float pBlue) {
        return create(pType, FastColor.ARGB32.colorFromFloat(1.0F, pRed, pGreen, pBlue));
    }
}
