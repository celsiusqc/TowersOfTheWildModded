package net.minecraft.world.inventory;

import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

public abstract class RecipeBookMenu<I extends RecipeInput, R extends Recipe<I>> extends AbstractContainerMenu {
    public RecipeBookMenu(MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }

    public void handlePlacement(boolean pPlaceAll, RecipeHolder<?> pRecipe, ServerPlayer pPlayer) {
        RecipeHolder<R> recipeholder = (RecipeHolder<R>)pRecipe;
        this.beginPlacingRecipe();

        try {
            new ServerPlaceRecipe<>(this).recipeClicked(pPlayer, recipeholder, pPlaceAll);
        } finally {
            this.finishPlacingRecipe((RecipeHolder<R>)pRecipe);
        }
    }

    protected void beginPlacingRecipe() {
    }

    protected void finishPlacingRecipe(RecipeHolder<R> p_345813_) {
    }

    public abstract void fillCraftSlotsStackedContents(StackedContents pItemHelper);

    public abstract void clearCraftingContent();

    public abstract boolean recipeMatches(RecipeHolder<R> pRecipe);

    public abstract int getResultSlotIndex();

    public abstract int getGridWidth();

    public abstract int getGridHeight();

    public abstract int getSize();

    public java.util.List<net.minecraft.client.RecipeBookCategories> getRecipeBookCategories() {
         return net.minecraft.client.RecipeBookCategories.getCategories(this.getRecipeBookType());
    }

    public abstract RecipeBookType getRecipeBookType();

    public abstract boolean shouldMoveToInventory(int pSlotIndex);
}
