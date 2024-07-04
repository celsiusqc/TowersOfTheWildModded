package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class StonecutterRecipe extends SingleItemRecipe {
    public StonecutterRecipe(String pGroup, Ingredient pIngredient, ItemStack pResult) {
        super(RecipeType.STONECUTTING, RecipeSerializer.STONECUTTER, pGroup, pIngredient, pResult);
    }

    public boolean matches(SingleRecipeInput p_344927_, Level p_345392_) {
        return this.ingredient.test(p_344927_.item());
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.STONECUTTER);
    }
}
