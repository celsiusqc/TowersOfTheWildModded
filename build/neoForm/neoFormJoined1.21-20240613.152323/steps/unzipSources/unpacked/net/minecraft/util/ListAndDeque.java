package net.minecraft.util;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;
import java.util.RandomAccess;
import javax.annotation.Nullable;

public interface ListAndDeque<T> extends Serializable, Cloneable, Deque<T>, List<T>, RandomAccess {
    ListAndDeque<T> reversed();

    @Override
    T getFirst();

    @Override
    T getLast();

    @Override
    void addFirst(T pElement);

    @Override
    void addLast(T pElement);

    @Override
    T removeFirst();

    @Override
    T removeLast();

    @Override
    default boolean offer(T pElement) {
        return this.offerLast(pElement);
    }

    @Override
    default T remove() {
        return this.removeFirst();
    }

    @Nullable
    @Override
    default T poll() {
        return this.pollFirst();
    }

    @Override
    default T element() {
        return this.getFirst();
    }

    @Nullable
    @Override
    default T peek() {
        return this.peekFirst();
    }

    @Override
    default void push(T pElement) {
        this.addFirst(pElement);
    }

    @Override
    default T pop() {
        return this.removeFirst();
    }
}
