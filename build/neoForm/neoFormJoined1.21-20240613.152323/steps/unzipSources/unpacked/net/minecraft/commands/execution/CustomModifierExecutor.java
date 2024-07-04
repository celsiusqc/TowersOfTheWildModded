package net.minecraft.commands.execution;

import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.List;

public interface CustomModifierExecutor<T> {
    void apply(T pOriginalSource, List<T> pSoruces, ContextChain<T> pContextChain, ChainModifiers pChainModifiers, ExecutionControl<T> pExecutionControl);

    public interface ModifierAdapter<T> extends RedirectModifier<T>, CustomModifierExecutor<T> {
        @Override
        default Collection<T> apply(CommandContext<T> pContext) throws CommandSyntaxException {
            throw new UnsupportedOperationException("This function should not run");
        }
    }
}
