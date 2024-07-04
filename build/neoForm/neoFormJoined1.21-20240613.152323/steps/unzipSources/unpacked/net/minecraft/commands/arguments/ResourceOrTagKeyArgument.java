package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class ResourceOrTagKeyArgument<T> implements ArgumentType<ResourceOrTagKeyArgument.Result<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
    final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceOrTagKeyArgument(ResourceKey<? extends Registry<T>> pRegistryKey) {
        this.registryKey = pRegistryKey;
    }

    public static <T> ResourceOrTagKeyArgument<T> resourceOrTagKey(ResourceKey<? extends Registry<T>> pRegistryKey) {
        return new ResourceOrTagKeyArgument<>(pRegistryKey);
    }

    public static <T> ResourceOrTagKeyArgument.Result<T> getResourceOrTagKey(
        CommandContext<CommandSourceStack> pContext, String pArgument, ResourceKey<Registry<T>> pRegistryKey, DynamicCommandExceptionType pDynamicCommandExceptionType
    ) throws CommandSyntaxException {
        ResourceOrTagKeyArgument.Result<?> result = pContext.getArgument(pArgument, ResourceOrTagKeyArgument.Result.class);
        Optional<ResourceOrTagKeyArgument.Result<T>> optional = result.cast(pRegistryKey);
        return optional.orElseThrow(() -> pDynamicCommandExceptionType.create(result));
    }

    public ResourceOrTagKeyArgument.Result<T> parse(StringReader pReader) throws CommandSyntaxException {
        if (pReader.canRead() && pReader.peek() == '#') {
            int i = pReader.getCursor();

            try {
                pReader.skip();
                ResourceLocation resourcelocation1 = ResourceLocation.read(pReader);
                return new ResourceOrTagKeyArgument.TagResult<>(TagKey.create(this.registryKey, resourcelocation1));
            } catch (CommandSyntaxException commandsyntaxexception) {
                pReader.setCursor(i);
                throw commandsyntaxexception;
            }
        } else {
            ResourceLocation resourcelocation = ResourceLocation.read(pReader);
            return new ResourceOrTagKeyArgument.ResourceResult<>(ResourceKey.create(this.registryKey, resourcelocation));
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
        return pContext.getSource() instanceof SharedSuggestionProvider sharedsuggestionprovider
            ? sharedsuggestionprovider.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL, pBuilder, pContext)
            : pBuilder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ResourceOrTagKeyArgument.Info<T>.Template> {
        public void serializeToNetwork(ResourceOrTagKeyArgument.Info<T>.Template pTemplate, FriendlyByteBuf pBuffer) {
            pBuffer.writeResourceKey(pTemplate.registryKey);
        }

        public ResourceOrTagKeyArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
            return new ResourceOrTagKeyArgument.Info.Template(pBuffer.readRegistryKey());
        }

        public void serializeToJson(ResourceOrTagKeyArgument.Info<T>.Template pTemplate, JsonObject pJson) {
            pJson.addProperty("registry", pTemplate.registryKey.location().toString());
        }

        public ResourceOrTagKeyArgument.Info<T>.Template unpack(ResourceOrTagKeyArgument<T> pArgument) {
            return new ResourceOrTagKeyArgument.Info.Template(pArgument.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceOrTagKeyArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(ResourceKey<? extends Registry<T>> pRegistryKey) {
                this.registryKey = pRegistryKey;
            }

            public ResourceOrTagKeyArgument<T> instantiate(CommandBuildContext pContext) {
                return new ResourceOrTagKeyArgument<>(this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }

    static record ResourceResult<T>(ResourceKey<T> key) implements ResourceOrTagKeyArgument.Result<T> {
        @Override
        public Either<ResourceKey<T>, TagKey<T>> unwrap() {
            return Either.left(this.key);
        }

        @Override
        public <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> pRegistryKey) {
            return this.key.cast(pRegistryKey).map(ResourceOrTagKeyArgument.ResourceResult::new);
        }

        public boolean test(Holder<T> pHolder) {
            return pHolder.is(this.key);
        }

        @Override
        public String asPrintable() {
            return this.key.location().toString();
        }
    }

    public interface Result<T> extends Predicate<Holder<T>> {
        Either<ResourceKey<T>, TagKey<T>> unwrap();

        <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> pRegistryKey);

        String asPrintable();
    }

    static record TagResult<T>(TagKey<T> key) implements ResourceOrTagKeyArgument.Result<T> {
        @Override
        public Either<ResourceKey<T>, TagKey<T>> unwrap() {
            return Either.right(this.key);
        }

        @Override
        public <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> pRegistryKey) {
            return this.key.cast(pRegistryKey).map(ResourceOrTagKeyArgument.TagResult::new);
        }

        public boolean test(Holder<T> pHolder) {
            return pHolder.is(this.key);
        }

        @Override
        public String asPrintable() {
            return "#" + this.key.location();
        }
    }
}
