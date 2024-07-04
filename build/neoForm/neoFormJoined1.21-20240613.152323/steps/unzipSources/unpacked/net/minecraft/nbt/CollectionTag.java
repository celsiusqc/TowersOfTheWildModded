package net.minecraft.nbt;

import java.util.AbstractList;

public abstract class CollectionTag<T extends Tag> extends AbstractList<T> implements Tag {
    public abstract T set(int pIndex, T pTag);

    public abstract void add(int pIndex, T pTag);

    public abstract T remove(int pIndex);

    public abstract boolean setTag(int pIndex, Tag pTag);

    public abstract boolean addTag(int pIndex, Tag pTag);

    public abstract byte getElementType();
}
