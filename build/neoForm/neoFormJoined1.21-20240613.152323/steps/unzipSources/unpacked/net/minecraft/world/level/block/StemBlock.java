package net.minecraft.world.level.block;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StemBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<StemBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308841_ -> p_308841_.group(
                    ResourceKey.codec(Registries.BLOCK).fieldOf("fruit").forGetter(p_304695_ -> p_304695_.fruit),
                    ResourceKey.codec(Registries.BLOCK).fieldOf("attached_stem").forGetter(p_304684_ -> p_304684_.attachedStem),
                    ResourceKey.codec(Registries.ITEM).fieldOf("seed").forGetter(p_304883_ -> p_304883_.seed),
                    propertiesCodec()
                )
                .apply(p_308841_, StemBlock::new)
    );
    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    protected static final float AABB_OFFSET = 1.0F;
    protected static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
        Block.box(7.0, 0.0, 7.0, 9.0, 2.0, 9.0),
        Block.box(7.0, 0.0, 7.0, 9.0, 4.0, 9.0),
        Block.box(7.0, 0.0, 7.0, 9.0, 6.0, 9.0),
        Block.box(7.0, 0.0, 7.0, 9.0, 8.0, 9.0),
        Block.box(7.0, 0.0, 7.0, 9.0, 10.0, 9.0),
        Block.box(7.0, 0.0, 7.0, 9.0, 12.0, 9.0),
        Block.box(7.0, 0.0, 7.0, 9.0, 14.0, 9.0),
        Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0)
    };
    private final ResourceKey<Block> fruit;
    private final ResourceKey<Block> attachedStem;
    private final ResourceKey<Item> seed;

    @Override
    public MapCodec<StemBlock> codec() {
        return CODEC;
    }

    public StemBlock(ResourceKey<Block> p_304976_, ResourceKey<Block> p_304671_, ResourceKey<Item> p_304942_, BlockBehaviour.Properties p_154730_) {
        super(p_154730_);
        this.fruit = p_304976_;
        this.attachedStem = p_304671_;
        this.seed = p_304942_;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, Integer.valueOf(0)));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE_BY_AGE[pState.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.is(Blocks.FARMLAND);
    }

    /**
     * Performs a random tick on a block.
     */
    @Override
    protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!pLevel.isAreaLoaded(pPos, 1)) return; // Forge: prevent loading unloaded chunks when checking neighbor's light
        if (pLevel.getRawBrightness(pPos, 0) >= 9) {
            float f = CropBlock.getGrowthSpeed(this, pLevel, pPos);
            if (net.neoforged.neoforge.common.CommonHooks.canCropGrow(pLevel, pPos, pState, pRandom.nextInt((int)(25.0F / f) + 1) == 0)) {
                int i = pState.getValue(AGE);
                if (i < 7) {
                    pLevel.setBlock(pPos, pState.setValue(AGE, Integer.valueOf(i + 1)), 2);
                } else {
                    Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
                    BlockPos blockpos = pPos.relative(direction);
                    BlockState blockstate = pLevel.getBlockState(blockpos.below());
                    if (pLevel.isEmptyBlock(blockpos) && (blockstate.is(Blocks.FARMLAND) || blockstate.is(BlockTags.DIRT))) { // TODO 1.20.3 PORTING: reimplement canSustainPlant check
                        Registry<Block> registry = pLevel.registryAccess().registryOrThrow(Registries.BLOCK);
                        Optional<Block> optional = registry.getOptional(this.fruit);
                        Optional<Block> optional1 = registry.getOptional(this.attachedStem);
                        if (optional.isPresent() && optional1.isPresent()) {
                            pLevel.setBlockAndUpdate(blockpos, optional.get().defaultBlockState());
                            pLevel.setBlockAndUpdate(pPos, optional1.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
                        }
                    }
                }
                net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(pLevel, pPos, pState);
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return new ItemStack(DataFixUtils.orElse(pLevel.registryAccess().registryOrThrow(Registries.ITEM).getOptional(this.seed), this));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return pState.getValue(AGE) != 7;
    }

    @Override
    public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        int i = Math.min(7, pState.getValue(AGE) + Mth.nextInt(pLevel.random, 2, 5));
        BlockState blockstate = pState.setValue(AGE, Integer.valueOf(i));
        pLevel.setBlock(pPos, blockstate, 2);
        if (i == 7) {
            blockstate.randomTick(pLevel, pPos, pLevel.random);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
    }

    //FORGE START
    @Override
    public net.neoforged.neoforge.common.PlantType getPlantType(BlockGetter world, BlockPos pos) {
        return net.neoforged.neoforge.common.PlantType.CROP;
    }
}
