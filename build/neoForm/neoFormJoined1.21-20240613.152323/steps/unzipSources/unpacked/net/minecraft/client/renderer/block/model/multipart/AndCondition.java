package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AndCondition implements Condition {
    public static final String TOKEN = "AND";
    private final Iterable<? extends Condition> conditions;

    public AndCondition(Iterable<? extends Condition> pConditions) {
        this.conditions = pConditions;
    }

    @Override
    public Predicate<BlockState> getPredicate(StateDefinition<Block, BlockState> pDefinition) {
        List<Predicate<BlockState>> list = Streams.stream(this.conditions).map(p_111916_ -> p_111916_.getPredicate(pDefinition)).collect(Collectors.toList());
        return p_111919_ -> list.stream().allMatch(p_173502_ -> p_173502_.test(p_111919_));
    }
}
