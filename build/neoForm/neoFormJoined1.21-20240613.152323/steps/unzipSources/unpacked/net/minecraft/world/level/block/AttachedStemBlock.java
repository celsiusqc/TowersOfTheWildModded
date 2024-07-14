package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AttachedStemBlock extends BushBlock {
    public static final MapCodec<AttachedStemBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308799_ -> p_308799_.group(
                    ResourceKey.codec(Registries.BLOCK).fieldOf("fruit").forGetter(p_304711_ -> p_304711_.fruit),
                    ResourceKey.codec(Registries.BLOCK).fieldOf("stem").forGetter(p_304975_ -> p_304975_.stem),
                    ResourceKey.codec(Registries.ITEM).fieldOf("seed").forGetter(p_304775_ -> p_304775_.seed),
                    propertiesCodec()
                )
                .apply(p_308799_, AttachedStemBlock::new)
    );
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    protected static final float AABB_OFFSET = 2.0F;
    private static final Map<Direction, VoxelShape> AABBS = Maps.newEnumMap(
        ImmutableMap.of(
            Direction.SOUTH,
            Block.box(6.0, 0.0, 6.0, 10.0, 10.0, 16.0),
            Direction.WEST,
            Block.box(0.0, 0.0, 6.0, 10.0, 10.0, 10.0),
            Direction.NORTH,
            Block.box(6.0, 0.0, 0.0, 10.0, 10.0, 10.0),
            Direction.EAST,
            Block.box(6.0, 0.0, 6.0, 16.0, 10.0, 10.0)
        )
    );
    private final ResourceKey<Block> fruit;
    private final ResourceKey<Block> stem;
    private final ResourceKey<Item> seed;

    @Override
    public MapCodec<AttachedStemBlock> codec() {
        return CODEC;
    }

    public AttachedStemBlock(ResourceKey<Block> p_304582_, ResourceKey<Block> p_304450_, ResourceKey<Item> p_304828_, BlockBehaviour.Properties p_152062_) {
        super(p_152062_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.stem = p_304582_;
        this.fruit = p_304450_;
        this.seed = p_304828_;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return AABBS.get(pState.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (!pFacingState.is(this.fruit) && pFacing == pState.getValue(FACING)) {
            Optional<Block> optional = pLevel.registryAccess().registryOrThrow(Registries.BLOCK).getOptional(this.stem);
            if (optional.isPresent()) {
                return optional.get().defaultBlockState().trySetValue(StemBlock.AGE, Integer.valueOf(7));
            }
        }

        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return new ItemStack(DataFixUtils.orElse(pLevel.registryAccess().registryOrThrow(Registries.ITEM).getOptional(this.seed), this));
    }

    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#rotate} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#mirror} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }
}
