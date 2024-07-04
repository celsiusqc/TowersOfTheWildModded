package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CampfireBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CampfireBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308808_ -> p_308808_.group(
                    Codec.BOOL.fieldOf("spawn_particles").forGetter(p_304361_ -> p_304361_.spawnParticles),
                    Codec.intRange(0, 1000).fieldOf("fire_damage").forGetter(p_304360_ -> p_304360_.fireDamage),
                    propertiesCodec()
                )
                .apply(p_308808_, CampfireBlock::new)
    );
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty SIGNAL_FIRE = BlockStateProperties.SIGNAL_FIRE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape VIRTUAL_FENCE_POST = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
    private static final int SMOKE_DISTANCE = 5;
    private final boolean spawnParticles;
    private final int fireDamage;

    @Override
    public MapCodec<CampfireBlock> codec() {
        return CODEC;
    }

    public CampfireBlock(boolean p_51236_, int p_51237_, BlockBehaviour.Properties p_51238_) {
        super(p_51238_);
        this.spawnParticles = p_51236_;
        this.fireDamage = p_51237_;
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(LIT, Boolean.valueOf(true))
                .setValue(SIGNAL_FIRE, Boolean.valueOf(false))
                .setValue(WATERLOGGED, Boolean.valueOf(false))
                .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
    ) {
        if (pLevel.getBlockEntity(pPos) instanceof CampfireBlockEntity campfireblockentity) {
            ItemStack itemstack = pPlayer.getItemInHand(pHand);
            Optional<RecipeHolder<CampfireCookingRecipe>> optional = campfireblockentity.getCookableRecipe(itemstack);
            if (optional.isPresent()) {
                if (!pLevel.isClientSide && campfireblockentity.placeFood(pPlayer, itemstack, optional.get().value().getCookingTime())) {
                    pPlayer.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
                    return ItemInteractionResult.SUCCESS;
                }

                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (pState.getValue(LIT) && pEntity instanceof LivingEntity) {
            pEntity.hurt(pLevel.damageSources().campfire(), (float)this.fireDamage);
        }

        super.entityInside(pState, pLevel, pPos, pEntity);
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof CampfireBlockEntity) {
                Containers.dropContents(pLevel, pPos, ((CampfireBlockEntity)blockentity).getItems());
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        LevelAccessor levelaccessor = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        boolean flag = levelaccessor.getFluidState(blockpos).getType() == Fluids.WATER;
        return this.defaultBlockState()
            .setValue(WATERLOGGED, Boolean.valueOf(flag))
            .setValue(SIGNAL_FIRE, Boolean.valueOf(this.isSmokeSource(levelaccessor.getBlockState(blockpos.below()))))
            .setValue(LIT, Boolean.valueOf(!flag))
            .setValue(FACING, pContext.getHorizontalDirection());
    }

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    protected BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }

        return pFacing == Direction.DOWN
            ? pState.setValue(SIGNAL_FIRE, Boolean.valueOf(this.isSmokeSource(pFacingState)))
            : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    /**
     * @return whether the given block state produces the thicker signal fire smoke when put below a campfire.
     */
    private boolean isSmokeSource(BlockState pState) {
        return pState.is(Blocks.HAY_BLOCK);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    /**
     * The type of render function called. MODEL for mixed tesr and static model, MODELBLOCK_ANIMATED for TESR-only, LIQUID for vanilla liquids, INVISIBLE to skip all rendering
     * @deprecated call via {@link IBlockState#getRenderType()} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(LIT)) {
            if (pRandom.nextInt(10) == 0) {
                pLevel.playLocalSound(
                    (double)pPos.getX() + 0.5,
                    (double)pPos.getY() + 0.5,
                    (double)pPos.getZ() + 0.5,
                    SoundEvents.CAMPFIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    0.5F + pRandom.nextFloat(),
                    pRandom.nextFloat() * 0.7F + 0.6F,
                    false
                );
            }

            if (this.spawnParticles && pRandom.nextInt(5) == 0) {
                for (int i = 0; i < pRandom.nextInt(1) + 1; i++) {
                    pLevel.addParticle(
                        ParticleTypes.LAVA,
                        (double)pPos.getX() + 0.5,
                        (double)pPos.getY() + 0.5,
                        (double)pPos.getZ() + 0.5,
                        (double)(pRandom.nextFloat() / 2.0F),
                        5.0E-5,
                        (double)(pRandom.nextFloat() / 2.0F)
                    );
                }
            }
        }
    }

    public static void dowse(@Nullable Entity pEntity, LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide()) {
            for (int i = 0; i < 20; i++) {
                makeParticles((Level)pLevel, pPos, pState.getValue(SIGNAL_FIRE), true);
            }
        }

        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof CampfireBlockEntity) {
            ((CampfireBlockEntity)blockentity).dowse();
        }

        pLevel.gameEvent(pEntity, GameEvent.BLOCK_CHANGE, pPos);
    }

    @Override
    public boolean placeLiquid(LevelAccessor pLevel, BlockPos pPos, BlockState pState, FluidState pFluidState) {
        if (!pState.getValue(BlockStateProperties.WATERLOGGED) && pFluidState.getType() == Fluids.WATER) {
            boolean flag = pState.getValue(LIT);
            if (flag) {
                if (!pLevel.isClientSide()) {
                    pLevel.playSound(null, pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                dowse(null, pLevel, pPos, pState);
            }

            pLevel.setBlock(pPos, pState.setValue(WATERLOGGED, Boolean.valueOf(true)).setValue(LIT, Boolean.valueOf(false)), 3);
            pLevel.scheduleTick(pPos, pFluidState.getType(), pFluidState.getType().getTickDelay(pLevel));
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        BlockPos blockpos = pHit.getBlockPos();
        if (!pLevel.isClientSide
            && pProjectile.isOnFire()
            && pProjectile.mayInteract(pLevel, blockpos)
            && !pState.getValue(LIT)
            && !pState.getValue(WATERLOGGED)) {
            pLevel.setBlock(blockpos, pState.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)), 11);
        }
    }

    public static void makeParticles(Level pLevel, BlockPos pPos, boolean pIsSignalFire, boolean pSpawnExtraSmoke) {
        RandomSource randomsource = pLevel.getRandom();
        SimpleParticleType simpleparticletype = pIsSignalFire ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
        pLevel.addAlwaysVisibleParticle(
            simpleparticletype,
            true,
            (double)pPos.getX() + 0.5 + randomsource.nextDouble() / 3.0 * (double)(randomsource.nextBoolean() ? 1 : -1),
            (double)pPos.getY() + randomsource.nextDouble() + randomsource.nextDouble(),
            (double)pPos.getZ() + 0.5 + randomsource.nextDouble() / 3.0 * (double)(randomsource.nextBoolean() ? 1 : -1),
            0.0,
            0.07,
            0.0
        );
        if (pSpawnExtraSmoke) {
            pLevel.addParticle(
                ParticleTypes.SMOKE,
                (double)pPos.getX() + 0.5 + randomsource.nextDouble() / 4.0 * (double)(randomsource.nextBoolean() ? 1 : -1),
                (double)pPos.getY() + 0.4,
                (double)pPos.getZ() + 0.5 + randomsource.nextDouble() / 4.0 * (double)(randomsource.nextBoolean() ? 1 : -1),
                0.0,
                0.005,
                0.0
            );
        }
    }

    public static boolean isSmokeyPos(Level pLevel, BlockPos pPos) {
        for (int i = 1; i <= 5; i++) {
            BlockPos blockpos = pPos.below(i);
            BlockState blockstate = pLevel.getBlockState(blockpos);
            if (isLitCampfire(blockstate)) {
                return true;
            }

            boolean flag = Shapes.joinIsNotEmpty(VIRTUAL_FENCE_POST, blockstate.getCollisionShape(pLevel, blockpos, CollisionContext.empty()), BooleanOp.AND); // FORGE: Fix MC-201374
            if (flag) {
                BlockState blockstate1 = pLevel.getBlockState(blockpos.below());
                return isLitCampfire(blockstate1);
            }
        }

        return false;
    }

    public static boolean isLitCampfire(BlockState pState) {
        return pState.hasProperty(LIT) && pState.is(BlockTags.CAMPFIRES) && pState.getValue(LIT);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
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
        pBuilder.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new CampfireBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide) {
            return pState.getValue(LIT) ? createTickerHelper(pBlockEntityType, BlockEntityType.CAMPFIRE, CampfireBlockEntity::particleTick) : null;
        } else {
            return pState.getValue(LIT)
                ? createTickerHelper(pBlockEntityType, BlockEntityType.CAMPFIRE, CampfireBlockEntity::cookTick)
                : createTickerHelper(pBlockEntityType, BlockEntityType.CAMPFIRE, CampfireBlockEntity::cooldownTick);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
        return false;
    }

    public static boolean canLight(BlockState pState) {
        return pState.is(BlockTags.CAMPFIRES, p_51262_ -> p_51262_.hasProperty(WATERLOGGED) && p_51262_.hasProperty(LIT))
            && !pState.getValue(WATERLOGGED)
            && !pState.getValue(LIT);
    }
}
