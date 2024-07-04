package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public interface RecipeOutput extends net.neoforged.neoforge.common.extensions.IRecipeOutputExtension {
    default void accept(ResourceLocation pLocation, Recipe<?> pRecipe, @Nullable AdvancementHolder pAdvancement) {
        accept(pLocation, pRecipe, pAdvancement, new net.neoforged.neoforge.common.conditions.ICondition[0]);
    }

    Advancement.Builder advancement();
}
