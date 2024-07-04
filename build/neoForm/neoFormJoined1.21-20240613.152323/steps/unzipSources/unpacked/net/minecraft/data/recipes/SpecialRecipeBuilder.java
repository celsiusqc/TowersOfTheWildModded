package net.minecraft.data.recipes;

import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;

public class SpecialRecipeBuilder {
    private final Function<CraftingBookCategory, Recipe<?>> factory;

    public SpecialRecipeBuilder(Function<CraftingBookCategory, Recipe<?>> p_312708_) {
        this.factory = p_312708_;
    }

    public static SpecialRecipeBuilder special(Function<CraftingBookCategory, Recipe<?>> p_312084_) {
        return new SpecialRecipeBuilder(p_312084_);
    }

    public void save(RecipeOutput pRecipeOutput, String pRecipeId) {
        this.save(pRecipeOutput, ResourceLocation.parse(pRecipeId));
    }

    public void save(RecipeOutput pRecipeOutput, ResourceLocation pRecipeId) {
        pRecipeOutput.accept(pRecipeId, this.factory.apply(CraftingBookCategory.MISC), null);
    }
}
