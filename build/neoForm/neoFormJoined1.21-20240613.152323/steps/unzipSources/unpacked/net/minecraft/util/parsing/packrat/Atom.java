package net.minecraft.util.parsing.packrat;

public record Atom<T>(String name) {
    @Override
    public String toString() {
        return "<" + this.name + ">";
    }

    public static <T> Atom<T> of(String pName) {
        return new Atom<>(pName);
    }
}
