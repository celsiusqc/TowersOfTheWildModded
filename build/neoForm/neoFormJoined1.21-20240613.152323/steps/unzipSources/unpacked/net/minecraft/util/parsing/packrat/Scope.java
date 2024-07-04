package net.minecraft.util.parsing.packrat;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.Objects;
import javax.annotation.Nullable;

public final class Scope {
    private final Object2ObjectMap<Atom<?>, Object> values = new Object2ObjectArrayMap<>();

    public <T> void put(Atom<T> pAtom, @Nullable T pValue) {
        this.values.put(pAtom, pValue);
    }

    @Nullable
    public <T> T get(Atom<T> pAtom) {
        return (T)this.values.get(pAtom);
    }

    public <T> T getOrThrow(Atom<T> pAtom) {
        return Objects.requireNonNull(this.get(pAtom));
    }

    public <T> T getOrDefault(Atom<T> pAtom, T pDefaultValue) {
        return Objects.requireNonNullElse(this.get(pAtom), pDefaultValue);
    }

    @Nullable
    @SafeVarargs
    public final <T> T getAny(Atom<T>... pAtoms) {
        for (Atom<T> atom : pAtoms) {
            T t = this.get(atom);
            if (t != null) {
                return t;
            }
        }

        return null;
    }

    @SafeVarargs
    public final <T> T getAnyOrThrow(Atom<T>... pAtoms) {
        return Objects.requireNonNull(this.getAny(pAtoms));
    }

    @Override
    public String toString() {
        return this.values.toString();
    }

    public void putAll(Scope pScope) {
        this.values.putAll(pScope.values);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther instanceof Scope scope ? this.values.equals(scope.values) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }
}
