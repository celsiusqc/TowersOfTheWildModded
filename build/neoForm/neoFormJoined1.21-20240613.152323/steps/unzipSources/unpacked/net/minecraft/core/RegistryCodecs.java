package net.minecraft.core;

import com.mojang.serialization.Codec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

public class RegistryCodecs {
    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
        return homogeneousList(pRegistryKey, pElementCodec, false);
    }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, boolean pDisallowInline) {
        return HolderSetCodec.create(pRegistryKey, RegistryFileCodec.create(pRegistryKey, pElementCodec), pDisallowInline);
    }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey) {
        return homogeneousList(pRegistryKey, false);
    }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey, boolean pDisallowInline) {
        return HolderSetCodec.create(pRegistryKey, RegistryFixedCodec.create(pRegistryKey), pDisallowInline);
    }
}
