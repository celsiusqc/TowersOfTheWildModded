package net.minecraft.commands.arguments;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ResourceOrIdArgument<T> implements ArgumentType<Holder<T>> {
    private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
    public static final DynamicCommandExceptionType ERROR_FAILED_TO_PARSE = new DynamicCommandExceptionType(
        p_335811_ -> Component.translatableEscape("argument.resource_or_id.failed_to_parse", p_335811_)
    );
    private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("argument.resource_or_id.invalid"));
    private final HolderLookup.Provider registryLookup;
    private final boolean hasRegistry;
    private final Codec<Holder<T>> codec;

    protected ResourceOrIdArgument(CommandBuildContext pRegistryLookup, ResourceKey<Registry<T>> pRegistryKey, Codec<Holder<T>> pCodec) {
        this.registryLookup = pRegistryLookup;
        this.hasRegistry = pRegistryLookup.lookup(pRegistryKey).isPresent();
        this.codec = pCodec;
    }

    public static ResourceOrIdArgument.LootTableArgument lootTable(CommandBuildContext pContext) {
        return new ResourceOrIdArgument.LootTableArgument(pContext);
    }

    public static Holder<LootTable> getLootTable(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return getResource(pContext, pName);
    }

    public static ResourceOrIdArgument.LootModifierArgument lootModifier(CommandBuildContext pContext) {
        return new ResourceOrIdArgument.LootModifierArgument(pContext);
    }

    public static Holder<LootItemFunction> getLootModifier(CommandContext<CommandSourceStack> pContext, String pName) {
        return getResource(pContext, pName);
    }

    public static ResourceOrIdArgument.LootPredicateArgument lootPredicate(CommandBuildContext pContext) {
        return new ResourceOrIdArgument.LootPredicateArgument(pContext);
    }

    public static Holder<LootItemCondition> getLootPredicate(CommandContext<CommandSourceStack> pContext, String pName) {
        return getResource(pContext, pName);
    }

    private static <T> Holder<T> getResource(CommandContext<CommandSourceStack> pContext, String pName) {
        return pContext.getArgument(pName, Holder.class);
    }

    @Nullable
    public Holder<T> parse(StringReader pReader) throws CommandSyntaxException {
        Tag tag = parseInlineOrId(pReader);
        if (!this.hasRegistry) {
            return null;
        } else {
            RegistryOps<Tag> registryops = this.registryLookup.createSerializationContext(NbtOps.INSTANCE);
            return this.codec.parse(registryops, tag).getOrThrow(p_335883_ -> ERROR_FAILED_TO_PARSE.createWithContext(pReader, p_335883_));
        }
    }

    @VisibleForTesting
    static Tag parseInlineOrId(StringReader pReader) throws CommandSyntaxException {
        int i = pReader.getCursor();
        Tag tag = new TagParser(pReader).readValue();
        if (hasConsumedWholeArg(pReader)) {
            return tag;
        } else {
            pReader.setCursor(i);
            ResourceLocation resourcelocation = ResourceLocation.read(pReader);
            if (hasConsumedWholeArg(pReader)) {
                return StringTag.valueOf(resourcelocation.toString());
            } else {
                pReader.setCursor(i);
                throw ERROR_INVALID.createWithContext(pReader);
            }
        }
    }

    private static boolean hasConsumedWholeArg(StringReader pReader) {
        return !pReader.canRead() || pReader.peek() == ' ';
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class LootModifierArgument extends ResourceOrIdArgument<LootItemFunction> {
        protected LootModifierArgument(CommandBuildContext pContext) {
            super(pContext, Registries.ITEM_MODIFIER, LootItemFunctions.CODEC);
        }
    }

    public static class LootPredicateArgument extends ResourceOrIdArgument<LootItemCondition> {
        protected LootPredicateArgument(CommandBuildContext pContext) {
            super(pContext, Registries.PREDICATE, LootItemCondition.CODEC);
        }
    }

    public static class LootTableArgument extends ResourceOrIdArgument<LootTable> {
        protected LootTableArgument(CommandBuildContext pContext) {
            super(pContext, Registries.LOOT_TABLE, LootTable.CODEC);
        }
    }
}
