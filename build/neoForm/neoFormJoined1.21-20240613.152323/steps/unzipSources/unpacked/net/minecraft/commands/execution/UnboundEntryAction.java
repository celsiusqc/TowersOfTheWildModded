package net.minecraft.commands.execution;

@FunctionalInterface
public interface UnboundEntryAction<T> {
    void execute(T pSource, ExecutionContext<T> pExecutionContext, Frame pFrame);

    default EntryAction<T> bind(T pSource) {
        return (p_309422_, p_309423_) -> this.execute(pSource, p_309422_, p_309423_);
    }
}
