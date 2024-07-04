package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

public class SmithingTransformRecipeBuilder {
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final RecipeCategory category;
    private final Item result;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public SmithingTransformRecipeBuilder(Ingredient p_266973_, Ingredient p_267047_, Ingredient p_267009_, RecipeCategory p_266694_, Item p_267183_) {
        this.category = p_266694_;
        this.template = p_266973_;
        this.base = p_267047_;
        this.addition = p_267009_;
        this.result = p_267183_;
    }

    public static SmithingTransformRecipeBuilder smithing(
        Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, RecipeCategory pCategory, Item pResult
    ) {
        return new SmithingTransformRecipeBuilder(pTemplate, pBase, pAddition, pCategory, pResult);
    }

    public SmithingTransformRecipeBuilder unlocks(String pKey, Criterion<?> pCriterion) {
        this.criteria.put(pKey, pCriterion);
        return this;
    }

    public void save(RecipeOutput pRecipeOutput, String pRecipeId) {
        this.save(pRecipeOutput, ResourceLocation.parse(pRecipeId));
    }

    public void save(RecipeOutput pRecipeOutput, ResourceLocation pRecipeId) {
        this.ensureValid(pRecipeId);
        Advancement.Builder advancement$builder = pRecipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId))
            .rewards(AdvancementRewards.Builder.recipe(pRecipeId))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        SmithingTransformRecipe smithingtransformrecipe = new SmithingTransformRecipe(this.template, this.base, this.addition, new ItemStack(this.result));
        pRecipeOutput.accept(pRecipeId, smithingtransformrecipe, advancement$builder.build(pRecipeId.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }

    private void ensureValid(ResourceLocation pLocation) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pLocation);
        }
    }
}
