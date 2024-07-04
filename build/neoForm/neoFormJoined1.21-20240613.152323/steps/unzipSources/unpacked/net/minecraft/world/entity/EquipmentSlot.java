package net.minecraft.world.entity;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum EquipmentSlot implements StringRepresentable {
    MAINHAND(EquipmentSlot.Type.HAND, 0, 0, "mainhand"),
    OFFHAND(EquipmentSlot.Type.HAND, 1, 5, "offhand"),
    FEET(EquipmentSlot.Type.HUMANOID_ARMOR, 0, 1, 1, "feet"),
    LEGS(EquipmentSlot.Type.HUMANOID_ARMOR, 1, 1, 2, "legs"),
    CHEST(EquipmentSlot.Type.HUMANOID_ARMOR, 2, 1, 3, "chest"),
    HEAD(EquipmentSlot.Type.HUMANOID_ARMOR, 3, 1, 4, "head"),
    BODY(EquipmentSlot.Type.ANIMAL_ARMOR, 0, 1, 6, "body");

    public static final int NO_COUNT_LIMIT = 0;
    public static final StringRepresentable.EnumCodec<EquipmentSlot> CODEC = StringRepresentable.fromEnum(EquipmentSlot::values);
    private final EquipmentSlot.Type type;
    private final int index;
    private final int countLimit;
    private final int filterFlag;
    private final String name;

    private EquipmentSlot(EquipmentSlot.Type p_348560_, int p_348539_, int p_348468_, int p_348612_, String p_348519_) {
        this.type = p_348560_;
        this.index = p_348539_;
        this.countLimit = p_348468_;
        this.filterFlag = p_348612_;
        this.name = p_348519_;
    }

    private EquipmentSlot(EquipmentSlot.Type pType, int pIndex, int pFilterFlag, String pName) {
        this(pType, pIndex, 0, pFilterFlag, pName);
    }

    public EquipmentSlot.Type getType() {
        return this.type;
    }

    public int getIndex() {
        return this.index;
    }

    public int getIndex(int pBaseIndex) {
        return pBaseIndex + this.index;
    }

    public ItemStack limit(ItemStack p_348466_) {
        return this.countLimit > 0 ? p_348466_.split(this.countLimit) : p_348466_;
    }

    public int getFilterFlag() {
        return this.filterFlag;
    }

    public String getName() {
        return this.name;
    }

    public boolean isArmor() {
        return this.type == EquipmentSlot.Type.HUMANOID_ARMOR || this.type == EquipmentSlot.Type.ANIMAL_ARMOR;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static EquipmentSlot byName(String pTargetName) {
        EquipmentSlot equipmentslot = CODEC.byName(pTargetName);
        if (equipmentslot != null) {
            return equipmentslot;
        } else {
            throw new IllegalArgumentException("Invalid slot '" + pTargetName + "'");
        }
    }

    public static enum Type {
        HAND,
        HUMANOID_ARMOR,
        ANIMAL_ARMOR;
    }
}
