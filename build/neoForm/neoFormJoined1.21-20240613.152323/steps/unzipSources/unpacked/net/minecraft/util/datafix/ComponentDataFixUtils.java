package net.minecraft.util.datafix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.GsonHelper;

public class ComponentDataFixUtils {
    private static final String EMPTY_CONTENTS = createTextComponentJson("");

    public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> pOps, String pText) {
        String s = createTextComponentJson(pText);
        return new Dynamic<>(pOps, pOps.createString(s));
    }

    public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> pOps) {
        return new Dynamic<>(pOps, pOps.createString(EMPTY_CONTENTS));
    }

    private static String createTextComponentJson(String pText) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("text", pText);
        return GsonHelper.toStableString(jsonobject);
    }

    public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> pOps, String pTranslationKey) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("translate", pTranslationKey);
        return new Dynamic<>(pOps, pOps.createString(GsonHelper.toStableString(jsonobject)));
    }

    public static <T> Dynamic<T> wrapLiteralStringAsComponent(Dynamic<T> pDynamic) {
        return DataFixUtils.orElse(pDynamic.asString().map(p_304989_ -> createPlainTextComponent(pDynamic.getOps(), p_304989_)).result(), pDynamic);
    }

    public static Dynamic<?> rewriteFromLenient(Dynamic<?> pDynamic) {
        Optional<String> optional = pDynamic.asString().result();
        if (optional.isEmpty()) {
            return pDynamic;
        } else {
            String s = optional.get();
            if (!s.isEmpty() && !s.equals("null")) {
                char c0 = s.charAt(0);
                char c1 = s.charAt(s.length() - 1);
                if (c0 == '"' && c1 == '"' || c0 == '{' && c1 == '}' || c0 == '[' && c1 == ']') {
                    try {
                        JsonElement jsonelement = JsonParser.parseString(s);
                        if (jsonelement.isJsonPrimitive()) {
                            return createPlainTextComponent(pDynamic.getOps(), jsonelement.getAsString());
                        }

                        return pDynamic.createString(GsonHelper.toStableString(jsonelement));
                    } catch (JsonParseException jsonparseexception) {
                    }
                }

                return createPlainTextComponent(pDynamic.getOps(), s);
            } else {
                return createEmptyComponent(pDynamic.getOps());
            }
        }
    }

    public static Optional<String> extractTranslationString(String pData) {
        try {
            JsonElement jsonelement = JsonParser.parseString(pData);
            if (jsonelement.isJsonObject()) {
                JsonObject jsonobject = jsonelement.getAsJsonObject();
                JsonElement jsonelement1 = jsonobject.get("translate");
                if (jsonelement1 != null && jsonelement1.isJsonPrimitive()) {
                    return Optional.of(jsonelement1.getAsString());
                }
            }
        } catch (JsonParseException jsonparseexception) {
        }

        return Optional.empty();
    }
}
