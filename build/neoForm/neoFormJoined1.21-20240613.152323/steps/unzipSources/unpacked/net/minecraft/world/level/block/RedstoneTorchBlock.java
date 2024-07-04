package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RedstoneTorchBlock extends BaseTorchBlock {
    public static final MapCodec<RedstoneTorchBlock> CODEC = simpleCodec(RedstoneTorchBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final Map<BlockGetter, List<RedstoneTorchBlock.Toggle>> RECENT_TOGGLES = new WeakHashMap<>();
    public static final int RECENT_TOGGLE_TIMER = 60;
    public static final int MAX_RECENT_TOGGLES = 8;
    public static final int RESTART_DELAY = 160;
    private static final int TOGGLE_DELAY = 2;

    @Override
    public MapCodec<? extends RedstoneTorchBlock> codec() {
        return CODEC;
    }

    public RedstoneTorchBlock(BlockBehaviour.Properties p_55678_) {
        super(p_55678_);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.valueOf(true)));
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        for (Direction direction : Direction.values()) {
            pLevel.updateNeighborsAt(pPos.relative(direction), this);
        }
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving) {
            for (Direction direction : Direction.values()) {
                pLevel.updateNeighborsAt(pPos.relative(direction), this);
            }
        }
    }

    /**
     * Returns the signal this block emits in the given direction.
     *
     * <p>
     * NOTE: directions in redstone signal related methods are backwards, so this method
     * checks for the signal emitted in the <i>opposite</i> direction of the one given.
     *
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getSignal} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(LIT) && Direction.UP != pSide ? 15 : 0;
    }

    protected boolean hasNeighborSignal(Level pLevel, BlockPos pPos, BlockState pState) {
        return pLevel.hasSignal(pPos.below(), Direction.DOWN);
    }

    @Override
    protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        boolean flag = this.hasNeighborSignal(pLevel, pPos, pState);
        List<RedstoneTorchBlock.Toggle> list = RECENT_TOGGLES.get(pLevel);

        while (list != null && !list.isEmpty() && pLevel.getGameTime() - list.get(0).when > 60L) {
            list.remove(0);
        }

        if (pState.getValue(LIT)) {
            if (flag) {
                pLevel.setBlock(pPos, pState.setValue(LIT, Boolean.valueOf(false)), 3);
                if (isToggledTooFrequently(pLevel, pPos, true)) {
                    pLevel.levelEvent(1502, pPos, 0);
                    pLevel.scheduleTick(pPos, pLevel.getBlockState(pPos).getBlock(), 160);
                }
            }
        } else if (!flag && !isToggledTooFrequently(pLevel, pPos, false)) {
            pLevel.setBlock(pPos, pState.setValue(LIT, Boolean.valueOf(true)), 3);
        }
    }

    @Override
    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if (pState.getValue(LIT) == this.hasNeighborSignal(pLevel, pPos, pState) && !pLevel.getBlockTicks().willTickThisTick(pPos, this)) {
            pLevel.scheduleTick(pPos, this, 2);
        }
    }

    /**
     * Returns the direct signal this block emits in the given direction.
     *
     * <p>
     * NOTE: directions in redstone signal related methods are backwards, so this method
     * checks for the signal emitted in the <i>opposite</i> direction of the one given.
     *
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getDirectSignal} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pSide == Direction.DOWN ? pBlockState.getSignal(pBlockAccess, pPos, pSide) : 0;
    }

    /**
     * Returns whether this block is capable of emitting redstone signals.
     *
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#isSignalSource} whenever possible. Implementing/overriding is fine.
     */
    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(LIT)) {
            double d0 = (double)pPos.getX() + 0.5 + (pRandom.nextDouble() - 0.5) * 0.2;
            double d1 = (double)pPos.getY() + 0.7 + (pRandom.nextDouble() - 0.5) * 0.2;
            double d2 = (double)pPos.getZ() + 0.5 + (pRandom.nextDouble() - 0.5) * 0.2;
            pLevel.addParticle(DustParticleOptions.REDSTONE, d0, d1, d2, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LIT);
    }

    private static boolean isToggledTooFrequently(Level pLevel, BlockPos pPos, boolean pLogToggle) {
        List<RedstoneTorchBlock.Toggle> list = RECENT_TOGGLES.computeIfAbsent(pLevel, p_55680_ -> Lists.newArrayList());
        if (pLogToggle) {
            list.add(new RedstoneTorchBlock.Toggle(pPos.immutable(), pLevel.getGameTime()));
        }

        int i = 0;

        for (RedstoneTorchBlock.Toggle redstonetorchblock$toggle : list) {
            if (redstonetorchblock$toggle.pos.equals(pPos)) {
                if (++i >= 8) {
                    return true;
                }
            }
        }

        return false;
    }

    public static class Toggle {
        final BlockPos pos;
        final long when;

        public Toggle(BlockPos pPos, long pWhen) {
            this.pos = pPos;
            this.when = pWhen;
        }
    }
}
