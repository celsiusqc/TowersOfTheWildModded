package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;

public interface ResourceMetadata {
    ResourceMetadata EMPTY = new ResourceMetadata() {
        @Override
        public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_215584_) {
            return Optional.empty();
        }
    };
    IoSupplier<ResourceMetadata> EMPTY_SUPPLIER = () -> EMPTY;

    static ResourceMetadata fromJsonStream(InputStream pStream) throws IOException {
        ResourceMetadata resourcemetadata;
        try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(pStream, StandardCharsets.UTF_8))) {
            final JsonObject jsonobject = GsonHelper.parse(bufferedreader);
            resourcemetadata = new ResourceMetadata() {
                @Override
                public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_215589_) {
                    String s = p_215589_.getMetadataSectionName();
                    return jsonobject.has(s) ? Optional.of(p_215589_.fromJson(GsonHelper.getAsJsonObject(jsonobject, s))) : Optional.empty();
                }
            };
        }

        return resourcemetadata;
    }

    <T> Optional<T> getSection(MetadataSectionSerializer<T> pSerializer);

    default ResourceMetadata copySections(Collection<MetadataSectionSerializer<?>> pSerializers) {
        ResourceMetadata.Builder resourcemetadata$builder = new ResourceMetadata.Builder();

        for (MetadataSectionSerializer<?> metadatasectionserializer : pSerializers) {
            this.copySection(resourcemetadata$builder, metadatasectionserializer);
        }

        return resourcemetadata$builder.build();
    }

    private <T> void copySection(ResourceMetadata.Builder pBuilder, MetadataSectionSerializer<T> pSerializer) {
        this.getSection(pSerializer).ifPresent(p_293816_ -> pBuilder.put(pSerializer, (T)p_293816_));
    }

    public static class Builder {
        private final ImmutableMap.Builder<MetadataSectionSerializer<?>, Object> map = ImmutableMap.builder();

        public <T> ResourceMetadata.Builder put(MetadataSectionSerializer<T> pKey, T pValue) {
            this.map.put(pKey, pValue);
            return this;
        }

        public ResourceMetadata build() {
            final ImmutableMap<MetadataSectionSerializer<?>, Object> immutablemap = this.map.build();
            return immutablemap.isEmpty() ? ResourceMetadata.EMPTY : new ResourceMetadata() {
                @Override
                public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_295383_) {
                    return Optional.ofNullable((T)immutablemap.get(p_295383_));
                }
            };
        }
    }
}
