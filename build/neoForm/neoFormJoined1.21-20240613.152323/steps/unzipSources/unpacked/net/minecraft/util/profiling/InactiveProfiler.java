package net.minecraft.util.profiling;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.apache.commons.lang3.tuple.Pair;

public class InactiveProfiler implements ProfileCollector {
    public static final InactiveProfiler INSTANCE = new InactiveProfiler();

    private InactiveProfiler() {
    }

    @Override
    public void startTick() {
    }

    @Override
    public void endTick() {
    }

    /**
     * Start section
     */
    @Override
    public void push(String pName) {
    }

    @Override
    public void push(Supplier<String> pNameSupplier) {
    }

    @Override
    public void markForCharting(MetricCategory pCategory) {
    }

    @Override
    public void pop() {
    }

    @Override
    public void popPush(String pName) {
    }

    @Override
    public void popPush(Supplier<String> pNameSupplier) {
    }

    @Override
    public void incrementCounter(String pCounterName, int pIncrement) {
    }

    @Override
    public void incrementCounter(Supplier<String> pCounterNameSupplier, int pIncrement) {
    }

    @Override
    public ProfileResults getResults() {
        return EmptyProfileResults.EMPTY;
    }

    @Nullable
    @Override
    public ActiveProfiler.PathEntry getEntry(String pEntryId) {
        return null;
    }

    @Override
    public Set<Pair<String, MetricCategory>> getChartedPaths() {
        return ImmutableSet.of();
    }
}
