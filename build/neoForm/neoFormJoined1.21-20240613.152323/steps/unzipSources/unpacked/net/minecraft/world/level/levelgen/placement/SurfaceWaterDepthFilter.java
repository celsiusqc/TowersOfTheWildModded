package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public class SurfaceWaterDepthFilter extends PlacementFilter {
    public static final MapCodec<SurfaceWaterDepthFilter> CODEC = RecordCodecBuilder.mapCodec(
        p_191953_ -> p_191953_.group(Codec.INT.fieldOf("max_water_depth").forGetter(p_191959_ -> p_191959_.maxWaterDepth))
                .apply(p_191953_, SurfaceWaterDepthFilter::new)
    );
    private final int maxWaterDepth;

    private SurfaceWaterDepthFilter(int p_191949_) {
        this.maxWaterDepth = p_191949_;
    }

    public static SurfaceWaterDepthFilter forMaxDepth(int pMaxWaterDepth) {
        return new SurfaceWaterDepthFilter(pMaxWaterDepth);
    }

    @Override
    protected boolean shouldPlace(PlacementContext pContext, RandomSource pRandom, BlockPos pPos) {
        int i = pContext.getHeight(Heightmap.Types.OCEAN_FLOOR, pPos.getX(), pPos.getZ());
        int j = pContext.getHeight(Heightmap.Types.WORLD_SURFACE, pPos.getX(), pPos.getZ());
        return j - i <= this.maxWaterDepth;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.SURFACE_WATER_DEPTH_FILTER;
    }
}
