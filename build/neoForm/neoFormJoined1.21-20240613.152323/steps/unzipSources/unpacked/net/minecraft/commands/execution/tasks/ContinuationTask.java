package net.minecraft.commands.execution.tasks;

import java.util.List;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;

public class ContinuationTask<T, P> implements EntryAction<T> {
    private final ContinuationTask.TaskProvider<T, P> taskFactory;
    private final List<P> arguments;
    private final CommandQueueEntry<T> selfEntry;
    private int index;

    private ContinuationTask(ContinuationTask.TaskProvider<T, P> pTaskFactory, List<P> pArguments, Frame pFrame) {
        this.taskFactory = pTaskFactory;
        this.arguments = pArguments;
        this.selfEntry = new CommandQueueEntry<>(pFrame, this);
    }

    @Override
    public void execute(ExecutionContext<T> pContext, Frame pFrame) {
        P p = this.arguments.get(this.index);
        pContext.queueNext(this.taskFactory.create(pFrame, p));
        if (++this.index < this.arguments.size()) {
            pContext.queueNext(this.selfEntry);
        }
    }

    public static <T, P> void schedule(ExecutionContext<T> pExecutionContext, Frame pFrame, List<P> pArguments, ContinuationTask.TaskProvider<T, P> pTaskProvider) {
        int i = pArguments.size();
        switch (i) {
            case 0:
                break;
            case 1:
                pExecutionContext.queueNext(pTaskProvider.create(pFrame, pArguments.get(0)));
                break;
            case 2:
                pExecutionContext.queueNext(pTaskProvider.create(pFrame, pArguments.get(0)));
                pExecutionContext.queueNext(pTaskProvider.create(pFrame, pArguments.get(1)));
                break;
            default:
                pExecutionContext.queueNext((new ContinuationTask<>(pTaskProvider, pArguments, pFrame)).selfEntry);
        }
    }

    @FunctionalInterface
    public interface TaskProvider<T, P> {
        CommandQueueEntry<T> create(Frame pFrame, P pArgument);
    }
}
