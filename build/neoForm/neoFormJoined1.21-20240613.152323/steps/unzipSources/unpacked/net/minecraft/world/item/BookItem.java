package net.minecraft.world.item;

public class BookItem extends Item {
    public BookItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Checks isDamagable and if it cannot be stacked
     */
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return pStack.getCount() == 1;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
