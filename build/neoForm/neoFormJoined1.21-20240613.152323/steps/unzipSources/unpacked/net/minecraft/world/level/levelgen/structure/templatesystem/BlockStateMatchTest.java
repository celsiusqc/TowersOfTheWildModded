package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class BlockStateMatchTest extends RuleTest {
    public static final MapCodec<BlockStateMatchTest> CODEC = BlockState.CODEC
        .fieldOf("block_state")
        .xmap(BlockStateMatchTest::new, p_74099_ -> p_74099_.blockState);
    private final BlockState blockState;

    public BlockStateMatchTest(BlockState p_74093_) {
        this.blockState = p_74093_;
    }

    @Override
    public boolean test(BlockState pState, RandomSource pRandom) {
        return pState == this.blockState;
    }

    @Override
    protected RuleTestType<?> getType() {
        return RuleTestType.BLOCKSTATE_TEST;
    }
}
