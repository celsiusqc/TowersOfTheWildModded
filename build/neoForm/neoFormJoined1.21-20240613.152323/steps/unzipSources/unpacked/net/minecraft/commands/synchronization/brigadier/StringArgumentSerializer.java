package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType.StringType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class StringArgumentSerializer implements ArgumentTypeInfo<StringArgumentType, StringArgumentSerializer.Template> {
    public void serializeToNetwork(StringArgumentSerializer.Template pTemplate, FriendlyByteBuf pBuffer) {
        pBuffer.writeEnum(pTemplate.type);
    }

    public StringArgumentSerializer.Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
        StringType stringtype = pBuffer.readEnum(StringType.class);
        return new StringArgumentSerializer.Template(stringtype);
    }

    public void serializeToJson(StringArgumentSerializer.Template pTemplate, JsonObject pJson) {
        pJson.addProperty("type", switch (pTemplate.type) {
            case SINGLE_WORD -> "word";
            case QUOTABLE_PHRASE -> "phrase";
            case GREEDY_PHRASE -> "greedy";
        });
    }

    public StringArgumentSerializer.Template unpack(StringArgumentType pArgument) {
        return new StringArgumentSerializer.Template(pArgument.getType());
    }

    public final class Template implements ArgumentTypeInfo.Template<StringArgumentType> {
        final StringType type;

        public Template(StringType pType) {
            this.type = pType;
        }

        public StringArgumentType instantiate(CommandBuildContext pContext) {
            return switch (this.type) {
                case SINGLE_WORD -> StringArgumentType.word();
                case QUOTABLE_PHRASE -> StringArgumentType.string();
                case GREEDY_PHRASE -> StringArgumentType.greedyString();
            };
        }

        @Override
        public ArgumentTypeInfo<StringArgumentType, ?> type() {
            return StringArgumentSerializer.this;
        }
    }
}
