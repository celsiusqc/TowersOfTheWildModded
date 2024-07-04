package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public class GlobalPalette<T> implements Palette<T> {
    private final IdMap<T> registry;

    public GlobalPalette(IdMap<T> pRegistry) {
        this.registry = pRegistry;
    }

    public static <A> Palette<A> create(int pBits, IdMap<A> pRegistry, PaletteResize<A> pResizeHandler, List<A> pValues) {
        return new GlobalPalette<>(pRegistry);
    }

    @Override
    public int idFor(T pState) {
        int i = this.registry.getId(pState);
        return i == -1 ? 0 : i;
    }

    @Override
    public boolean maybeHas(Predicate<T> pFilter) {
        return true;
    }

    @Override
    public T valueFor(int pId) {
        T t = this.registry.byId(pId);
        if (t == null) {
            throw new MissingPaletteEntryException(pId);
        } else {
            return t;
        }
    }

    @Override
    public void read(FriendlyByteBuf pBuffer) {
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
    }

    @Override
    public int getSerializedSize() {
        return 0;
    }

    @Override
    public int getSize() {
        return this.registry.size();
    }

    @Override
    public Palette<T> copy() {
        return this;
    }
}
