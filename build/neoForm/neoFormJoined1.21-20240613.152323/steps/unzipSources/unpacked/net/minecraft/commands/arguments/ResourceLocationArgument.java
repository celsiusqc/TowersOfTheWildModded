package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

public class ResourceLocationArgument implements ArgumentType<ResourceLocation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ADVANCEMENT = new DynamicCommandExceptionType(
        p_304104_ -> Component.translatableEscape("advancement.advancementNotFound", p_304104_)
    );
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_RECIPE = new DynamicCommandExceptionType(
        p_304105_ -> Component.translatableEscape("recipe.notFound", p_304105_)
    );

    public static ResourceLocationArgument id() {
        return new ResourceLocationArgument();
    }

    public static AdvancementHolder getAdvancement(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        ResourceLocation resourcelocation = getId(pContext, pName);
        AdvancementHolder advancementholder = pContext.getSource().getAdvancement(resourcelocation);
        if (advancementholder == null) {
            throw ERROR_UNKNOWN_ADVANCEMENT.create(resourcelocation);
        } else {
            return advancementholder;
        }
    }

    public static RecipeHolder<?> getRecipe(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        RecipeManager recipemanager = pContext.getSource().getRecipeManager();
        ResourceLocation resourcelocation = getId(pContext, pName);
        return recipemanager.byKey(resourcelocation).orElseThrow(() -> ERROR_UNKNOWN_RECIPE.create(resourcelocation));
    }

    public static ResourceLocation getId(CommandContext<CommandSourceStack> pContext, String pName) {
        return pContext.getArgument(pName, ResourceLocation.class);
    }

    public ResourceLocation parse(StringReader pReader) throws CommandSyntaxException {
        return ResourceLocation.read(pReader);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
