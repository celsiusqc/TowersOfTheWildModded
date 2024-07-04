package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FrogspawnBlock extends Block {
    public static final MapCodec<FrogspawnBlock> CODEC = simpleCodec(FrogspawnBlock::new);
    private static final int MIN_TADPOLES_SPAWN = 2;
    private static final int MAX_TADPOLES_SPAWN = 5;
    private static final int DEFAULT_MIN_HATCH_TICK_DELAY = 3600;
    private static final int DEFAULT_MAX_HATCH_TICK_DELAY = 12000;
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.5, 16.0);
    private static int minHatchTickDelay = 3600;
    private static int maxHatchTickDelay = 12000;

    @Override
    public MapCodec<FrogspawnBlock> codec() {
        return CODEC;
    }

    public FrogspawnBlock(BlockBehaviour.Properties p_221177_) {
        super(p_221177_);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return mayPlaceOn(pLevel, pPos.below());
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
        pLevel.scheduleTick(pPos, this, getFrogspawnHatchDelay(pLevel.getRandom()));
    }

    private static int getFrogspawnHatchDelay(RandomSource pRandom) {
        return pRandom.nextInt(minHatchTickDelay, maxHatchTickDelay);
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(
        BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos
    ) {
        return !this.canSurvive(pState, pLevel, pPos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
    }

    @Override
    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!this.canSurvive(pState, pLevel, pPos)) {
            this.destroyBlock(pLevel, pPos);
        } else {
            this.hatchFrogspawn(pLevel, pPos, pRandom);
        }
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (pEntity.getType().equals(EntityType.FALLING_BLOCK)) {
            this.destroyBlock(pLevel, pPos);
        }
    }

    private static boolean mayPlaceOn(BlockGetter pLevel, BlockPos pPos) {
        FluidState fluidstate = pLevel.getFluidState(pPos);
        FluidState fluidstate1 = pLevel.getFluidState(pPos.above());
        return fluidstate.getType() == Fluids.WATER && fluidstate1.getType() == Fluids.EMPTY;
    }

    private void hatchFrogspawn(ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        this.destroyBlock(pLevel, pPos);
        pLevel.playSound(null, pPos, SoundEvents.FROGSPAWN_HATCH, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.spawnTadpoles(pLevel, pPos, pRandom);
    }

    private void destroyBlock(Level pLevel, BlockPos pPos) {
        pLevel.destroyBlock(pPos, false);
    }

    private void spawnTadpoles(ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        int i = pRandom.nextInt(2, 6);

        for (int j = 1; j <= i; j++) {
            Tadpole tadpole = EntityType.TADPOLE.create(pLevel);
            if (tadpole != null) {
                double d0 = (double)pPos.getX() + this.getRandomTadpolePositionOffset(pRandom);
                double d1 = (double)pPos.getZ() + this.getRandomTadpolePositionOffset(pRandom);
                int k = pRandom.nextInt(1, 361);
                tadpole.moveTo(d0, (double)pPos.getY() - 0.5, d1, (float)k, 0.0F);
                tadpole.setPersistenceRequired();
                pLevel.addFreshEntity(tadpole);
            }
        }
    }

    private double getRandomTadpolePositionOffset(RandomSource pRandom) {
        double d0 = 0.2F;
        return Mth.clamp(pRandom.nextDouble(), 0.2F, 0.7999999970197678);
    }

    @VisibleForTesting
    public static void setHatchDelay(int pMinHatchDelay, int pMaxHatchDelay) {
        minHatchTickDelay = pMinHatchDelay;
        maxHatchTickDelay = pMaxHatchDelay;
    }

    @VisibleForTesting
    public static void setDefaultHatchDelay() {
        minHatchTickDelay = 3600;
        maxHatchTickDelay = 12000;
    }
}
