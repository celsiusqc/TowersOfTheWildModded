package net.minecraft.world.item;

import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class EnchantedBookItem extends Item {
    public EnchantedBookItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Checks isDamagable and if it cannot be stacked
     */
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }

    /**
     * Returns the ItemStack of an enchanted version of this item.
     */
    public static ItemStack createForEnchantment(EnchantmentInstance pInstance) {
        ItemStack itemstack = new ItemStack(Items.ENCHANTED_BOOK);
        itemstack.enchant(pInstance.enchantment, pInstance.level);
        return itemstack;
    }
}
