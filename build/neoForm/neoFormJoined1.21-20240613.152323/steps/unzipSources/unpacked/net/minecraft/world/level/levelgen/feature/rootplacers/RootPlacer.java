package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.FluidState;

public abstract class RootPlacer {
    public static final Codec<RootPlacer> CODEC = BuiltInRegistries.ROOT_PLACER_TYPE.byNameCodec().dispatch(RootPlacer::type, RootPlacerType::codec);
    protected final IntProvider trunkOffsetY;
    protected final BlockStateProvider rootProvider;
    protected final Optional<AboveRootPlacement> aboveRootPlacement;

    protected static <P extends RootPlacer> P3<Mu<P>, IntProvider, BlockStateProvider, Optional<AboveRootPlacement>> rootPlacerParts(Instance<P> pInstance) {
        return pInstance.group(
            IntProvider.CODEC.fieldOf("trunk_offset_y").forGetter(p_225897_ -> p_225897_.trunkOffsetY),
            BlockStateProvider.CODEC.fieldOf("root_provider").forGetter(p_225895_ -> p_225895_.rootProvider),
            AboveRootPlacement.CODEC.optionalFieldOf("above_root_placement").forGetter(p_225888_ -> p_225888_.aboveRootPlacement)
        );
    }

    public RootPlacer(IntProvider pTrunkOffset, BlockStateProvider pRootProvider, Optional<AboveRootPlacement> pAboveRootPlacement) {
        this.trunkOffsetY = pTrunkOffset;
        this.rootProvider = pRootProvider;
        this.aboveRootPlacement = pAboveRootPlacement;
    }

    protected abstract RootPlacerType<?> type();

    public abstract boolean placeRoots(
        LevelSimulatedReader pLevel,
        BiConsumer<BlockPos, BlockState> pBlockSetter,
        RandomSource pRandom,
        BlockPos pPos,
        BlockPos pTrunkOrigin,
        TreeConfiguration pTreeConfig
    );

    protected boolean canPlaceRoot(LevelSimulatedReader pLevel, BlockPos pPos) {
        return TreeFeature.validTreePos(pLevel, pPos);
    }

    protected void placeRoot(
        LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, RandomSource pRandom, BlockPos pPos, TreeConfiguration pTreeConfig
    ) {
        if (this.canPlaceRoot(pLevel, pPos)) {
            pBlockSetter.accept(pPos, this.getPotentiallyWaterloggedState(pLevel, pPos, this.rootProvider.getState(pRandom, pPos)));
            if (this.aboveRootPlacement.isPresent()) {
                AboveRootPlacement aboverootplacement = this.aboveRootPlacement.get();
                BlockPos blockpos = pPos.above();
                if (pRandom.nextFloat() < aboverootplacement.aboveRootPlacementChance()
                    && pLevel.isStateAtPosition(blockpos, BlockBehaviour.BlockStateBase::isAir)) {
                    pBlockSetter.accept(
                        blockpos,
                        this.getPotentiallyWaterloggedState(pLevel, blockpos, aboverootplacement.aboveRootProvider().getState(pRandom, blockpos))
                    );
                }
            }
        }
    }

    protected BlockState getPotentiallyWaterloggedState(LevelSimulatedReader pLevel, BlockPos pPos, BlockState pState) {
        if (pState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean flag = pLevel.isFluidAtPosition(pPos, p_225890_ -> p_225890_.is(FluidTags.WATER));
            return pState.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(flag));
        } else {
            return pState;
        }
    }

    public BlockPos getTrunkOrigin(BlockPos pPos, RandomSource pRandom) {
        return pPos.above(this.trunkOffsetY.sample(pRandom));
    }
}
