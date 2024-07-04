package net.minecraft.tags;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;

public class TagNetworkSerialization {
    public static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> serializeTagsToNetwork(
        LayeredRegistryAccess<RegistryLayer> pRegistryAccess
    ) {
        return RegistrySynchronization.networkSafeRegistries(pRegistryAccess)
            .map(p_203949_ -> Pair.of(p_203949_.key(), serializeToNetwork(p_203949_.value())))
            .filter(p_321439_ -> p_321439_.getSecond().size() > 0)
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    private static <T> TagNetworkSerialization.NetworkPayload serializeToNetwork(Registry<T> pRegistry) {
        Map<ResourceLocation, IntList> map = new HashMap<>();
        pRegistry.getTags().forEach(p_339471_ -> {
            HolderSet<T> holderset = p_339471_.getSecond();
            IntList intlist = new IntArrayList(holderset.size());

            for (Holder<T> holder : holderset) {
                if (holder.kind() != Holder.Kind.REFERENCE) {
                    throw new IllegalStateException("Can't serialize unregistered value " + holder);
                }

                intlist.add(pRegistry.getId(holder.value()));
            }

            map.put(p_339471_.getFirst().location(), intlist);
        });
        return new TagNetworkSerialization.NetworkPayload(map);
    }

    static <T> void deserializeTagsFromNetwork(
        ResourceKey<? extends Registry<T>> pRegistryKey,
        Registry<T> pRegistry,
        TagNetworkSerialization.NetworkPayload pNetworkPayload,
        TagNetworkSerialization.TagOutput<T> pOutput
    ) {
        pNetworkPayload.tags.forEach((p_248278_, p_248279_) -> {
            TagKey<T> tagkey = TagKey.create(pRegistryKey, p_248278_);
            List<Holder<T>> list = p_248279_.intStream().mapToObj(pRegistry::getHolder).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
            pOutput.accept(tagkey, list);
        });
    }

    public static final class NetworkPayload {
        final Map<ResourceLocation, IntList> tags;

        NetworkPayload(Map<ResourceLocation, IntList> pTags) {
            this.tags = pTags;
        }

        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeMap(this.tags, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeIntIdList);
        }

        public static TagNetworkSerialization.NetworkPayload read(FriendlyByteBuf pBuffer) {
            return new TagNetworkSerialization.NetworkPayload(pBuffer.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readIntIdList));
        }

        public int size() {
            return this.tags.size();
        }

        public <T> void applyToRegistry(Registry<T> pRegistry) {
            if (this.size() != 0) {
                Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>(this.size());
                TagNetworkSerialization.deserializeTagsFromNetwork(pRegistry.key(), pRegistry, this, map::put);
                pRegistry.bindTags(map);
            }
        }
    }

    @FunctionalInterface
    public interface TagOutput<T> {
        void accept(TagKey<T> pKey, List<Holder<T>> pValues);
    }
}
