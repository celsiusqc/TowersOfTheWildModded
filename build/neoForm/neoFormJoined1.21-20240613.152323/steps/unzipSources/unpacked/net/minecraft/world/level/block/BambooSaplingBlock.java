package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BambooSaplingBlock extends Block implements BonemealableBlock {
    public static final MapCodec<BambooSaplingBlock> CODEC = simpleCodec(BambooSaplingBlock::new);
    protected static final float SAPLING_AABB_OFFSET = 4.0F;
    protected static final VoxelShape SAPLING_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 12.0, 12.0);

    @Override
    public MapCodec<BambooSaplingBlock> codec() {
        return CODEC;
    }

    public BambooSaplingBlock(BlockBehaviour.Properties p_48957_) {
        super(p_48957_);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Vec3 vec3 = pState.getOffset(pLevel, pPos);
        return SAPLING_SHAPE.move(vec3.x, vec3.y, vec3.z);
    }

    /**
     * Performs a random tick on a block.
     */
    @Override
    protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pRandom.nextInt(3) == 0 && pLevel.isEmptyBlock(pPos.above()) && pLevel.getRawBrightness(pPos.above(), 0) >= 9) {
            this.growBamboo(pLevel, pPos);
        }
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        net.neoforged.neoforge.common.util.TriState soilDecision = pLevel.getBlockState(pPos.below()).canSustainPlant(pLevel, pPos.below(), net.minecraft.core.Direction.UP, pState);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        return pLevel.getBlockState(pPos.below()).is(BlockTags.BAMBOO_PLANTABLE_ON);
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (!pState.canSurvive(pLevel, pCurrentPos)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if (pFacing == Direction.UP && pFacingState.is(Blocks.BAMBOO)) {
                pLevel.setBlock(pCurrentPos, Blocks.BAMBOO.defaultBlockState(), 2);
            }

            return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return new ItemStack(Items.BAMBOO);
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
        this.growBamboo(pLevel, pPos);
    }

    /**
     * Get the hardness of this Block relative to the ability of the given player
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getDestroyProgress} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected float getDestroyProgress(BlockState pState, Player pPlayer, BlockGetter pLevel, BlockPos pPos) {
        return pPlayer.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SWORD_DIG) ? 1.0F : super.getDestroyProgress(pState, pPlayer, pLevel, pPos);
    }

    protected void growBamboo(Level pLevel, BlockPos pState) {
        pLevel.setBlock(pState.above(), Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.LEAVES, BambooLeaves.SMALL), 3);
    }
}
