package net.minecraft.util.parsing.packrat;

import java.util.Optional;

public interface Rule<S, T> {
    Optional<T> parse(ParseState<S> pParseState);

    static <S, T> Rule<S, T> fromTerm(Term<S> pChild, Rule.RuleAction<S, T> pAction) {
        return new Rule.WrappedTerm<>(pAction, pChild);
    }

    static <S, T> Rule<S, T> fromTerm(Term<S> pChild, Rule.SimpleRuleAction<T> pAction) {
        return new Rule.WrappedTerm<>((p_336011_, p_336192_) -> Optional.of(pAction.run(p_336192_)), pChild);
    }

    @FunctionalInterface
    public interface RuleAction<S, T> {
        Optional<T> run(ParseState<S> pParseState, Scope pScope);
    }

    @FunctionalInterface
    public interface SimpleRuleAction<T> {
        T run(Scope pScope);
    }

    public static record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
        @Override
        public Optional<T> parse(ParseState<S> p_336049_) {
            Scope scope = new Scope();
            return this.child.parse(p_336049_, scope, Control.UNBOUND) ? this.action.run(p_336049_, scope) : Optional.empty();
        }
    }
}
