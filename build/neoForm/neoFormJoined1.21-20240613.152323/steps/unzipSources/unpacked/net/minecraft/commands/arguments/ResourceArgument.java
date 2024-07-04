package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ResourceArgument<T> implements ArgumentType<Holder.Reference<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_NOT_SUMMONABLE_ENTITY = new DynamicCommandExceptionType(
        p_304094_ -> Component.translatableEscape("entity.not_summonable", p_304094_)
    );
    public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_RESOURCE = new Dynamic2CommandExceptionType(
        (p_304095_, p_304096_) -> Component.translatableEscape("argument.resource.not_found", p_304095_, p_304096_)
    );
    public static final Dynamic3CommandExceptionType ERROR_INVALID_RESOURCE_TYPE = new Dynamic3CommandExceptionType(
        (p_304097_, p_304098_, p_304099_) -> Component.translatableEscape("argument.resource.invalid_type", p_304097_, p_304098_, p_304099_)
    );
    final ResourceKey<? extends Registry<T>> registryKey;
    private final HolderLookup<T> registryLookup;

    public ResourceArgument(CommandBuildContext pContext, ResourceKey<? extends Registry<T>> pRegistryKey) {
        this.registryKey = pRegistryKey;
        this.registryLookup = pContext.lookupOrThrow(pRegistryKey);
    }

    public static <T> ResourceArgument<T> resource(CommandBuildContext pContext, ResourceKey<? extends Registry<T>> pRegistryKey) {
        return new ResourceArgument<>(pContext, pRegistryKey);
    }

    public static <T> Holder.Reference<T> getResource(CommandContext<CommandSourceStack> pContext, String pArgument, ResourceKey<Registry<T>> pRegistryKey) throws CommandSyntaxException {
        Holder.Reference<T> reference = pContext.getArgument(pArgument, Holder.Reference.class);
        ResourceKey<?> resourcekey = reference.key();
        if (resourcekey.isFor(pRegistryKey)) {
            return reference;
        } else {
            throw ERROR_INVALID_RESOURCE_TYPE.create(resourcekey.location(), resourcekey.registry(), pRegistryKey.location());
        }
    }

    public static Holder.Reference<Attribute> getAttribute(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return getResource(pContext, pArgument, Registries.ATTRIBUTE);
    }

    public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return getResource(pContext, pArgument, Registries.CONFIGURED_FEATURE);
    }

    public static Holder.Reference<Structure> getStructure(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return getResource(pContext, pArgument, Registries.STRUCTURE);
    }

    public static Holder.Reference<EntityType<?>> getEntityType(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return getResource(pContext, pArgument, Registries.ENTITY_TYPE);
    }

    public static Holder.Reference<EntityType<?>> getSummonableEntityType(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        Holder.Reference<EntityType<?>> reference = getResource(pContext, pArgument, Registries.ENTITY_TYPE);
        if (!reference.value().canSummon()) {
            throw ERROR_NOT_SUMMONABLE_ENTITY.create(reference.key().location().toString());
        } else {
            return reference;
        }
    }

    public static Holder.Reference<MobEffect> getMobEffect(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return getResource(pContext, pArgument, Registries.MOB_EFFECT);
    }

    public static Holder.Reference<Enchantment> getEnchantment(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return getResource(pContext, pArgument, Registries.ENCHANTMENT);
    }

    public Holder.Reference<T> parse(StringReader pBuilder) throws CommandSyntaxException {
        ResourceLocation resourcelocation = ResourceLocation.read(pBuilder);
        ResourceKey<T> resourcekey = ResourceKey.create(this.registryKey, resourcelocation);
        return this.registryLookup
            .get(resourcekey)
            .orElseThrow(() -> ERROR_UNKNOWN_RESOURCE.createWithContext(pBuilder, resourcelocation, this.registryKey.location()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
        return SharedSuggestionProvider.suggestResource(this.registryLookup.listElementIds().map(ResourceKey::location), pBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceArgument<T>, ResourceArgument.Info<T>.Template> {
        public void serializeToNetwork(ResourceArgument.Info<T>.Template pTemplate, FriendlyByteBuf pBuffer) {
            pBuffer.writeResourceKey(pTemplate.registryKey);
        }

        public ResourceArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
            return new ResourceArgument.Info.Template(pBuffer.readRegistryKey());
        }

        public void serializeToJson(ResourceArgument.Info<T>.Template pTemplate, JsonObject pJson) {
            pJson.addProperty("registry", pTemplate.registryKey.location().toString());
        }

        public ResourceArgument.Info<T>.Template unpack(ResourceArgument<T> pArgument) {
            return new ResourceArgument.Info.Template(pArgument.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(ResourceKey<? extends Registry<T>> pRegistryKey) {
                this.registryKey = pRegistryKey;
            }

            public ResourceArgument<T> instantiate(CommandBuildContext pContext) {
                return new ResourceArgument<>(pContext, this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }
}
