package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;

public interface ErrorCollector<S> {
    void store(int pCursor, SuggestionSupplier<S> pSuggestions, Object pReason);

    default void store(int pCursor, Object pReason) {
        this.store(pCursor, SuggestionSupplier.empty(), pReason);
    }

    void finish(int pCursor);

    public static class LongestOnly<S> implements ErrorCollector<S> {
        private final List<ErrorEntry<S>> entries = new ArrayList<>();
        private int lastCursor = -1;

        private void discardErrorsFromShorterParse(int pCursor) {
            if (pCursor > this.lastCursor) {
                this.lastCursor = pCursor;
                this.entries.clear();
            }
        }

        @Override
        public void finish(int pCursor) {
            this.discardErrorsFromShorterParse(pCursor);
        }

        @Override
        public void store(int pCursor, SuggestionSupplier<S> pSuggestions, Object pReason) {
            this.discardErrorsFromShorterParse(pCursor);
            if (pCursor == this.lastCursor) {
                this.entries.add(new ErrorEntry<>(pCursor, pSuggestions, pReason));
            }
        }

        public List<ErrorEntry<S>> entries() {
            return this.entries;
        }

        public int cursor() {
            return this.lastCursor;
        }
    }
}
