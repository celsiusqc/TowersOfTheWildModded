package net.minecraft.world.item.alchemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.Ingredient;

public class PotionBrewing {
    public static final int BREWING_TIME_SECONDS = 20;
    public static final PotionBrewing EMPTY = new PotionBrewing(List.of(), List.of(), List.of());
    private final List<Ingredient> containers;
    private final List<PotionBrewing.Mix<Potion>> potionMixes;
    private final List<PotionBrewing.Mix<Item>> containerMixes;
    private final net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry registry;

    PotionBrewing(List<Ingredient> pContainers, List<PotionBrewing.Mix<Potion>> pPotionMixes, List<PotionBrewing.Mix<Item>> pContainerMixes) {
        this(pContainers, pPotionMixes, pContainerMixes, List.of());
    }

    PotionBrewing(List<Ingredient> pContainers, List<PotionBrewing.Mix<Potion>> pPotionMixes, List<PotionBrewing.Mix<Item>> pContainerMixes, List<net.neoforged.neoforge.common.brewing.IBrewingRecipe> recipes) {
        this.containers = pContainers;
        this.potionMixes = pPotionMixes;
        this.containerMixes = pContainerMixes;
        this.registry = new net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry(recipes);
    }

    public boolean isIngredient(ItemStack pStack) {
        return this.registry.isValidIngredient(pStack) || this.isContainerIngredient(pStack) || this.isPotionIngredient(pStack);
    }

    /**
     * Checks if an item stack is a valid input for brewing,
     * for use in the lower 3 slots where water bottles would normally go.
     */
    public boolean isInput(ItemStack stack) {
        return this.registry.isValidInput(stack) || isContainer(stack);
    }

    /**
     * Retrieves recipes that use the more general interface.
     * This does NOT include the container and potion mixes.
     */
    public List<net.neoforged.neoforge.common.brewing.IBrewingRecipe> getRecipes() {
        return registry.recipes();
    }

    private boolean isContainer(ItemStack pStack) {
        for (Ingredient ingredient : this.containers) {
            if (ingredient.test(pStack)) {
                return true;
            }
        }

        return false;
    }

    public boolean isContainerIngredient(ItemStack pStack) {
        for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
            if (mix.ingredient.test(pStack)) {
                return true;
            }
        }

        return false;
    }

    public boolean isPotionIngredient(ItemStack pStack) {
        for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
            if (mix.ingredient.test(pStack)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBrewablePotion(Holder<Potion> pPotion) {
        for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
            if (mix.to.is(pPotion)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasMix(ItemStack pReagent, ItemStack pPotionItem) {
        if (registry.hasOutput(pReagent, pPotionItem)) return true;
        return !this.isContainer(pReagent) ? false : this.hasContainerMix(pReagent, pPotionItem) || this.hasPotionMix(pReagent, pPotionItem);
    }

    public boolean hasContainerMix(ItemStack pReagent, ItemStack pPotionItem) {
        for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
            if (pReagent.is(mix.from) && mix.ingredient.test(pPotionItem)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPotionMix(ItemStack pReagent, ItemStack pPotionItem) {
        Optional<Holder<Potion>> optional = pReagent.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
        if (optional.isEmpty()) {
            return false;
        } else {
            for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
                if (mix.from.is(optional.get()) && mix.ingredient.test(pPotionItem)) {
                    return true;
                }
            }

            return false;
        }
    }

    public ItemStack mix(ItemStack pPotion, ItemStack pPotionItem) {
        if (pPotionItem.isEmpty()) {
            return pPotionItem;
        } else {
            var customMix = registry.getOutput(pPotionItem, pPotion); // Parameters are swapped compared to what vanilla passes!
            if (!customMix.isEmpty()) return customMix;
            Optional<Holder<Potion>> optional = pPotionItem.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
            if (optional.isEmpty()) {
                return pPotionItem;
            } else {
                for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
                    if (pPotionItem.is(mix.from) && mix.ingredient.test(pPotion)) {
                        return PotionContents.createItemStack(mix.to.value(), optional.get());
                    }
                }

                for (PotionBrewing.Mix<Potion> mix1 : this.potionMixes) {
                    if (mix1.from.is(optional.get()) && mix1.ingredient.test(pPotion)) {
                        return PotionContents.createItemStack(pPotionItem.getItem(), mix1.to);
                    }
                }

                return pPotionItem;
            }
        }
    }

    public static PotionBrewing bootstrap(FeatureFlagSet pEnabledFeatures) {
        PotionBrewing.Builder potionbrewing$builder = new PotionBrewing.Builder(pEnabledFeatures);
        addVanillaMixes(potionbrewing$builder);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent(potionbrewing$builder));
        return potionbrewing$builder.build();
    }

    public static void addVanillaMixes(PotionBrewing.Builder pBuilder) {
        pBuilder.addContainer(Items.POTION);
        pBuilder.addContainer(Items.SPLASH_POTION);
        pBuilder.addContainer(Items.LINGERING_POTION);
        pBuilder.addContainerRecipe(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
        pBuilder.addContainerRecipe(Items.SPLASH_POTION, Items.DRAGON_BREATH, Items.LINGERING_POTION);
        pBuilder.addMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
        pBuilder.addMix(Potions.WATER, Items.REDSTONE, Potions.MUNDANE);
        pBuilder.addMix(Potions.WATER, Items.NETHER_WART, Potions.AWKWARD);
        pBuilder.addStartMix(Items.BREEZE_ROD, Potions.WIND_CHARGED);
        pBuilder.addStartMix(Items.SLIME_BLOCK, Potions.OOZING);
        pBuilder.addStartMix(Items.STONE, Potions.INFESTED);
        pBuilder.addStartMix(Items.COBWEB, Potions.WEAVING);
        pBuilder.addMix(Potions.AWKWARD, Items.GOLDEN_CARROT, Potions.NIGHT_VISION);
        pBuilder.addMix(Potions.NIGHT_VISION, Items.REDSTONE, Potions.LONG_NIGHT_VISION);
        pBuilder.addMix(Potions.NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);
        pBuilder.addMix(Potions.LONG_NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.LONG_INVISIBILITY);
        pBuilder.addMix(Potions.INVISIBILITY, Items.REDSTONE, Potions.LONG_INVISIBILITY);
        pBuilder.addStartMix(Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);
        pBuilder.addMix(Potions.FIRE_RESISTANCE, Items.REDSTONE, Potions.LONG_FIRE_RESISTANCE);
        pBuilder.addStartMix(Items.RABBIT_FOOT, Potions.LEAPING);
        pBuilder.addMix(Potions.LEAPING, Items.REDSTONE, Potions.LONG_LEAPING);
        pBuilder.addMix(Potions.LEAPING, Items.GLOWSTONE_DUST, Potions.STRONG_LEAPING);
        pBuilder.addMix(Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
        pBuilder.addMix(Potions.LONG_LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
        pBuilder.addMix(Potions.SLOWNESS, Items.REDSTONE, Potions.LONG_SLOWNESS);
        pBuilder.addMix(Potions.SLOWNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SLOWNESS);
        pBuilder.addMix(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER);
        pBuilder.addMix(Potions.TURTLE_MASTER, Items.REDSTONE, Potions.LONG_TURTLE_MASTER);
        pBuilder.addMix(Potions.TURTLE_MASTER, Items.GLOWSTONE_DUST, Potions.STRONG_TURTLE_MASTER);
        pBuilder.addMix(Potions.SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
        pBuilder.addMix(Potions.LONG_SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
        pBuilder.addStartMix(Items.SUGAR, Potions.SWIFTNESS);
        pBuilder.addMix(Potions.SWIFTNESS, Items.REDSTONE, Potions.LONG_SWIFTNESS);
        pBuilder.addMix(Potions.SWIFTNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SWIFTNESS);
        pBuilder.addMix(Potions.AWKWARD, Items.PUFFERFISH, Potions.WATER_BREATHING);
        pBuilder.addMix(Potions.WATER_BREATHING, Items.REDSTONE, Potions.LONG_WATER_BREATHING);
        pBuilder.addStartMix(Items.GLISTERING_MELON_SLICE, Potions.HEALING);
        pBuilder.addMix(Potions.HEALING, Items.GLOWSTONE_DUST, Potions.STRONG_HEALING);
        pBuilder.addMix(Potions.HEALING, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        pBuilder.addMix(Potions.STRONG_HEALING, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
        pBuilder.addMix(Potions.HARMING, Items.GLOWSTONE_DUST, Potions.STRONG_HARMING);
        pBuilder.addMix(Potions.POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        pBuilder.addMix(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        pBuilder.addMix(Potions.STRONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
        pBuilder.addStartMix(Items.SPIDER_EYE, Potions.POISON);
        pBuilder.addMix(Potions.POISON, Items.REDSTONE, Potions.LONG_POISON);
        pBuilder.addMix(Potions.POISON, Items.GLOWSTONE_DUST, Potions.STRONG_POISON);
        pBuilder.addStartMix(Items.GHAST_TEAR, Potions.REGENERATION);
        pBuilder.addMix(Potions.REGENERATION, Items.REDSTONE, Potions.LONG_REGENERATION);
        pBuilder.addMix(Potions.REGENERATION, Items.GLOWSTONE_DUST, Potions.STRONG_REGENERATION);
        pBuilder.addStartMix(Items.BLAZE_POWDER, Potions.STRENGTH);
        pBuilder.addMix(Potions.STRENGTH, Items.REDSTONE, Potions.LONG_STRENGTH);
        pBuilder.addMix(Potions.STRENGTH, Items.GLOWSTONE_DUST, Potions.STRONG_STRENGTH);
        pBuilder.addMix(Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
        pBuilder.addMix(Potions.WEAKNESS, Items.REDSTONE, Potions.LONG_WEAKNESS);
        pBuilder.addMix(Potions.AWKWARD, Items.PHANTOM_MEMBRANE, Potions.SLOW_FALLING);
        pBuilder.addMix(Potions.SLOW_FALLING, Items.REDSTONE, Potions.LONG_SLOW_FALLING);
    }

    public static class Builder {
        private final List<Ingredient> containers = new ArrayList<>();
        private final List<PotionBrewing.Mix<Potion>> potionMixes = new ArrayList<>();
        private final List<PotionBrewing.Mix<Item>> containerMixes = new ArrayList<>();
        private final List<net.neoforged.neoforge.common.brewing.IBrewingRecipe> recipes = new ArrayList<>();
        private final FeatureFlagSet enabledFeatures;

        public Builder(FeatureFlagSet pEnabledFeatures) {
            this.enabledFeatures = pEnabledFeatures;
        }

        private static void expectPotion(Item pItem) {
            if (!(pItem instanceof PotionItem)) {
                throw new IllegalArgumentException("Expected a potion, got: " + BuiltInRegistries.ITEM.getKey(pItem));
            }
        }

        public void addContainerRecipe(Item pInput, Item pReagent, Item pResult) {
            if (pInput.isEnabled(this.enabledFeatures) && pReagent.isEnabled(this.enabledFeatures) && pResult.isEnabled(this.enabledFeatures)) {
                expectPotion(pInput);
                expectPotion(pResult);
                this.containerMixes
                    .add(new PotionBrewing.Mix<>(pInput.builtInRegistryHolder(), Ingredient.of(pReagent), pResult.builtInRegistryHolder()));
            }
        }

        public void addContainer(Item pContainer) {
            if (pContainer.isEnabled(this.enabledFeatures)) {
                expectPotion(pContainer);
                this.containers.add(Ingredient.of(pContainer));
            }
        }

        public void addMix(Holder<Potion> pInput, Item pReagent, Holder<Potion> pResult) {
            if (pInput.value().isEnabled(this.enabledFeatures)
                && pReagent.isEnabled(this.enabledFeatures)
                && pResult.value().isEnabled(this.enabledFeatures)) {
                this.potionMixes.add(new PotionBrewing.Mix<>(pInput, Ingredient.of(pReagent), pResult));
            }
        }

        public void addStartMix(Item pReagent, Holder<Potion> pResult) {
            if (pResult.value().isEnabled(this.enabledFeatures)) {
                this.addMix(Potions.WATER, pReagent, Potions.MUNDANE);
                this.addMix(Potions.AWKWARD, pReagent, pResult);
            }
        }

        /**
         * Adds a new simple brewing recipe.
         *
         * @param input      the ingredient that goes in the same slot as water bottles would
         * @param ingredient the ingredient that goes in the same slot as nether wart would
         * @param output     the item stack that will replace the input once brewing is done
         */
        public void addRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
            addRecipe(new net.neoforged.neoforge.common.brewing.BrewingRecipe(input, ingredient, output));
        }

        /**
         * Adds a new brewing recipe with custom logic.
         */
        public void addRecipe(net.neoforged.neoforge.common.brewing.IBrewingRecipe recipe) {
            this.recipes.add(recipe);
        }

        public PotionBrewing build() {
            return new PotionBrewing(List.copyOf(this.containers), List.copyOf(this.potionMixes), List.copyOf(this.containerMixes), List.copyOf(this.recipes));
        }
    }

    public static record Mix<T>(Holder<T> from, Ingredient ingredient, Holder<T> to) {
    }
}
