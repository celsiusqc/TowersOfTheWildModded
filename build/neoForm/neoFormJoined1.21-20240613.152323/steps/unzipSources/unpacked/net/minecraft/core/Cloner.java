package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;

public class Cloner<T> {
    private final Codec<T> directCodec;

    Cloner(Codec<T> pDirectCodec) {
        this.directCodec = pDirectCodec;
    }

    public T clone(T pObject, HolderLookup.Provider pLookupProvider1, HolderLookup.Provider pLookupProvider2) {
        DynamicOps<Object> dynamicops = pLookupProvider1.createSerializationContext(JavaOps.INSTANCE);
        DynamicOps<Object> dynamicops1 = pLookupProvider2.createSerializationContext(JavaOps.INSTANCE);
        Object object = this.directCodec
            .encodeStart(dynamicops, pObject)
            .getOrThrow(p_312200_ -> new IllegalStateException("Failed to encode: " + p_312200_));
        return this.directCodec.parse(dynamicops1, object).getOrThrow(p_312832_ -> new IllegalStateException("Failed to decode: " + p_312832_));
    }

    public static class Factory {
        private final Map<ResourceKey<? extends Registry<?>>, Cloner<?>> codecs = new HashMap<>();

        public <T> Cloner.Factory addCodec(ResourceKey<? extends Registry<? extends T>> pRegistryKey, Codec<T> pCodec) {
            this.codecs.put(pRegistryKey, new Cloner<>(pCodec));
            return this;
        }

        @Nullable
        public <T> Cloner<T> cloner(ResourceKey<? extends Registry<? extends T>> pRegistryKey) {
            return (Cloner<T>)this.codecs.get(pRegistryKey);
        }
    }
}
