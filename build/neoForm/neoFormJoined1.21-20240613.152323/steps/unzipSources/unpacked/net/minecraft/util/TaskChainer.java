package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;

@FunctionalInterface
public interface TaskChainer {
    Logger LOGGER = LogUtils.getLogger();

    static TaskChainer immediate(final Executor pExecutor) {
        return new TaskChainer() {
            @Override
            public <T> void append(CompletableFuture<T> p_307340_, Consumer<T> p_307235_) {
                p_307340_.thenAcceptAsync(p_307235_, pExecutor).exceptionally(p_307528_ -> {
                    LOGGER.error("Task failed", p_307528_);
                    return null;
                });
            }
        };
    }

    default void append(Runnable pTask) {
        this.append(CompletableFuture.completedFuture(null), p_307168_ -> pTask.run());
    }

    <T> void append(CompletableFuture<T> pFuture, Consumer<T> pConsumer);
}
