package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeCollection {
    private final RegistryAccess registryAccess;
    private final List<RecipeHolder<?>> recipes;
    private final boolean singleResultItem;
    private final Set<RecipeHolder<?>> craftable = Sets.newHashSet();
    private final Set<RecipeHolder<?>> fitsDimensions = Sets.newHashSet();
    private final Set<RecipeHolder<?>> known = Sets.newHashSet();

    public RecipeCollection(RegistryAccess pRegistryAccess, List<RecipeHolder<?>> pRecipes) {
        this.registryAccess = pRegistryAccess;
        this.recipes = ImmutableList.copyOf(pRecipes);
        if (pRecipes.size() <= 1) {
            this.singleResultItem = true;
        } else {
            this.singleResultItem = allRecipesHaveSameResult(pRegistryAccess, pRecipes);
        }
    }

    private static boolean allRecipesHaveSameResult(RegistryAccess pRegistryAccess, List<RecipeHolder<?>> pRecipes) {
        int i = pRecipes.size();
        ItemStack itemstack = pRecipes.get(0).value().getResultItem(pRegistryAccess);

        for (int j = 1; j < i; j++) {
            ItemStack itemstack1 = pRecipes.get(j).value().getResultItem(pRegistryAccess);
            if (!ItemStack.isSameItemSameComponents(itemstack, itemstack1)) {
                return false;
            }
        }

        return true;
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public boolean hasKnownRecipes() {
        return !this.known.isEmpty();
    }

    public void updateKnownRecipes(RecipeBook pBook) {
        for (RecipeHolder<?> recipeholder : this.recipes) {
            if (pBook.contains(recipeholder)) {
                this.known.add(recipeholder);
            }
        }
    }

    public void canCraft(StackedContents pHandler, int pWidth, int pHeight, RecipeBook pBook) {
        for (RecipeHolder<?> recipeholder : this.recipes) {
            boolean flag = recipeholder.value().canCraftInDimensions(pWidth, pHeight) && pBook.contains(recipeholder);
            if (flag) {
                this.fitsDimensions.add(recipeholder);
            } else {
                this.fitsDimensions.remove(recipeholder);
            }

            if (flag && pHandler.canCraft(recipeholder.value(), null)) {
                this.craftable.add(recipeholder);
            } else {
                this.craftable.remove(recipeholder);
            }
        }
    }

    public boolean isCraftable(RecipeHolder<?> pRecipe) {
        return this.craftable.contains(pRecipe);
    }

    public boolean hasCraftable() {
        return !this.craftable.isEmpty();
    }

    public boolean hasFitting() {
        return !this.fitsDimensions.isEmpty();
    }

    public List<RecipeHolder<?>> getRecipes() {
        return this.recipes;
    }

    public List<RecipeHolder<?>> getRecipes(boolean pOnlyCraftable) {
        List<RecipeHolder<?>> list = Lists.newArrayList();
        Set<RecipeHolder<?>> set = pOnlyCraftable ? this.craftable : this.fitsDimensions;

        for (RecipeHolder<?> recipeholder : this.recipes) {
            if (set.contains(recipeholder)) {
                list.add(recipeholder);
            }
        }

        return list;
    }

    /**
     * @param pCraftable If true, this method will only return craftable recipes. If
     *                   false, this method will only return uncraftable recipes.
     */
    public List<RecipeHolder<?>> getDisplayRecipes(boolean pCraftable) {
        List<RecipeHolder<?>> list = Lists.newArrayList();

        for (RecipeHolder<?> recipeholder : this.recipes) {
            if (this.fitsDimensions.contains(recipeholder) && this.craftable.contains(recipeholder) == pCraftable) {
                list.add(recipeholder);
            }
        }

        return list;
    }

    public boolean hasSingleResultItem() {
        return this.singleResultItem;
    }
}
