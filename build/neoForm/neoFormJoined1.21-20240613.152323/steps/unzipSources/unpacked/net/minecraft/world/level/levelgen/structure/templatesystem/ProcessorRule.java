package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.Passthrough;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;

public class ProcessorRule {
    public static final Passthrough DEFAULT_BLOCK_ENTITY_MODIFIER = Passthrough.INSTANCE;
    public static final Codec<ProcessorRule> CODEC = RecordCodecBuilder.create(
        p_338110_ -> p_338110_.group(
                    RuleTest.CODEC.fieldOf("input_predicate").forGetter(p_163747_ -> p_163747_.inputPredicate),
                    RuleTest.CODEC.fieldOf("location_predicate").forGetter(p_163745_ -> p_163745_.locPredicate),
                    PosRuleTest.CODEC.lenientOptionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter(p_163743_ -> p_163743_.posPredicate),
                    BlockState.CODEC.fieldOf("output_state").forGetter(p_163741_ -> p_163741_.outputState),
                    RuleBlockEntityModifier.CODEC
                        .lenientOptionalFieldOf("block_entity_modifier", DEFAULT_BLOCK_ENTITY_MODIFIER)
                        .forGetter(p_277333_ -> p_277333_.blockEntityModifier)
                )
                .apply(p_338110_, ProcessorRule::new)
    );
    private final RuleTest inputPredicate;
    private final RuleTest locPredicate;
    private final PosRuleTest posPredicate;
    private final BlockState outputState;
    private final RuleBlockEntityModifier blockEntityModifier;

    public ProcessorRule(RuleTest pInputPredicate, RuleTest pLocPredicate, BlockState pOutputState) {
        this(pInputPredicate, pLocPredicate, PosAlwaysTrueTest.INSTANCE, pOutputState);
    }

    public ProcessorRule(RuleTest pInputPredicate, RuleTest pLocPredicate, PosRuleTest pPosPredicate, BlockState pOutputState) {
        this(pInputPredicate, pLocPredicate, pPosPredicate, pOutputState, DEFAULT_BLOCK_ENTITY_MODIFIER);
    }

    public ProcessorRule(RuleTest pInputPredicate, RuleTest pLocPredicate, PosRuleTest pPosPredicate, BlockState pOutputState, RuleBlockEntityModifier p_277808_) {
        this.inputPredicate = pInputPredicate;
        this.locPredicate = pLocPredicate;
        this.posPredicate = pPosPredicate;
        this.outputState = pOutputState;
        this.blockEntityModifier = p_277808_;
    }

    /**
     * @param pInputState    The incoming state from the structure.
     * @param pExistingState The current state in the world.
     * @param pLocalPos      The local position of the target state, relative to the
     *                       structure origin.
     * @param pRelativePos   The actual position of the target state. {@code
     *                       existingState} is the current in world state at this
     *                       position.
     * @param pStructurePos  The origin position of the structure.
     */
    public boolean test(BlockState pInputState, BlockState pExistingState, BlockPos pLocalPos, BlockPos pRelativePos, BlockPos pStructurePos, RandomSource pRandom) {
        return this.inputPredicate.test(pInputState, pRandom)
            && this.locPredicate.test(pExistingState, pRandom)
            && this.posPredicate.test(pLocalPos, pRelativePos, pStructurePos, pRandom);
    }

    public BlockState getOutputState() {
        return this.outputState;
    }

    @Nullable
    public CompoundTag getOutputTag(RandomSource pRandom, @Nullable CompoundTag pTag) {
        return this.blockEntityModifier.apply(pRandom, pTag);
    }
}
