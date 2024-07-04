package net.minecraft.world.scores;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;

public interface ScoreAccess {
    int get();

    void set(int pValue);

    default int add(int pIncrement) {
        int i = this.get() + pIncrement;
        this.set(i);
        return i;
    }

    default int increment() {
        return this.add(1);
    }

    default void reset() {
        this.set(0);
    }

    boolean locked();

    void unlock();

    void lock();

    @Nullable
    Component display();

    void display(@Nullable Component pValue);

    void numberFormatOverride(@Nullable NumberFormat pFormat);
}
