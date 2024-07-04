package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FunctionArgument implements ArgumentType<FunctionArgument.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "#foo");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
        p_304129_ -> Component.translatableEscape("arguments.function.tag.unknown", p_304129_)
    );
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_FUNCTION = new DynamicCommandExceptionType(
        p_304130_ -> Component.translatableEscape("arguments.function.unknown", p_304130_)
    );

    public static FunctionArgument functions() {
        return new FunctionArgument();
    }

    public FunctionArgument.Result parse(StringReader pReader) throws CommandSyntaxException {
        if (pReader.canRead() && pReader.peek() == '#') {
            pReader.skip();
            final ResourceLocation resourcelocation1 = ResourceLocation.read(pReader);
            return new FunctionArgument.Result() {
                @Override
                public Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> p_120943_) throws CommandSyntaxException {
                    return FunctionArgument.getFunctionTag(p_120943_, resourcelocation1);
                }

                @Override
                public Pair<ResourceLocation, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
                    CommandContext<CommandSourceStack> p_120945_
                ) throws CommandSyntaxException {
                    return Pair.of(resourcelocation1, Either.right(FunctionArgument.getFunctionTag(p_120945_, resourcelocation1)));
                }

                @Override
                public Pair<ResourceLocation, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> p_314710_) throws CommandSyntaxException {
                    return Pair.of(resourcelocation1, FunctionArgument.getFunctionTag(p_314710_, resourcelocation1));
                }
            };
        } else {
            final ResourceLocation resourcelocation = ResourceLocation.read(pReader);
            return new FunctionArgument.Result() {
                @Override
                public Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> p_120952_) throws CommandSyntaxException {
                    return Collections.singleton(FunctionArgument.getFunction(p_120952_, resourcelocation));
                }

                @Override
                public Pair<ResourceLocation, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
                    CommandContext<CommandSourceStack> p_120954_
                ) throws CommandSyntaxException {
                    return Pair.of(resourcelocation, Either.left(FunctionArgument.getFunction(p_120954_, resourcelocation)));
                }

                @Override
                public Pair<ResourceLocation, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> p_314709_) throws CommandSyntaxException {
                    return Pair.of(resourcelocation, Collections.singleton(FunctionArgument.getFunction(p_314709_, resourcelocation)));
                }
            };
        }
    }

    static CommandFunction<CommandSourceStack> getFunction(CommandContext<CommandSourceStack> pContext, ResourceLocation pId) throws CommandSyntaxException {
        return pContext.getSource().getServer().getFunctions().get(pId).orElseThrow(() -> ERROR_UNKNOWN_FUNCTION.create(pId.toString()));
    }

    static Collection<CommandFunction<CommandSourceStack>> getFunctionTag(CommandContext<CommandSourceStack> pContext, ResourceLocation pId) throws CommandSyntaxException {
        Collection<CommandFunction<CommandSourceStack>> collection = pContext.getSource().getServer().getFunctions().getTag(pId);
        if (collection == null) {
            throw ERROR_UNKNOWN_TAG.create(pId.toString());
        } else {
            return collection;
        }
    }

    public static Collection<CommandFunction<CommandSourceStack>> getFunctions(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return pContext.getArgument(pName, FunctionArgument.Result.class).create(pContext);
    }

    public static Pair<ResourceLocation, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> getFunctionOrTag(
        CommandContext<CommandSourceStack> pContext, String pName
    ) throws CommandSyntaxException {
        return pContext.getArgument(pName, FunctionArgument.Result.class).unwrap(pContext);
    }

    public static Pair<ResourceLocation, Collection<CommandFunction<CommandSourceStack>>> getFunctionCollection(
        CommandContext<CommandSourceStack> pContext, String pName
    ) throws CommandSyntaxException {
        return pContext.getArgument(pName, FunctionArgument.Result.class).unwrapToCollection(pContext);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public interface Result {
        Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;

        Pair<ResourceLocation, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
            CommandContext<CommandSourceStack> pContext
        ) throws CommandSyntaxException;

        Pair<ResourceLocation, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
    }
}
