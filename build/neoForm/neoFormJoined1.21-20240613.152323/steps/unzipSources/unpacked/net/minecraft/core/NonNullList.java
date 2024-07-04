package net.minecraft.core;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;

public class NonNullList<E> extends AbstractList<E> {

    /**
     * Neo: utility method to construct a Codec for a NonNullList
     * @param entryCodec the codec to use for the elements
     * @param <E> the element type
     * @return a codec that encodes as a list, and decodes into NonNullList
     */
    public static <E> com.mojang.serialization.Codec<NonNullList<E>> codecOf(com.mojang.serialization.Codec<E> entryCodec) {
        return entryCodec.listOf().xmap(NonNullList::copyOf, java.util.function.Function.identity());
    }

    /**
     * Neo: utility method to construct an immutable NonNullList from a given collection
     * @param entries the collection to make a copy of
     * @param <E> the type of the elements in the list
     * @return a new immutable NonNullList
     * @throws NullPointerException if entries is null, or if it contains any nulls
     */
    public static <E> NonNullList<E> copyOf(java.util.Collection<? extends E> entries) {
        return new NonNullList<>(List.copyOf(entries), null);
    }

    private final List<E> list;
    @Nullable
    private final E defaultValue;

    public static <E> NonNullList<E> create() {
        return new NonNullList<>(Lists.newArrayList(), null);
    }

    public static <E> NonNullList<E> createWithCapacity(int pInitialCapacity) {
        return new NonNullList<>(Lists.newArrayListWithCapacity(pInitialCapacity), null);
    }

    /**
     * Creates a new NonNullList with <i>fixed</i> size and default value. The list will be filled with the default value.
     */
    public static <E> NonNullList<E> withSize(int pSize, E pDefaultValue) {
        Validate.notNull(pDefaultValue);
        Object[] aobject = new Object[pSize];
        Arrays.fill(aobject, pDefaultValue);
        return new NonNullList<>(Arrays.asList((E[])aobject), pDefaultValue);
    }

    @SafeVarargs
    public static <E> NonNullList<E> of(E pDefaultValue, E... pElements) {
        return new NonNullList<>(Arrays.asList(pElements), pDefaultValue);
    }

    protected NonNullList(List<E> pList, @Nullable E pDefaultValue) {
        this.list = pList;
        this.defaultValue = pDefaultValue;
    }

    @Nonnull
    @Override
    public E get(int pIndex) {
        return this.list.get(pIndex);
    }

    @Override
    public E set(int pIndex, E pValue) {
        Validate.notNull(pValue);
        return this.list.set(pIndex, pValue);
    }

    @Override
    public void add(int pIndex, E pValue) {
        Validate.notNull(pValue);
        this.list.add(pIndex, pValue);
    }

    @Override
    public E remove(int pIndex) {
        return this.list.remove(pIndex);
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public void clear() {
        if (this.defaultValue == null) {
            super.clear();
        } else {
            for (int i = 0; i < this.size(); i++) {
                this.set(i, this.defaultValue);
            }
        }
    }
}
