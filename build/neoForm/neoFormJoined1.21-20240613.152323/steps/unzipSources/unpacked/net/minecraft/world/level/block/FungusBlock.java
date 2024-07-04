package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FungusBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<FungusBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_344656_ -> p_344656_.group(
                    ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter(p_304367_ -> p_304367_.feature),
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("grows_on").forGetter(p_304365_ -> p_304365_.requiredBlock),
                    propertiesCodec()
                )
                .apply(p_344656_, FungusBlock::new)
    );
    protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 9.0, 12.0);
    private static final double BONEMEAL_SUCCESS_PROBABILITY = 0.4;
    private final Block requiredBlock;
    private final ResourceKey<ConfiguredFeature<?, ?>> feature;

    @Override
    public MapCodec<FungusBlock> codec() {
        return CODEC;
    }

    public FungusBlock(ResourceKey<ConfiguredFeature<?, ?>> p_259087_, Block p_260223_, BlockBehaviour.Properties p_259749_) {
        super(p_259749_);
        this.feature = p_259087_;
        this.requiredBlock = p_260223_;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(BlockTags.NYLIUM) || pState.is(Blocks.MYCELIUM) || pState.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(pState, pLevel, pPos);
    }

    private Optional<? extends Holder<ConfiguredFeature<?, ?>>> getFeature(LevelReader pLevel) {
        return pLevel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(this.feature);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        BlockState blockstate = pLevel.getBlockState(pPos.below());
        return blockstate.is(this.requiredBlock);
    }

    @Override
    public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        return (double)pRandom.nextFloat() < 0.4;
    }

    @Override
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        // Neo: Fire the BlockGrowFeatureEvent and change the ifPresent call to use the event's result.
        var event = net.neoforged.neoforge.event.EventHooks.fireBlockGrowFeature(pLevel, pRandom, pPos, this.getFeature(pLevel).orElse(null));
        if (event.isCanceled()) return;
        Optional.ofNullable(event.getFeature()).ifPresent(p_256352_ -> p_256352_.value().place(pLevel, pLevel.getChunkSource().getGenerator(), pRandom, pPos));
    }
}
