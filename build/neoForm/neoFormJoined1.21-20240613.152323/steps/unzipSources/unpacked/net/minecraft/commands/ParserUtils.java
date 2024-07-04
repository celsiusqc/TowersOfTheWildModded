package net.minecraft.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Field;
import net.minecraft.CharPredicate;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;

public class ParserUtils {
    private static final Field JSON_READER_POS = Util.make(() -> {
        try {
            Field field = JsonReader.class.getDeclaredField("pos");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException nosuchfieldexception) {
            throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", nosuchfieldexception);
        }
    });
    private static final Field JSON_READER_LINESTART = Util.make(() -> {
        try {
            Field field = JsonReader.class.getDeclaredField("lineStart");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException nosuchfieldexception) {
            throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", nosuchfieldexception);
        }
    });

    private static int getPos(JsonReader pReader) {
        try {
            return JSON_READER_POS.getInt(pReader) - JSON_READER_LINESTART.getInt(pReader);
        } catch (IllegalAccessException illegalaccessexception) {
            throw new IllegalStateException("Couldn't read position of JsonReader", illegalaccessexception);
        }
    }

    public static <T> T parseJson(HolderLookup.Provider pRegistries, StringReader pReader, Codec<T> pCodec) {
        JsonReader jsonreader = new JsonReader(new java.io.StringReader(pReader.getRemaining()));
        jsonreader.setLenient(false);

        Object object;
        try {
            JsonElement jsonelement = Streams.parse(jsonreader);
            object = pCodec.parse(pRegistries.createSerializationContext(JsonOps.INSTANCE), jsonelement).getOrThrow(JsonParseException::new);
        } catch (StackOverflowError stackoverflowerror) {
            throw new JsonParseException(stackoverflowerror);
        } finally {
            pReader.setCursor(pReader.getCursor() + getPos(jsonreader));
        }

        return (T)object;
    }

    public static String readWhile(StringReader pReader, CharPredicate pPredicate) {
        int i = pReader.getCursor();

        while (pReader.canRead() && pPredicate.test(pReader.peek())) {
            pReader.skip();
        }

        return pReader.getString().substring(i, pReader.getCursor());
    }
}
