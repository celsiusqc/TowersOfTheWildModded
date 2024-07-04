package net.minecraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.commands.execution.TraceCallbacks;

public interface ExecutionCommandSource<T extends ExecutionCommandSource<T>> {
    boolean hasPermission(int pPermissionLevel);

    T withCallback(CommandResultCallback pCallback);

    CommandResultCallback callback();

    default T clearCallbacks() {
        return this.withCallback(CommandResultCallback.EMPTY);
    }

    CommandDispatcher<T> dispatcher();

    void handleError(CommandExceptionType pExceptionType, Message pMessage, boolean pSuccess, @Nullable TraceCallbacks pTraceCallbacks);

    boolean isSilent();

    default void handleError(CommandSyntaxException pException, boolean pSuccess, @Nullable TraceCallbacks pTraceCallbacks) {
        this.handleError(pException.getType(), pException.getRawMessage(), pSuccess, pTraceCallbacks);
    }

    static <T extends ExecutionCommandSource<T>> ResultConsumer<T> resultConsumer() {
        return (p_309418_, p_309419_, p_309420_) -> p_309418_.getSource().callback().onResult(p_309419_, p_309420_);
    }
}
