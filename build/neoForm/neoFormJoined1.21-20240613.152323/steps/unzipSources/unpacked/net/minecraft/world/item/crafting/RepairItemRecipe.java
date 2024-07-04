package net.minecraft.world.item.crafting;

import com.mojang.datafixers.util.Pair;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class RepairItemRecipe extends CustomRecipe {
    public RepairItemRecipe(CraftingBookCategory pCategory) {
        super(pCategory);
    }

    @Nullable
    private Pair<ItemStack, ItemStack> getItemsToCombine(CraftingInput p_345543_) {
        ItemStack itemstack = null;
        ItemStack itemstack1 = null;

        for (int i = 0; i < p_345543_.size(); i++) {
            ItemStack itemstack2 = p_345543_.getItem(i);
            if (!itemstack2.isEmpty()) {
                if (itemstack == null) {
                    itemstack = itemstack2;
                } else {
                    if (itemstack1 != null) {
                        return null;
                    }

                    itemstack1 = itemstack2;
                }
            }
        }

        return itemstack != null && itemstack1 != null && canCombine(itemstack, itemstack1) ? Pair.of(itemstack, itemstack1) : null;
    }

    private static boolean canCombine(ItemStack pStack1, ItemStack pStack2) {
        return pStack2.is(pStack1.getItem())
            && pStack1.getCount() == 1
            && pStack2.getCount() == 1
            && pStack1.has(DataComponents.MAX_DAMAGE)
            && pStack2.has(DataComponents.MAX_DAMAGE)
            && pStack1.has(DataComponents.DAMAGE)
            && pStack2.has(DataComponents.DAMAGE)
            && pStack1.isRepairable()
            && pStack2.isRepairable();
    }

    public boolean matches(CraftingInput p_345243_, Level p_44139_) {
        return this.getItemsToCombine(p_345243_) != null;
    }

    public ItemStack assemble(CraftingInput p_346224_, HolderLookup.Provider p_335610_) {
        Pair<ItemStack, ItemStack> pair = this.getItemsToCombine(p_346224_);
        if (pair == null) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemstack = pair.getFirst();
            ItemStack itemstack1 = pair.getSecond();
            int i = Math.max(itemstack.getMaxDamage(), itemstack1.getMaxDamage());
            int j = itemstack.getMaxDamage() - itemstack.getDamageValue();
            int k = itemstack1.getMaxDamage() - itemstack1.getDamageValue();
            int l = j + k + i * 5 / 100;
            ItemStack itemstack2 = new ItemStack(itemstack.getItem());
            itemstack2.set(DataComponents.MAX_DAMAGE, i);
            itemstack2.setDamageValue(Math.max(i - l, 0));
            ItemEnchantments itemenchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemstack);
            ItemEnchantments itemenchantments1 = EnchantmentHelper.getEnchantmentsForCrafting(itemstack1);
            EnchantmentHelper.updateEnchantments(
                itemstack2,
                p_344422_ -> p_335610_.lookupOrThrow(Registries.ENCHANTMENT)
                        .listElements()
                        .filter(p_344414_ -> p_344414_.is(EnchantmentTags.CURSE))
                        .forEach(p_344418_ -> {
                            int i1 = Math.max(itemenchantments.getLevel(p_344418_), itemenchantments1.getLevel(p_344418_));
                            if (i1 > 0) {
                                p_344422_.upgrade(p_344418_, i1);
                            }
                        })
            );
            return itemstack2;
        }
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth * pHeight >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.REPAIR_ITEM;
    }
}
