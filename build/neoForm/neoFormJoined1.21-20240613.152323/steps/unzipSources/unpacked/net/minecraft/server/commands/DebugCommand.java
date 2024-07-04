package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ProfileResults;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class DebugCommand {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(Component.translatable("commands.debug.notRunning"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(
        Component.translatable("commands.debug.alreadyRunning")
    );
    static final SimpleCommandExceptionType NO_RECURSIVE_TRACES = new SimpleCommandExceptionType(Component.translatable("commands.debug.function.noRecursion"));
    static final SimpleCommandExceptionType NO_RETURN_RUN = new SimpleCommandExceptionType(Component.translatable("commands.debug.function.noReturnRun"));

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(
            Commands.literal("debug")
                .requires(p_180073_ -> p_180073_.hasPermission(3))
                .then(Commands.literal("start").executes(p_180069_ -> start(p_180069_.getSource())))
                .then(Commands.literal("stop").executes(p_136918_ -> stop(p_136918_.getSource())))
                .then(
                    Commands.literal("function")
                        .requires(p_180071_ -> p_180071_.hasPermission(3))
                        .then(
                            Commands.argument("name", FunctionArgument.functions())
                                .suggests(FunctionCommand.SUGGEST_FUNCTION)
                                .executes(new DebugCommand.TraceCustomExecutor())
                        )
                )
        );
    }

    private static int start(CommandSourceStack pSource) throws CommandSyntaxException {
        MinecraftServer minecraftserver = pSource.getServer();
        if (minecraftserver.isTimeProfilerRunning()) {
            throw ERROR_ALREADY_RUNNING.create();
        } else {
            minecraftserver.startTimeProfiler();
            pSource.sendSuccess(() -> Component.translatable("commands.debug.started"), true);
            return 0;
        }
    }

    private static int stop(CommandSourceStack pSource) throws CommandSyntaxException {
        MinecraftServer minecraftserver = pSource.getServer();
        if (!minecraftserver.isTimeProfilerRunning()) {
            throw ERROR_NOT_RUNNING.create();
        } else {
            ProfileResults profileresults = minecraftserver.stopTimeProfiler();
            double d0 = (double)profileresults.getNanoDuration() / (double)TimeUtil.NANOSECONDS_PER_SECOND;
            double d1 = (double)profileresults.getTickDuration() / d0;
            pSource.sendSuccess(
                () -> Component.translatable(
                        "commands.debug.stopped",
                        String.format(Locale.ROOT, "%.2f", d0),
                        profileresults.getTickDuration(),
                        String.format(Locale.ROOT, "%.2f", d1)
                    ),
                true
            );
            return (int)d1;
        }
    }

    static class TraceCustomExecutor
        extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
        implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        public void runGuarded(
            CommandSourceStack pSource, ContextChain<CommandSourceStack> pContextChain, ChainModifiers pChainModifiers, ExecutionControl<CommandSourceStack> pExecutionControl
        ) throws CommandSyntaxException {
            if (pChainModifiers.isReturn()) {
                throw DebugCommand.NO_RETURN_RUN.create();
            } else if (pExecutionControl.tracer() != null) {
                throw DebugCommand.NO_RECURSIVE_TRACES.create();
            } else {
                CommandContext<CommandSourceStack> commandcontext = pContextChain.getTopContext();
                Collection<CommandFunction<CommandSourceStack>> collection = FunctionArgument.getFunctions(commandcontext, "name");
                MinecraftServer minecraftserver = pSource.getServer();
                String s = "debug-trace-" + Util.getFilenameFormattedDateTime() + ".txt";
                CommandDispatcher<CommandSourceStack> commanddispatcher = pSource.getServer().getFunctions().getDispatcher();
                int i = 0;

                try {
                    Path path = minecraftserver.getFile("debug");
                    Files.createDirectories(path);
                    final PrintWriter printwriter = new PrintWriter(Files.newBufferedWriter(path.resolve(s), StandardCharsets.UTF_8));
                    DebugCommand.Tracer debugcommand$tracer = new DebugCommand.Tracer(printwriter);
                    pExecutionControl.tracer(debugcommand$tracer);

                    for (final CommandFunction<CommandSourceStack> commandfunction : collection) {
                        try {
                            CommandSourceStack commandsourcestack = pSource.withSource(debugcommand$tracer).withMaximumPermission(2);
                            InstantiatedFunction<CommandSourceStack> instantiatedfunction = commandfunction.instantiate(null, commanddispatcher);
                            pExecutionControl.queueNext((new CallFunction<CommandSourceStack>(instantiatedfunction, CommandResultCallback.EMPTY, false) {
                                public void execute(CommandSourceStack p_309660_, ExecutionContext<CommandSourceStack> p_309654_, Frame p_309674_) {
                                    printwriter.println(commandfunction.id());
                                    super.execute(p_309660_, p_309654_, p_309674_);
                                }
                            }).bind(commandsourcestack));
                            i += instantiatedfunction.entries().size();
                        } catch (FunctionInstantiationException functioninstantiationexception) {
                            pSource.sendFailure(functioninstantiationexception.messageComponent());
                        }
                    }
                } catch (IOException | UncheckedIOException uncheckedioexception) {
                    DebugCommand.LOGGER.warn("Tracing failed", (Throwable)uncheckedioexception);
                    pSource.sendFailure(Component.translatable("commands.debug.function.traceFailed"));
                }

                int j = i;
                pExecutionControl.queueNext(
                    (p_305943_, p_309635_) -> {
                        if (collection.size() == 1) {
                            pSource.sendSuccess(
                                () -> Component.translatable(
                                        "commands.debug.function.success.single", j, Component.translationArg(collection.iterator().next().id()), s
                                    ),
                                true
                            );
                        } else {
                            pSource.sendSuccess(() -> Component.translatable("commands.debug.function.success.multiple", j, collection.size(), s), true);
                        }
                    }
                );
            }
        }
    }

    static class Tracer implements CommandSource, TraceCallbacks {
        public static final int INDENT_OFFSET = 1;
        private final PrintWriter output;
        private int lastIndent;
        private boolean waitingForResult;

        Tracer(PrintWriter pOutput) {
            this.output = pOutput;
        }

        private void indentAndSave(int pIndent) {
            this.printIndent(pIndent);
            this.lastIndent = pIndent;
        }

        private void printIndent(int pIndent) {
            for (int i = 0; i < pIndent + 1; i++) {
                this.output.write("    ");
            }
        }

        private void newLine() {
            if (this.waitingForResult) {
                this.output.println();
                this.waitingForResult = false;
            }
        }

        @Override
        public void onCommand(int pDepth, String pCommand) {
            this.newLine();
            this.indentAndSave(pDepth);
            this.output.print("[C] ");
            this.output.print(pCommand);
            this.waitingForResult = true;
        }

        @Override
        public void onReturn(int pDepth, String pCommand, int pReturnValue) {
            if (this.waitingForResult) {
                this.output.print(" -> ");
                this.output.println(pReturnValue);
                this.waitingForResult = false;
            } else {
                this.indentAndSave(pDepth);
                this.output.print("[R = ");
                this.output.print(pReturnValue);
                this.output.print("] ");
                this.output.println(pCommand);
            }
        }

        @Override
        public void onCall(int pDepth, ResourceLocation pFunction, int pCommands) {
            this.newLine();
            this.indentAndSave(pDepth);
            this.output.print("[F] ");
            this.output.print(pFunction);
            this.output.print(" size=");
            this.output.println(pCommands);
        }

        @Override
        public void onError(String pErrorMessage) {
            this.newLine();
            this.indentAndSave(this.lastIndent + 1);
            this.output.print("[E] ");
            this.output.print(pErrorMessage);
        }

        @Override
        public void sendSystemMessage(Component pComponent) {
            this.newLine();
            this.printIndent(this.lastIndent + 1);
            this.output.print("[M] ");
            this.output.println(pComponent.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        @Override
        public boolean alwaysAccepts() {
            return true;
        }

        @Override
        public void close() {
            IOUtils.closeQuietly((Writer)this.output);
        }
    }
}
