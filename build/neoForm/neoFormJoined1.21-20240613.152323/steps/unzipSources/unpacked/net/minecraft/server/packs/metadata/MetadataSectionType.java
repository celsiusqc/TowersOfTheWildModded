package net.minecraft.server.packs.metadata;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

public interface MetadataSectionType<T> extends MetadataSectionSerializer<T> {
    JsonObject toJson(T pData);

    static <T> MetadataSectionType<T> fromCodec(final String pName, final Codec<T> pCodec) {
        return new MetadataSectionType<T>() {
            @Override
            public String getMetadataSectionName() {
                return pName;
            }

            @Override
            public T fromJson(JsonObject p_249450_) {
                return pCodec.parse(JsonOps.INSTANCE, p_249450_).getOrThrow(JsonParseException::new);
            }

            @Override
            public JsonObject toJson(T p_250691_) {
                return pCodec.encodeStart(JsonOps.INSTANCE, p_250691_).getOrThrow(IllegalArgumentException::new).getAsJsonObject();
            }
        };
    }
}
