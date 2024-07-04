package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public class HeightmapPlacement extends PlacementModifier {
    public static final MapCodec<HeightmapPlacement> CODEC = RecordCodecBuilder.mapCodec(
        p_191701_ -> p_191701_.group(Heightmap.Types.CODEC.fieldOf("heightmap").forGetter(p_191705_ -> p_191705_.heightmap))
                .apply(p_191701_, HeightmapPlacement::new)
    );
    private final Heightmap.Types heightmap;

    private HeightmapPlacement(Heightmap.Types p_191699_) {
        this.heightmap = p_191699_;
    }

    public static HeightmapPlacement onHeightmap(Heightmap.Types pHeightmap) {
        return new HeightmapPlacement(pHeightmap);
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext pContext, RandomSource pRandom, BlockPos pPos) {
        int i = pPos.getX();
        int j = pPos.getZ();
        int k = pContext.getHeight(this.heightmap, i, j);
        return k > pContext.getMinBuildHeight() ? Stream.of(new BlockPos(i, k, j)) : Stream.of();
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.HEIGHTMAP;
    }
}
