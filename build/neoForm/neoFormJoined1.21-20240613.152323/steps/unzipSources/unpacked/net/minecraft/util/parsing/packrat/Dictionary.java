package net.minecraft.util.parsing.packrat;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class Dictionary<S> {
    private final Map<Atom<?>, Rule<S, ?>> terms = new HashMap<>();

    public <T> void put(Atom<T> pAtom, Rule<S, T> pRule) {
        Rule<S, ?> rule = this.terms.putIfAbsent(pAtom, pRule);
        if (rule != null) {
            throw new IllegalArgumentException("Trying to override rule: " + pAtom);
        }
    }

    public <T> void put(Atom<T> pAtom, Term<S> pTerm, Rule.RuleAction<S, T> pRuleAction) {
        this.put(pAtom, Rule.fromTerm(pTerm, pRuleAction));
    }

    public <T> void put(Atom<T> pAtom, Term<S> pTerm, Rule.SimpleRuleAction<T> pSimpleRuleAction) {
        this.put(pAtom, Rule.fromTerm(pTerm, pSimpleRuleAction));
    }

    @Nullable
    public <T> Rule<S, T> get(Atom<T> pAtom) {
        return (Rule<S, T>)this.terms.get(pAtom);
    }
}
