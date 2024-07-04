package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.lighting.LightEngine;

public class NyliumBlock extends Block implements BonemealableBlock {
    public static final MapCodec<NyliumBlock> CODEC = simpleCodec(NyliumBlock::new);

    @Override
    public MapCodec<NyliumBlock> codec() {
        return CODEC;
    }

    public NyliumBlock(BlockBehaviour.Properties p_55057_) {
        super(p_55057_);
    }

    private static boolean canBeNylium(BlockState pState, LevelReader pReader, BlockPos pPos) {
        BlockPos blockpos = pPos.above();
        BlockState blockstate = pReader.getBlockState(blockpos);
        int i = LightEngine.getLightBlockInto(pReader, pState, pPos, blockstate, blockpos, Direction.UP, blockstate.getLightBlock(pReader, blockpos));
        return i < pReader.getMaxLightLevel();
    }

    /**
     * Performs a random tick on a block.
     */
    @Override
    protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!canBeNylium(pState, pLevel, pPos)) {
            pLevel.setBlockAndUpdate(pPos, Blocks.NETHERRACK.defaultBlockState());
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return pLevel.getBlockState(pPos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        BlockPos blockpos = pPos.above();
        ChunkGenerator chunkgenerator = pLevel.getChunkSource().getGenerator();
        Registry<ConfiguredFeature<?, ?>> registry = pLevel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        if (blockstate.is(Blocks.CRIMSON_NYLIUM)) {
            this.place(registry, NetherFeatures.CRIMSON_FOREST_VEGETATION_BONEMEAL, pLevel, chunkgenerator, pRandom, blockpos);
        } else if (blockstate.is(Blocks.WARPED_NYLIUM)) {
            this.place(registry, NetherFeatures.WARPED_FOREST_VEGETATION_BONEMEAL, pLevel, chunkgenerator, pRandom, blockpos);
            this.place(registry, NetherFeatures.NETHER_SPROUTS_BONEMEAL, pLevel, chunkgenerator, pRandom, blockpos);
            if (pRandom.nextInt(8) == 0) {
                this.place(registry, NetherFeatures.TWISTING_VINES_BONEMEAL, pLevel, chunkgenerator, pRandom, blockpos);
            }
        }
    }

    private void place(
        Registry<ConfiguredFeature<?, ?>> pFeatureRegistry,
        ResourceKey<ConfiguredFeature<?, ?>> pFeatureKey,
        ServerLevel pLevel,
        ChunkGenerator pChunkGenerator,
        RandomSource pRandom,
        BlockPos pPos
    ) {
        pFeatureRegistry.getHolder(pFeatureKey).ifPresent(p_255920_ -> p_255920_.value().place(pLevel, pChunkGenerator, pRandom, pPos));
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}
