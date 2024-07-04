package net.minecraft.world.item.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedEntry;

/**
 * Defines an immutable instance of an enchantment and its level.
 */
public class EnchantmentInstance extends WeightedEntry.IntrusiveBase {
    /**
     * The enchantment being represented.
     */
    public final Holder<Enchantment> enchantment;
    /**
     * The level of the enchantment.
     */
    public final int level;

    public EnchantmentInstance(Holder<Enchantment> p_345467_, int p_44951_) {
        super(p_345467_.value().getWeight());
        this.enchantment = p_345467_;
        this.level = p_44951_;
    }
}
