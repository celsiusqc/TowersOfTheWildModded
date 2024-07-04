package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

public class TestFinder<T> implements StructureBlockPosFinder, TestFunctionFinder {
    static final TestFunctionFinder NO_FUNCTIONS = Stream::empty;
    static final StructureBlockPosFinder NO_STRUCTURES = Stream::empty;
    private final TestFunctionFinder testFunctionFinder;
    private final StructureBlockPosFinder structureBlockPosFinder;
    private final CommandSourceStack source;
    private final Function<TestFinder<T>, T> contextProvider;

    @Override
    public Stream<BlockPos> findStructureBlockPos() {
        return this.structureBlockPosFinder.findStructureBlockPos();
    }

    TestFinder(CommandSourceStack pSource, Function<TestFinder<T>, T> pContextProvider, TestFunctionFinder pTestFunctionFinder, StructureBlockPosFinder pStructureBlockPosFinder) {
        this.source = pSource;
        this.contextProvider = pContextProvider;
        this.testFunctionFinder = pTestFunctionFinder;
        this.structureBlockPosFinder = pStructureBlockPosFinder;
    }

    T get() {
        return this.contextProvider.apply(this);
    }

    public CommandSourceStack source() {
        return this.source;
    }

    @Override
    public Stream<TestFunction> findTestFunctions() {
        return this.testFunctionFinder.findTestFunctions();
    }

    public static class Builder<T> {
        private final Function<TestFinder<T>, T> contextProvider;
        private final UnaryOperator<Supplier<Stream<TestFunction>>> testFunctionFinderWrapper;
        private final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper;

        public Builder(Function<TestFinder<T>, T> pContextProvider) {
            this.contextProvider = pContextProvider;
            this.testFunctionFinderWrapper = p_329857_ -> p_329857_;
            this.structureBlockPosFinderWrapper = p_329858_ -> p_329858_;
        }

        private Builder(
            Function<TestFinder<T>, T> pContextProvider, UnaryOperator<Supplier<Stream<TestFunction>>> pTestFunctionFinderWrapper, UnaryOperator<Supplier<Stream<BlockPos>>> pStructureBlockPosFinderWrapper
        ) {
            this.contextProvider = pContextProvider;
            this.testFunctionFinderWrapper = pTestFunctionFinderWrapper;
            this.structureBlockPosFinderWrapper = pStructureBlockPosFinderWrapper;
        }

        public TestFinder.Builder<T> createMultipleCopies(int pCount) {
            return new TestFinder.Builder<>(this.contextProvider, createCopies(pCount), createCopies(pCount));
        }

        private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int pCount) {
            return p_329860_ -> {
                List<Q> list = new LinkedList<>();
                List<Q> list1 = ((Stream)p_329860_.get()).toList();

                for (int i = 0; i < pCount; i++) {
                    list.addAll(list1);
                }

                return list::stream;
            };
        }

        private T build(CommandSourceStack pSource, TestFunctionFinder pTestFunctionFinder, StructureBlockPosFinder pStructureBlockPosFinder) {
            return new TestFinder<>(
                    pSource,
                    this.contextProvider,
                    this.testFunctionFinderWrapper.apply(pTestFunctionFinder::findTestFunctions)::get,
                    this.structureBlockPosFinderWrapper.apply(pStructureBlockPosFinder::findStructureBlockPos)::get
                )
                .get();
        }

        public T radius(CommandContext<CommandSourceStack> pContext, int pRadius) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findStructureBlocks(blockpos, pRadius, commandsourcestack.getLevel())
            );
        }

        public T nearest(CommandContext<CommandSourceStack> pContext) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.findNearestStructureBlock(blockpos, 15, commandsourcestack.getLevel()).stream()
            );
        }

        public T allNearby(CommandContext<CommandSourceStack> pContext) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findStructureBlocks(blockpos, 200, commandsourcestack.getLevel())
            );
        }

        public T lookedAt(CommandContext<CommandSourceStack> pContext) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            return this.build(
                commandsourcestack,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.lookedAtStructureBlockPos(
                        BlockPos.containing(commandsourcestack.getPosition()), commandsourcestack.getPlayer().getCamera(), commandsourcestack.getLevel()
                    )
            );
        }

        public T allTests(CommandContext<CommandSourceStack> pContext) {
            return this.build(
                pContext.getSource(),
                () -> GameTestRegistry.getAllTestFunctions().stream().filter(p_329855_ -> !p_329855_.manualOnly()),
                TestFinder.NO_STRUCTURES
            );
        }

        public T allTestsInClass(CommandContext<CommandSourceStack> pContext, String pClassName) {
            return this.build(
                pContext.getSource(),
                () -> GameTestRegistry.getTestFunctionsForClassName(pClassName).filter(p_329856_ -> !p_329856_.manualOnly()),
                TestFinder.NO_STRUCTURES
            );
        }

        public T failedTests(CommandContext<CommandSourceStack> pContext, boolean pOnlyRequired) {
            return this.build(
                pContext.getSource(),
                () -> GameTestRegistry.getLastFailedTests().filter(p_320430_ -> !pOnlyRequired || p_320430_.required()),
                TestFinder.NO_STRUCTURES
            );
        }

        public T byArgument(CommandContext<CommandSourceStack> pContext, String pArgumentName) {
            return this.build(pContext.getSource(), () -> Stream.of(TestFunctionArgument.getTestFunction(pContext, pArgumentName)), TestFinder.NO_STRUCTURES);
        }

        public T locateByName(CommandContext<CommandSourceStack> pContext, String pName) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.findStructureByTestFunction(blockpos, 1024, commandsourcestack.getLevel(), pName)
            );
        }

        public T failedTests(CommandContext<CommandSourceStack> pContext) {
            return this.failedTests(pContext, false);
        }
    }
}
