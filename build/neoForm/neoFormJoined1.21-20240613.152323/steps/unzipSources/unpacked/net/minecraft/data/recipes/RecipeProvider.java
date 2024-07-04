package net.minecraft.data.recipes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.EnterBlockTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public abstract class RecipeProvider implements DataProvider {
    protected final PackOutput.PathProvider recipePathProvider;
    protected final PackOutput.PathProvider advancementPathProvider;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private static final Map<BlockFamily.Variant, BiFunction<ItemLike, ItemLike, RecipeBuilder>> SHAPE_BUILDERS = ImmutableMap.<BlockFamily.Variant, BiFunction<ItemLike, ItemLike, RecipeBuilder>>builder()
        .put(BlockFamily.Variant.BUTTON, (p_176733_, p_176734_) -> buttonBuilder(p_176733_, Ingredient.of(p_176734_)))
        .put(BlockFamily.Variant.CHISELED, (p_248037_, p_248038_) -> chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, p_248037_, Ingredient.of(p_248038_)))
        .put(BlockFamily.Variant.CUT, (p_248026_, p_248027_) -> cutBuilder(RecipeCategory.BUILDING_BLOCKS, p_248026_, Ingredient.of(p_248027_)))
        .put(BlockFamily.Variant.DOOR, (p_176714_, p_176715_) -> doorBuilder(p_176714_, Ingredient.of(p_176715_)))
        .put(BlockFamily.Variant.CUSTOM_FENCE, (p_176708_, p_176709_) -> fenceBuilder(p_176708_, Ingredient.of(p_176709_)))
        .put(BlockFamily.Variant.FENCE, (p_248031_, p_248032_) -> fenceBuilder(p_248031_, Ingredient.of(p_248032_)))
        .put(BlockFamily.Variant.CUSTOM_FENCE_GATE, (p_176698_, p_176699_) -> fenceGateBuilder(p_176698_, Ingredient.of(p_176699_)))
        .put(BlockFamily.Variant.FENCE_GATE, (p_248035_, p_248036_) -> fenceGateBuilder(p_248035_, Ingredient.of(p_248036_)))
        .put(BlockFamily.Variant.SIGN, (p_176688_, p_176689_) -> signBuilder(p_176688_, Ingredient.of(p_176689_)))
        .put(BlockFamily.Variant.SLAB, (p_248017_, p_248018_) -> slabBuilder(RecipeCategory.BUILDING_BLOCKS, p_248017_, Ingredient.of(p_248018_)))
        .put(BlockFamily.Variant.STAIRS, (p_176674_, p_176675_) -> stairBuilder(p_176674_, Ingredient.of(p_176675_)))
        .put(BlockFamily.Variant.PRESSURE_PLATE, (p_248039_, p_248040_) -> pressurePlateBuilder(RecipeCategory.REDSTONE, p_248039_, Ingredient.of(p_248040_)))
        .put(BlockFamily.Variant.POLISHED, (p_248019_, p_248020_) -> polishedBuilder(RecipeCategory.BUILDING_BLOCKS, p_248019_, Ingredient.of(p_248020_)))
        .put(BlockFamily.Variant.TRAPDOOR, (p_176638_, p_176639_) -> trapdoorBuilder(p_176638_, Ingredient.of(p_176639_)))
        .put(BlockFamily.Variant.WALL, (p_248024_, p_248025_) -> wallBuilder(RecipeCategory.DECORATIONS, p_248024_, Ingredient.of(p_248025_)))
        .build();

    public RecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        this.recipePathProvider = pOutput.createRegistryElementsPathProvider(Registries.RECIPE);
        this.advancementPathProvider = pOutput.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
        this.registries = pRegistries;
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput pOutput) {
        return this.registries.thenCompose(p_323133_ -> this.run(pOutput, p_323133_));
    }

    protected CompletableFuture<?> run(final CachedOutput pOutput, final HolderLookup.Provider pRegistries) {
        final Set<ResourceLocation> set = Sets.newHashSet();
        final List<CompletableFuture<?>> list = new ArrayList<>();
        this.buildRecipes(
            new RecipeOutput() {
                @Override
                public void accept(ResourceLocation p_312039_, Recipe<?> p_312254_, @Nullable AdvancementHolder p_311794_, net.neoforged.neoforge.common.conditions.ICondition... conditions) {
                    if (!set.add(p_312039_)) {
                        throw new IllegalStateException("Duplicate recipe " + p_312039_);
                    } else {
                        list.add(DataProvider.saveStable(pOutput, pRegistries, Recipe.CONDITIONAL_CODEC, Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(p_312254_, conditions)), RecipeProvider.this.recipePathProvider.json(p_312039_)));
                        if (p_311794_ != null) {
                            list.add(
                                DataProvider.saveStable(
                                    pOutput,
                                    pRegistries,
                                    Advancement.CONDITIONAL_CODEC,
                                    Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(p_311794_.value(), conditions)),
                                    RecipeProvider.this.advancementPathProvider.json(p_311794_.id())
                                )
                            );
                        }
                    }
                }

                @Override
                public Advancement.Builder advancement() {
                    return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                }
            }
        );
        return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
    }

    protected CompletableFuture<?> buildAdvancement(CachedOutput pOutput, HolderLookup.Provider pRegistries, AdvancementHolder pAdvancement) {
        return buildAdvancement(pOutput, pRegistries, pAdvancement, new net.neoforged.neoforge.common.conditions.ICondition[0]);
    }

    protected CompletableFuture<?> buildAdvancement(CachedOutput pOutput, HolderLookup.Provider pRegistries, AdvancementHolder pAdvancement, net.neoforged.neoforge.common.conditions.ICondition... conditions) {
        return DataProvider.saveStable(pOutput, pRegistries, Advancement.CONDITIONAL_CODEC, Optional.of(new net.neoforged.neoforge.common.conditions.WithConditions<>(pAdvancement.value(), conditions)), this.advancementPathProvider.json(pAdvancement.id()));
    }

    protected abstract void buildRecipes(RecipeOutput pRecipeOutput);

    protected void generateForEnabledBlockFamilies(RecipeOutput pEnabledFeatures, FeatureFlagSet p_251836_) {
        BlockFamilies.getAllFamilies().filter(BlockFamily::shouldGenerateRecipe).forEach(p_313461_ -> generateRecipes(pEnabledFeatures, p_313461_, p_251836_));
    }

    protected static void oneToOneConversionRecipe(RecipeOutput pRecipeOutput, ItemLike pResult, ItemLike pIngredient, @Nullable String pGroup) {
        oneToOneConversionRecipe(pRecipeOutput, pResult, pIngredient, pGroup, 1);
    }

    protected static void oneToOneConversionRecipe(RecipeOutput pRecipeOutput, ItemLike pResult, ItemLike pIngredient, @Nullable String pGroup, int pResultCount) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, pResult, pResultCount)
            .requires(pIngredient)
            .group(pGroup)
            .unlockedBy(getHasName(pIngredient), has(pIngredient))
            .save(pRecipeOutput, getConversionRecipeName(pResult, pIngredient));
    }

    protected static void oreSmelting(
        RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup
    ) {
        oreCooking(
            pRecipeOutput,
            RecipeSerializer.SMELTING_RECIPE,
            SmeltingRecipe::new,
            pIngredients,
            pCategory,
            pResult,
            pExperience,
            pCookingTime,
            pGroup,
            "_from_smelting"
        );
    }

    protected static void oreBlasting(
        RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup
    ) {
        oreCooking(
            pRecipeOutput,
            RecipeSerializer.BLASTING_RECIPE,
            BlastingRecipe::new,
            pIngredients,
            pCategory,
            pResult,
            pExperience,
            pCookingTime,
            pGroup,
            "_from_blasting"
        );
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(
        RecipeOutput pRecipeOutput,
        RecipeSerializer<T> pSerializer,
        AbstractCookingRecipe.Factory<T> pRecipeFactory,
        List<ItemLike> pIngredients,
        RecipeCategory pCategory,
        ItemLike pResult,
        float pExperience,
        int pCookingTime,
        String pGroup,
        String pSuffix
    ) {
        for (ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pSerializer, pRecipeFactory)
                .group(pGroup)
                .unlockedBy(getHasName(itemlike), has(itemlike))
                .save(pRecipeOutput, getItemName(pResult) + pSuffix + "_" + getItemName(itemlike));
        }
    }

    protected static void netheriteSmithing(RecipeOutput pRecipeOutput, Item pIngredientItem, RecipeCategory pCategory, Item pResultItem) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(pIngredientItem), Ingredient.of(Items.NETHERITE_INGOT), pCategory, pResultItem
            )
            .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
            .save(pRecipeOutput, getItemName(pResultItem) + "_smithing");
    }

    protected static void trimSmithing(RecipeOutput pRecipeOutput, Item pIngredientItem, ResourceLocation pLocation) {
        SmithingTrimRecipeBuilder.smithingTrim(
                Ingredient.of(pIngredientItem), Ingredient.of(ItemTags.TRIMMABLE_ARMOR), Ingredient.of(ItemTags.TRIM_MATERIALS), RecipeCategory.MISC
            )
            .unlocks("has_smithing_trim_template", has(pIngredientItem))
            .save(pRecipeOutput, pLocation);
    }

    protected static void twoByTwoPacker(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pPacked, ItemLike pUnpacked) {
        ShapedRecipeBuilder.shaped(pCategory, pPacked, 1)
            .define('#', pUnpacked)
            .pattern("##")
            .pattern("##")
            .unlockedBy(getHasName(pUnpacked), has(pUnpacked))
            .save(pRecipeOutput);
    }

    protected static void threeByThreePacker(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pPacked, ItemLike pUnpacked, String pCriterionName) {
        ShapelessRecipeBuilder.shapeless(pCategory, pPacked).requires(pUnpacked, 9).unlockedBy(pCriterionName, has(pUnpacked)).save(pRecipeOutput);
    }

    protected static void threeByThreePacker(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pPacked, ItemLike pUnpacked) {
        threeByThreePacker(pRecipeOutput, pCategory, pPacked, pUnpacked, getHasName(pUnpacked));
    }

    protected static void planksFromLog(RecipeOutput pRecipeOutput, ItemLike pPlanks, TagKey<Item> pLogs, int pResultCount) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, pPlanks, pResultCount)
            .requires(pLogs)
            .group("planks")
            .unlockedBy("has_log", has(pLogs))
            .save(pRecipeOutput);
    }

    protected static void planksFromLogs(RecipeOutput pRecipeOutput, ItemLike pPlanks, TagKey<Item> pLogs, int pResult) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, pPlanks, pResult)
            .requires(pLogs)
            .group("planks")
            .unlockedBy("has_logs", has(pLogs))
            .save(pRecipeOutput);
    }

    protected static void woodFromLogs(RecipeOutput pRecipeOutput, ItemLike pWood, ItemLike pLog) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, pWood, 3)
            .define('#', pLog)
            .pattern("##")
            .pattern("##")
            .group("bark")
            .unlockedBy("has_log", has(pLog))
            .save(pRecipeOutput);
    }

    protected static void woodenBoat(RecipeOutput pRecipeOutput, ItemLike pBoat, ItemLike pMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, pBoat)
            .define('#', pMaterial)
            .pattern("# #")
            .pattern("###")
            .group("boat")
            .unlockedBy("in_water", insideOf(Blocks.WATER))
            .save(pRecipeOutput);
    }

    protected static void chestBoat(RecipeOutput pRecipeOutput, ItemLike pBoat, ItemLike pMaterial) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, pBoat)
            .requires(Blocks.CHEST)
            .requires(pMaterial)
            .group("chest_boat")
            .unlockedBy("has_boat", has(ItemTags.BOATS))
            .save(pRecipeOutput);
    }

    protected static RecipeBuilder buttonBuilder(ItemLike pButton, Ingredient pMaterial) {
        return ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, pButton).requires(pMaterial);
    }

    protected static RecipeBuilder doorBuilder(ItemLike pDoor, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, pDoor, 3).define('#', pMaterial).pattern("##").pattern("##").pattern("##");
    }

    protected static RecipeBuilder fenceBuilder(ItemLike pFence, Ingredient pMaterial) {
        int i = pFence == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
        Item item = pFence == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
        return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pFence, i).define('W', pMaterial).define('#', item).pattern("W#W").pattern("W#W");
    }

    protected static RecipeBuilder fenceGateBuilder(ItemLike pFenceGate, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, pFenceGate).define('#', Items.STICK).define('W', pMaterial).pattern("#W#").pattern("#W#");
    }

    protected static void pressurePlate(RecipeOutput pRecipeOutput, ItemLike pPressurePlate, ItemLike pMaterial) {
        pressurePlateBuilder(RecipeCategory.REDSTONE, pPressurePlate, Ingredient.of(pMaterial)).unlockedBy(getHasName(pMaterial), has(pMaterial)).save(pRecipeOutput);
    }

    protected static RecipeBuilder pressurePlateBuilder(RecipeCategory pCategory, ItemLike pPressurePlate, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(pCategory, pPressurePlate).define('#', pMaterial).pattern("##");
    }

    protected static void slab(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pSlab, ItemLike pMaterial) {
        slabBuilder(pCategory, pSlab, Ingredient.of(pMaterial)).unlockedBy(getHasName(pMaterial), has(pMaterial)).save(pRecipeOutput);
    }

    protected static RecipeBuilder slabBuilder(RecipeCategory pCategory, ItemLike pSlab, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(pCategory, pSlab, 6).define('#', pMaterial).pattern("###");
    }

    protected static RecipeBuilder stairBuilder(ItemLike pStairs, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, pStairs, 4).define('#', pMaterial).pattern("#  ").pattern("## ").pattern("###");
    }

    protected static RecipeBuilder trapdoorBuilder(ItemLike pTrapdoor, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, pTrapdoor, 2).define('#', pMaterial).pattern("###").pattern("###");
    }

    protected static RecipeBuilder signBuilder(ItemLike pSign, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pSign, 3)
            .group("sign")
            .define('#', pMaterial)
            .define('X', Items.STICK)
            .pattern("###")
            .pattern("###")
            .pattern(" X ");
    }

    protected static void hangingSign(RecipeOutput pRecipeOutput, ItemLike pSign, ItemLike pMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pSign, 6)
            .group("hanging_sign")
            .define('#', pMaterial)
            .define('X', Items.CHAIN)
            .pattern("X X")
            .pattern("###")
            .pattern("###")
            .unlockedBy("has_stripped_logs", has(pMaterial))
            .save(pRecipeOutput);
    }

    protected static void colorBlockWithDye(RecipeOutput pRecipeOutput, List<Item> pDyes, List<Item> pDyeableItems, String pGroup) {
        for (int i = 0; i < pDyes.size(); i++) {
            Item item = pDyes.get(i);
            Item item1 = pDyeableItems.get(i);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, item1)
                .requires(item)
                .requires(Ingredient.of(pDyeableItems.stream().filter(p_288265_ -> !p_288265_.equals(item1)).map(ItemStack::new)))
                .group(pGroup)
                .unlockedBy("has_needed_dye", has(item))
                .save(pRecipeOutput, "dye_" + getItemName(item1));
        }
    }

    protected static void carpet(RecipeOutput pRecipeOutput, ItemLike pCarpet, ItemLike pMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pCarpet, 3)
            .define('#', pMaterial)
            .pattern("##")
            .group("carpet")
            .unlockedBy(getHasName(pMaterial), has(pMaterial))
            .save(pRecipeOutput);
    }

    protected static void bedFromPlanksAndWool(RecipeOutput pRecipeOutput, ItemLike pBed, ItemLike pWool) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pBed)
            .define('#', pWool)
            .define('X', ItemTags.PLANKS)
            .pattern("###")
            .pattern("XXX")
            .group("bed")
            .unlockedBy(getHasName(pWool), has(pWool))
            .save(pRecipeOutput);
    }

    protected static void banner(RecipeOutput pRecipeOutput, ItemLike pBanner, ItemLike pMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pBanner)
            .define('#', pMaterial)
            .define('|', Items.STICK)
            .pattern("###")
            .pattern("###")
            .pattern(" | ")
            .group("banner")
            .unlockedBy(getHasName(pMaterial), has(pMaterial))
            .save(pRecipeOutput);
    }

    protected static void stainedGlassFromGlassAndDye(RecipeOutput pRecipeOutput, ItemLike pStainedGlass, ItemLike pDye) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, pStainedGlass, 8)
            .define('#', Blocks.GLASS)
            .define('X', pDye)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .group("stained_glass")
            .unlockedBy("has_glass", has(Blocks.GLASS))
            .save(pRecipeOutput);
    }

    protected static void stainedGlassPaneFromStainedGlass(RecipeOutput pRecipeOutput, ItemLike pStainedGlassPane, ItemLike pStainedGlass) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pStainedGlassPane, 16)
            .define('#', pStainedGlass)
            .pattern("###")
            .pattern("###")
            .group("stained_glass_pane")
            .unlockedBy("has_glass", has(pStainedGlass))
            .save(pRecipeOutput);
    }

    protected static void stainedGlassPaneFromGlassPaneAndDye(RecipeOutput pRecipeOutput, ItemLike pStainedGlassPane, ItemLike pDye) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, pStainedGlassPane, 8)
            .define('#', Blocks.GLASS_PANE)
            .define('$', pDye)
            .pattern("###")
            .pattern("#$#")
            .pattern("###")
            .group("stained_glass_pane")
            .unlockedBy("has_glass_pane", has(Blocks.GLASS_PANE))
            .unlockedBy(getHasName(pDye), has(pDye))
            .save(pRecipeOutput, getConversionRecipeName(pStainedGlassPane, Blocks.GLASS_PANE));
    }

    protected static void coloredTerracottaFromTerracottaAndDye(RecipeOutput pRecipeOutput, ItemLike pTerracotta, ItemLike pDye) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, pTerracotta, 8)
            .define('#', Blocks.TERRACOTTA)
            .define('X', pDye)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .group("stained_terracotta")
            .unlockedBy("has_terracotta", has(Blocks.TERRACOTTA))
            .save(pRecipeOutput);
    }

    protected static void concretePowder(RecipeOutput pRecipeOutput, ItemLike pConcretePowder, ItemLike pDye) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, pConcretePowder, 8)
            .requires(pDye)
            .requires(Blocks.SAND, 4)
            .requires(Blocks.GRAVEL, 4)
            .group("concrete_powder")
            .unlockedBy("has_sand", has(Blocks.SAND))
            .unlockedBy("has_gravel", has(Blocks.GRAVEL))
            .save(pRecipeOutput);
    }

    protected static void candle(RecipeOutput pRecipeOutput, ItemLike pCandle, ItemLike pDye) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, pCandle)
            .requires(Blocks.CANDLE)
            .requires(pDye)
            .group("dyed_candle")
            .unlockedBy(getHasName(pDye), has(pDye))
            .save(pRecipeOutput);
    }

    protected static void wall(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pWall, ItemLike pMaterial) {
        wallBuilder(pCategory, pWall, Ingredient.of(pMaterial)).unlockedBy(getHasName(pMaterial), has(pMaterial)).save(pRecipeOutput);
    }

    protected static RecipeBuilder wallBuilder(RecipeCategory pCategory, ItemLike pWall, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(pCategory, pWall, 6).define('#', pMaterial).pattern("###").pattern("###");
    }

    protected static void polished(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pResult, ItemLike pMaterial) {
        polishedBuilder(pCategory, pResult, Ingredient.of(pMaterial)).unlockedBy(getHasName(pMaterial), has(pMaterial)).save(pRecipeOutput);
    }

    protected static RecipeBuilder polishedBuilder(RecipeCategory pCategory, ItemLike pResult, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(pCategory, pResult, 4).define('S', pMaterial).pattern("SS").pattern("SS");
    }

    protected static void cut(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pCutResult, ItemLike pMaterial) {
        cutBuilder(pCategory, pCutResult, Ingredient.of(pMaterial)).unlockedBy(getHasName(pMaterial), has(pMaterial)).save(pRecipeOutput);
    }

    protected static ShapedRecipeBuilder cutBuilder(RecipeCategory pCategory, ItemLike pCutResult, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(pCategory, pCutResult, 4).define('#', pMaterial).pattern("##").pattern("##");
    }

    protected static void chiseled(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pChiseledResult, ItemLike pMaterial) {
        chiseledBuilder(pCategory, pChiseledResult, Ingredient.of(pMaterial)).unlockedBy(getHasName(pMaterial), has(pMaterial)).save(pRecipeOutput);
    }

    protected static void mosaicBuilder(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pResult, ItemLike pMaterial) {
        ShapedRecipeBuilder.shaped(pCategory, pResult)
            .define('#', pMaterial)
            .pattern("#")
            .pattern("#")
            .unlockedBy(getHasName(pMaterial), has(pMaterial))
            .save(pRecipeOutput);
    }

    protected static ShapedRecipeBuilder chiseledBuilder(RecipeCategory pCategory, ItemLike pChiseledResult, Ingredient pMaterial) {
        return ShapedRecipeBuilder.shaped(pCategory, pChiseledResult).define('#', pMaterial).pattern("#").pattern("#");
    }

    protected static void stonecutterResultFromBase(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pResult, ItemLike pMaterial) {
        stonecutterResultFromBase(pRecipeOutput, pCategory, pResult, pMaterial, 1);
    }

    protected static void stonecutterResultFromBase(RecipeOutput pRecipeOutput, RecipeCategory pCategory, ItemLike pResult, ItemLike pMaterial, int pResultCount) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(pMaterial), pCategory, pResult, pResultCount)
            .unlockedBy(getHasName(pMaterial), has(pMaterial))
            .save(pRecipeOutput, getConversionRecipeName(pResult, pMaterial) + "_stonecutting");
    }

    protected static void smeltingResultFromBase(RecipeOutput pRecipeOutput, ItemLike pResult, ItemLike pIngredient) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(pIngredient), RecipeCategory.BUILDING_BLOCKS, pResult, 0.1F, 200)
            .unlockedBy(getHasName(pIngredient), has(pIngredient))
            .save(pRecipeOutput);
    }

    protected static void nineBlockStorageRecipes(
        RecipeOutput pRecipeOutput, RecipeCategory pUnpackedCategory, ItemLike pUnpacked, RecipeCategory pPackedCategory, ItemLike pPacked
    ) {
        nineBlockStorageRecipes(
            pRecipeOutput, pUnpackedCategory, pUnpacked, pPackedCategory, pPacked, getSimpleRecipeName(pPacked), null, getSimpleRecipeName(pUnpacked), null
        );
    }

    protected static void nineBlockStorageRecipesWithCustomPacking(
        RecipeOutput pRecipeOutput, RecipeCategory pUnpackedCategory, ItemLike pUnpacked, RecipeCategory pPackedCategory, ItemLike pPacked, String pPackedName, String pPackedGroup
    ) {
        nineBlockStorageRecipes(pRecipeOutput, pUnpackedCategory, pUnpacked, pPackedCategory, pPacked, pPackedName, pPackedGroup, getSimpleRecipeName(pUnpacked), null);
    }

    protected static void nineBlockStorageRecipesRecipesWithCustomUnpacking(
        RecipeOutput pRecipeOutput, RecipeCategory pUnpackedCategory, ItemLike pUnpacked, RecipeCategory pPackedCategory, ItemLike pPacked, String pUnpackedName, String pUnpackedGroup
    ) {
        nineBlockStorageRecipes(pRecipeOutput, pUnpackedCategory, pUnpacked, pPackedCategory, pPacked, getSimpleRecipeName(pPacked), null, pUnpackedName, pUnpackedGroup);
    }

    protected static void nineBlockStorageRecipes(
        RecipeOutput pRecipeOutput,
        RecipeCategory pUnpackedCategory,
        ItemLike pUnpacked,
        RecipeCategory pPackedCategory,
        ItemLike pPacked,
        String pPackedName,
        @Nullable String pPackedGroup,
        String pUnpackedName,
        @Nullable String pUnpackedGroup
    ) {
        ShapelessRecipeBuilder.shapeless(pUnpackedCategory, pUnpacked, 9)
            .requires(pPacked)
            .group(pUnpackedGroup)
            .unlockedBy(getHasName(pPacked), has(pPacked))
            .save(pRecipeOutput, ResourceLocation.parse(pUnpackedName));
        ShapedRecipeBuilder.shaped(pPackedCategory, pPacked)
            .define('#', pUnpacked)
            .pattern("###")
            .pattern("###")
            .pattern("###")
            .group(pPackedGroup)
            .unlockedBy(getHasName(pUnpacked), has(pUnpacked))
            .save(pRecipeOutput, ResourceLocation.parse(pPackedName));
    }

    protected static void copySmithingTemplate(RecipeOutput pRecipeOutput, ItemLike pTemplate, TagKey<Item> pBaseMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, pTemplate, 2)
            .define('#', Items.DIAMOND)
            .define('C', pBaseMaterial)
            .define('S', pTemplate)
            .pattern("#S#")
            .pattern("#C#")
            .pattern("###")
            .unlockedBy(getHasName(pTemplate), has(pTemplate))
            .save(pRecipeOutput);
    }

    protected static void copySmithingTemplate(RecipeOutput pRecipeOutput, ItemLike pTemplate, ItemLike pBaseItem) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, pTemplate, 2)
            .define('#', Items.DIAMOND)
            .define('C', pBaseItem)
            .define('S', pTemplate)
            .pattern("#S#")
            .pattern("#C#")
            .pattern("###")
            .unlockedBy(getHasName(pTemplate), has(pTemplate))
            .save(pRecipeOutput);
    }

    protected static void copySmithingTemplate(RecipeOutput p_350466_, ItemLike p_350799_, Ingredient p_350344_) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, p_350799_, 2)
            .define('#', Items.DIAMOND)
            .define('C', p_350344_)
            .define('S', p_350799_)
            .pattern("#S#")
            .pattern("#C#")
            .pattern("###")
            .unlockedBy(getHasName(p_350799_), has(p_350799_))
            .save(p_350466_);
    }

    protected static <T extends AbstractCookingRecipe> void cookRecipes(
        RecipeOutput pRecipeOutput, String pCookingMethod, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> pRecipeFactory, int pCookingTime
    ) {
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.BEEF, Items.COOKED_BEEF, 0.35F);
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.CHICKEN, Items.COOKED_CHICKEN, 0.35F);
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.COD, Items.COOKED_COD, 0.35F);
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.KELP, Items.DRIED_KELP, 0.1F);
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.SALMON, Items.COOKED_SALMON, 0.35F);
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.MUTTON, Items.COOKED_MUTTON, 0.35F);
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.PORKCHOP, Items.COOKED_PORKCHOP, 0.35F);
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.POTATO, Items.BAKED_POTATO, 0.35F);
        simpleCookingRecipe(pRecipeOutput, pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.RABBIT, Items.COOKED_RABBIT, 0.35F);
    }

    protected static <T extends AbstractCookingRecipe> void simpleCookingRecipe(
        RecipeOutput pRecipeOutput,
        String pCookingMethod,
        RecipeSerializer<T> pCookingSerializer,
        AbstractCookingRecipe.Factory<T> pRecipeFactory,
        int pCookingTime,
        ItemLike pMaterial,
        ItemLike pResult,
        float pExperience
    ) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(pMaterial), RecipeCategory.FOOD, pResult, pExperience, pCookingTime, pCookingSerializer, pRecipeFactory)
            .unlockedBy(getHasName(pMaterial), has(pMaterial))
            .save(pRecipeOutput, getItemName(pResult) + "_from_" + pCookingMethod);
    }

    protected static void waxRecipes(RecipeOutput pRecipeOutput, FeatureFlagSet pRequiredFeatures) {
        HoneycombItem.WAXABLES
            .get()
            .forEach(
                (p_337490_, p_337491_) -> {
                    if (p_337491_.requiredFeatures().isSubsetOf(pRequiredFeatures)) {
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, p_337491_)
                            .requires(p_337490_)
                            .requires(Items.HONEYCOMB)
                            .group(getItemName(p_337491_))
                            .unlockedBy(getHasName(p_337490_), has(p_337490_))
                            .save(pRecipeOutput, getConversionRecipeName(p_337491_, Items.HONEYCOMB));
                    }
                }
            );
    }

    protected static void grate(RecipeOutput pRecipeOutput, Block pGrateBlock, Block pMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, pGrateBlock, 4)
            .define('M', pMaterial)
            .pattern(" M ")
            .pattern("M M")
            .pattern(" M ")
            .unlockedBy(getHasName(pMaterial), has(pMaterial))
            .save(pRecipeOutput);
    }

    protected static void copperBulb(RecipeOutput pRecipeOutput, Block pBulbBlock, Block pMaterial) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, pBulbBlock, 4)
            .define('C', pMaterial)
            .define('R', Items.REDSTONE)
            .define('B', Items.BLAZE_ROD)
            .pattern(" C ")
            .pattern("CBC")
            .pattern(" R ")
            .unlockedBy(getHasName(pMaterial), has(pMaterial))
            .save(pRecipeOutput);
    }

    protected static void generateRecipes(RecipeOutput pRecipeOutput, BlockFamily pBlockFamily, FeatureFlagSet pRequiredFeatures) {
        pBlockFamily.getVariants()
            .forEach(
                (p_313457_, p_313458_) -> {
                    if (p_313458_.requiredFeatures().isSubsetOf(pRequiredFeatures)) {
                        BiFunction<ItemLike, ItemLike, RecipeBuilder> bifunction = SHAPE_BUILDERS.get(p_313457_);
                        ItemLike itemlike = getBaseBlock(pBlockFamily, p_313457_);
                        if (bifunction != null) {
                            RecipeBuilder recipebuilder = bifunction.apply(p_313458_, itemlike);
                            pBlockFamily.getRecipeGroupPrefix()
                                .ifPresent(
                                    p_293701_ -> recipebuilder.group(p_293701_ + (p_313457_ == BlockFamily.Variant.CUT ? "" : "_" + p_313457_.getRecipeGroup()))
                                );
                            recipebuilder.unlockedBy(pBlockFamily.getRecipeUnlockedBy().orElseGet(() -> getHasName(itemlike)), has(itemlike));
                            recipebuilder.save(pRecipeOutput);
                        }

                        if (p_313457_ == BlockFamily.Variant.CRACKED) {
                            smeltingResultFromBase(pRecipeOutput, p_313458_, itemlike);
                        }
                    }
                }
            );
    }

    protected static Block getBaseBlock(BlockFamily pFamily, BlockFamily.Variant pVariant) {
        if (pVariant == BlockFamily.Variant.CHISELED) {
            if (!pFamily.getVariants().containsKey(BlockFamily.Variant.SLAB)) {
                throw new IllegalStateException("Slab is not defined for the family.");
            } else {
                return pFamily.get(BlockFamily.Variant.SLAB);
            }
        } else {
            return pFamily.getBaseBlock();
        }
    }

    protected static Criterion<EnterBlockTrigger.TriggerInstance> insideOf(Block pBlock) {
        return CriteriaTriggers.ENTER_BLOCK
            .createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(pBlock.builtInRegistryHolder()), Optional.empty()));
    }

    protected static Criterion<InventoryChangeTrigger.TriggerInstance> has(MinMaxBounds.Ints pCount, ItemLike pItem) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(pItem).withCount(pCount));
    }

    protected static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike pItemLike) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(pItemLike));
    }

    protected static Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> pTag) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(pTag));
    }

    protected static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate.Builder... pItems) {
        return inventoryTrigger(Arrays.stream(pItems).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
    }

    protected static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate... pPredicates) {
        return CriteriaTriggers.INVENTORY_CHANGED
            .createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(pPredicates)));
    }

    protected static String getHasName(ItemLike pItemLike) {
        return "has_" + getItemName(pItemLike);
    }

    protected static String getItemName(ItemLike pItemLike) {
        return BuiltInRegistries.ITEM.getKey(pItemLike.asItem()).getPath();
    }

    protected static String getSimpleRecipeName(ItemLike pItemLike) {
        return getItemName(pItemLike);
    }

    protected static String getConversionRecipeName(ItemLike pResult, ItemLike pIngredient) {
        return getItemName(pResult) + "_from_" + getItemName(pIngredient);
    }

    protected static String getSmeltingRecipeName(ItemLike pItemLike) {
        return getItemName(pItemLike) + "_from_smelting";
    }

    protected static String getBlastingRecipeName(ItemLike pItemLike) {
        return getItemName(pItemLike) + "_from_blasting";
    }

    @Override
    public final String getName() {
        return "Recipes";
    }
}
