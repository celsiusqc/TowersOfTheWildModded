package net.minecraft.resources;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.GsonHelper;

/**
 * An immutable location of a resource, in terms of a path and namespace.
 * <p>
 * This is used as an identifier for a resource, usually for those housed in a {@link net.minecraft.core.Registry}, such as blocks and items.
 * <p>
 * {@code minecraft} is always taken as the default namespace for a resource location when none is explicitly stated. When using this for registering objects, this namespace <strong>should</strong> only be used for resources added by Minecraft itself.
 * <p>
 * Generally, and by the implementation of {@link #toString()}, the string representation of this class is expressed in the form {@code namespace:path}. The colon is also used as the default separator for parsing strings as a {@code ResourceLocation}.
 * @see net.minecraft.resources.ResourceKey
 */
public final class ResourceLocation implements Comparable<ResourceLocation> {
    public static final Codec<ResourceLocation> CODEC = Codec.STRING
        .<ResourceLocation>comapFlatMap(ResourceLocation::read, ResourceLocation::toString)
        .stable();
    public static final StreamCodec<ByteBuf, ResourceLocation> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
        .map(ResourceLocation::parse, ResourceLocation::toString);
    public static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("argument.id.invalid"));
    public static final char NAMESPACE_SEPARATOR = ':';
    public static final String DEFAULT_NAMESPACE = "minecraft";
    public static final String REALMS_NAMESPACE = "realms";
    private final String namespace;
    private final String path;

    private ResourceLocation(String pNamespace, String pPath) {
        assert isValidNamespace(pNamespace);

        assert isValidPath(pPath);

        this.namespace = pNamespace;
        this.path = pPath;
    }

    private static ResourceLocation createUntrusted(String p_352179_, String p_352115_) {
        return new ResourceLocation(assertValidNamespace(p_352179_, p_352115_), assertValidPath(p_352179_, p_352115_));
    }

    public static ResourceLocation fromNamespaceAndPath(String p_350583_, String p_350305_) {
        return createUntrusted(p_350583_, p_350305_);
    }

    public static ResourceLocation parse(String p_350801_) {
        return bySeparator(p_350801_, ':');
    }

    public static ResourceLocation withDefaultNamespace(String p_350998_) {
        return new ResourceLocation("minecraft", assertValidPath("minecraft", p_350998_));
    }

    /**
     * Attempts to parse the specified {@code location} as a {@code ResourceLocation} by splitting it into a
     * namespace and path by a colon.
     * <p>
     * If no colon is present in the {@code location}, the namespace defaults to {@code minecraft}, taking the {@code location} as the path.
     * @return the parsed resource location; otherwise {@code null} if there is a non {@code [a-z0-9_.-]} character in the decomposed namespace or a non {@code [a-z0-9/._-]} character in the decomposed path
     * @see #of(String, char)
     *
     * @param pLocation the location string to try to parse as a {@code
     *                  ResourceLocation}
     */
    @Nullable
    public static ResourceLocation tryParse(String pLocation) {
        return tryBySeparator(pLocation, ':');
    }

    @Nullable
    public static ResourceLocation tryBuild(String pNamespace, String pPath) {
        return isValidNamespace(pNamespace) && isValidPath(pPath) ? new ResourceLocation(pNamespace, pPath) : null;
    }

    public static ResourceLocation bySeparator(String p_135823_, char p_135824_) {
        int i = p_135823_.indexOf(p_135824_);
        if (i >= 0) {
            String s = p_135823_.substring(i + 1);
            if (i != 0) {
                String s1 = p_135823_.substring(0, i);
                return createUntrusted(s1, s);
            } else {
                return withDefaultNamespace(s);
            }
        } else {
            return withDefaultNamespace(p_135823_);
        }
    }

    @Nullable
    public static ResourceLocation tryBySeparator(String p_352469_, char p_352244_) {
        int i = p_352469_.indexOf(p_352244_);
        if (i >= 0) {
            String s = p_352469_.substring(i + 1);
            if (!isValidPath(s)) {
                return null;
            } else if (i != 0) {
                String s1 = p_352469_.substring(0, i);
                return isValidNamespace(s1) ? new ResourceLocation(s1, s) : null;
            } else {
                return new ResourceLocation("minecraft", s);
            }
        } else {
            return isValidPath(p_352469_) ? new ResourceLocation("minecraft", p_352469_) : null;
        }
    }

    public static DataResult<ResourceLocation> read(String p_135838_) {
        try {
            return DataResult.success(parse(p_135838_));
        } catch (ResourceLocationException resourcelocationexception) {
            return DataResult.error(() -> "Not a valid resource location: " + p_135838_ + " " + resourcelocationexception.getMessage());
        }
    }

    public String getPath() {
        return this.path;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public ResourceLocation withPath(String pPath) {
        return new ResourceLocation(this.namespace, assertValidPath(this.namespace, pPath));
    }

    public ResourceLocation withPath(UnaryOperator<String> pPathOperator) {
        return this.withPath(pPathOperator.apply(this.path));
    }

    public ResourceLocation withPrefix(String pPathPrefix) {
        return this.withPath(pPathPrefix + this.path);
    }

    public ResourceLocation withSuffix(String pPathSuffix) {
        return this.withPath(this.path + pPathSuffix);
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof ResourceLocation resourcelocation)
                ? false
                : this.namespace.equals(resourcelocation.namespace) && this.path.equals(resourcelocation.path);
        }
    }

    @Override
    public int hashCode() {
        return 31 * this.namespace.hashCode() + this.path.hashCode();
    }

    public int compareTo(ResourceLocation pOther) {
        int i = this.path.compareTo(pOther.path);
        if (i == 0) {
            i = this.namespace.compareTo(pOther.namespace);
        }

        return i;
    }

    // Normal compare sorts by path first, this compares namespace first.
    public int compareNamespaced(ResourceLocation o) {
        int ret = this.namespace.compareTo(o.namespace);
        return ret != 0 ? ret : this.path.compareTo(o.path);
    }

    public String toDebugFileName() {
        return this.toString().replace('/', '_').replace(':', '_');
    }

    public String toLanguageKey() {
        return this.namespace + "." + this.path;
    }

    public String toShortLanguageKey() {
        return this.namespace.equals("minecraft") ? this.path : this.toLanguageKey();
    }

    public String toLanguageKey(String pType) {
        return pType + "." + this.toLanguageKey();
    }

    public String toLanguageKey(String pType, String pKey) {
        return pType + "." + this.toLanguageKey() + "." + pKey;
    }

    private static String readGreedy(StringReader pReader) {
        int i = pReader.getCursor();

        while (pReader.canRead() && isAllowedInResourceLocation(pReader.peek())) {
            pReader.skip();
        }

        return pReader.getString().substring(i, pReader.getCursor());
    }

    public static ResourceLocation read(StringReader pReader) throws CommandSyntaxException {
        int i = pReader.getCursor();
        String s = readGreedy(pReader);

        try {
            return parse(s);
        } catch (ResourceLocationException resourcelocationexception) {
            pReader.setCursor(i);
            throw ERROR_INVALID.createWithContext(pReader);
        }
    }

    public static ResourceLocation readNonEmpty(StringReader pReader) throws CommandSyntaxException {
        int i = pReader.getCursor();
        String s = readGreedy(pReader);
        if (s.isEmpty()) {
            throw ERROR_INVALID.createWithContext(pReader);
        } else {
            try {
                return parse(s);
            } catch (ResourceLocationException resourcelocationexception) {
                pReader.setCursor(i);
                throw ERROR_INVALID.createWithContext(pReader);
            }
        }
    }

    public static boolean isAllowedInResourceLocation(char pCharacter) {
        return pCharacter >= '0' && pCharacter <= '9'
            || pCharacter >= 'a' && pCharacter <= 'z'
            || pCharacter == '_'
            || pCharacter == ':'
            || pCharacter == '/'
            || pCharacter == '.'
            || pCharacter == '-';
    }

    /**
     * @return {@code true} if the specified {@code path} is valid: consists only of {@code [a-z0-9/._-]} characters
     */
    public static boolean isValidPath(String pPath) {
        for (int i = 0; i < pPath.length(); i++) {
            if (!validPathChar(pPath.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return {@code true} if the specified {@code namespace} is valid: consists only of {@code [a-z0-9_.-]} characters
     */
    public static boolean isValidNamespace(String pNamespace) {
        for (int i = 0; i < pNamespace.length(); i++) {
            if (!validNamespaceChar(pNamespace.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static String assertValidNamespace(String pNamespace, String pPath) {
        if (!isValidNamespace(pNamespace)) {
            throw new ResourceLocationException("Non [a-z0-9_.-] character in namespace of location: " + pNamespace + ":" + pPath);
        } else {
            return pNamespace;
        }
    }

    public static boolean validPathChar(char pPathChar) {
        return pPathChar == '_'
            || pPathChar == '-'
            || pPathChar >= 'a' && pPathChar <= 'z'
            || pPathChar >= '0' && pPathChar <= '9'
            || pPathChar == '/'
            || pPathChar == '.';
    }

    public static boolean validNamespaceChar(char pNamespaceChar) {
        return pNamespaceChar == '_' || pNamespaceChar == '-' || pNamespaceChar >= 'a' && pNamespaceChar <= 'z' || pNamespaceChar >= '0' && pNamespaceChar <= '9' || pNamespaceChar == '.';
    }

    private static String assertValidPath(String pNamespace, String pPath) {
        if (!isValidPath(pPath)) {
            throw new ResourceLocationException("Non [a-z0-9/._-] character in path of location: " + pNamespace + ":" + pPath);
        } else {
            return pPath;
        }
    }

    public static class Serializer implements JsonDeserializer<ResourceLocation>, JsonSerializer<ResourceLocation> {
        public ResourceLocation deserialize(JsonElement pJson, Type pTypeOfT, JsonDeserializationContext pContext) throws JsonParseException {
            return ResourceLocation.parse(GsonHelper.convertToString(pJson, "location"));
        }

        public JsonElement serialize(ResourceLocation pSrc, Type pTypeOfSrc, JsonSerializationContext pContext) {
            return new JsonPrimitive(pSrc.toString());
        }
    }
}
