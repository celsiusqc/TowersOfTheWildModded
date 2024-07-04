package net.minecraft.commands.execution;

import javax.annotation.Nullable;
import net.minecraft.commands.ExecutionCommandSource;

public interface ExecutionControl<T> {
    void queueNext(EntryAction<T> pEntry);

    void tracer(@Nullable TraceCallbacks pTracer);

    @Nullable
    TraceCallbacks tracer();

    Frame currentFrame();

    static <T extends ExecutionCommandSource<T>> ExecutionControl<T> create(final ExecutionContext<T> pExecutionContext, final Frame pFrame) {
        return new ExecutionControl<T>() {
            @Override
            public void queueNext(EntryAction<T> p_309579_) {
                pExecutionContext.queueNext(new CommandQueueEntry<>(pFrame, p_309579_));
            }

            @Override
            public void tracer(@Nullable TraceCallbacks p_309633_) {
                pExecutionContext.tracer(p_309633_);
            }

            @Nullable
            @Override
            public TraceCallbacks tracer() {
                return pExecutionContext.tracer();
            }

            @Override
            public Frame currentFrame() {
                return pFrame;
            }
        };
    }
}
