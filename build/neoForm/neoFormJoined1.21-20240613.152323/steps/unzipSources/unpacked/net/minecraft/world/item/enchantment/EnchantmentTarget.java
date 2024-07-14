package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EnchantmentTarget implements StringRepresentable {
    ATTACKER("attacker"),
    DAMAGING_ENTITY("damaging_entity"),
    VICTIM("victim");

    public static final Codec<EnchantmentTarget> CODEC = StringRepresentable.fromEnum(EnchantmentTarget::values);
    private final String id;

    private EnchantmentTarget(String p_345108_) {
        this.id = p_345108_;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}