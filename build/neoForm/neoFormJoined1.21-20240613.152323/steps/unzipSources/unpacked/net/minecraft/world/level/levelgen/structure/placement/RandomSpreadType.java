package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum RandomSpreadType implements StringRepresentable {
    LINEAR("linear"),
    TRIANGULAR("triangular");

    public static final Codec<RandomSpreadType> CODEC = StringRepresentable.fromEnum(RandomSpreadType::values);
    private final String id;

    private RandomSpreadType(String pId) {
        this.id = pId;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public int evaluate(RandomSource pRandom, int pBound) {
        return switch (this) {
            case LINEAR -> pRandom.nextInt(pBound);
            case TRIANGULAR -> (pRandom.nextInt(pBound) + pRandom.nextInt(pBound)) / 2;
        };
    }
}
