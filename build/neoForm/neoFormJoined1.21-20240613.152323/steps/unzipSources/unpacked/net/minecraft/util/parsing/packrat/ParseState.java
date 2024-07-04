package net.minecraft.util.parsing.packrat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

public abstract class ParseState<S> {
    private final Map<ParseState.CacheKey<?>, ParseState.CacheEntry<?>> ruleCache = new HashMap<>();
    private final Dictionary<S> dictionary;
    private final ErrorCollector<S> errorCollector;

    protected ParseState(Dictionary<S> pDictionary, ErrorCollector<S> pErrorCollector) {
        this.dictionary = pDictionary;
        this.errorCollector = pErrorCollector;
    }

    public ErrorCollector<S> errorCollector() {
        return this.errorCollector;
    }

    public <T> Optional<T> parseTopRule(Atom<T> pAtom) {
        Optional<T> optional = this.parse(pAtom);
        if (optional.isPresent()) {
            this.errorCollector.finish(this.mark());
        }

        return optional;
    }

    public <T> Optional<T> parse(Atom<T> pAtom) {
        ParseState.CacheKey<T> cachekey = new ParseState.CacheKey<>(pAtom, this.mark());
        ParseState.CacheEntry<T> cacheentry = this.lookupInCache(cachekey);
        if (cacheentry != null) {
            this.restore(cacheentry.mark());
            return cacheentry.value;
        } else {
            Rule<S, T> rule = this.dictionary.get(pAtom);
            if (rule == null) {
                throw new IllegalStateException("No symbol " + pAtom);
            } else {
                Optional<T> optional = rule.parse(this);
                this.storeInCache(cachekey, optional);
                return optional;
            }
        }
    }

    @Nullable
    private <T> ParseState.CacheEntry<T> lookupInCache(ParseState.CacheKey<T> pKey) {
        return (ParseState.CacheEntry<T>)this.ruleCache.get(pKey);
    }

    private <T> void storeInCache(ParseState.CacheKey<T> pKey, Optional<T> pValue) {
        this.ruleCache.put(pKey, new ParseState.CacheEntry<>(pValue, this.mark()));
    }

    public abstract S input();

    public abstract int mark();

    public abstract void restore(int pCursor);

    static record CacheEntry<T>(Optional<T> value, int mark) {
    }

    static record CacheKey<T>(Atom<T> name, int mark) {
    }
}
