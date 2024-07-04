package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class SingleValuePalette<T> implements Palette<T> {
    private final IdMap<T> registry;
    @Nullable
    private T value;
    private final PaletteResize<T> resizeHandler;

    public SingleValuePalette(IdMap<T> pRegistry, PaletteResize<T> pResizeHandler, List<T> pValue) {
        this.registry = pRegistry;
        this.resizeHandler = pResizeHandler;
        if (pValue.size() > 0) {
            Validate.isTrue(pValue.size() <= 1, "Can't initialize SingleValuePalette with %d values.", (long)pValue.size());
            this.value = pValue.get(0);
        }
    }

    public static <A> Palette<A> create(int pBits, IdMap<A> pRegistry, PaletteResize<A> pResizeHandler, List<A> pValue) {
        return new SingleValuePalette<>(pRegistry, pResizeHandler, pValue);
    }

    @Override
    public int idFor(T pState) {
        if (this.value != null && this.value != pState) {
            return this.resizeHandler.onResize(1, pState);
        } else {
            this.value = pState;
            return 0;
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> pFilter) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return pFilter.test(this.value);
        }
    }

    @Override
    public T valueFor(int pId) {
        if (this.value != null && pId == 0) {
            return this.value;
        } else {
            throw new IllegalStateException("Missing Palette entry for id " + pId + ".");
        }
    }

    @Override
    public void read(FriendlyByteBuf pBuffer) {
        this.value = this.registry.byIdOrThrow(pBuffer.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            pBuffer.writeVarInt(this.registry.getId(this.value));
        }
    }

    @Override
    public int getSerializedSize() {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return VarInt.getByteSize(this.registry.getId(this.value));
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public Palette<T> copy() {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return this;
        }
    }
}
