package net.minecraft.recipebook;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

public interface PlaceRecipe<T> {
    default void placeRecipe(int pWidth, int pHeight, int pOutputSlot, RecipeHolder<?> pRecipe, Iterator<T> pIngredients, int pMaxAmount) {
        int i = pWidth;
        int j = pHeight;
        if (pRecipe.value() instanceof ShapedRecipe shapedrecipe) {
            i = shapedrecipe.getWidth();
            j = shapedrecipe.getHeight();
        }

        int k1 = 0;

        for (int k = 0; k < pHeight; k++) {
            if (k1 == pOutputSlot) {
                k1++;
            }

            boolean flag = (float)j < (float)pHeight / 2.0F;
            int l = Mth.floor((float)pHeight / 2.0F - (float)j / 2.0F);
            if (flag && l > k) {
                k1 += pWidth;
                k++;
            }

            for (int i1 = 0; i1 < pWidth; i1++) {
                if (!pIngredients.hasNext()) {
                    return;
                }

                flag = (float)i < (float)pWidth / 2.0F;
                l = Mth.floor((float)pWidth / 2.0F - (float)i / 2.0F);
                int j1 = i;
                boolean flag1 = i1 < i;
                if (flag) {
                    j1 = l + i;
                    flag1 = l <= i1 && i1 < l + i;
                }

                if (flag1) {
                    this.addItemToSlot(pIngredients.next(), k1, pMaxAmount, i1, k);
                } else if (j1 == i1) {
                    k1 += pWidth - i1;
                    break;
                }

                k1++;
            }
        }
    }

    void addItemToSlot(T p_346420_, int p_135416_, int p_135417_, int p_135418_, int p_135419_);
}
