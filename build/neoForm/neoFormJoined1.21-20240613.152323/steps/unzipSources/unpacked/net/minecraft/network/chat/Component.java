package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Message;
import com.mojang.serialization.JsonOps;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.contents.DataSource;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;

public interface Component extends Message, FormattedText {
    Style getStyle();

    ComponentContents getContents();

    @Override
    default String getString() {
        return FormattedText.super.getString();
    }

    /**
     * Get the plain text of this FormattedText, without any styling or formatting codes, limited to {@code maxLength} characters.
     */
    default String getString(int pMaxLength) {
        StringBuilder stringbuilder = new StringBuilder();
        this.visit(p_130673_ -> {
            int i = pMaxLength - stringbuilder.length();
            if (i <= 0) {
                return STOP_ITERATION;
            } else {
                stringbuilder.append(p_130673_.length() <= i ? p_130673_ : p_130673_.substring(0, i));
                return Optional.empty();
            }
        });
        return stringbuilder.toString();
    }

    List<Component> getSiblings();

    @Nullable
    default String tryCollapseToString() {
        if (this.getContents() instanceof PlainTextContents plaintextcontents && this.getSiblings().isEmpty() && this.getStyle().isEmpty()) {
            return plaintextcontents.text();
        }

        return null;
    }

    default MutableComponent plainCopy() {
        return MutableComponent.create(this.getContents());
    }

    default MutableComponent copy() {
        return new MutableComponent(this.getContents(), new ArrayList<>(this.getSiblings()), this.getStyle());
    }

    FormattedCharSequence getVisualOrderText();

    @Override
    default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> pAcceptor, Style pStyle) {
        Style style = this.getStyle().applyTo(pStyle);
        Optional<T> optional = this.getContents().visit(pAcceptor, style);
        if (optional.isPresent()) {
            return optional;
        } else {
            for (Component component : this.getSiblings()) {
                Optional<T> optional1 = component.visit(pAcceptor, style);
                if (optional1.isPresent()) {
                    return optional1;
                }
            }

            return Optional.empty();
        }
    }

    @Override
    default <T> Optional<T> visit(FormattedText.ContentConsumer<T> pAcceptor) {
        Optional<T> optional = this.getContents().visit(pAcceptor);
        if (optional.isPresent()) {
            return optional;
        } else {
            for (Component component : this.getSiblings()) {
                Optional<T> optional1 = component.visit(pAcceptor);
                if (optional1.isPresent()) {
                    return optional1;
                }
            }

            return Optional.empty();
        }
    }

    default List<Component> toFlatList() {
        return this.toFlatList(Style.EMPTY);
    }

    default List<Component> toFlatList(Style pStyle) {
        List<Component> list = Lists.newArrayList();
        this.visit((p_178403_, p_178404_) -> {
            if (!p_178404_.isEmpty()) {
                list.add(literal(p_178404_).withStyle(p_178403_));
            }

            return Optional.empty();
        }, pStyle);
        return list;
    }

    default boolean contains(Component pOther) {
        if (this.equals(pOther)) {
            return true;
        } else {
            List<Component> list = this.toFlatList();
            List<Component> list1 = pOther.toFlatList(this.getStyle());
            return Collections.indexOfSubList(list, list1) != -1;
        }
    }

    static Component nullToEmpty(@Nullable String pText) {
        return (Component)(pText != null ? literal(pText) : CommonComponents.EMPTY);
    }

    static MutableComponent literal(String pText) {
        return MutableComponent.create(PlainTextContents.create(pText));
    }

    static MutableComponent translatable(String pKey) {
        return MutableComponent.create(new TranslatableContents(pKey, null, TranslatableContents.NO_ARGS));
    }

    static MutableComponent translatable(String pKey, Object... pArgs) {
        return MutableComponent.create(new TranslatableContents(pKey, null, pArgs));
    }

    static MutableComponent translatableEscape(String pKey, Object... pArgs) {
        for (int i = 0; i < pArgs.length; i++) {
            Object object = pArgs[i];
            if (!TranslatableContents.isAllowedPrimitiveArgument(object) && !(object instanceof Component)) {
                pArgs[i] = String.valueOf(object);
            }
        }

        return translatable(pKey, pArgs);
    }

    static MutableComponent translatableWithFallback(String pKey, @Nullable String pFallback) {
        return MutableComponent.create(new TranslatableContents(pKey, pFallback, TranslatableContents.NO_ARGS));
    }

    static MutableComponent translatableWithFallback(String pKey, @Nullable String pFallback, Object... pArgs) {
        return MutableComponent.create(new TranslatableContents(pKey, pFallback, pArgs));
    }

    static MutableComponent empty() {
        return MutableComponent.create(PlainTextContents.EMPTY);
    }

    static MutableComponent keybind(String pName) {
        return MutableComponent.create(new KeybindContents(pName));
    }

    static MutableComponent nbt(String pNbtPathPattern, boolean pInterpreting, Optional<Component> pSeparator, DataSource pDataSource) {
        return MutableComponent.create(new NbtContents(pNbtPathPattern, pInterpreting, pSeparator, pDataSource));
    }

    static MutableComponent score(String pName, String pObjective) {
        return MutableComponent.create(new ScoreContents(pName, pObjective));
    }

    static MutableComponent selector(String pPattern, Optional<Component> pSeparator) {
        return MutableComponent.create(new SelectorContents(pPattern, pSeparator));
    }

    static Component translationArg(Date pDate) {
        return literal(pDate.toString());
    }

    static Component translationArg(Message pMessage) {
        return (Component)(pMessage instanceof Component component ? component : literal(pMessage.getString()));
    }

    static Component translationArg(UUID pUuid) {
        return literal(pUuid.toString());
    }

    static Component translationArg(ResourceLocation pLocation) {
        return literal(pLocation.toString());
    }

    static Component translationArg(ChunkPos pChunkPos) {
        return literal(pChunkPos.toString());
    }

    static Component translationArg(URI p_352152_) {
        return literal(p_352152_.toString());
    }

    public static class Serializer {
        private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

        private Serializer() {
        }

        static MutableComponent deserialize(JsonElement pJson, HolderLookup.Provider pProvider) {
            return (MutableComponent)ComponentSerialization.CODEC
                .parse(pProvider.createSerializationContext(JsonOps.INSTANCE), pJson)
                .getOrThrow(JsonParseException::new);
        }

        static JsonElement serialize(Component pComponent, HolderLookup.Provider pProvider) {
            return ComponentSerialization.CODEC
                .encodeStart(pProvider.createSerializationContext(JsonOps.INSTANCE), pComponent)
                .getOrThrow(JsonParseException::new);
        }

        public static String toJson(Component pComponent, HolderLookup.Provider pRegistries) {
            return GSON.toJson(serialize(pComponent, pRegistries));
        }

        @Nullable
        public static MutableComponent fromJson(String pJson, HolderLookup.Provider pRegistries) {
            JsonElement jsonelement = JsonParser.parseString(pJson);
            return jsonelement == null ? null : deserialize(jsonelement, pRegistries);
        }

        @Nullable
        public static MutableComponent fromJson(@Nullable JsonElement pJson, HolderLookup.Provider pRegistries) {
            return pJson == null ? null : deserialize(pJson, pRegistries);
        }

        @Nullable
        public static MutableComponent fromJsonLenient(String pJson, HolderLookup.Provider pRegistries) {
            JsonReader jsonreader = new JsonReader(new StringReader(pJson));
            jsonreader.setLenient(true);
            JsonElement jsonelement = JsonParser.parseReader(jsonreader);
            return jsonelement == null ? null : deserialize(jsonelement, pRegistries);
        }
    }

    public static class SerializerAdapter implements JsonDeserializer<MutableComponent>, JsonSerializer<Component> {
        private final HolderLookup.Provider registries;

        public SerializerAdapter(HolderLookup.Provider pRegistries) {
            this.registries = pRegistries;
        }

        public MutableComponent deserialize(JsonElement pJson, Type pTypeOfT, JsonDeserializationContext pContext) throws JsonParseException {
            return Component.Serializer.deserialize(pJson, this.registries);
        }

        public JsonElement serialize(Component pSrc, Type pTypeOfSrc, JsonSerializationContext pContext) {
            return Component.Serializer.serialize(pSrc, this.registries);
        }
    }
}
