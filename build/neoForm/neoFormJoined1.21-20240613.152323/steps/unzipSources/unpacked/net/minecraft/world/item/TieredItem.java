package net.minecraft.world.item;

public class TieredItem extends Item {
    private final Tier tier;

    public TieredItem(Tier pTier, Item.Properties pProperties) {
        super(pProperties.durability(pTier.getUses()));
        this.tier = pTier;
    }

    public Tier getTier() {
        return this.tier;
    }

    @Override
    public int getEnchantmentValue() {
        return this.tier.getEnchantmentValue();
    }

    /**
     * Return whether this item is repairable in an anvil.
     */
    @Override
    public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
        return this.tier.getRepairIngredient().test(pRepair) || super.isValidRepairItem(pToRepair, pRepair);
    }
}
