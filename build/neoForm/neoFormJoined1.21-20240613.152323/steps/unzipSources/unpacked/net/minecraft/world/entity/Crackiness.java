package net.minecraft.world.entity;

import net.minecraft.world.item.ItemStack;

public class Crackiness {
    public static final Crackiness GOLEM = new Crackiness(0.75F, 0.5F, 0.25F);
    public static final Crackiness WOLF_ARMOR = new Crackiness(0.95F, 0.69F, 0.32F);
    private final float fractionLow;
    private final float fractionMedium;
    private final float fractionHigh;

    private Crackiness(float pFractionLow, float pFractionMedium, float pFractionHigh) {
        this.fractionLow = pFractionLow;
        this.fractionMedium = pFractionMedium;
        this.fractionHigh = pFractionHigh;
    }

    public Crackiness.Level byFraction(float pFraction) {
        if (pFraction < this.fractionHigh) {
            return Crackiness.Level.HIGH;
        } else if (pFraction < this.fractionMedium) {
            return Crackiness.Level.MEDIUM;
        } else {
            return pFraction < this.fractionLow ? Crackiness.Level.LOW : Crackiness.Level.NONE;
        }
    }

    public Crackiness.Level byDamage(ItemStack pStack) {
        return !pStack.isDamageableItem() ? Crackiness.Level.NONE : this.byDamage(pStack.getDamageValue(), pStack.getMaxDamage());
    }

    public Crackiness.Level byDamage(int pDamage, int pDurability) {
        return this.byFraction((float)(pDurability - pDamage) / (float)pDurability);
    }

    public static enum Level {
        NONE,
        LOW,
        MEDIUM,
        HIGH;
    }
}
