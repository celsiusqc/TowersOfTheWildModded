package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class CoralBlock extends Block {
    public static final MapCodec<Block> DEAD_CORAL_FIELD = BuiltInRegistries.BLOCK.byNameCodec().fieldOf("dead");
    public static final MapCodec<CoralBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308817_ -> p_308817_.group(DEAD_CORAL_FIELD.forGetter(p_304453_ -> p_304453_.deadBlock), propertiesCodec()).apply(p_308817_, CoralBlock::new)
    );
    private final Block deadBlock;

    public CoralBlock(Block p_52130_, BlockBehaviour.Properties p_52131_) {
        super(p_52131_);
        this.deadBlock = p_52130_;
    }

    @Override
    public MapCodec<CoralBlock> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!this.scanForWater(pLevel, pPos)) {
            pLevel.setBlock(pPos, this.deadBlock.defaultBlockState(), 2);
        }
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (!this.scanForWater(pLevel, pCurrentPos)) {
            pLevel.scheduleTick(pCurrentPos, this, 60 + pLevel.getRandom().nextInt(40));
        }

        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    protected boolean scanForWater(BlockGetter pLevel, BlockPos pPos) {
        BlockState state = pLevel.getBlockState(pPos);
        for (Direction direction : Direction.values()) {
            FluidState fluidstate = pLevel.getFluidState(pPos.relative(direction));
            if (state.canBeHydrated(pLevel, pPos, fluidstate, pPos.relative(direction))) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        if (!this.scanForWater(pContext.getLevel(), pContext.getClickedPos())) {
            pContext.getLevel().scheduleTick(pContext.getClickedPos(), this, 60 + pContext.getLevel().getRandom().nextInt(40));
        }

        return this.defaultBlockState();
    }
}
