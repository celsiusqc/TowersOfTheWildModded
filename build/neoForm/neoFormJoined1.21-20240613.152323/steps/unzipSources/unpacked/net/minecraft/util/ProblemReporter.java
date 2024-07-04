package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public interface ProblemReporter {
    ProblemReporter forChild(String pName);

    void report(String pMessage);

    public static class Collector implements ProblemReporter {
        private final Multimap<String, String> problems;
        private final Supplier<String> path;
        @Nullable
        private String pathCache;

        public Collector() {
            this(HashMultimap.create(), () -> "");
        }

        private Collector(Multimap<String, String> pProblems, Supplier<String> pPath) {
            this.problems = pProblems;
            this.path = pPath;
        }

        private String getPath() {
            if (this.pathCache == null) {
                this.pathCache = this.path.get();
            }

            return this.pathCache;
        }

        @Override
        public ProblemReporter forChild(String pName) {
            return new ProblemReporter.Collector(this.problems, () -> this.getPath() + pName);
        }

        @Override
        public void report(String pMessage) {
            this.problems.put(this.getPath(), pMessage);
        }

        public Multimap<String, String> get() {
            return ImmutableMultimap.copyOf(this.problems);
        }

        public Optional<String> getReport() {
            Multimap<String, String> multimap = this.get();
            if (!multimap.isEmpty()) {
                String s = multimap.asMap()
                    .entrySet()
                    .stream()
                    .map(p_344277_ -> " at " + p_344277_.getKey() + ": " + String.join("; ", p_344277_.getValue()))
                    .collect(Collectors.joining("\n"));
                return Optional.of(s);
            } else {
                return Optional.empty();
            }
        }
    }
}
