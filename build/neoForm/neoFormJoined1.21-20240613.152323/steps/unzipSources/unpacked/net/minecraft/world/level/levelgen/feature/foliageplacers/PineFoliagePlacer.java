package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class PineFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<PineFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
        p_68698_ -> foliagePlacerParts(p_68698_)
                .and(IntProvider.codec(0, 24).fieldOf("height").forGetter(p_161500_ -> p_161500_.height))
                .apply(p_68698_, PineFoliagePlacer::new)
    );
    private final IntProvider height;

    public PineFoliagePlacer(IntProvider p_161486_, IntProvider p_161487_, IntProvider p_161488_) {
        super(p_161486_, p_161487_);
        this.height = p_161488_;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return FoliagePlacerType.PINE_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(
        LevelSimulatedReader pLevel,
        FoliagePlacer.FoliageSetter pBlockSetter,
        RandomSource pRandom,
        TreeConfiguration pConfig,
        int pMaxFreeTreeHeight,
        FoliagePlacer.FoliageAttachment pAttachment,
        int pFoliageHeight,
        int pFoliageRadius,
        int pOffset
    ) {
        int i = 0;

        for (int j = pOffset; j >= pOffset - pFoliageHeight; j--) {
            this.placeLeavesRow(pLevel, pBlockSetter, pRandom, pConfig, pAttachment.pos(), i, j, pAttachment.doubleTrunk());
            if (i >= 1 && j == pOffset - pFoliageHeight + 1) {
                i--;
            } else if (i < pFoliageRadius + pAttachment.radiusOffset()) {
                i++;
            }
        }
    }

    @Override
    public int foliageRadius(RandomSource pRandom, int pRadius) {
        return super.foliageRadius(pRandom, pRadius) + pRandom.nextInt(Math.max(pRadius + 1, 1));
    }

    @Override
    public int foliageHeight(RandomSource pRandom, int pHeight, TreeConfiguration pConfig) {
        return this.height.sample(pRandom);
    }

    /**
     * Skips certain positions based on the provided shape, such as rounding corners randomly.
     * The coordinates are passed in as absolute value, and should be within [0, {@code range}].
     */
    @Override
    protected boolean shouldSkipLocation(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge) {
        return pLocalX == pRange && pLocalZ == pRange && pRange > 0;
    }
}
