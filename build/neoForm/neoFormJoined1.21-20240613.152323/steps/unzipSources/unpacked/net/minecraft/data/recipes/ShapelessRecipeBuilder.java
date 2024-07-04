package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

public class ShapelessRecipeBuilder implements RecipeBuilder {
    private final RecipeCategory category;
    private final Item result;
    private final int count;
    private final ItemStack resultStack; // Neo: add stack result support
    private final NonNullList<Ingredient> ingredients = NonNullList.create();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;

    public ShapelessRecipeBuilder(RecipeCategory pCategory, ItemLike pResult, int pCount) {
        this(pCategory, new ItemStack(pResult, pCount));
    }

    public ShapelessRecipeBuilder(RecipeCategory p_250837_, ItemStack result) {
        this.category = p_250837_;
        this.result = result.getItem();
        this.count = result.getCount();
        this.resultStack = result;
    }

    /**
     * Creates a new builder for a shapeless recipe.
     */
    public static ShapelessRecipeBuilder shapeless(RecipeCategory pCategory, ItemLike pResult) {
        return new ShapelessRecipeBuilder(pCategory, pResult, 1);
    }

    /**
     * Creates a new builder for a shapeless recipe.
     */
    public static ShapelessRecipeBuilder shapeless(RecipeCategory pCategory, ItemLike pResult, int pCount) {
        return new ShapelessRecipeBuilder(pCategory, pResult, pCount);
    }

    public static ShapelessRecipeBuilder shapeless(RecipeCategory p_252339_, ItemStack result) {
        return new ShapelessRecipeBuilder(p_252339_, result);
    }

    /**
     * Adds an ingredient that can be any item in the given tag.
     */
    public ShapelessRecipeBuilder requires(TagKey<Item> pTag) {
        return this.requires(Ingredient.of(pTag));
    }

    /**
     * Adds an ingredient of the given item.
     */
    public ShapelessRecipeBuilder requires(ItemLike pItem) {
        return this.requires(pItem, 1);
    }

    /**
     * Adds the given ingredient multiple times.
     */
    public ShapelessRecipeBuilder requires(ItemLike pItem, int pQuantity) {
        for (int i = 0; i < pQuantity; i++) {
            this.requires(Ingredient.of(pItem));
        }

        return this;
    }

    /**
     * Adds an ingredient.
     */
    public ShapelessRecipeBuilder requires(Ingredient pIngredient) {
        return this.requires(pIngredient, 1);
    }

    /**
     * Adds an ingredient multiple times.
     */
    public ShapelessRecipeBuilder requires(Ingredient pIngredient, int pQuantity) {
        for (int i = 0; i < pQuantity; i++) {
            this.ingredients.add(pIngredient);
        }

        return this;
    }

    public ShapelessRecipeBuilder unlockedBy(String pName, Criterion<?> pCriterion) {
        this.criteria.put(pName, pCriterion);
        return this;
    }

    public ShapelessRecipeBuilder group(@Nullable String pGroupName) {
        this.group = pGroupName;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(RecipeOutput pRecipeOutput, ResourceLocation pId) {
        this.ensureValid(pId);
        Advancement.Builder advancement$builder = pRecipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pId))
            .rewards(AdvancementRewards.Builder.recipe(pId))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        ShapelessRecipe shapelessrecipe = new ShapelessRecipe(
            Objects.requireNonNullElse(this.group, ""),
            RecipeBuilder.determineBookCategory(this.category),
            this.resultStack,
            this.ingredients
        );
        pRecipeOutput.accept(pId, shapelessrecipe, advancement$builder.build(pId.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }

    /**
     * Makes sure that this recipe is valid and obtainable.
     */
    private void ensureValid(ResourceLocation pId) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pId);
        }
    }
}
