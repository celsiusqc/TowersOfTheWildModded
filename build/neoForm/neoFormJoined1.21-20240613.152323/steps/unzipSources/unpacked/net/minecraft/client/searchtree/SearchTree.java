package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface SearchTree<T> {
    static <T> SearchTree<T> empty() {
        return p_344720_ -> List.of();
    }

    static <T> SearchTree<T> plainText(List<T> p_346366_, Function<T, Stream<String>> p_346287_) {
        if (p_346366_.isEmpty()) {
            return empty();
        } else {
            SuffixArray<T> suffixarray = new SuffixArray<>();

            for (T t : p_346366_) {
                p_346287_.apply(t).forEach(p_344960_ -> suffixarray.add(t, p_344960_.toLowerCase(Locale.ROOT)));
            }

            suffixarray.generate();
            return suffixarray::search;
        }
    }

    /**
     * Searches this search tree for the given text.
     * <p>
     * If the query does not contain a {@code :}, then only {@link #byName} is searched. If it does contain a colon, both {@link #byName} and {@link #byId} are searched and the results are merged using a {@link MergingIterator}.
     * @return A list of all matching items in this search tree.
     */
    List<T> search(String pQuery);
}
