package net.minecraft.commands.execution;

public record CommandQueueEntry<T>(Frame frame, EntryAction<T> action) {
    public void execute(ExecutionContext<T> pContext) {
        this.action.execute(pContext, this.frame);
    }
}
