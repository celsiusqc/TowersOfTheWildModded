package net.minecraft.commands.execution.tasks;

import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.InstantiatedFunction;

public class CallFunction<T extends ExecutionCommandSource<T>> implements UnboundEntryAction<T> {
    private final InstantiatedFunction<T> function;
    private final CommandResultCallback resultCallback;
    private final boolean returnParentFrame;

    public CallFunction(InstantiatedFunction<T> pFunction, CommandResultCallback pResultCallback, boolean pReturnParentFrame) {
        this.function = pFunction;
        this.resultCallback = pResultCallback;
        this.returnParentFrame = pReturnParentFrame;
    }

    public void execute(T pSource, ExecutionContext<T> pExecutionContext, Frame pFrame) {
        pExecutionContext.incrementCost();
        List<UnboundEntryAction<T>> list = this.function.entries();
        TraceCallbacks tracecallbacks = pExecutionContext.tracer();
        if (tracecallbacks != null) {
            tracecallbacks.onCall(pFrame.depth(), this.function.id(), this.function.entries().size());
        }

        int i = pFrame.depth() + 1;
        Frame.FrameControl frame$framecontrol = this.returnParentFrame ? pFrame.frameControl() : pExecutionContext.frameControlForDepth(i);
        Frame frame = new Frame(i, this.resultCallback, frame$framecontrol);
        ContinuationTask.schedule(pExecutionContext, frame, list, (p_309431_, p_309432_) -> new CommandQueueEntry<>(p_309431_, p_309432_.bind(pSource)));
    }
}
