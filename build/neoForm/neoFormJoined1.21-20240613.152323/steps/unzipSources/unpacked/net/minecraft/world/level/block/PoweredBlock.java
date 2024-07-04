package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PoweredBlock extends Block {
    public static final MapCodec<PoweredBlock> CODEC = simpleCodec(PoweredBlock::new);

    @Override
    public MapCodec<PoweredBlock> codec() {
        return CODEC;
    }

    public PoweredBlock(BlockBehaviour.Properties p_55206_) {
        super(p_55206_);
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
        return 15;
    }
}
