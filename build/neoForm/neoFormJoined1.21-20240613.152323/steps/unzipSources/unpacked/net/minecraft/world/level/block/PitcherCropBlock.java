package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PitcherCropBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<PitcherCropBlock> CODEC = simpleCodec(PitcherCropBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    public static final int MAX_AGE = 4;
    private static final int DOUBLE_PLANT_AGE_INTERSECTION = 3;
    private static final int BONEMEAL_INCREASE = 1;
    private static final VoxelShape FULL_UPPER_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);
    private static final VoxelShape FULL_LOWER_SHAPE = Block.box(3.0, -1.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape COLLISION_SHAPE_BULB = Block.box(5.0, -1.0, 5.0, 11.0, 3.0, 11.0);
    private static final VoxelShape COLLISION_SHAPE_CROP = Block.box(3.0, -1.0, 3.0, 13.0, 5.0, 13.0);
    private static final VoxelShape[] UPPER_SHAPE_BY_AGE = new VoxelShape[]{Block.box(3.0, 0.0, 3.0, 13.0, 11.0, 13.0), FULL_UPPER_SHAPE};
    private static final VoxelShape[] LOWER_SHAPE_BY_AGE = new VoxelShape[]{
        COLLISION_SHAPE_BULB, Block.box(3.0, -1.0, 3.0, 13.0, 14.0, 13.0), FULL_LOWER_SHAPE, FULL_LOWER_SHAPE, FULL_LOWER_SHAPE
    };

    @Override
    public MapCodec<PitcherCropBlock> codec() {
        return CODEC;
    }

    public PitcherCropBlock(BlockBehaviour.Properties p_277780_) {
        super(p_277780_);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState();
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pState.getValue(HALF) == DoubleBlockHalf.UPPER
            ? UPPER_SHAPE_BY_AGE[Math.min(Math.abs(4 - (pState.getValue(AGE) + 1)), UPPER_SHAPE_BY_AGE.length - 1)]
            : LOWER_SHAPE_BY_AGE[pState.getValue(AGE)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (pState.getValue(AGE) == 0) {
            return COLLISION_SHAPE_BULB;
        } else {
            return pState.getValue(HALF) == DoubleBlockHalf.LOWER
                ? COLLISION_SHAPE_CROP
                : super.getCollisionShape(pState, pLevel, pPos, pContext);
        }
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    public BlockState updateShape(
        BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
    ) {
        if (isDouble(pState.getValue(AGE))) {
            return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
        } else {
            return pState.canSurvive(pLevel, pCurrentPos) ? pState : Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        net.neoforged.neoforge.common.util.TriState soilDecision = pLevel.getBlockState(pPos.below()).canSustainPlant(pLevel, pPos.below(), Direction.UP, pState);
        return isLower(pState) && !sufficientLight(pLevel, pPos) ? soilDecision.isTrue() : super.canSurvive(pState, pLevel, pPos);
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return pState.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
        super.createBlockStateDefinition(pBuilder);
    }

    @Override
    public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (pEntity instanceof Ravager && pLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            pLevel.destroyBlock(pPos, true, pEntity);
        }

        super.entityInside(pState, pLevel, pPos, pEntity);
    }

    @Override
    public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        return false;
    }

    /**
     * Called by BlockItem after this block has been placed.
     */
    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
    }

    @Override
    public boolean isRandomlyTicking(BlockState pState) {
        return pState.getValue(HALF) == DoubleBlockHalf.LOWER && !this.isMaxAge(pState);
    }

    /**
     * Performs a random tick on a block.
     */
    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        float f = CropBlock.getGrowthSpeed(pState, pLevel, pPos);
        boolean flag = pRandom.nextInt((int)(25.0F / f) + 1) == 0;
        if (flag) {
            this.grow(pLevel, pState, pPos, 1);
        }
    }

    private void grow(ServerLevel pLevel, BlockState pState, BlockPos pPos, int pAgeIncrement) {
        int i = Math.min(pState.getValue(AGE) + pAgeIncrement, 4);
        if (this.canGrow(pLevel, pPos, pState, i)) {
            BlockState blockstate = pState.setValue(AGE, Integer.valueOf(i));
            pLevel.setBlock(pPos, blockstate, 2);
            if (isDouble(i)) {
                pLevel.setBlock(pPos.above(), blockstate.setValue(HALF, DoubleBlockHalf.UPPER), 3);
            }
        }
    }

    private static boolean canGrowInto(LevelReader pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        return blockstate.isAir() || blockstate.is(Blocks.PITCHER_CROP);
    }

    private static boolean sufficientLight(LevelReader pLevel, BlockPos pPos) {
        return CropBlock.hasSufficientLight(pLevel, pPos);
    }

    private static boolean isLower(BlockState pState) {
        return pState.is(Blocks.PITCHER_CROP) && pState.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    private static boolean isDouble(int pAge) {
        return pAge >= 3;
    }

    private boolean canGrow(LevelReader pReader, BlockPos pPos, BlockState pState, int pAge) {
        return !this.isMaxAge(pState) && sufficientLight(pReader, pPos) && (!isDouble(pAge) || canGrowInto(pReader, pPos.above()));
    }

    private boolean isMaxAge(BlockState pState) {
        return pState.getValue(AGE) >= 4;
    }

    @Nullable
    private PitcherCropBlock.PosAndState getLowerHalf(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        if (isLower(pState)) {
            return new PitcherCropBlock.PosAndState(pPos, pState);
        } else {
            BlockPos blockpos = pPos.below();
            BlockState blockstate = pLevel.getBlockState(blockpos);
            return isLower(blockstate) ? new PitcherCropBlock.PosAndState(blockpos, blockstate) : null;
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        PitcherCropBlock.PosAndState pitchercropblock$posandstate = this.getLowerHalf(pLevel, pPos, pState);
        return pitchercropblock$posandstate == null
            ? false
            : this.canGrow(
                pLevel, pitchercropblock$posandstate.pos, pitchercropblock$posandstate.state, pitchercropblock$posandstate.state.getValue(AGE) + 1
            );
    }

    @Override
    public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
        PitcherCropBlock.PosAndState pitchercropblock$posandstate = this.getLowerHalf(pLevel, pPos, pState);
        if (pitchercropblock$posandstate != null) {
            this.grow(pLevel, pitchercropblock$posandstate.state, pitchercropblock$posandstate.pos, 1);
        }
    }

    static record PosAndState(BlockPos pos, BlockState state) {
    }
}
