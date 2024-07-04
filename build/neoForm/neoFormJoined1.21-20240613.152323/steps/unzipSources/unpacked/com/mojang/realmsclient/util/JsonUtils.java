package com.mojang.realmsclient.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JsonUtils {
    public static <T> T getRequired(String pKey, JsonObject pJson, Function<JsonObject, T> pOutput) {
        JsonElement jsonelement = pJson.get(pKey);
        if (jsonelement == null || jsonelement.isJsonNull()) {
            throw new IllegalStateException("Missing required property: " + pKey);
        } else if (!jsonelement.isJsonObject()) {
            throw new IllegalStateException("Required property " + pKey + " was not a JsonObject as espected");
        } else {
            return pOutput.apply(jsonelement.getAsJsonObject());
        }
    }

    @Nullable
    public static <T> T getOptional(String pKey, JsonObject pJson, Function<JsonObject, T> pOutput) {
        JsonElement jsonelement = pJson.get(pKey);
        if (jsonelement == null || jsonelement.isJsonNull()) {
            return null;
        } else if (!jsonelement.isJsonObject()) {
            throw new IllegalStateException("Required property " + pKey + " was not a JsonObject as espected");
        } else {
            return pOutput.apply(jsonelement.getAsJsonObject());
        }
    }

    public static String getRequiredString(String pKey, JsonObject pJson) {
        String s = getStringOr(pKey, pJson, null);
        if (s == null) {
            throw new IllegalStateException("Missing required property: " + pKey);
        } else {
            return s;
        }
    }

    public static String getRequiredStringOr(String pKey, JsonObject pJson, String pDefaultValue) {
        JsonElement jsonelement = pJson.get(pKey);
        if (jsonelement != null) {
            return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsString();
        } else {
            return pDefaultValue;
        }
    }

    @Nullable
    public static String getStringOr(String pKey, JsonObject pJson, @Nullable String pDefaultValue) {
        JsonElement jsonelement = pJson.get(pKey);
        if (jsonelement != null) {
            return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsString();
        } else {
            return pDefaultValue;
        }
    }

    @Nullable
    public static UUID getUuidOr(String pKey, JsonObject pJson, @Nullable UUID pDefaultValue) {
        String s = getStringOr(pKey, pJson, null);
        return s == null ? pDefaultValue : UndashedUuid.fromStringLenient(s);
    }

    public static int getIntOr(String pKey, JsonObject pJson, int pDefaultValue) {
        JsonElement jsonelement = pJson.get(pKey);
        if (jsonelement != null) {
            return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsInt();
        } else {
            return pDefaultValue;
        }
    }

    public static long getLongOr(String pKey, JsonObject pJson, long pDefaultValue) {
        JsonElement jsonelement = pJson.get(pKey);
        if (jsonelement != null) {
            return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsLong();
        } else {
            return pDefaultValue;
        }
    }

    public static boolean getBooleanOr(String pKey, JsonObject pJson, boolean pDefaultValue) {
        JsonElement jsonelement = pJson.get(pKey);
        if (jsonelement != null) {
            return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsBoolean();
        } else {
            return pDefaultValue;
        }
    }

    public static Date getDateOr(String pKey, JsonObject pJson) {
        JsonElement jsonelement = pJson.get(pKey);
        return jsonelement != null ? new Date(Long.parseLong(jsonelement.getAsString())) : new Date();
    }
}
